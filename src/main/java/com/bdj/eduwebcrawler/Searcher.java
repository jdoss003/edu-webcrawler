package com.bdj.eduwebcrawler;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Searcher {
    private IndexSearcher searcher;

    public Searcher(String indexDir) throws IOException {
        this(Paths.get(indexDir));
    }

    public Searcher(Path indexDir) throws IOException {
        //this directory will contain the indexes. FS directory takes Path object
        Directory dir = FSDirectory.open(indexDir);

        //make reader
        IndexReader reader = DirectoryReader.open(dir);

        //make searcher
        searcher = new IndexSearcher(reader);
    }

    public Document searchByURL(String key, String url) throws IOException{
        //create query
        Term t = new Term(key, url);
        Query query = new TermQuery(t);
        TopDocs result = searcher.search(query, 1);

        //see if result matches given url. Return document with url else return nothing
        ScoreDoc[] hit = result.scoreDocs;
        int docId = hit[0].doc;
        Document d = searcher.doc(docId);
        if (d.get("url").equals(url))
            return d;
        else
            //Todo if queried url doesnt match returned, what do?
            System.out.println("Could not find document with that url");
            return null;
    }
}
