package com.bdj.eduwebcrawler;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

public enum EduWebcrawler
{
    INSTANCE;

    private static final String CONFIG_PATH = "./crawler_config.toml";
    private static final Predicate<String> URL_PREDICATE = s -> (s.startsWith("/") || s.contains(".edu")) && !s.contains("@");

    private final CrawlerConfig config;
    private final QueuedWorkerPool<PageInfo> downloaders;
    private final QueuedWorkerPool<PageInfo> processors;
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
        while (!downloaders.isEmpty() || !shouldStop())
        {
            try
            {
                downloaders.checkThreadStates();
                processors.checkThreadStates();
            }
            catch (Throwable t)
            {
                downloaders.stop();
                processors.stop();
                throw t;
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
        try (Stream<Path> files = Files.find(Paths.get(config.getDataPath()), Integer.MAX_VALUE, (p, a) -> p.getNameCount() > 0))
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
                    if ((maxDepth < 0 || maxDepth > info.getDepth()) && !Files.exists(Paths.get(URLUtils.getSavePath(info.getUrl()))))
                    {
                        info.setDoc(Jsoup.connect(info.getUrl()).get());
                        processors.add(info);
                        Thread.sleep(1000L);
                    }
                }
                catch (IOException | InterruptedException e)
                {
                    e.printStackTrace();
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
                        count.incrementAndGet();
                        dataSize.addAndGet(toSave.getBytes().length);

                        URLUtils.foreachURL(info.getDoc(), URL_PREDICATE, link ->
                        {
                            link = URLUtils.normalizeURL(info.getUrl(), link);
                            downloaders.add(new PageInfo(link, info.getDepth() + 1));
                        });

                    }
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
}
