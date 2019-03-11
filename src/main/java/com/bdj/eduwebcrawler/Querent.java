package com.bdj.eduwebcrawler;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class Querent {

    public static void run() {
        try{
            Searcher searcher = new Searcher("Lucene/index/");
            while(true) {
                System.out.println("Enter a query of any length. To stop enter QUIT.");
                Scanner reader = new Scanner(System.in);  // Reading from System.in
                String n = reader.next();
                if (n.equals("QUIT")) {
                    break;
                }
                try {
                    ScoreDoc[] hits = searcher.queryText(n);
                    int cnt = 0;
                    for (ScoreDoc hit: hits) {
                        Document d = searcher.getDocByDocId(hit.doc);
                        Map<String, Integer> kws = searcher.getKeywordsByDocId(hit.doc);
                        System.out.println("Rank: " + cnt);
                        System.out.println("url: " + d.get("url"));
                        System.out.println("Keywords: ");
                        Iterator it = kws.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry)it.next();
                            System.out.println("         " + pair.getKey() + " freq: " + pair.getValue());
                            it.remove(); // avoids a ConcurrentModificationException
                        }
                    }
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
