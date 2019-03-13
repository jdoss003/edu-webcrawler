package com.bdj.eduwebcrawler;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class Main
{
    public static void main(String[] args)
    {
        OptionParser parser = new OptionParser();
        OptionSpec<Void> crawlOption = parser.accepts("crawl", "Crawl .edu websites and extract information");
        OptionSpec<Void> rebuildOption = parser.accepts("rebuild", "Loads crawled html files into Lucene DB");
        OptionSpec<Void> queryOption = parser.accepts("query", "Query the database");
        OptionSpec<Void> visualizationOption = parser.accepts("visualize", "Visualize the graph of crawled websites");
        OptionSet optionSet = parser.parse(args);

        if (optionSet.has(crawlOption))
        {
            EduWebcrawler.INSTANCE.run();
        }

        if (optionSet.has(rebuildOption))
        {
            Loader.run();
        }

        if (optionSet.has(queryOption))
        {
            Querent.run();
        }

        if (optionSet.has(visualizationOption))
        {
            GraphUI.run();
        }
    }
}
