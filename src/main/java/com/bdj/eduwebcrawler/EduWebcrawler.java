package com.bdj.eduwebcrawler;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum EduWebcrawler
{
    INSTANCE;

    private static final String CONFIG_PATH = "./crawler_config.toml";
    public static final Predicate<String> URL_PREDICATE = s -> (s.startsWith("/") || s.contains(".edu")) && !(s.contains("@") || s.endsWith("docx") || s.endsWith("pdf"));

    private final CrawlerConfig config;
    private final QueuedWorkerPool<PageInfo> downloaders;
    private final QueuedWorkerPool<PageInfo> processors;
    private AtomicInteger count = new AtomicInteger(0);
    private AtomicLong dataSize = new AtomicLong(0L);

    private ConcurrentMap<String, String> updated = new ConcurrentHashMap<>();

    EduWebcrawler()
    {
        final FileConfig fileConfig = FileConfig.builder(Paths.get(CONFIG_PATH)).build();
        fileConfig.load();
        fileConfig.close();
        config = new CrawlerConfig(fileConfig);

        downloaders = new QueuedWorkerPool<>("DOWNLOAD", config.getDownloaders(), this::downloader);
        processors = new QueuedWorkerPool<>("PROCESS", config.getProcessors(), this::processor);

        config.getSeedList().stream().map(PageInfo::new).forEach(downloaders::add);
    }

    public CrawlerConfig getConfig()
    {
        return this.config;
    }

    public void run()
    {
        scanDataDir();
        downloaders.start();
        processors.start();

        int i = 0;
        int j = 0;
        while (!shouldStop())
        {
            try
            {
                downloaders.checkThreadStates();
                processors.checkThreadStates();

                if (downloaders.isEmpty() && processors.isEmpty() && ++i > 10000)
                {
                    System.out.println("Hit dead end");
                    break;
                }
                else
                {
                    i = 0;
                    int c = count.get();
                    int k = c % 20;
                    if (k > j)
                    {
                        j = k;
                        System.out.println("Doc count: " + c);
                    }
                }
                Thread.sleep(100L);
            }
            catch (Throwable t)
            {
                downloaders.stop();
                processors.stop();
                throw new RuntimeException(t);
            }
        }
        downloaders.finish();
        while (!processors.isEmpty())
        {
            try
            {
                processors.checkThreadStates();
            }
            catch (Throwable t)
            {
                processors.stop();
                throw t;
            }
        }
        processors.finish();
    }

    private void scanDataDir()
    {
        try (Stream<Path> files = Files.find(Paths.get(config.getDataPath()), Integer.MAX_VALUE, (p, a) -> p.getNameCount() > 0 && p.toString().endsWith(".html")))
        {
            files.forEach(path ->
            {
                try
                {
                    count.incrementAndGet();
                    dataSize.addAndGet(Files.size(path));
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            });
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void downloader()
    {
        while (!downloaders.isFinished())
        {
            downloaders.getNextItem().ifPresent(info ->
            {
                try
                {
                    int maxDepth = config.getMaxRedirects();
                    if (maxDepth < 0 || maxDepth > info.getDepth())
                    {
                        Path path = Paths.get(URLUtils.getSavePath(info.getUrl()));
                        String hash = null;

                        if (updated.containsKey(path.toString()))
                        {
                            return;
                        }

                        if (Files.exists(path))
                        {
                            hash = Files.lines(Paths.get(path.toString() + ".md5")).collect(Collectors.joining("\n"));
                            if (!config.shouldUpdatePages())
                            {
                                info.setDoc(Jsoup.parse(path.toFile(), StandardCharsets.UTF_8.name()));
                            }
                        }

                        if (info.getDoc() == null)
                        {
                            info.setDoc(Jsoup.parse(Jsoup.connect(info.getUrl()).get().html()));
                            Thread.sleep(500L);
                        }

                        if (hash == null || !hash.equals(getMD5Hash(info.getDoc().text().getBytes())))
                        {
                            processors.add(info);
                            if (hash != null)
                            {
                                count.decrementAndGet();

                                updated.put(path.toString(), hash);
                                dataSize.addAndGet(-Files.size(path));
                                System.out.println("Hash difference for: " + info.getUrl());
                            }
                        }
                        else
                        {
                            URLUtils.foreachURL(info.getDoc(), link ->
                            {
                                try
                                {
                                    link = URLUtils.normalizeURL(info.getUrl(), link);
                                    if (URL_PREDICATE.test(link))
                                    {
                                        downloaders.add(new PageInfo(link, info.getDepth() + 1));
                                    }
                                }
                                catch (IllegalArgumentException e) {}
                            });
                        }
                    }
                }
                catch (IOException | InterruptedException e)
                {
                    //e.printStackTrace();
                }
            });
        }
    }

    private void processor()
    {
        while (!processors.isFinished())
        {
            processors.getNextItem().ifPresent(info ->
            {
                try
                {
                    Path path = Paths.get(URLUtils.getSavePath(info.getUrl()));
                    if (!Files.exists(path))
                    {
                        Files.createDirectories(path.getParent());
                        String toSave = info.getUrl().endsWith("/robots.txt") ? info.getDoc().body().wholeText() : info.getDoc().html();

                        Files.write(path, toSave.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        saveHash(path, Jsoup.parse(info.getDoc().html()).text().getBytes());

                        count.incrementAndGet();
                        dataSize.addAndGet(Files.size(path));
                    }

                    URLUtils.foreachURL(info.getDoc(), link ->
                    {
                        try
                        {
                            link = URLUtils.normalizeURL(info.getUrl(), link);
                            if (URL_PREDICATE.test(link))
                            {
                                downloaders.add(new PageInfo(link, info.getDepth() + 1));
                            }
                        }
                        catch (IllegalArgumentException e) {}
                    });
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
        }
    }

    private boolean shouldStop()
    {
        if (config.getMaxPages() >= 0 && count.get() > config.getMaxPages())
        {
            return true;
        }
        return config.getMaxDataSize() >= 0L && dataSize.get() > config.getMaxDataSize();
    }

    private static String getMD5Hash(byte[] bytes)
    {
        try
        {
            String pad = String.format("%0" + 30 + "d", 0);
            MessageDigest digest = MessageDigest.getInstance("md5");
            String hash = new BigInteger(1, digest.digest(bytes)).toString(16);
            return (pad + hash).substring(hash.length());
        }
        catch (NoSuchAlgorithmException e)
        {
            // Never gonna happen
        }

        return "";
    }

    private static void saveHash(Path path, byte[] bytes)
    {
        try
        {
            Files.write(Paths.get(path.toString() + ".md5"), Collections.singletonList(getMD5Hash(bytes)), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
