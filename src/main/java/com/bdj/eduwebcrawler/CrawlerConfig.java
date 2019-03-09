package com.bdj.eduwebcrawler;

import com.electronwill.nightconfig.core.UnmodifiableConfig;

import java.util.Arrays;
import java.util.List;

public class CrawlerConfig
{
    private final UnmodifiableConfig config;
    private final int downloaders;
    private final int processors;
    private final int maxPages;
    private final int maxRedirects;
    private final long maxDataSize;
    private final String dataPath;
    private final List<String> seedList;

    public CrawlerConfig(UnmodifiableConfig config)
    {
        this.config = config;
        this.downloaders = config.getIntOrElse("downloaders", 2);
        this.processors = config.getIntOrElse("processors", 4);
        this.maxPages = config.getIntOrElse("maxpages", -1);
        this.maxRedirects = config.getIntOrElse("maxredirects", -1);
        this.maxDataSize = config.getLongOrElse("maxdatasize", -1);
        this.dataPath = config.<String>getOptional("datapath").orElseThrow(() -> new RuntimeException("Config missing 'datapath'!"));
        String seeds = config.<String>getOptional("seedlist").orElseThrow(() -> new RuntimeException("Config missing 'seedlist!'"));
        this.seedList = Arrays.asList(seeds.split("\n"));

        if (this.maxPages < 0 && this.maxDataSize < 0)
        {
            throw new RuntimeException("Config must specify a positive value for 'maxpages' OR 'maxdatasize'");
        }
    }

    public int getDownloaders()
    {
        return this.downloaders;
    }

    public int getProcessors()
    {
        return this.processors;
    }

    public int getMaxPages()
    {
        return this.maxPages;
    }

    public int getMaxRedirects()
    {
        return this.maxRedirects;
    }

    public long getMaxDataSize()
    {
        return this.maxDataSize;
    }

    public String getDataPath()
    {
        return this.dataPath;
    }

    public List<String> getSeedList()
    {
        return this.seedList;
    }
}
