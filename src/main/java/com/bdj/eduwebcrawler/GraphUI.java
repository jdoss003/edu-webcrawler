package com.bdj.eduwebcrawler;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.stream.file.FileSourceGEXF;
import org.graphstream.ui.view.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Queue;


public class GraphUI{
 	public boolean loop= true;

 	public static void SetName(Node Nnode, String url)
	{
		Nnode.addAttribute("ui.label",url);
	}
	public static void SetXYZ(Node Nnode,Integer x, Integer y, Integer z)
	{
		Nnode.setAttribute("xyz",x,y,z);
	}
	public void viewClosed(String id) {
		loop = false;
	}
	public void Clicks() {

		Graph graph = new SingleGraph("Clicks");
		Viewer viewer = graph.display();


		viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);


		ViewerPipe fromViewer = viewer.newViewerPipe();
		fromViewer.addViewerListener((ViewerListener) this);
		fromViewer.addSink(graph);



		while(loop) {
			fromViewer.pump(); // or fromViewer.blockingPump();


		}
	}
	public void buttonPushed(String id) {
		System.out.println("Button pushed on node "+id);
	}

	public void buttonReleased(String id) {
		System.out.println("Button released on node "+id);
	}
 	public static void Seed() {
 		Graph mainGraph = new SingleGraph("NodeClicks");
 		Viewer views = mainGraph.display();
 		for(int i=0; i <10;++i) {
			mainGraph.addNode(Integer.toString(i));
			Node x=mainGraph.getNode(Integer.toString(i));
			x.addAttribute("ui.label","A"+Integer.toString(i));
			if(i>0) {
				mainGraph.addEdge("EDGE" + Integer.toString(i),  Integer.toString(i-1),
						 Integer.toString(i), true);
			}

		}

 		views.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);

 	}
 	public static Queue NodeFiles(Queue e) throws IOException {
		String fileName="";
		File file= new File(fileName);
		FileReader fr=new FileReader(file);
		BufferedReader br=new BufferedReader(fr);
		String line;
		while((line=br.readLine())!=null)
		{
			e.add(line);
		}
		return e;
	}



	 public static void main(String [] args) {


		 Queue<String> Files;

		 Graph graph = new SingleGraph("ViewerMap");
		 Viewer views=graph.display();
		 View viewOpt = views.getDefaultView();
		 graph.addNode("sample");
		 Node y=graph.getNode("sample");
		 y.setAttribute("xyz",2,1,0);
		 graph.addNode("s");
		 y=graph.getNode("s");
		 y.setAttribute("xyz",3,1,0);
		 graph.addNode("middle");
		 y=graph.getNode("middle");
		 y.setAttribute("xyz",2.5,1,0);
		 SetName(y,"url");
		 // when node is selected execute subgraph
		 //Seed();
	 }
 }

 //seeds and use the int (get_degree) levels
// 3 options edu web-crawler query visuals
/*
  public void IGraph() {
        Graph graphnew = new SingleGraph("InputGraph");
        Viewer Vviewers = graphnew.display();
        Vviewers.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);
        ViewerPipe fPipe = Vviewers.newViewerPipe();
        fPipe.addViewerListener((ViewerListener) this);
        fPipe.addSink(graphnew);
        while (loop) {
            fPipe.pump();
        }
    }

 */
//Parent shares node already and same depth return false
/*public boolean HasEdgeParent(Graph g, Node x){
		String Child=x.getId();
		if(x.hasEdgeBetween(FileSourceGEXF.GEXFConstants.PARENTAttribute));
			return false;
			return true;

	}*/
