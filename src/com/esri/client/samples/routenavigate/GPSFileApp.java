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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import com.esri.client.local.ArcGISLocalTiledLayer;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.gps.FileGPSWatcher;
import com.esri.core.gps.GPSEventListener;
import com.esri.core.gps.GPSException;
import com.esri.core.gps.GPSStatus;
import com.esri.core.gps.GeoPosition;
import com.esri.core.gps.IGPSWatcher;
import com.esri.core.gps.Satellite;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.MarkerSymbol;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.map.GPSLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.LayerList;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.runtime.ArcGISRuntime;

/***
 * This sample shows the use of GPS functionality using a {@link FileGPSWatcher}.
 * A custom GPSEventListener is created which in this case responds to
 * position changes by overriding the <code>onPositionChanged</code> method.
 * When a new position is received, the location information is displayed
 * in a text area in the application, and the position is displayed on the map
 * as a graphic.
 */
public class GPSFileApp {

  // JMap
  private JMap map;
  // layer to add graphics for display
  private GraphicsLayer graphicsLayer;
  // GPS watcher
  private IGPSWatcher gpsWatcher;
  // position changes are in WGS-84
  private final static SpatialReference WGS84 = SpatialReference.create(4326);
  // file separator
  private static final String FSP = System.getProperty("file.separator");
  // text area for location information
  private final JTextArea txtLocationInfo = new JTextArea("Press 'start' to start the File GPS Watcher.\n" +
      "The location information will display here.");

