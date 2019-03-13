package com.bdj.eduwebcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Loader
{
    public static void run()
    {
        try (
                Indexer index = new Indexer("Lucene/index/"); Stream<Path> files = Files.find(Paths.get(EduWebcrawler.INSTANCE.getConfig().getDataPath()), Integer.MAX_VALUE, (p, a) -> p.getNameCount() > 0 && p.toString().endsWith(".html"))
        )
        {
            System.out.println("Loading HTML information into Lucene Database.\n");
            index.deleteAll();
            int i = 0;
            int j = 0;
            for (Path path : files.collect(Collectors.toList()))
            {
                try
                {
                    Document doc = HTMLUtils.getDoc(path.toFile(), StandardCharsets.UTF_8.name());
                    String text = HTMLUtils.getText(doc);
                    String title = HTMLUtils.getTitle(doc);
                    String description = HTMLUtils.getDescription(doc);
                    String keywords = HTMLUtils.getKeywords(doc);
                    String url = EduWebcrawler.getInfoValue(Paths.get(path.toString() + ".info"), "url");
                    Set<String> childURLs = new HashSet<>();
                    URLUtils.foreachURL(Jsoup.parse(path.toFile(), StandardCharsets.UTF_8.name()), link ->
                    {
                        link = URLUtils.normalizeURL(url, link);
                        if (EduWebcrawler.INSTANCE.URL_PREDICATE.test(link))
                        {
                            childURLs.add(link);
                        }
                    });
                    index.addDoc(url, title, description, keywords, childURLs, text);
                    int k = ++i / 10;
                    if (k > j)
                    {
                        j = k;
                        System.out.println("Documents loaded: " + i);
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
