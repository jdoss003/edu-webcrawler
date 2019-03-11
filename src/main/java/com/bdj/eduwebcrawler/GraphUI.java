package com.bdj.eduwebcrawler;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.view.*;



 public class GraphUI{
 	protected boolean loop= true;

 	public static void Seed() {
 		Graph mainGraph = new SingleGraph("NodeClicks");
 		Viewer views = mainGraph.display();

 		views.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);

 	}

	 public static void main(String [] args) {
		 Graph graph = new SingleGraph("ViewerMap");
		 Viewer views=graph.display();
		 View viewOpt = views.getDefaultView();
		 graph.addNode("sample");
		 // when node is selected execute subgraph
		 Seed();
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