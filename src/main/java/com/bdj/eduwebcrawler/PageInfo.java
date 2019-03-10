package com.bdj.eduwebcrawler;

import org.jsoup.nodes.Document;

public class PageInfo
{
    private String url;
    private String savePath;
    private int depth;
    private Document doc;

    public PageInfo(String url)
    {
        this(url, 0);
    }

    public PageInfo(String url, int depth)
    {
        this(url, depth, null);
    }

    public PageInfo(String url, int depth, Document doc)
    {
        this.url = url;
        this.savePath = URLUtils.getSavePath(url);
        this.depth = depth;
        this.doc = doc;
    }

    public String getUrl()
    {
        return this.url;
    }

    public String getSavePath()
    {
        return this.savePath;
    }

    public int getDepth()
    {
        return this.depth;
    }

    public Document getDoc()
    {
        return this.doc;
    }

    public void setDoc(Document doc)
    {
        this.doc = doc;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof PageInfo && this.savePath.equals(((PageInfo)o).savePath);
    }
}
