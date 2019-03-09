package com.bdj.eduwebcrawler;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.view.*;

 public class GraphUI{
 	protected boolean loop= true;
	 public static void main(String [] args) {
	 	new GraphUI();
	 }

	 public  GraphUI() {
	 	Graph mainGraph= new SingleGraph("NodeClicks");
	 	Viewer views= mainGraph.display();

	 	views.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);


	 }

 }