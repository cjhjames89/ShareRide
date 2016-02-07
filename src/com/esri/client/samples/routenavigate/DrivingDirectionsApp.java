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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import com.esri.toolkit.overlays.DrawingCompleteEvent;
import com.esri.toolkit.overlays.DrawingCompleteListener;
import com.esri.toolkit.overlays.DrawingOverlay;
import com.esri.toolkit.overlays.DrawingOverlay.DrawingMode;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.RouteResult;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;

/***
 * This sample shows how to get driving directions in a route between two 
 * or more stops, using a {@link RouteTask}. The task parameters need to 
 * be populated and passed to the task's asynchronous solve method. Task parameters 
 * include the stop locations, the barriers (point barriers only in this 
 * sample), the spatial reference of the stops/barriers, whether to 
 * preserve the order of the stops (indicated by a number in the sample), 
 * and whether to return driving directions.
 * <p>
 * Various other options can be set for this task, such as whether to preserve
 * the first and last stop or allow the resulting route to start and end at
 * any one of the stops.  A {@link DrawingOverlay} is used to draw the
 * stop and barrier graphics onto the map.
 */
public class DrivingDirectionsApp {

  private JMap map;
  private boolean preserveOrder = false;
  private int numStops = 0;
  private int stepRoute = 0;

  // graphics IDs
  ArrayList<Integer> stepIDs = new ArrayList<>();

