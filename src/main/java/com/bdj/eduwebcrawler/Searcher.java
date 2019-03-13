package com.bdj.eduwebcrawler;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;


public class Searcher {
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private IndexReader reader;

    public Searcher(String indexDir) throws IOException {
        this(Paths.get(indexDir));
    }

    public Searcher(Path indexDir) throws IOException {
        //this directory will contain the indexes. FS directory takes Path object
        Directory dir = FSDirectory.open(indexDir);

        //make reader
        reader = DirectoryReader.open(dir);

        //make analyzer
        analyzer = new StandardAnalyzer();

        //make searcher
        searcher = new IndexSearcher(reader);
    }

    public Document searchByURL(String key, String url) throws IOException{
        //create query
        Term t = new Term(key, url);
        TermQuery query = new TermQuery(t);
        TopDocs result = searcher.search(query, 1);

        //see if result matches given url. Return document with url else return nothing
        ScoreDoc[] hit = result.scoreDocs;
        if(hit.length == 0){
            return null;
        }
        int docId = hit[0].doc;
        Document d = searcher.doc(docId);
        if (d.get("url").equals(url))
            return d;
        return null;
    }

    public ScoreDoc[] queryText(String querystr) throws IOException, ParseException {
        Query q = new QueryParser("text", analyzer).parse(querystr);
        TopDocs result = searcher.search(q, 10);

        //get hits and return
        return result.scoreDocs;
    }

    public String getURL(Document doc) {
        return doc.get("url");
    }

    public String[] getChildURLs(Document doc) { return doc.getValues("childURLs");}

    public Document getDocByDocId(int id) throws IOException {return searcher.doc(id);}

    public Map<String, Integer> getKeywords(String key, String url) throws IOException{
        //create query
        Term t = new Term(key, url);
        TermQuery query = new TermQuery(t);
        TopDocs result = searcher.search(query, 1);

        //Get term vector and return top 10 terms in doc
        ScoreDoc[] hit = result.scoreDocs;
        if(hit.length == 0){
            return null;
        }
        int docId = hit[0].doc;
        return(getKeywordsByDocId(docId));
    }

    public Map<String, Integer> getKeywordsByDocId(int id) throws IOException {
        Terms tv = reader.getTermVector(id, "text");
        TermsEnum iter = tv.iterator();
        BytesRef ref = null;
        Map<String, Integer> keywords = new HashMap<>();
        while ((ref = iter.next()) != null){
            keywords.put(iter.term().utf8ToString(), (int)iter.totalTermFreq());
        }
        //sort hashmap and return top 10 entries
        return keywords.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}
