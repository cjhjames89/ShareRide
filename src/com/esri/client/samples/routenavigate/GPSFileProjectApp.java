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
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.gps.FileGPSWatcher;
import com.esri.core.gps.FixStatus;
import com.esri.core.gps.GPSEventListener;
import com.esri.core.gps.GPSException;
import com.esri.core.gps.GPSStatus;
import com.esri.core.gps.GeoPosition;
import com.esri.core.gps.IGPSWatcher;
import com.esri.core.gps.Satellite;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.LayerList;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;

import java.awt.Font;
import java.io.File;

import javax.swing.BorderFactory;

import com.esri.core.geometry.Polyline;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.runtime.ArcGISRuntime;

/**
 * This application shows how to display GPS points in a map by projecting
 * them to the map's spatial reference on the fly. It also shows how to make
 * use of the GPS status (<code>GPSStatus</code>), referring to the status
 * of the GPS device, and the fix status (<code>FixStatus</code>), which
 * provides information about the quality of the signal the device may be
 * receiving.
 * <p>
 * GPS data is received as latitude and longitude coordinates and is stored
 * in a <code>GeoPosition</code> in decimal degrees, in the wgs84 spatial
 * reference.  In order to display the point and trail graphics in our map
 * we reproject the point and polyline to the map's spatial reference using
 * the static method <code>GeometryEngine.project</code>.
 */
public class GPSFileProjectApp {

  // resources
  private final String BASE_LAYER =
      "http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer";

  // the spatial reference that our GPS points come through as
  private static SpatialReference wgs84 = SpatialReference.create(4326);
  // to store the map's spatial reference which is set by the first layer added
  private SpatialReference spatialRefMap;

  // JMap
  private JMap map;
  // layer to add graphics for display
  private GraphicsLayer graphicsLayer;
  // GPS watcher
  private IGPSWatcher gpsWatcher;

  // UI elements and constants
  private JButton startButton;
  private JButton stopButton;
  private JTextArea gpsStatusArea;
  private JTextArea fixStatusArea;
  private static final int BUTTON_WIDTH = 170;
  private static final int BUTTON_HEIGHT = 25;
  private static final int PANEL_WIDTH = 100;
  private static final int PANEL_HEIGHT = 65;
  private static final String FSP = System.getProperty("file.separator");

  // settings for our FileGPSWatcher
  private boolean loopPlayback = false;
  private int playbackRate = 400; // in milliseconds

  // ------------------------------------------------------------------------
  // Constructor
  // ------------------------------------------------------------------------
  public GPSFileProjectApp() {

  }

  // ------------------------------------------------------------------------
  // Core functionality
  // ------------------------------------------------------------------------
  class MyGeoPositionListener implements GPSEventListener {

    JMap          jMap;
    GraphicsLayer gpsLayer;
    Graphic       gpsPointGraphic;
    int           gpsPointGraphicId = 0;
    Point         prevPoint;
    boolean       wasInvalid = false;

    // symbology
    SimpleMarkerSymbol SYM_GPS_POINT =
        new SimpleMarkerSymbol(new Color(240, 0, 0), 10, Style.CIRCLE);
    SimpleLineSymbol SYM_GPS_TRAIL = new SimpleLineSymbol(new Color(200, 0, 0, 160), 2);
    SimpleLineSymbol SYM_GPS_TRAIL_UNSURE = new SimpleLineSymbol(Color.LIGHT_GRAY, 2);

    public MyGeoPositionListener(JMap jMap, GraphicsLayer gpsLayer) {
      this.jMap     = jMap;
      this.gpsLayer = gpsLayer;
      SYM_GPS_TRAIL_UNSURE.setStyle(SimpleLineSymbol.Style.DASH_DOT_DOT);
    }

    @Override
    public void onStatusChanged(GPSStatus newStatus) {
      gpsStatusArea.setText(newStatus.toString());
    }

    @Override
    public void onNMEASentenceReceived(String newSentence) {
    }

    @Override
    public void onSatellitesInViewChanged(Map<Integer, Satellite> satellitesInView) {
    }

