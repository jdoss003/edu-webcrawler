package com.bdj.eduwebcrawler;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.apache.lucene.document.Document;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.view.*;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.System.exit;


public class GraphUI implements ViewerListener{
 	protected boolean loop= true;
	public String IDClicked; //TODO Get this to work maybe???
	public boolean Screen;
	private static CrawlerConfig config;
	private static final String CONFIG_PATH = "./crawler_config.toml";
	public Searcher searcher;


 	public static void SetName(Node Nnode, String url)
	{
		Nnode.addAttribute("ui.label", url);
	}
	public static void SetXYZ(Node Nnode,Integer x, Integer y, Integer z)
	{
		Nnode.setAttribute("xyz",x,y,z);
	}
	public void viewClosed(String id) {
		loop = false;
	}

	public void buttonPushed(String id) {

	}

	public void buttonReleased(String id) {
		System.out.println("\nButton pushed on node " + id);
		//create new graph if id matches seed url
		if(Screen)
			new GraphUI(true, id);
		else
		{
			try{
				Document d = searcher.searchByURL("url", id);
				if(d != null){
					if(d.getField("title") != null)
						System.out.println("Title:\n" + d.get("title"));
					if(d.getField("description") != null)
						System.out.println("Description:\n" + d.get("description"));
					if(d.getField("keywords") != null)
						System.out.println("Keywords:\n" + d.get("keywords"));
					else {
						Map<String, Integer> kws = searcher.getKeywords("url", id);
						if(kws == null){
							return;
						}
						System.out.println("Keywords: ");
						Iterator it = kws.entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry pair = (Map.Entry)it.next();
							System.out.println("          " + pair.getKey() + " freq: " + pair.getValue());
							it.remove(); // avoids a ConcurrentModificationException
						}
					}
				}
				else System.out.println("No Info on Node Available");
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
	}

	public void addEdge(Graph graph, String node1, String node2){
 		graph.addEdge((node1+node2), node1, node2);
	}

 	public GraphUI(boolean exit, String sid) {
 		Graph mainGraph = new SingleGraph("NodeClicks", true, false);
 		Viewer views = mainGraph.display();

 		//If child graph
		if (exit) {
			try{
				searcher = new Searcher("Lucene/index");

				Document d = searcher.searchByURL("url", sid);
				if(d == null) {
					System.out.println("Seed URL not in Lucene!!!");
					exit(1);
				}
				//Add chosen seed url and its children to the graph
				String url = d.get("url");
				List<String> cURLs = Arrays.asList(searcher.getChildURLs(d));
				mainGraph.addNode(url);
				Node n = mainGraph.getNode(url);
				n.addAttribute("ui.style","shape:circle;fill-color:red;size:20px;");

				for(String child: cURLs) {
					Document chdoc = searcher.searchByURL("url", child);
					if(chdoc != null) {
						try{
							mainGraph.addNode(child);
							Node y = mainGraph.getNode(child);
							y.addAttribute("ui.style","shape:triangle;fill-color:orange;size:15px;");
							mainGraph.addEdge((url + child), url, child, true);
						}
						catch (IdAlreadyInUseException e){
							System.out.println("Node " + child + " already made");
						}
					}
				}

				//get 5 hops worth of data
				for( int i = 0; i < 5; i++) {
					//Begin depth first search of graph
					Iterator <Node> iter = n.getBreadthFirstIterator();
					iter.next();
					Node node = null;

					//Begin adding child nodes to graph
					while(iter.hasNext())
					{
						node = iter.next();
						//get node URl
						String id = node.getId();
						Document doc = searcher.searchByURL("url", id);
						if(doc != null) {
							List<String> childURLs = Arrays.asList(searcher.getChildURLs(doc));
							//iterate through urls and add any that dont already exist
							for(String child: childURLs) {
								Document chdoc = searcher.searchByURL("url", child);
								if(chdoc != null) {
									try{
										//add node and edge
										mainGraph.addNode(child);
										Node y = mainGraph.getNode(child);
										y.addAttribute("ui.style","shape:triangle;fill-color:yellow;size:15px;");
										mainGraph.addEdge((id + child), id, child, true);
									}
									catch (IdAlreadyInUseException e){
										System.out.println("Node " + child + " already made");
									}
								}
							}
						}
					}
				}
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
		//If main "Seed Graph"
		else {
			//add seeds to graph for later choosing
			List<String> seedList = config.getSeedList();

			int i = 0;
			for(String seed: seedList) {
				mainGraph.addNode(seed);
				Node y = mainGraph.getNode(seed);
				SetName(y, seed);
				y.addAttribute("ui.style","shape:circle;fill-color: yellow;size:15px;");
				y.setAttribute("xyz",2.5, i, 0);
				i++;
			}
		}

		ViewerPipe fviews= views.newViewerPipe();
 		fviews.addViewerListener(this);
 		fviews.addSink(mainGraph);

 		//exit application handlers
		if(exit) {
			views.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);
			Screen=false;
		}
		else{
			views.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);
			Screen=true;
		}
		while(loop) fviews.pump();
 	}

	 public static void run() {

		 final FileConfig fileConfig = FileConfig.builder(Paths.get(CONFIG_PATH)).build();
		 fileConfig.load();
		 fileConfig.close();
		 config = new CrawlerConfig(fileConfig);

		 //make first "Seed" graph
		 new GraphUI(false, "");
		 exit(0);
	 }
 }