  private NAFeaturesAsFeature stops = new NAFeaturesAsFeature();
  private NAFeaturesAsFeature barriers = new NAFeaturesAsFeature();
  private GraphicsLayer graphicsLayer;
  private GraphicsLayer routeLayer;
  private SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.BLUE, 5);
  private DrawingOverlay myDrawingOverlay;
  private RouteTask task;

  private JTextArea directionsLabel;
  private JComponent contentPane;
  private JButton stepsButton;

  private static final String SOLVE_BUTTON = " Solve route "; 
  private static final String STOP_BUTTON = " Add a stop ";
  private static final String BARRIER_BUTTON = " Add a barrier ";
  private static final String TURN_BUTTON = " Turn by turn "; 
  private static final String RESET_BUTTON = " Reset ";
  private static final String PRESERVE_ORDER = "Preserve order of stops"; 
  private static final int PANEL_WIDTH = 200;

  // ------------------------------------------------------------------------
  // Constructor
  // ------------------------------------------------------------------------
  public DrivingDirectionsApp() {
    try {
      task = RouteTask.createOnlineRouteTask(
          "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route", null);  
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(contentPane,
          wrap("A server error has occured. " + e.getLocalizedMessage()), "", JOptionPane.WARNING_MESSAGE);
    } 
  }

  // ------------------------------------------------------------------------
  // Core functionality
  // ------------------------------------------------------------------------
  /**
   * Creates a routing task to asynchronously find the best route
   * between our stop locations; by default the order of the stops is preserved.
   */
  private void doRouting() {
    RouteParameters parameters = null;

    try { 
      parameters = task.retrieveDefaultRouteTaskParameters();
      parameters.setOutSpatialReference(map.getSpatialReference());
      stops.setSpatialReference(map.getSpatialReference());
      parameters.setStops(stops);
      parameters.setReturnDirections(true);
      parameters.setFindBestSequence(Boolean.valueOf(!preserveOrder)); // opposite of 'preserve order of stops'
      if (barriers.getFeatures().size() > 0) {
        barriers.setSpatialReference(map.getSpatialReference());
        parameters.setPointBarriers(barriers);
      }
      task.solve(parameters, new CallbackListener<RouteResult>() {

        @Override
        public void onError(Throwable e) {
          e.printStackTrace();
          JOptionPane.showMessageDialog(contentPane,
              wrap("A server error solving the route has occured. "+ e.getLocalizedMessage()), "", JOptionPane.WARNING_MESSAGE);
        }

        @Override
        public void onCallback(RouteResult result) {
          showResult(result);
        }
      });

    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(contentPane,
          wrap("An error retrieving route parameters has occured. "+ e.getLocalizedMessage()), "", JOptionPane.WARNING_MESSAGE);
    }
  }


  /**
   * Shows the result route on the map and adds 
   * the route summary to the text area
   *
   * @param result the result
   */
  private void showResult(RouteResult result) {
    if (result != null) {
      String routeSummary;
      Route topRoute = result.getRoutes().get(0);
      topRoute.getRoutingDirections();

      // add route segments to the route layer
      for (RouteDirection rd : topRoute.getRoutingDirections()) {
        HashMap<String, Object> attribs = new HashMap<>();
        attribs.put("text", rd.getText());
        attribs.put("time", Double.valueOf(rd.getMinutes()));
        attribs.put("length", Double.valueOf(rd.getLength()));
        Graphic a = new Graphic(rd.getGeometry(), routeSymbol, attribs);  
        stepIDs.add(Integer.valueOf(routeLayer.addGraphic(a)));
      }

      // add route as a graphic to the graphics layer
      Graphic routeGraphic = new Graphic(topRoute.getRouteGraphic().getGeometry(),
          new SimpleLineSymbol(Color.BLUE, 2.0f), 0);
      graphicsLayer.addGraphic(routeGraphic);

      // Get the full route summary 
      routeSummary = String.format(
          "%s%nTotal time: %.1f minutes %nLength: %.1f miles",
          topRoute.getRouteName(), Double.valueOf(topRoute.getTotalMinutes()),
          Double.valueOf(topRoute.getTotalMiles()));
      directionsLabel.setText(routeSummary);
      // Zoom to the extent of the entire route
      Polygon bufferedExtent = GeometryEngine.buffer(topRoute.getEnvelope(), map.getSpatialReference(), 500, null);
      map.setExtent(bufferedExtent);
    }
    else {
      // set no route found message
      JOptionPane.showMessageDialog(map.getParent(), "No route found!");
    }
  }

  /**
   * Displays turn by turn directions in the user panel and map.
   */
  private void doTurnbyTurn() {
    if (stepRoute < routeLayer.getNumberOfGraphics()) {
      Graphic selected = routeLayer.getGraphic(stepIDs.get(stepRoute).intValue());      
      // Highlight route segment on the map
      routeLayer.select(stepIDs.get(stepRoute).intValue());
      String direction = ((String) selected.getAttributeValue("text"));
      Double time = (Double) selected.getAttributeValue("time");
      Double length = (Double) selected.getAttributeValue("length");
      String label = String.format("%s%nTime: %.1f minutes, Length: %.1f miles",
          direction, time, length);
      directionsLabel.setText(label);
      stepRoute++;
    }
    else {
      directionsLabel.setText("Route ends");
      stepsButton.setEnabled(false);
    }
  }

  /**
   * Creates the map, layers and overlays.
   * 
   * @return a map.
   */
  private JMap createMap() {

    JMap jMap = new JMap();

    ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
        "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer");
    jMap.getLayers().add(tiledLayer);

    // set map extent to San Diego
    jMap.setExtent(new Envelope(-13054452, 3847753, -13017762, 3866957.78));

    // stops/barriers/route graphics 
    graphicsLayer = new GraphicsLayer();
    // route segment graphics (for turn by turn directions)
    routeLayer = new GraphicsLayer();
    // set highlight color  
    routeLayer.setSelectionColor(Color.RED);
    jMap.getLayers().add(routeLayer);
    jMap.getLayers().add(graphicsLayer);

    myDrawingOverlay = new DrawingOverlay();
    jMap.addMapOverlay(myDrawingOverlay);
    myDrawingOverlay.setActive(true);
    myDrawingOverlay.addDrawingCompleteListener(new DrawingCompleteListener() {

      @Override
      public void drawingCompleted(DrawingCompleteEvent arg0) {
        Graphic graphic = (Graphic) myDrawingOverlay.getAndClearFeature();
        graphicsLayer.addGraphic(graphic);
        if (graphic.getAttributeValue("type").equals("Stop")) {
          numStops++;
          stops.addFeature(graphic);
          graphicsLayer.addGraphic(new Graphic(graphic.getGeometry(), new TextSymbol(12, String
              .valueOf(numStops), Color.WHITE), 1));
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
  /**
   * Creates the panel to display route content.
   *
   * @return the component
   */
  private Component createPanel() {
    JComponent panel = new JPanel();
    panel.setLocation(10, 50);      
    panel.setBackground(new Color(0, 0, 0, 100));
    panel.setBorder(new LineBorder(Color.BLACK, 3, false));
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));     
    panel.setSize(PANEL_WIDTH, 200); 

    // title 
    JTextField txtTitle = new JTextField(); 
    txtTitle.setText("Driving Directions"); 
    txtTitle.setHorizontalAlignment(SwingConstants.CENTER); 
    txtTitle.setFont(new Font(txtTitle.getFont().getName(), Font.PLAIN, 14)); 
    txtTitle.setAlignmentX(Component.CENTER_ALIGNMENT);  
    txtTitle.setBackground(new Color(0, 0, 0, 255)); 
    txtTitle.setForeground(Color.WHITE); 
    txtTitle.setMaximumSize(new Dimension(PANEL_WIDTH, 30));

    // scrollpane and route/turn by turn directions text area 
    directionsLabel = new JTextArea(); 
    directionsLabel.setLineWrap(true); 
    directionsLabel.setWrapStyleWord(true);
    directionsLabel.setEditable(true);
    directionsLabel.setFont(new Font(directionsLabel.getFont().getName(), directionsLabel.getFont().getStyle(), 12)); 
    directionsLabel.setMaximumSize(new Dimension(200, 100));
    directionsLabel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
    directionsLabel.setMinimumSize(new Dimension(PANEL_WIDTH, 150));    
    JScrollPane scrollPane = new JScrollPane(directionsLabel);  

    // button - when clicked, shows turn by turn directions 
    stepsButton = new JButton(TURN_BUTTON); 
    stepsButton.setMaximumSize(new Dimension(PANEL_WIDTH, 25));
    stepsButton.setMinimumSize(new Dimension(PANEL_WIDTH, 25));
    stepsButton.addActionListener(new ActionListener() {
      @Override 
      public void actionPerformed(ActionEvent e) {
        doTurnbyTurn();
      } 
    }); 
    stepsButton.setAlignmentX(Component.CENTER_ALIGNMENT);

    // group the above UI items into a panel
    panel.add(txtTitle); 
    panel.add(Box.createVerticalStrut(5));  
    panel.add(scrollPane); 
    panel.add(Box.createVerticalStrut(5)); 
    panel.add(stepsButton); 
    panel.add(Box.createVerticalStrut(5)); 

    return panel;
  }

  // ------------------------------------------------------------------------
  // Static methods
  // ------------------------------------------------------------------------
  /**
   * Starting point of this application.
   * @param args arguments to this application.
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          // instance of this application
          DrivingDirectionsApp drivingApp = new DrivingDirectionsApp();

          JFrame appWindow = drivingApp.createWindow();
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
  /**
   * Creates the UI tool bar: add stops, add barriers, solve route and 
   * reset buttons are created and set up in our tool bar.
   *
   * @param drawingOverlay the drawing overlay used by the toolbar buttons
   * @return the tool bar
   */
  private JToolBar createToolBar(final DrawingOverlay drawingOverlay) {

    JToolBar toolBar = new JToolBar();
    toolBar.setLayout(new FlowLayout(FlowLayout.CENTER));

    // stop
    final JButton stopButton = new JButton(STOP_BUTTON);
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
    final JButton barrierButton = new JButton(BARRIER_BUTTON);
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
    final JButton routeButton = new JButton(SOLVE_BUTTON);
    routeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        stopButton.setEnabled(false);
        barrierButton.setEnabled(false);
        routeButton.setEnabled(false);
        stepsButton.setEnabled(true);
        doRouting();
      }
    });
    toolBar.add(routeButton);

    // reset
    JButton resetButton = new JButton(RESET_BUTTON);
    resetButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        stopButton.setEnabled(true);
        barrierButton.setEnabled(true);
        routeButton.setEnabled(true);
        stepsButton.setEnabled(false);
        directionsLabel.setText("");
        graphicsLayer.removeAll();
        routeLayer.removeAll();
        stops.clearFeatures();
        barriers.clearFeatures();
        stepIDs.clear();
        numStops = 0;
        stepRoute = 0;
      }
    });
    toolBar.add(resetButton);

    // stop order
    JCheckBox stopOrderCheck = new JCheckBox(PRESERVE_ORDER);
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
   * Creates the UI: a content pane containing a map and user panel, and a 
   * tool bar.
   * @return a contentPane.
   */
  private JComponent createUI() {
    map = createMap();
    contentPane = createContentPane(); 
    contentPane.add(createPanel());
    contentPane.add(createToolBar(myDrawingOverlay), BorderLayout.NORTH);
    contentPane.add(map);

    return contentPane;
  }

  /**
   * Creates a window.
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame("Driving Directions Application");
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
    window.add(createUI());
    return window;
  }

  /**
   * Creates a content pane.
   * @return a content pane.
   */
  private static JLayeredPane createContentPane() {
    JLayeredPane contentPane = new JLayeredPane(); 
    contentPane.setBounds(100, 100, 1000, 700); 
    contentPane.setLayout(new BorderLayout(0, 0)); 
    contentPane.setVisible(true); 
    return contentPane;
  }
  
  private String wrap(String str) {
    // create a HTML string that wraps text when longer
    return "<html><p style='width:200px;'>" + str + "</html>";
  }
}