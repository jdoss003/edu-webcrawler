package com.bdj.eduwebcrawler;

import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Indexer implements AutoCloseable {
    private IndexWriter writer;

    public Indexer(String indexDir) throws IOException {
        this(Paths.get(indexDir));
    }

    public Indexer(Path directory) throws IOException {
        //this directory will contain the indexes. FS directory takes Path object
        Directory indexDirectory = FSDirectory.open(directory);

        //standard analyzer does analysis bases on basic grammar, removes stopwords, converts to lowercase
        StandardAnalyzer analyzer = new StandardAnalyzer();

        //create the indexer
        writer = new IndexWriter(indexDirectory, new IndexWriterConfig(analyzer));
    }

    public void deleteAll() throws IOException {
        //Delete all content where IndexWriter writes
        writer.deleteAll();
    }

    public void close() throws IOException {
        //close IndexWriter
        writer.close();
    }

    public void addDoc(String url, List<String> parentURLs, List<String> childURLs, String text) throws IOException {
        //create new document
        Document doc = new Document();

        //add fields to document
        doc.add(new StringField("url", url, StringField.Store.YES));
        doc.add(new TextField("text", text, TextField.Store.YES));

        for (String temp : parentURLs){
            doc.add(new StringField("parentURLs", temp, StringField.Store.YES));
        }

        for (String temp : childURLs){
            doc.add(new StringField("childURLs", temp, StringField.Store.YES));
        }

        //add document to index
        writer.addDocument(doc);
    }
}
