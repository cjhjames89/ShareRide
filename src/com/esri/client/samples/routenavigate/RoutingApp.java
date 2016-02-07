/* Copyright 2014 Esri

All rights reserved under the copyright laws of the United States
and applicable international laws, treaties, and conventions.

You may freely redistribute and use this sample code, with or
without modification, provided you include the original copyright
notice and use restrictions.

See the use restrictions.*/
package com.esri.client.samples.routenavigate;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.esri.toolkit.overlays.DrawingCompleteEvent;
import com.esri.toolkit.overlays.DrawingCompleteListener;
import com.esri.toolkit.overlays.DrawingOverlay;
import com.esri.toolkit.overlays.DrawingOverlay.DrawingMode;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.RouteResult;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;

/***
 * This sample shows how to find a route between two or more stops, using
 * a {@link RouteTask}.  The task parameters need to be populated and
 * passed to the task's solve. Task parameters include the stop locations, 
 * the barriers (point barriers only in this sample), the spatial reference 
 * of the stops/barriers, and whether to preserve the order of the stops 
 * (indicated by a number in the sample).
 * <p>
 * Various other options can be set for this task, such as whether to preserve
 * the first and last stop or allow the resulting route to start and end at
 * any one of the stops.  A {@link DrawingOverlay} is used to draw the
 * stop and barrier graphics onto the map.
 */
public class RoutingApp {

  private JMap map;
  private JComponent contentPane;
  private NAFeaturesAsFeature stops = new NAFeaturesAsFeature();
  private NAFeaturesAsFeature barriers = new NAFeaturesAsFeature();
  private GraphicsLayer graphicsLayer;
  private DrawingOverlay myDrawingOverlay;
  private int numStops = 0;
  private int count = 0;
  private Graphic[] graphArray =  new Graphic[2];
  private SpatialReference sparticalReferencePass;
//  public Graphic[] SetGraphArray
//  { 
//	  return graphArray;
//  }
//  
  
  private boolean preserveOrder = false;
  private JButton stopButton;

  // ------------------------------------------------------------------------
  // Constructor
  // ------------------------------------------------------------------------
  public RoutingApp() {
  }

  // ------------------------------------------------------------------------
  // Core functionality
  // ------------------------------------------------------------------------

  private void doRouting() {

    RouteResult result = null;
    RouteParameters parameters = null;

    try {
      RouteTask task = RouteTask.createOnlineRouteTask(
          "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route", null);
      parameters = task.retrieveDefaultRouteTaskParameters();
      sparticalReferencePass = map.getSpatialReference();
      parameters.setOutSpatialReference(sparticalReferencePass);
      //!!!!!!!!!!!!!
      
      stops.setSpatialReference(map.getSpatialReference());
      parameters.setStops(stops);
      parameters.setFindBestSequence(Boolean.valueOf(!preserveOrder)); // opposite of 'preserve order of stops'
      if (barriers.getFeatures().size() > 0) {
        barriers.setSpatialReference(map.getSpatialReference());
        parameters.setPointBarriers(barriers);
      }
      //
      //result = task.solve(parameters);
      //showResult(result);
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(contentPane,
          wrap("An error has occured. "+ e.getLocalizedMessage()), "", JOptionPane.WARNING_MESSAGE);
    }
  }

//  private void showResult(RouteResult result) {
//    if (result != null) {
//      // display the top route on the map as a graphic
//      Route topRoute = result.getRoutes().get(0);
//      Graphic routeGraphic = new Graphic(topRoute.getRouteGraphic().getGeometry(),
//          new SimpleLineSymbol(Color.BLUE, 2.0f));
//      graphicsLayer.addGraphic(routeGraphic);
//      System.out.println(result.toString());
//    }
//  }

  /**
   * Creates the map, sets the initial extent.
   * 
   * @return a map.
   */
  private JMap createMap() {

    final JMap jMap = new JMap();

    ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
        "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer");
    jMap.getLayers().add(tiledLayer);
    jMap.setExtent(new Envelope(-13054452, 3847753, -13017762, 3866957.78));

    // graphics layer for our stop/barrier/route graphics
    graphicsLayer = new GraphicsLayer();
    jMap.getLayers().add(graphicsLayer);

    // create drawing overlay, add to map, and activate
    myDrawingOverlay = new DrawingOverlay();
    jMap.addMapOverlay(myDrawingOverlay);
    myDrawingOverlay.setActive(true);
    
    myDrawingOverlay.addDrawingCompleteListener(new DrawingCompleteListener() {

      @Override
      public void drawingCompleted(DrawingCompleteEvent arg0) {
        Graphic graphic = (Graphic) myDrawingOverlay.getAndClearFeature();
        graphicsLayer.addGraphic(graphic);
        if (graphic.getAttributeValue("type").equals("Stop")) {
        	count++;
            if(count >=2) 
        	  stopButton.setEnabled(false);
          numStops++;
          stops.addFeature(graphic); //graphic is a stop
          graphArray[count] = graphic;
          
          
          graphicsLayer.addGraphic(new Graphic(graphic.getGeometry(), new TextSymbol(12, String
              .valueOf(numStops), Color.WHITE)));
        } else if (graphic.getAttributeValue("type").equals("Barrier")) {
          barriers.addFeature(graphic);
        }
      }
    });

