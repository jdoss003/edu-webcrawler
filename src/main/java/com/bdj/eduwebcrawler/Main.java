package com.bdj.eduwebcrawler;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.util.Arrays;

public class Main
{
    public static void main(String[] args)
    {
        OptionParser parser = new OptionParser();
        OptionSpec<Void> crawlOption = parser.accepts("crawl", "Crawl .edu websites and extract information");
        OptionSpec<Void> queryOption = parser.accepts("query", "Querent the database");
        OptionSpec<Void> visualizationOption = parser.accepts("visualize", "Visualize the graph of crawled websites");
        OptionSet optionSet = parser.parse(args);

        if (optionSet.has(crawlOption)) {
            EduWebcrawler.INSTANCE.run();
        }

        if (optionSet.has(queryOption)) {
            //TODO: Querent Option
        }

        if (optionSet.has(visualizationOption)) {
            //TODO: Visualization Option
        }
    }
}
