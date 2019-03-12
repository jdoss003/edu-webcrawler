package com.bdj.eduwebcrawler;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

public class URLUtils
{
    public static String getDomainName(String url)
    {
        try
        {
            URL u = new URL(url.trim());
            String host = u.getHost().toLowerCase();
            int epos = host.lastIndexOf(".");
            int pos = host.lastIndexOf(".", epos - 1);
            return pos != -1 ? host.substring(pos + 1, epos) : host.substring(0, epos);
        }
        catch (MalformedURLException e) {}
        return url;
    }

    public static String getSavePath(String url)
    {
        try
        {
            URL u = new URL(url.trim());
            String host = u.getHost().replaceAll("^www[1-9]*\\.", "").toLowerCase();
            int epos = host.lastIndexOf(".");
            int pos = host.lastIndexOf(".", epos - 1);
            String domain = pos != -1 ? host.substring(pos + 1, epos) : host.substring(0, epos);
            String prefix = pos > 0 ? host.substring(0, pos).replaceAll("[./]*$", "") : "";
            String suffix = url.substring(url.indexOf(u.getHost()) + u.getHost().length()).replaceAll("[./]*$", "");
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
            ret = ret.replaceAll("\\.", "/").replaceAll("//[/]*", "/").replaceAll("/html$", ".html");
            return (ret.endsWith("/txt") ? ret.replaceAll("/txt$", ".txt") : (!ret.endsWith(".html") ? ret + ".html" : ret)).toLowerCase();
        }
        catch (MalformedURLException e) {}
        catch (Throwable t)
        {
            System.out.println("Erroring URL: " + url);
            throw t;
        }
        return null;
    }

    public static void foreachURL(Document doc, Consumer<String> consumer)
    {
        Elements elements = doc.getElementsByAttribute("href");

        elements.forEach(e ->
        {
            String link = e.attr("href");
            consumer.accept(link);
        });
    }

    public static String normalizeURL(String curr, String link)
    {
        try
        {
            URL u = new URL(curr.trim());
            link = new URL(u, link.trim()).toString();
            int pos = link.indexOf('#');
            if (pos != -1)
                link = link.substring(0, pos);
            pos = link.indexOf('?');
            if (pos != -1)
                link = link.substring(0, pos);

            return link;
        }
        catch (MalformedURLException ex)
        {
            //ex.printStackTrace();
        }
        return link;
    }
}