    return jMap;
  }

  // ------------------------------------------------------------------------
  // Public methods
  // ------------------------------------------------------------------------
  public JComponent createUI() {

    // create components
    contentPane = createContentPane();
    map = createMap();
    final JToolBar toolBar = createToolBar(myDrawingOverlay);

    // add them to our content pane
    contentPane.add(map, BorderLayout.CENTER);
    contentPane.add(toolBar, BorderLayout.NORTH);

    return contentPane;
  }

  // ------------------------------------------------------------------------
  // Static methods
  // ------------------------------------------------------------------------
  /**
   * Starting point of this application.
   * @param args arguments to this application.
   */
  public static void main(String[] args) {
	  /* Hannah*/
//	  JOptionPane.showMessageDialog(frame,
//			    "Eggs are not supposed to be green.");
	  
	  Object[] options = {"Driver",
              "Rider"};
	  int n = JOptionPane.showOptionDialog(null,
			  "Are you a driver or a rider?",
			  "Who are you?",
			  JOptionPane.YES_NO_OPTION,
			  JOptionPane.QUESTION_MESSAGE,
			  null,
			  options, null);
	  if(n==0)
		  System.out.println("Driver");
	  else 
		  System.out.println("Rider");
	  
 
	  String userIn = JOptionPane.showInputDialog(null, "Tolerence time in minutes");
	  int time = Integer.parseInt(userIn);
	  System.out.printf("The user's name is '%s'.\n", userIn);
	  
	  //send parameter
	  //n is the client type
	  //ParamData params = new ParamData(graphArray, n, time, sparticalReferencePass);
	  

	  
	  /* Hannah*/
	  
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          // instance of this application
          RoutingApp routingApp = new RoutingApp();

          // create the UI, including the map, for the application.
          JFrame appWindow = routingApp.createWindow();
          appWindow.add(routingApp.createUI());
          appWindow.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  // ------------------------------------------------------------------------
  // Private methods
  // ------------------------------------------------------------------------
  private JToolBar createToolBar(final DrawingOverlay drawingOverlay) {

    JToolBar toolBar = new JToolBar();
    toolBar.setLayout(new FlowLayout(FlowLayout.CENTER));

    // stop
    stopButton = new JButton(" Add a stop ");
    stopButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("type", "Stop");
        drawingOverlay.setUp(
            DrawingMode.POINT,
            new SimpleMarkerSymbol(Color.BLUE, 25, Style.CIRCLE),
            attributes);
      }
    });
    toolBar.add(stopButton);

    // barrier
    JButton barrierButton = new JButton(" Add a barrier ");
    barrierButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("type", "Barrier");
        drawingOverlay.setUp(
            DrawingMode.POINT,
            new SimpleMarkerSymbol(Color.BLACK, 16, Style.X),
            attributes);
      }
    });
    toolBar.add(barrierButton);

    // solve
    JButton routeButton = new JButton(" Solve route ");
    routeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doRouting();
      }
    });
    toolBar.add(routeButton);

    // reset
    JButton resetButton = new JButton("  Reset  ");
    resetButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        graphicsLayer.removeAll();
        stops.clearFeatures();
        barriers.clearFeatures();
        count = 0;
        stopButton.setEnabled(true);
      }
    });
    toolBar.add(resetButton);

    // stop order
    JCheckBox stopOrderCheck = new JCheckBox("Preserve order of stops");
    stopOrderCheck.setSelected(preserveOrder);
    stopOrderCheck.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent arg0) {
        if (arg0.getStateChange() == ItemEvent.DESELECTED) {
          preserveOrder = false;
        } else if (arg0.getStateChange() == ItemEvent.SELECTED) {
          preserveOrder = true;
        }
      }
    });
    toolBar.add(stopOrderCheck);

    return toolBar;
  }

  /**
   * Creates a window.
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame("Online Routing Application");
    window.setBounds(0, 0, 1000, 700);
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.getContentPane().setLayout(new BorderLayout(0, 0));
    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent windowEvent) {
        super.windowClosing(windowEvent);
        if (map != null) {
          map.dispose();
        }
      }
    });
    return window;
  }

  /**
   * Creates a content pane.
   * @return a content pane.
   */
  private static JComponent createContentPane() {
    JComponent contentPane = new JPanel();
    contentPane.setBounds(0, 0, 1000, 700);
    contentPane.setLayout(new BorderLayout(0, 0));
    contentPane.setVisible(true);
    return contentPane;
  }
  
  private String wrap(String str) {
    // create a HTML string that wraps text when longer
    return "<html><p style='width:200px;'>" + str + "</html>";
  }
}
