package com.bdj.eduwebcrawler;

import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;


import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
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

    public void addDoc(String url, String title, String description, String keywords, List<String> childURLs, String text) throws IOException {
        //create new document
        Document doc = new Document();

        //add fields to document
        //url
        doc.add(new StringField("url", url, StringField.Store.YES));

        //child urls
        for (String temp : childURLs){
            doc.add(new StringField("childURLs", temp, StringField.Store.YES));
        }

        //add text to doc
        FieldType type = new FieldType();
        type.setTokenized(true);
        type.setStoreTermVectors(true);
        type.setStored(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        doc.add(new Field("text", text, type));

        //title
        if (!title.isEmpty()) {
            doc.add(new StringField("title", title, StringField.Store.YES));
        }

        //descriptions
        if (!description.isEmpty()) {
            doc.add(new StringField("description", description, StringField.Store.YES));
        }

        //keywords
        if (!keywords.isEmpty()) {
            doc.add(new StringField("keywords", keywords, StringField.Store.YES));
        }

        //add document to index
        writer.addDocument(doc);
    }
}
