package com.bdj.eduwebcrawler;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class Querent
{

    public static void run()
    {
        try
        {
            Searcher searcher = new Searcher("Lucene/index/");
            while (true)
            {
                System.out.println("\nEnter a query of any length. To stop enter QUIT.");
                Scanner reader = new Scanner(System.in);  // Reading from System.in
                String n = reader.nextLine();
                if (n.equals("QUIT"))
                {
                    break;
                }
                try
                {
                    ScoreDoc[] hits = searcher.queryText(n);
                    int cnt = 1;
                    for (ScoreDoc hit : hits)
                    {
                        Document d = searcher.getDocByDocId(hit.doc);
                        System.out.println("\nRank: " + cnt);
                        if (d.getField("title") != null)
                        {
                            System.out.println("Title:\n" + d.get("title"));
                        }
                        System.out.println("URL: " + d.get("url"));
                        if (d.getField("description") != null)
                        {
                            System.out.println("Description:\n" + d.get("description"));
                        }
                        if (d.getField("keywords") != null)
                        {
                            System.out.println("Keywords:\n" + d.get("keywords"));
                        }
                        else
                        {
                            Map<String, Integer> kws = searcher.getKeywordsByDocId(hit.doc);
                            System.out.println("Keywords: ");
                            Iterator it = kws.entrySet().iterator();
                            while (it.hasNext())
                            {
                                Map.Entry pair = (Map.Entry)it.next();
                                System.out.println("          " + pair.getKey() + " freq: " + pair.getValue());
                                it.remove(); // avoids a ConcurrentModificationException
                            }
                        }
                        cnt++;
                    }
                }
                catch (ParseException e)
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
