package com.bdj.eduwebcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.io.File;
import java.io.IOException;


public class HTMLUtils {

    public static Document getDoc(File file, String html) throws IOException {
        return Jsoup.parse(file, html);
    }

    public static String getText(Document d) {
        if(d.body() == null){
            return "";
        }
        return d.body().text();
    }

    public static String getTitle(Document d) {
        return d.title();
    }

    public static String getDescription(Document d) {
        Elements metaTags = d.getElementsByTag("meta");
        return getter(metaTags, "description");
    }

    public static String getKeywords(Document d) {
        Elements metaTags = d.getElementsByTag("meta");
        return getter(metaTags, "keywords");
    }

    private static String getter(Elements metaTags, String attr) {
        for (Element metaTag : metaTags) {
            String content = metaTag.attr("content");
            String name = metaTag.attr("name");
            if(attr.equals(name)) {
                return content;
            }
        }
        return "";
    }
}
