package com.bdj.eduwebcrawler;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HTMLUtils {

    public static Document getDoc(File file, String html) throws IOException {
        return Jsoup.parse(file, html);
    }

    public static String getText(Document d) {
        return d.body().text();
    }

    public static String getTitle(Document d) {
        return d.title();
    }

    public static String getDescription(Document d) {
        Elements metaTags = d.getElementsByTag("meta");
        metaTags.attr("name", "description");
        return metaTags.text();
    }

    public static String getKeywords(Document d) {
        Elements metaTags = d.getElementsByTag("meta");
        metaTags.attr("name", "keywords");
        return metaTags.text();
    }
}
