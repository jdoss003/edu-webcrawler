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
	//	System.out.println("NODE "+ id);


	}



	public void buttonReleased(String id) {

		System.out.println("Clicked on node " + id);

		//create new graph if id matches seed url

		if(Screen)

			new GraphUI(true, id);

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

				Searcher searcher = new Searcher("Lucene/index");

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

				SetName(n, ("SEED: " + url));

				n.addAttribute("ui.style","shape:circle;fill-color:red;size:20px;");

				n.setAttribute("z", 0);



				for(String child: cURLs) {

					try{

						mainGraph.addNode(child);

						Node y = mainGraph.getNode(child);

						//SetName(y, child);

						y.addAttribute("ui.style","shape:triangle;fill-color:orange;size:15px;");

						mainGraph.addEdge((url + child), url, child, true);

					}

					catch (IdAlreadyInUseException e){

						System.out.println("Node " + child + " already made");

					}

				}

				for(int i=0;i<4;++i) {

					//Begin depth first search of graph

					Iterator<Node> iter = n.getBreadthFirstIterator();

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

								try{

									mainGraph.addNode(child);

									Node y = mainGraph.getNode(child);

									//SetName(y, child);

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

