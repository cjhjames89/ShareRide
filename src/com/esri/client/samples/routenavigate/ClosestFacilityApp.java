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
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;

import com.esri.toolkit.overlays.DrawingCompleteEvent;
import com.esri.toolkit.overlays.DrawingCompleteListener;
import com.esri.toolkit.overlays.DrawingOverlay;
import com.esri.toolkit.overlays.DrawingOverlay.DrawingMode;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.Symbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.tasks.na.ClosestFacilityParameters;
import com.esri.core.tasks.na.ClosestFacilityResult;
import com.esri.core.tasks.na.ClosestFacilityTask;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;

/***
 * This sample shows how to find the nearest facility to a location, using
 * a {@link ClosestFacilityTask}.  The task parameters need to be obtained, 
 * populated with desired values, and passed to the task's solve method. 
 * Task parameters include the facility locations, any barriers present 
 * (none shown in this sample), and the spatial reference of the facilities/barriers.
 * <p>
 * To use the sample, click on the map to place an incident graphic and
 * the task will be run asynchronously using this point to find the route 
 * to the nearest hospital shown on the map.
 */
public class ClosestFacilityApp {

  private JMap map;
  private JLayeredPane contentPane;
  private NAFeaturesAsFeature facilities = new NAFeaturesAsFeature();
  private GraphicsLayer graphicsLayer;
  private DrawingOverlay drawingOverlay;
  protected Symbol facilitySymbol;
  private Symbol routeSymbol;
  private Symbol incidentSymbol;
  protected SpatialReference mapSR;
  private ClosestFacilityTask task;

  // ------------------------------------------------------------------------
  // Constructor
  // ------------------------------------------------------------------------
  public ClosestFacilityApp() {
  }

  // ------------------------------------------------------------------------
  // Core functionality
  // ------------------------------------------------------------------------

  private void getClosestFacility(Graphic graphic) throws Exception {

    final ClosestFacilityParameters parameters = task.getDefaultParameters();
    parameters.setDefaultCutoff(Double.valueOf(30.0));

    NAFeaturesAsFeature incidents = new NAFeaturesAsFeature();
    incidents.addFeature(graphic);
    incidents.setSpatialReference(mapSR);
    parameters.setIncidents(incidents);

    facilities.setSpatialReference(mapSR);
    parameters.setFacilities(facilities);
    parameters.setOutSpatialReference(mapSR);

    SwingWorker<ClosestFacilityResult, Void> worker = new SwingWorker<ClosestFacilityResult, Void>() {
      private ClosestFacilityResult routeResult;

      @Override
      public ClosestFacilityResult doInBackground() {
        ClosestFacilityResult result = null;
        try {
          result = task.solve(parameters);
        } catch (Exception e) {
        e.printStackTrace();
      }
        return result;
      }

      @Override
      public void done() {
        try {
          routeResult = get();
          Route topRoute = routeResult.getRoutes().get(0);
          graphicsLayer.addGraphic(new Graphic(topRoute.getRouteGraphic().getGeometry(), routeSymbol));
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }
      }
    };
    worker.execute();
  }

  /**
   * Creates the map, sets the initial extent.
   * @return a map.
   */
  private JMap createMap() {

    JMap jMap = new JMap();

    ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
        "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer");
    jMap.getLayers().add(tiledLayer);

    // create symbols
    facilitySymbol = createFacilitySymbol();
    incidentSymbol = new SimpleMarkerSymbol(Color.BLACK, 18, Style.X);
    routeSymbol = new SimpleLineSymbol(Color.BLUE, 2.0f);

    // graphics layer for our facilities and route graphics
    graphicsLayer = new GraphicsLayer();
    jMap.getLayers().add(graphicsLayer);