  // symbology
  private static MarkerSymbol symPoint;
  static {
    InputStream imageStream = GPSLayer.class.getResourceAsStream("resources/arrow.png");
    try {
      BufferedImage image = ImageIO.read(imageStream);
      symPoint = new PictureMarkerSymbol(image);
    } catch (Exception e) {
      symPoint = new SimpleMarkerSymbol(new Color(200, 0, 0), 15, Style.DIAMOND);
      System.err.println("unable to create picture marker symbol");
    } finally {
      try {
        imageStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // ------------------------------------------------------------------------
  // Constructor
  // ------------------------------------------------------------------------
  public GPSFileApp() {

  }

  // ------------------------------------------------------------------------
  // Core functionality
  // ------------------------------------------------------------------------

  private void createGPSWatcher() {
    // create the custom GPS listener
    GPSEventListener gpsListener = new MyGeoPositionListener(map, graphicsLayer);
    // create the file-based GPS watcher using this listener
    gpsWatcher = new FileGPSWatcher(
        getPathSampleData() + "gps" + FSP + "campus.txt", 500, true, gpsListener);
  }

  class MyGeoPositionListener implements GPSEventListener {

    JMap          jMap;
    GraphicsLayer gpsLayer;
    Graphic       gpsPointGraphic;
    int           gpsPointGraphicId = 0;

    public MyGeoPositionListener(JMap jMap, GraphicsLayer gpsLayer) {
      this.jMap     = jMap;
      this.gpsLayer = gpsLayer;
    }

    @Override
    public void onStatusChanged(GPSStatus newStatus) {
    }

    @Override
    public void onNMEASentenceReceived(String newSentence) {
    }

    @Override
    public void onSatellitesInViewChanged(Map<Integer, Satellite> satellitesInView) {
    }

    @Override
    public void onPositionChanged(GeoPosition newPosition) {

      // display the location information in our text area
      txtLocationInfo.setText(newPosition.toString());

      // plot the new position on the map
      Point currPoint = new Point(
          newPosition.getLocation().getLongitude(),
          newPosition.getLocation().getLatitude());
      Point projectedPoint =
          (Point) GeometryEngine.project(currPoint, WGS84, jMap.getSpatialReference());
      if (gpsPointGraphicId != 0) {
        gpsLayer.removeGraphic(gpsPointGraphicId);
      }
      gpsPointGraphic = new Graphic(projectedPoint, symPoint);
      symPoint.setAngle((float) newPosition.getLocation().getCourse());
      gpsPointGraphicId = gpsLayer.addGraphic(gpsPointGraphic);
    }
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
          GPSFileApp gpsApp = new GPSFileApp();

          // create the UI, including the map, for the application.
          JFrame appWindow = gpsApp.createWindow();
          appWindow.add(gpsApp.createUI());
          appWindow.setVisible(true);
        } catch (Exception e) {
          // on any error, display the stack trace.
          e.printStackTrace();
        }
      }
    });
  }
  // ------------------------------------------------------------------------
  // Public methods
  // ------------------------------------------------------------------------
  /**
   * Creates and displays the UI, including the map, for this application.
   */
  public JComponent createUI() throws Exception {
    // application content
    JLayeredPane contentPane = createContentPane();

    // buttons
    final JButton btnStart = new JButton("Start");
    btnStart.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          gpsWatcher.start();
        } catch (GPSException ex) {
          ex.printStackTrace();
        }
      }
    });

    final JButton btnStop = new JButton("Stop");
    btnStop.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          gpsWatcher.stop();
        } catch (GPSException ex) {
          ex.printStackTrace();
        }
      }
    });

    // group the above UI items into a panel
    final JInternalFrame controlPanel = new JInternalFrame("Controls", true, false, true, true);
    BoxLayout boxLayout = new BoxLayout(controlPanel.getContentPane(), BoxLayout.Y_AXIS);
    controlPanel.getContentPane().setLayout(boxLayout);
    controlPanel.setLocation(10, 10);
    controlPanel.setSize(300, 250);
    controlPanel.setBorder(new LineBorder(Color.BLACK, 1, false));

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
    topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    topPanel.add(btnStart);
    topPanel.add(Box.createRigidArea(new Dimension(5,0)));
    topPanel.add(btnStop);

    controlPanel.getContentPane().add(topPanel);
    controlPanel.getContentPane().add(new JLabel("Location info: "));
    controlPanel.getContentPane().add(new JScrollPane(txtLocationInfo));

    // add the panel to the main window
    controlPanel.setVisible(true);
    contentPane.add(controlPanel);

    // map
    map = createMap();
    contentPane.add(map);

    return contentPane;
  }

  /**
   * This method is invoked reflectively by the application to view samples.
   * Ideally, this should be done in {@link MapEventListenerAdapter#mapDispose(MapEvent)}.
   */
  public void dispose() {
    try {
      if (gpsWatcher != null) {
        gpsWatcher.dispose();
      }
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }

  // ------------------------------------------------------------------------
  // Private methods
  // ------------------------------------------------------------------------
  /**
   * Creates a map.
   * @return a map.
   */
  private JMap createMap() throws Exception {
    final JMap jMap = new JMap();
    // -----------------------------------------------------------------------------------------
    // Base Layer - with US topology, focus on U.S by default
    // -----------------------------------------------------------------------------------------
    final ArcGISLocalTiledLayer baseLayer =
        new ArcGISLocalTiledLayer(getPathSampleData() + "tpks" + FSP + "Campus.tpk");
    jMap.addMapEventListener(new MapEventListenerAdapter() {
      @Override
      public void mapReady(final MapEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            createGPSWatcher();
          }
        });
      }
    });
    LayerList layers = jMap.getLayers();
    layers.add(baseLayer);

    // -----------------------------------------------------------------------------------------
    // Graphics Layer - to add GPS points
    // -----------------------------------------------------------------------------------------
    graphicsLayer = new GraphicsLayer();
    layers.add(graphicsLayer);

    return jMap;
  }

  /**
   * Creates a window.
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame("File GPS Application");
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
        if (gpsWatcher != null) {
          gpsWatcher.dispose();
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
    contentPane.setLayout(new BorderLayout(0, 0));
    contentPane.setVisible(true);
    return contentPane;
  }

  private String getPathSampleData() {
    String dataPath = null;
    String javaPath = ArcGISRuntime.getInstallDirectory();
    if (javaPath != null) {
      if (!(javaPath.endsWith("/") || javaPath.endsWith("\\"))){
        javaPath += FSP;
      }
      dataPath = javaPath + "sdk" + FSP + "samples" + FSP + "data" + FSP;
    }
    File dataFile = new File(dataPath);
    if (!dataFile.exists()) { 
      dataPath = ".." + FSP + "data" + FSP;
    }
    return dataPath;
  }
}
