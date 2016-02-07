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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JButton;
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
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureSet;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.Symbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.tasks.na.ServiceAreaResult;
import com.esri.core.tasks.na.ServiceAreaTask;
import com.esri.core.tasks.na.ServiceAreaParameters;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;

/***
 * This sample shows how to find the service areas around a point, using
 * a {@link ServiceAreaTask}.  The default task parameters need to be 
 * obtained, modified as desired, and passed to the task's solve method. 
 * Task parameters include the facility locations, any barriers present 
 * (none shown in this sample), and the spatial reference of the 
 * facilities/barriers.
 * <p>
 * To use the sample, click on the map to place a facility graphic and
 * the task will be run using this point to find the service area
 * polygons with the breaks set at 1, 2, and 3 minutes.
 */
public class ServiceAreaApp {

  private JMap map;
  private JComponent contentPane;
  private NAFeaturesAsFeature facilities = new NAFeaturesAsFeature();
  private NAFeaturesAsFeature barriers = new NAFeaturesAsFeature();
  private GraphicsLayer graphicsLayer;
  private DrawingOverlay myDrawingOverlay;

  // for symbolizing polygons
  private static int NUM_BREAKS = 3;
  private Symbol[] symbols = new Symbol[NUM_BREAKS];
  private Symbol facilitySymbol;

  // ------------------------------------------------------------------------
  // Constructor
  // ------------------------------------------------------------------------
  public ServiceAreaApp() {
  }

  // ------------------------------------------------------------------------
  // Core functionality
  // ------------------------------------------------------------------------

  private void getServiceAreas() {

    ServiceAreaTask task = new ServiceAreaTask(
        "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/ServiceArea");

    ServiceAreaParameters parameters = new ServiceAreaParameters();
    facilities.setSpatialReference(map.getSpatialReference());
    parameters.setFacilities(facilities);
    parameters.setDefaultBreaks(new Double[]{new Double(1.0), new Double(2.0), new Double(3.0)});
    if (barriers.getFeatures().size() > 0) {
      barriers.setSpatialReference(map.getSpatialReference());
      parameters.setPolylineBarriers(barriers);
    }
    parameters.setOutSpatialReference(map.getSpatialReference());
    // solve task asynchronously so as to not freeze the UI
    task.solve(parameters, new CallbackListener<ServiceAreaResult>() {

      @Override
      public void onError(final Throwable e) {
        e.printStackTrace();
        SwingUtilities.invokeLater(new Runnable() {

          @Override
          public void run() {
            JOptionPane.showMessageDialog(contentPane,
                wrap("An error has occured. " + e.getLocalizedMessage()), "",
                JOptionPane.WARNING_MESSAGE);
          }
        });
      }

      @Override
      public void onCallback(final ServiceAreaResult result) {
        if (result != null) {
          SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
              showResult(result);
            }
          });
        }
      }
    });
  }

  /**
   * Displays the result polygons for our service areas by adding 
   * them to a graphics layer already in our map.
   * 
   * @param result service area result containing service area polygons
   */
  private void showResult(ServiceAreaResult result) {
    FeatureSet polygons = result.getServiceAreaPolygons();
    int i = 0;
    for (Graphic graphic : polygons.getGraphics()) {
      graphicsLayer.addGraphic(new Graphic(graphic.getGeometry(), symbols[i%3]));
      i++;
    }
  }

  /**
   * Creates the map, sets the initial extent, creates a graphics layer, symbols, 
   * and a drawing overlay to draw facilities and barriers.
   * 
   * @return a map.
   */
  private JMap createMap() {

    JMap jMap = new JMap();

    ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
        "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer");
    jMap.getLayers().add(tiledLayer);
    jMap.setExtent(new Envelope(-13053325, 3850746, -13027560, 3863262.94));

    // create symbols
    SimpleLineSymbol outline = new SimpleLineSymbol(Color.BLACK, 1.0f);
    symbols[0] = new SimpleFillSymbol(new Color(255, 255, 0, 60), outline); // yellow
    symbols[1] = new SimpleFillSymbol(new Color(255, 120, 0, 60), outline); // orange
    symbols[2] = new SimpleFillSymbol(new Color(255, 0, 0, 60), outline); // red
    facilitySymbol = createFacilitySymbol();

    // graphics layer
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
        if (graphic.getAttributeValue("type").equals("Facility")) {
          facilities.addFeature(graphic);
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
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          // instance of this application
          ServiceAreaApp routingApp = new ServiceAreaApp();

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
    JButton stopButton = new JButton(" Add a facility ");
    stopButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("type", "Facility");
        drawingOverlay.setUp(
            DrawingMode.POINT,
            facilitySymbol,
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
            DrawingMode.POLYLINE,
            new SimpleLineSymbol(Color.BLACK, 2.0f),
            attributes);
      }
    });
    toolBar.add(barrierButton);

    // solve
    JButton routeButton = new JButton(" Show service areas ");
    routeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getServiceAreas();
      }
    });
    toolBar.add(routeButton);

    // reset
    JButton resetButton = new JButton("  Reset  ");
    resetButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        graphicsLayer.removeAll();
        facilities.clearFeatures();
        barriers.clearFeatures();
      }
    });
    toolBar.add(resetButton);

    return toolBar;
  }

  private Symbol createFacilitySymbol() {
    PictureMarkerSymbol symbol = null;
    BufferedImage image = null;

    try {
      URL url = new URL("http://static.arcgis.com/images/Symbols/SafetyHealth/FireStation.png");
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
    JFrame window = new JFrame("Online Routing Application");
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
  private static JComponent createContentPane() {
    JComponent contentPane = new JPanel();
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