    // set extent and create some facility graphics (hospitals in San Diego, USA)
    jMap.addMapEventListener(new MapEventListenerAdapter() {

      @Override
      public void mapReady(MapEvent event) {
        // store the spatial reference for reprojections from lat-lon
        mapSR = map.getSpatialReference();
        // San Diego center from lat-lon coordinate
        Point sanDiegoCenter = GeometryEngine.project(-117.1750, 32.727, mapSR);
        // set extent to a 10km x 10km area around the center of San Diego 
        event.getMap().setExtent(new Envelope(sanDiegoCenter, 10000, 10000));

        // create some facility graphics
        Graphic[] facilityGraphics = {
            new Graphic(GeometryEngine.project(-117.138368, 32.708657, mapSR), facilitySymbol),
            new Graphic(GeometryEngine.project(-117.163369, 32.724766, mapSR), facilitySymbol),
            new Graphic(GeometryEngine.project(-117.159477, 32.735328, mapSR), facilitySymbol),
            new Graphic(GeometryEngine.project(-117.159918, 32.751387, mapSR), facilitySymbol),
            new Graphic(GeometryEngine.project(-117.144708, 32.755919, mapSR), facilitySymbol),
            new Graphic(GeometryEngine.project(-117.201550, 32.752967, mapSR), facilitySymbol),
            new Graphic(GeometryEngine.project(-117.221417, 32.748656, mapSR), facilitySymbol)
        };
        // add them to the graphics layer for display and to our 'facilities' collection for the task
        graphicsLayer.addGraphics(facilityGraphics);
        facilities.addFeatures(facilityGraphics);
      }
    });

    // create drawing overlay, add to map, activate
    drawingOverlay = new DrawingOverlay();
    drawingOverlay.setUp(DrawingMode.POINT, incidentSymbol, null);
    jMap.addMapOverlay(drawingOverlay);
    drawingOverlay.setActive(true);
    drawingOverlay.addDrawingCompleteListener(new DrawingCompleteListener() {

      @Override
      public void drawingCompleted(DrawingCompleteEvent arg0) {
        Graphic graphic = (Graphic) drawingOverlay.getAndClearFeature();
        graphicsLayer.addGraphic(graphic);
        try {
        getClosestFacility(graphic);
        } catch (Exception e) {
          JOptionPane.showMessageDialog(contentPane,
              wrap("An error has occured. " + e.getLocalizedMessage()), "",
              JOptionPane.WARNING_MESSAGE);
          e.printStackTrace();
      }
      }
    });

    task = new ClosestFacilityTask(
        "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/ClosestFacility");

    return jMap;
  }

  // ------------------------------------------------------------------------
  // Public methods
  // ------------------------------------------------------------------------
  public JComponent createUI() {

    contentPane = createContentPane();
    map = createMap();
    JPanel description = createDescription();
    description.setLocation(10, 10);
    contentPane.add(description);
    contentPane.add(map);

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
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          // instance of this application
          ClosestFacilityApp cfApp = new ClosestFacilityApp();

          // create the UI, including the map, for the application.
          JFrame appWindow = cfApp.createWindow();
          appWindow.add(cfApp.createUI());
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
   * Creates a description for this application.
   * @return description
   */
  private JPanel createDescription() {
    JPanel descriptionContainer = new JPanel();
    descriptionContainer.setLayout(new BoxLayout(descriptionContainer, 0));
    descriptionContainer.setSize(220, 100);
    JTextArea description = new JTextArea(
        "Click on the map to place an incident graphic. The Closest Facility Task " +
        "will be run with the graphic to find the route to the nearest " +
        "hospital.");
    description.setFont(new Font("Verdana", Font.PLAIN, 11));
    description.setForeground(Color.WHITE);
    description.setBackground(new Color(0, 0, 0, 180));
    description.setEditable(false);
    description.setLineWrap(true);
    description.setWrapStyleWord(true);
    description.setBorder(BorderFactory.createEmptyBorder(5,10,5,5));
    descriptionContainer.add(description);
    descriptionContainer.setBackground(new Color(0, 0, 0, 0));
    descriptionContainer.setBorder(new LineBorder(Color.BLACK, 3, false));
    return descriptionContainer;
  }

  private Symbol createFacilitySymbol() {
    PictureMarkerSymbol symbol = null;
    BufferedImage image = null;

    try {
      URL url = new URL("http://static.arcgis.com/images/Symbols/SafetyHealth/Hospital.png");
      image = ImageIO.read(url);
      symbol = new PictureMarkerSymbol(image);
      symbol.setSize(40, 40);
    }
    catch (Exception e) {
      System.err.println("unable to create picture marker symbol");
      e.printStackTrace();
      return new SimpleMarkerSymbol(Color.RED, 15, Style.DIAMOND);
    }
    return symbol;
  }

  /**
   * Creates a window.
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame("Closest Facility Application");
    window.setBounds(100, 100, 1000, 700);
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
