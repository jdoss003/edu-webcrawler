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

import static java.lang.System.exit;


public class GraphUI implements ViewerListener{
 	protected boolean loop= true;
	public String IDClicked;
	public boolean Screen;
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

	public void buttonPushed(String id) {
		System.out.println("Button pushed on node "+id);
		IDClicked=id;
		//create new graph if id matches seed url
		if(Screen){
			new GraphUI(true);
		}

	}

	public void buttonReleased(String id) {
		//System.out.println("Results For:"+id);

	}


 	public GraphUI(boolean exit) {
 		Graph mainGraph = new SingleGraph("NodeClicks");
 		Viewer views = mainGraph.display();



 		for(int i=0; i <10;++i) {

 			String idHelper="www.ucr.edu"+Integer.toString(i);
			mainGraph.addNode(idHelper);
			Node x=mainGraph.getNode(idHelper);
			x.addAttribute("ui.label","www.ucr.edu"+Integer.toString(i));
			if(i==0) {
				x.addAttribute("ui.style", "shape:circle;fill-color: yellow;size:20px;");
			}
			if(i>0) {
				String idPrev="www.ucr.edu"+Integer.toString((i-1));
				mainGraph.addEdge("EDGE" +Integer.toString(i),idHelper,idPrev, true);
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

 			while(loop) {
			fviews.pump();

		}

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
		 final Integer  seed_Size=22;
		 Graph graph = new SingleGraph("ViewerMap");
		 Viewer views=graph.display();
		 View viewOpt = views.getDefaultView();
		 Node y;
		 // DOC Implementation






		 // DOC Implementation







		for(int i=0;i<seed_Size;++i) {
			graph.addNode("seed"+Integer.toString(i));
			y=graph.getNode("seed"+Integer.toString(i));
			SetName(y,"seed#");
			y.addAttribute("ui.style","shape:circle;fill-color: yellow;size:15px;");
			y.setAttribute("xyz",2.5,i,0);
		}
		 //y=graph.getNode("middle");

		 //SetName(y,"url");

		 new GraphUI(false);
			exit(0);

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
//22