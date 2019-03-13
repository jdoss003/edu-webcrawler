package com.bdj.eduwebcrawler;

import org.apache.lucene.document.Document;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.geom.Point2;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.view.Camera;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

import java.awt.*;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.System.exit;

public class GraphUI implements ViewerListener
{
    private static final Searcher searcher;
    private static Point3 clickStart = new Point3(0, 0, 0);
    private static Point mouseStart = new Point();

    static
    {
        try
        {
            searcher = new Searcher("Lucene/index");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean Screen;
    protected boolean loop = true;

    public GraphUI(boolean exit, String sid)
    {
        Graph mainGraph = new SingleGraph("NodeClicks", true, false);

        final Viewer viewer = mainGraph.display(true);
        final View view = viewer.getDefaultView();

        // https://stackoverflow.com/questions/44675827/how-to-zoom-into-a-graphstream-view
        ((Component)view).addMouseWheelListener(e ->
        {
            e.consume();
            int i = e.getWheelRotation();
            double factor = Math.pow(1.25, i);
            Camera cam = view.getCamera();
            double zoom = cam.getViewPercent() * factor;
            Point2 pxCenter = cam.transformGuToPx(cam.getViewCenter().x, cam.getViewCenter().y, 0);
            Point3 guClicked = cam.transformPxToGu(e.getX(), e.getY());
            double newRatioPx2Gu = cam.getMetrics().ratioPx2Gu / factor;
            double x = guClicked.x + (pxCenter.x - e.getX()) / newRatioPx2Gu;
            double y = guClicked.y - (pxCenter.y - e.getY()) / newRatioPx2Gu;
            cam.setViewCenter(x, y, 0);
            cam.setViewPercent(zoom);
        });

        //If child graph
        if (exit)
        {
            Document d;
            try
            {
                d = searcher.searchByURL("url", sid);

                if (d == null)
                {
                    throw new IllegalStateException("Seed URL not in Lucene!!!");
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            //Add chosen seed url and its children to the graph

            String[] children = searcher.getChildURLs(d);
            String url = d.get("url");
            mainGraph.addNode(url);
            Node n = mainGraph.getNode(url);
            SetName(n, ("SEED: " + url));
            n.addAttribute("ui.style", "shape:circle;fill-color:red;size:" + Math.min(Math.max(children.length / 5, 5), 20) + "px;");
            n.setAttribute("z", 0);
            addChildren(mainGraph, url, children, 1);
            System.out.println("Done loading graph!");
        }
        //If main "Seed Graph"
        else
        {
            //add seeds to graph for later choosing
            List<String> seedList = EduWebcrawler.INSTANCE.getConfig().getSeedList();

            int i = 0;
            for (String seed : seedList)
            {
                mainGraph.addNode(seed);
                Node y = mainGraph.getNode(seed);
                SetName(y, seed);
                y.addAttribute("ui.style", "shape:circle;fill-color: yellow;size:15px;");
                y.setAttribute("xyz", 2.5, i, 0);
                i++;
            }
        }

        ViewerPipe fviews = viewer.newViewerPipe();
        fviews.addViewerListener(this);
        fviews.addSink(mainGraph);

        //exit application handlers
        if (exit)
        {
            viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);
            Screen = false;
        }
        else
        {
            viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);
            Screen = true;
        }
        while (loop)
        {
            fviews.pump();
        }
    }

    public static void SetName(Node Nnode, String url)
    {
        Nnode.addAttribute("ui.label", url);
    }

    public static void SetXYZ(Node Nnode, Integer x, Integer y, Integer z)
    {
        Nnode.setAttribute("xyz", x, y, z);
    }

    public static void addChildren(Graph mainGraph, String url, String[] children, int depth)
    {
        if (depth > EduWebcrawler.INSTANCE.getConfig().getMaxRedirects())
        {
            return;
        }

        try
        {
            for (String child : children)
            {
                Document chdoc = searcher.searchByURL("url", child);
                if (chdoc != null)
                {
                    Node y = mainGraph.getNode(child);

                    if (y == null)
                    {
                        y = mainGraph.addNode(child);

                        Document doc = searcher.searchByURL("url", child);
                        String[] subChildren = doc != null ? searcher.getChildURLs(doc) : new String[0];

                        //SetName(y, child);

                        int numChildren = subChildren.length;
                        y.addAttribute("ui.style", "shape:triangle;fill-color:" + getColor(numChildren) + ";size:" + Math.min(Math.max(numChildren / 5, 10), 50) + "px;");

                        if (mainGraph.getEdge(url + child) == null)
                        {
                            mainGraph.addEdge((url + child), url, child, true);
                        }

                        if (numChildren > 0)
                        {
                            addChildren(mainGraph, child, subChildren, depth + 1);
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {

            e.printStackTrace();
        }
    }

    public static String getColor(int numChildren)
    {
        return numChildren > 20 ? "orange" : "yellow";
    }

    public static void run()
    {
        //make first "Seed" graph
        new GraphUI(false, "");
        exit(0);
    }

    public void viewClosed(String id)
    {
        loop = false;
    }

    public void buttonPushed(String id)
    {

    }

    public void buttonReleased(String id)
    {
        System.out.println("\nButton pushed on node " + id);
        //create new graph if id matches seed url
        if (Screen)
        {
            new GraphUI(true, id);
        }
        else
        {
            try
            {
                Document d = searcher.searchByURL("url", id);
                if (d != null)
                {
                    if (d.getField("title") != null)
                    {
                        System.out.println("Title:\n" + d.get("title"));
                    }
                    if (d.getField("description") != null)
                    {
                        System.out.println("Description:\n" + d.get("description"));
                    }
                    if (d.getField("keywords") != null)
                    {
                        System.out.println("Keywords:\n" + d.get("keywords"));
                    }
                    else
                    {
                        Map<String, Integer> kws = searcher.getKeywords("url", id);
                        if (kws == null)
                        {
                            return;
                        }
                        System.out.println("Keywords: ");
                        Iterator it = kws.entrySet().iterator();
                        while (it.hasNext())
                        {
                            Map.Entry pair = (Map.Entry)it.next();
                            System.out.println("          " + pair.getKey() + " freq: " + pair.getValue());
                            it.remove(); // avoids a ConcurrentModificationException
                        }
                    }
                }
                else
                {
                    System.out.println("No Info on Node Available");
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void addEdge(Graph graph, String node1, String node2)
    {
        graph.addEdge((node1 + node2), node1, node2);
    }
}