package com.bdj.eduwebcrawler;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Consumer;

public class URLUtils
{
    public static String getDomainName(String url)
    {
        int epos = url.indexOf(".edu");
        if (epos != -1)
        {
            int pos = url.lastIndexOf(".", epos - 1);
            if (pos == -1)
            {
                pos = url.lastIndexOf("/", epos - 1);
            }

            return pos != -1 ? url.substring(pos + 1, epos) : url.substring(0, epos);
        }
        return url;
    }

    public static String getSavePath(String url)
    {
        String domain = getDomainName(url);
        url = url.replaceAll("^http[s]*://","").replaceAll("^www[1-9]*\\.", "").replaceAll("[./]*$", "");
        int pos = url.indexOf(domain + ".edu");
        String prefix = pos > 0 ? url.substring(0, pos).replaceAll("[./]*$", "") : "";
        String suffix = url.substring(pos + domain.length() + 4).replaceAll("[./]*$", "");
        if (suffix.trim().isEmpty())
        {
            if (prefix.trim().isEmpty())
            {
                suffix = domain;
            }
            else
            {
                pos = prefix.lastIndexOf('.');
                suffix = pos > 0 ? prefix.substring(pos) : prefix;
            }
        }
        String ret = EduWebcrawler.INSTANCE.getConfig().getDataPath() + "/" + domain + "/" + prefix + "/" + suffix;
        ret = ret.replaceAll("\\.", "/").replaceAll("//", "/");
        return ret.endsWith("/txt") ? ret.replaceAll("/txt$", ".txt") : (!ret.endsWith(".html") ? ret + ".html" : ret);
    }

    public static void foreachURL(Document doc, Consumer<String> consumer)
    {
        Elements elements = doc.getElementsByAttribute("href");

        elements.forEach(e ->
        {
            if (e.tag().getName().equals("a"))
            {
                String link = e.attr("href");

                consumer.accept(link);
            }
        });
    }

    public static String normalizeURL(String curr, String link)
    {
        try
        {
            URL u = new URL(curr.trim());
            if (link.startsWith("/"))
                link = u.toURI().resolve(link.trim()).toString();
            int pos = link.indexOf('#');
            if (pos != -1)
                link = link.substring(0, pos);
            pos = link.indexOf('?');
            if (pos != -1)
                link = link.substring(0, pos);

            return link;
        }
        catch (MalformedURLException | URISyntaxException ex)
        {
            //ex.printStackTrace();
        }
        return link;
    }
}
