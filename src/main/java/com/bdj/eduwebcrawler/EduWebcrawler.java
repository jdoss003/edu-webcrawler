package com.bdj.eduwebcrawler;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

public enum EduWebcrawler
{
    INSTANCE;

    private static final boolean DEBUG = false;

    private static final String CONFIG_PATH = "./crawler_config.toml";
    public final Predicate<String> URL_PREDICATE;

    private final CrawlerConfig config;
    private final QueuedWorkerPool<PageInfo> downloaders;
    private final QueuedWorkerPool<PageInfo> processors;
    private final ConcurrentMap<String, Pair<Long, Long>> hitTimes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentHashMap<String, ConcurrentSkipListSet<String>>> processed = new ConcurrentHashMap<>();
    private AtomicInteger watchdog = new AtomicInteger(0);
    private AtomicInteger count = new AtomicInteger(0);
    private AtomicLong dataSize = new AtomicLong(0L);

    EduWebcrawler()
    {
        final FileConfig fileConfig = FileConfig.builder(Paths.get(CONFIG_PATH)).build();
        fileConfig.load();
        fileConfig.close();
        config = new CrawlerConfig(fileConfig);

        downloaders = new QueuedWorkerPool<>("DOWNLOAD", config.getDownloaders(), this::downloader);
        processors = new QueuedWorkerPool<>("PROCESS", config.getProcessors(), this::processor);

        URL_PREDICATE = s ->
        {
            try
            {
                boolean validTLD = false;
                URL u = new URL(s);
                for (String tld : config.getValidTLDs())
                {
                    if (u.getHost().toLowerCase().endsWith(tld))
                    {
                        validTLD = true;
                        break;
                    }
                }
                String extension = FilenameUtils.getExtension(u.getFile()).trim();
                boolean validExtension = extension.trim().isEmpty();
                if (validTLD && !validExtension)
                {
                    for (String ex: config.getValidExtensions())
                    {
                        if (extension.equals(ex))
                        {
                            validExtension = true;
                            break;
                        }
                    }
                }
                return validTLD && validExtension && !s.contains("@");
            }
            catch (MalformedURLException e) {}
            return false;
        };
    }

    public CrawlerConfig getConfig()
    {
        return this.config;
    }

    public void run()
    {
        config.getSeedList().stream().map(PageInfo::new).forEach(downloaders::add);

        try
        {
            Files.createDirectories(Paths.get(config.getDataPath()));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        scanDataDir();
        downloaders.start();
        processors.start();

        int j = 0;
        while (!shouldStop())
        {
            try
            {
                downloaders.checkThreadStates();
                processors.checkThreadStates();

                if (downloaders.isEmpty() && processors.isEmpty())
                {
                    if (watchdog.incrementAndGet() > 6000)
                    {
                        System.out.println("Hit dead end");
                        break;
                    }
                    Thread.sleep(100L);
                }

                int c = count.get();
                int k = c / 10;
                if (k > j)
                {
                    j = k;
                    System.out.println("Doc count: " + c);
                }
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
                        Path path = Paths.get(info.getSavePath());
                        String pathStr = path.toString();
                        String domain = URLUtils.getDomainName(info.getUrl());
                        String hash = null;

                        ConcurrentMap<String, ConcurrentSkipListSet<String>> domainList = processed.putIfAbsent(domain, new ConcurrentHashMap<>());
                        if (domainList == null)
                        {
                            domainList = processed.get(domain);
                        }

                        ConcurrentSkipListSet<String> pathList = domainList.putIfAbsent(path.getParent().toString(), new ConcurrentSkipListSet<>());
                        if (pathList == null)
                        {
                            pathList = domainList.get(path.getParent().toString());
                        }

                        if (pathList.contains(pathStr))
                        {
                            return;
                        }

                        if (Files.exists(path))
                        {
                            hash = getInfoValue(Paths.get(path.toString() + ".info"), "hash");
                            if (!config.shouldUpdatePages())
                            {
                                info.setDoc(Jsoup.parse(path.toFile(), StandardCharsets.UTF_8.name()));
                            }
                        }

                        if (info.getDoc() == null)
                        {
                            boolean flag;
                            synchronized(hitTimes)
                            {
                                flag = canHitServer(domain);
                                if (flag)
                                {
                                    hitTimes.put(domain, new Pair<>(-1L, -1L));
                                }
                            }

                            if (!flag)
                            {
                                downloaders.add(info);
                                return;
                            }

                           try
                           {
                               long start = System.currentTimeMillis();
                               info.setDoc(Jsoup.parse(Jsoup.connect(info.getUrl()).get().html()));
                               long end = System.currentTimeMillis();
                               long total = end - start;
                               hitTimes.put(domain, new Pair<>(end, total));
                           }
                           catch (IOException e)
                           {
                               if (DEBUG)
                               {
                                   System.out.println("Error getting page: " + info.getUrl());
                                   System.out.println(e);
                               }

                               long end = System.currentTimeMillis();
                               long total = end - 500L;
                               hitTimes.put(domain, new Pair<>(end, total));
                               return;
                           }
                        }

                        if (pathList.contains(pathStr))
                        {
                            return;
                        }
                        pathList.add(pathStr);
                        watchdog.set(0);

                        if (config.shouldUpdatePages() && hash != null && !hash.equals(getMD5Hash(info.getDoc().text().getBytes())))
                        {
                            info.setOverwrite(true);
                            count.decrementAndGet();

                            dataSize.addAndGet(-Files.size(path));
                            System.out.println("Hash difference for: " + info.getUrl());
                        }
                        processors.add(info);
                    }
                }
                catch (IOException | IllegalArgumentException  e) {}
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
                    watchdog.set(0);
                    Path path = Paths.get(info.getSavePath());
                    if (info.shouldOverwrite() || !Files.exists(path))
                    {
                        Files.deleteIfExists(path);
                        Files.deleteIfExists(Paths.get(path.toString() + ".info"));

                        Files.createDirectories(path.getParent());
                        String toSave = info.getUrl().endsWith("/robots.txt") ? info.getDoc().body().wholeText() : info.getDoc().html();

                        Files.write(path, toSave.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        saveInfo(path, info.getUrl(), Jsoup.parse(info.getDoc().html()).text().getBytes());

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

    private boolean canHitServer(String domain)
    {
        Pair<Long, Long> p = hitTimes.get(domain);

        if (p == null)
        {
            return true;
        }

        if (p.fst < 0L)
        {
            return false;
        }

        return (System.currentTimeMillis() - p.fst) < Math.max(p.snd * 3, 250L);
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

    private static void saveInfo(Path path, String url, byte[] bytes)
    {
        try
        {
            List<String> list = new ArrayList<>();
            list.add("url=" + url);
            list.add("hash=" + getMD5Hash(bytes));
            Files.write(Paths.get(path.toString() + ".info"), list, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static String getInfoValue(Path path, String key)
    {
        try(Stream<String> lines = Files.lines(path))
        {
            Optional<String> keyVal = lines.filter(s -> s.startsWith(key)).findFirst();
            if (keyVal.isPresent())
            {
                return keyVal.get().split("=")[1];
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return "";
    }
}