    @Override
    public void onPositionChanged(GeoPosition newPosition) {

      // create a Point geometry from incoming long/lat coordinates
      Point currPoint = new Point(
          newPosition.getLocation().getLongitude(),
          newPosition.getLocation().getLatitude());

      // get the associated fix status
      FixStatus fixStatus = newPosition.getLocation().getFixStatus();
      fixStatusArea.setText(fixStatus.toString());

      // only plot point/track if the new position has a valid fix status
      if (fixStatus != FixStatus.INVALID) {
        if (gpsPointGraphicId != 0) {
          gpsLayer.removeGraphic(gpsPointGraphicId);
        }

        // project point, if base layer spatial reference differs from wgs84
        Point pointProj = (Point) GeometryEngine.project(currPoint, wgs84, spatialRefMap);
        gpsPointGraphic = new Graphic(pointProj, SYM_GPS_POINT);
        SYM_GPS_POINT.setAngle((float) newPosition.getLocation().getCourse());

        if (prevPoint != null) {

          // auto-pan if point is outside of current extent
          if (!jMap.getExtent().contains(pointProj)) {
            jMap.panTo(pointProj);
          }

          // draw the latest GPS track as a polyline
          Polyline polyline = new Polyline();
          polyline.startPath(prevPoint);
          polyline.lineTo(currPoint);
          Polyline polylineProj = (Polyline) GeometryEngine.project(polyline, wgs84, spatialRefMap);

          // show the projected polyline by adding it to the graphics layer.
          Graphic polylineGraphic;
          if (wasInvalid) {
            polylineGraphic = new Graphic(polylineProj, SYM_GPS_TRAIL_UNSURE);
          } else {
            polylineGraphic = new Graphic(polylineProj, SYM_GPS_TRAIL);
          }
          gpsLayer.addGraphic(polylineGraphic);
        }
        else {
          // for first point, set extent
          jMap.zoomTo(new Envelope(pointProj, 1000, 1000));
        }

        prevPoint = currPoint;
        gpsPointGraphicId = gpsLayer.addGraphic(gpsPointGraphic);
        wasInvalid = false;
      }
      else {
        // Fix status was invalid
        wasInvalid = true;
      }
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
          GPSFileProjectApp gpsApp = new GPSFileProjectApp();

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

    // UI panels
    final JPanel buttonPanel = createButtonPanel();
    final JPanel statusPanel = createStatusPanel();

    // add the panels to the main window
    contentPane.add(buttonPanel);
    contentPane.add(statusPanel);

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

    // default extent to Edinburgh, Scotland
    jMap.setExtent(new Envelope(-377278, 7533440, -339747, 7558472.79));

    // -----------------------------------------------------------------------------------------
    // Base Layer
    // -----------------------------------------------------------------------------------------
    final ArcGISTiledMapServiceLayer baseLayer = new ArcGISTiledMapServiceLayer(BASE_LAYER);

    jMap.addMapEventListener(new MapEventListenerAdapter() {
      @Override
      public void mapReady(final MapEvent mapEvent) {
        SwingUtilities.invokeLater(new Runnable() {

          @Override
          public void run() {
            try {
              spatialRefMap = mapEvent.getMap().getSpatialReference();

              // create the custom GPSEventListener and feed it to the file watcher
              GPSEventListener gpsListener = new MyGeoPositionListener(jMap, graphicsLayer);
              gpsWatcher = new FileGPSWatcher(
                  getPathSampleData() + "gps" + FSP + "leith.txt", playbackRate, loopPlayback, gpsListener);
              startButton.setEnabled(true);
              stopButton.setEnabled(true);
            }
            catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        });
      }

    });
    LayerList layers = jMap.getLayers();
    layers.add(baseLayer);

    // -----------------------------------------------------------------------------------------
    // Graphics Layer - to add GPS points / track
    // -----------------------------------------------------------------------------------------
    graphicsLayer = new GraphicsLayer();
    layers.add(graphicsLayer);

    return jMap;
  }

  private JPanel createButtonPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setSize(PANEL_WIDTH, PANEL_HEIGHT);
    panel.setLocation(10, 10);

    // buttons
    startButton = createStartButton();
    stopButton = createStopButton();

    // layout all the components together into a panel
    panel.setBackground(new Color(0, 0, 0, 120));
    panel.add(startButton);
    panel.add(Box.createRigidArea(new Dimension(0,5)));
    panel.add(stopButton);
    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    return panel;
  }

  private JPanel createStatusPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setSize(180, PANEL_HEIGHT);
    panel.setLocation(PANEL_WIDTH + 20, 10);

    // status
    JPanel gpsStatusPanel = createGPSStatusPanel();
    JPanel fixStatusPanel = createFixStatusPanel();

    // layout all the components together into a panel
    panel.setBackground(new Color(0, 0, 0, 120));
    panel.add(gpsStatusPanel);
    panel.add(Box.createRigidArea(new Dimension(0,5)));
    panel.add(fixStatusPanel);
    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    return panel;
  }

  private JPanel createGPSStatusPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.setMaximumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
    panel.setMinimumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.setBackground(new Color(0, 0, 0, 0));
    JLabel ttlStatus = new JLabel("> GPS Status: ");
    ttlStatus.setFont(new Font("Monospaced", Font.BOLD, 12));
    ttlStatus.setForeground(Color.WHITE);
    gpsStatusArea = new JTextArea();
    gpsStatusArea.setFont(new Font("Monospaced", Font.BOLD, 14));
    gpsStatusArea.setEditable(false);
    panel.add(ttlStatus);
    panel.add(gpsStatusArea);

    return panel;
  }

  private JPanel createFixStatusPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.setMaximumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
    panel.setMinimumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.setBackground(new Color(0, 0, 0, 0));
    JLabel ttlStatus = new JLabel("> Fix Status: ");
    ttlStatus.setFont(new Font("Monospaced", Font.BOLD, 12));
    ttlStatus.setForeground(Color.WHITE);
    fixStatusArea = new JTextArea();
    fixStatusArea.setFont(new Font("Monospaced", Font.BOLD, 14));
    fixStatusArea.setEditable(false);
    panel.add(ttlStatus);
    panel.add(fixStatusArea);

    return panel;
  }

  private JButton createStartButton() {
    JButton button = new JButton("Start");
    button.setMaximumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
    button.setMinimumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          gpsWatcher.start();
        } catch (GPSException ex) {
          ex.printStackTrace();
        }
      }
    });
    button.setEnabled(false);
    return button;
  }

  private JButton createStopButton() {
    JButton button = new JButton("Stop");
    button.setMaximumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
    button.setMinimumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          gpsWatcher.stop();
        } catch (GPSException ex) {
          ex.printStackTrace();
        }
      }
    });
    button.setEnabled(false);
    return button;
  }

  /**
   * Creates a window.
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame("GPS File Application: GPS Status and Project");
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
