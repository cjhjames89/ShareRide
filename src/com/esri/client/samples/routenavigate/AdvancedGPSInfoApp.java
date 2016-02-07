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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

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
 * A custom GPSEventListener is created which responds to position changes,
 * changes in the satellites in view, changes in the GPS Status, and new
 * NMEA sentences received. Each of these functions is performed by overriding
 * the corresponding method from {@link GPSEventListener}. The information
 * coming in via these methods is displayed in the application for the user
 * to see.
 */
public class AdvancedGPSInfoApp {

  // JMap
  private JMap map;
  // layer to add graphics for display
  private GraphicsLayer gpsLayer;
  // GPS watcher
  private IGPSWatcher gpsWatcher;
  // position changes are in WGS-84
  private final static SpatialReference WGS84 = SpatialReference.create(4326);
  private static final String FSP = System.getProperty("file.separator");

  // UI to display result
  private DefaultTableModel tblModelSatellites = new DefaultTableModel();
  private final JTable      tblSatellites      = new JTable(tblModelSatellites);
  private final JLabel      lblStatus          = new JLabel();
  private final JTextArea   txtLocationInfo    = new JTextArea("Location Info");
  private DefaultTableModel tblModelNMEASentences = new DefaultTableModel();
  private final JTable      tblNMEASentences   = new JTable(tblModelNMEASentences);
  private JButton stopButton;
  private JButton startButton;

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
  // Constructors
  // ------------------------------------------------------------------------
  public AdvancedGPSInfoApp() {
    tblModelSatellites.addColumn("ID");
    tblModelSatellites.addColumn("Elevation");
    tblModelSatellites.addColumn("Azimuth");
    tblModelSatellites.addColumn("Signal Strength");
    tblModelSatellites.addColumn("System");

    tblModelNMEASentences.addColumn("Sentences (max. 100, sorted latest first)");
    txtLocationInfo.setRows(15);
  }

  // ------------------------------------------------------------------------
  // Core functionality
  // ------------------------------------------------------------------------
  class MyGeoPositionListener implements GPSEventListener {

    JMap          jMap;
    GraphicsLayer graphicsLayer;
    Graphic       gpsPointGraphic;
    int           gpsPointGraphicId = 0;

    public MyGeoPositionListener(JMap jMap, GraphicsLayer gpsLayer) {
      this.jMap     = jMap;
      this.graphicsLayer = gpsLayer;
    }

    @Override
    public void onStatusChanged(GPSStatus newStatus) {
      lblStatus.setText(newStatus.toString());
    }

    @Override
    public void onNMEASentenceReceived(String newSentence) {
      tblModelNMEASentences.insertRow(0, new Object[] {newSentence});
      if (tblModelNMEASentences.getRowCount() > 100) {
        tblModelNMEASentences.setRowCount(50);
      }
    }
    @Override
    public void onSatellitesInViewChanged(Map<Integer, Satellite> satellitesInView) {
      tblModelSatellites.setRowCount(0);
      for (Satellite satellite : satellitesInView.values()) {
        tblModelSatellites.insertRow(0, new Object[] {
            Integer.valueOf(satellite.getId()),
            Integer.valueOf(satellite.getElevation()),
            Integer.valueOf(satellite.getAzimuth()),
            Integer.valueOf(satellite.getSignalStrength()),
            satellite.getSatelliteSystem()});
      }
    }

    @Override
    public void onPositionChanged(GeoPosition newPosition) {
      txtLocationInfo.setText(newPosition.toString());
      Point currPoint = new Point(
          newPosition.getLocation().getLongitude(),
          newPosition.getLocation().getLatitude());
      Point projectedPoint =
          (Point) GeometryEngine.project(currPoint, WGS84, jMap.getSpatialReference());
      if (gpsPointGraphicId != 0) {
        graphicsLayer.removeGraphic(gpsPointGraphicId);
      }
      gpsPointGraphic = new Graphic(projectedPoint, symPoint);
      symPoint.setAngle((float) newPosition.getLocation().getCourse());
      gpsPointGraphicId = graphicsLayer.addGraphic(gpsPointGraphic);
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
          AdvancedGPSInfoApp gpsApp = new AdvancedGPSInfoApp();

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
    JComponent contentPane = new JPanel();
    contentPane.setLayout(new BorderLayout(1, 0));
    contentPane.setBackground(Color.DARK_GRAY);

    // buttons
    startButton = new JButton("Start");
    startButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          gpsWatcher.start();
        } catch (GPSException ex) {
          ex.printStackTrace();
        }
      }
    });
    startButton.setEnabled(false);

    stopButton = new JButton("Stop");
    stopButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          gpsWatcher.stop();
        } catch (GPSException ex) {
          ex.printStackTrace();
        }
      }
    });
    stopButton.setEnabled(false);

    // group the above UI items into a panel
    final JPanel controlPanel = new JPanel();
    controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
    controlPanel.setPreferredSize(new Dimension(350, 500));

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
    topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    topPanel.add(startButton);
    topPanel.add(Box.createRigidArea(new Dimension(5,0)));
    topPanel.add(stopButton);
    topPanel.add(Box.createRigidArea(new Dimension(15,0)));
    topPanel.add(new JLabel("GPS Status: "));
    topPanel.add(Box.createRigidArea(new Dimension(5,0)));
    topPanel.add(lblStatus);

    controlPanel.add(Box.createRigidArea(new Dimension(0,5)));
    controlPanel.add(topPanel);
    controlPanel.add(new JLabel("Location Info: "));
    controlPanel.add(new JScrollPane(txtLocationInfo));
    controlPanel.add(new JLabel("Satellites In View: "));
    controlPanel.add(new JScrollPane(tblSatellites));
    controlPanel.add(new JLabel("NMEA Sentences: "));
    controlPanel.add(new JScrollPane(tblNMEASentences));

    // add the panel to the main window
    controlPanel.setVisible(true);
    contentPane.add(controlPanel, BorderLayout.WEST);

    // map
    map = createMap();
    contentPane.add(map, BorderLayout.CENTER);

    return contentPane;
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
    jMap.setBackground(Color.WHITE);

    // base layer from a tile package
    final ArcGISLocalTiledLayer baseLayer =
        new ArcGISLocalTiledLayer(getPathSampleData() + "tpks" + FSP + "Campus.tpk");
    jMap.addMapEventListener(new MapEventListenerAdapter() {
      @Override
      public void mapReady(final MapEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            GPSEventListener gpsListener = new MyGeoPositionListener(jMap, gpsLayer);
            // create the file GPS watcher
            gpsWatcher = new FileGPSWatcher(
                getPathSampleData() + "gps" + FSP + "campus.txt", 1000, true, gpsListener);
            // enable buttons
            startButton.setEnabled(true);
            stopButton.setEnabled(true);
          }
        });
      }
    });
    LayerList layers = jMap.getLayers();
    layers.add(baseLayer);

    // -----------------------------------------------------------------------------------------
    // Graphics Layer - to add GPS points
    // -----------------------------------------------------------------------------------------
    gpsLayer = new GraphicsLayer();
    layers.add(gpsLayer);

    return jMap;
  }

  /**
   * Creates a window.
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame("Advanced GPS Info Application");
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
}
