package com.bdj.eduwebcrawler;

import org.jsoup.nodes.Document;

public class PageInfo
{
    private String url;
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
        this.depth = depth;
        this.doc = doc;
    }

    public String getUrl()
    {
        return this.url;
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
}
