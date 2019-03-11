package com.bdj.eduwebcrawler;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.stream.file.FileSourceGEXF;
import org.graphstream.ui.view.*;



 public class GraphUI{
 	protected boolean loop= true;

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
//Parent shares node already and same depth return false
/*public boolean HasEdgeParent(Graph g, Node x){
		String Child=x.getId();
		if(x.hasEdgeBetween(FileSourceGEXF.GEXFConstants.PARENTAttribute));
			return false;
			return true;

	}*/