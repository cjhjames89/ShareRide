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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.gps.GPSEventListener;
import com.esri.core.gps.GPSException;
import com.esri.core.gps.GPSStatus;
import com.esri.core.gps.GeoPosition;
import com.esri.core.gps.IGPSWatcher;
import com.esri.core.gps.Satellite;
import com.esri.core.gps.SerialPortGPSWatcher;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.LayerList;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;

/***
 * This sample shows the use of GPS functionality using a {@link SerialPortGPSWatcher}.
 * A custom GPSEventListener is created which in this case responds to
 * position changes by overriding the <code>onPositionChanged</code> method.
 * When a new position is received, the location information is displayed
 * in a text area in the application, and the position is displayed on the map
 * as a graphic.
 */
public class GPSComApp {

  // base map
  private final String URL_USA_TOPO =
      "http://services.arcgisonline.com/ArcGIS/rest/services/ESRI_StreetMap_World_2D/MapServer";
  // JMap
  private JMap map;
  // progress bar
  private JProgressBar progressBar;
  // layer to add graphics for display
  private GraphicsLayer graphicsLayer;
  // GPS watcher
  private IGPSWatcher gpsWatcher;

  // UI elements
  private JComponent contentPane;
  private DefaultTableModel tblModelSatellites = new DefaultTableModel();
  private final JTable      tblSatellites      = new JTable(tblModelSatellites);
  private final JLabel      lblStatus          = new JLabel();
  private final JTextArea   txtLocationInfo    = new JTextArea("Location Info");
  private DefaultTableModel tblModelNMEASentences = new DefaultTableModel();
  private final JTable      tblNMEASentences   = new JTable(tblModelNMEASentences);
  private JButton stopButton;
  private JButton startButton;

  // ------------------------------------------------------------------------
  // Constructor
  // ------------------------------------------------------------------------
  public GPSComApp() {
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
    GraphicsLayer gpsLayer;
    Graphic       gpsPointGraphic;
    int           gpsPointGraphicId = 0;
    Point         prevPoint;

    // symbology
    SimpleMarkerSymbol SYM_GPS_POINT =
        new SimpleMarkerSymbol(new Color(200, 0, 0), 18, Style.X);
    SimpleLineSymbol SYM_GPS_TRAIL = new SimpleLineSymbol(new Color(200, 0, 0, 160), 2);

    public MyGeoPositionListener(JMap jMap, GraphicsLayer gpsLayer) {
      this.jMap     = jMap;
      this.gpsLayer = gpsLayer;
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
      if (gpsPointGraphicId != 0) {
        gpsLayer.removeGraphic(gpsPointGraphicId);
      }
      gpsPointGraphic = new Graphic(currPoint, SYM_GPS_POINT);
      SYM_GPS_POINT.setAngle((float) newPosition.getLocation().getCourse());
      gpsPointGraphicId = gpsLayer.addGraphic(gpsPointGraphic);

      if (prevPoint != null) {
        Polyline polyline = new Polyline();
        polyline.startPath(prevPoint);
        polyline.lineTo(currPoint);
        // show the new line by adding it to the graphics layer.
        Graphic polylineGraphic = new Graphic(polyline, SYM_GPS_TRAIL);
        gpsLayer.addGraphic(polylineGraphic);
      } else {
        jMap.setExtent(new Envelope(currPoint, 10, 10));
      }
      prevPoint = currPoint;
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
          GPSComApp gpsApp = new GPSComApp();

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
   * @return the UI for this sample.
   * @throws Exception in case of any error.
   */
  public JComponent createUI() throws Exception {
    // application content
    contentPane = new JPanel();
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

    // progress bar
    progressBar = createProgressBar(controlPanel);

    controlPanel.add(Box.createRigidArea(new Dimension(0,5)));
    controlPanel.add(topPanel);
    controlPanel.add(progressBar);
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
   * Creates a window.
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame("Serial Port GPS Application");
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
   * Creates a progress bar.
   * @param parent progress bar's parent. The horizontal axis of the progress bar will be
   * center-aligned to the parent.
   * @return a progress bar.
   */
  private static JProgressBar createProgressBar(final JComponent parent) {
    final JProgressBar progressBar = new JProgressBar();
    progressBar.setSize(260, 20);
    parent.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        progressBar.setLocation(
            parent.getWidth()/2 - progressBar.getWidth()/2,
            parent.getHeight() - progressBar.getHeight() - 20);
      }
    });
    progressBar.setStringPainted(true);
    progressBar.setIndeterminate(true);
    return progressBar;
  }

  /**
   * Updates progress bar UI from the Swing's Event Dispatch Thread.
   * @param str string to be set.
   * @param visible flag to indicate visibility of the progress bar.
   */
  private void updateProgressBarUI(final String str, final boolean visible) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (str != null) {
          progressBar.setString(str);
        }
        progressBar.setVisible(visible);
      }
    }); 
  }

  /**
   * Creates a map.
   * @return a map.
   */
  private JMap createMap() throws Exception {
    final JMap jMap = new JMap();
    jMap.setBackground(Color.WHITE);

    // add a tiled map service layer
    final ArcGISTiledMapServiceLayer baseLayer = new ArcGISTiledMapServiceLayer(URL_USA_TOPO);
    jMap.addMapEventListener(new MapEventListenerAdapter() {
      @Override
      public void mapReady(final MapEvent arg0) {
        Runnable findGPSTask = new Runnable() {
          @Override
          public void run() {
            GPSEventListener gpsListener = new MyGeoPositionListener(jMap, graphicsLayer);

            System.out.println("Searching for GPS port...");
            updateProgressBarUI("Searching for GPS port...", true);

            try {
              gpsWatcher = new SerialPortGPSWatcher(gpsListener);

              // enabled buttons if we get to here
              System.out.println("GPS port found.");
              startButton.setEnabled(true);
              stopButton.setEnabled(true);
              lblStatus.setText(gpsWatcher.getStatus().toString());
            } catch (Exception e) {
              // for displaying info to user
              JOptionPane.showMessageDialog(contentPane,
                  wrap(e.getLocalizedMessage()), "Exception", JOptionPane.ERROR_MESSAGE);
              e.printStackTrace();
            } finally {
              updateProgressBarUI(null, false);
            }
          }
        };
        Thread findGPSThread = new Thread(findGPSTask);
        findGPSThread.start();
      }
    });

    LayerList layers = jMap.getLayers();
    layers.add(baseLayer);

    // Graphics Layer to add GPS points
    graphicsLayer = new GraphicsLayer();
    layers.add(graphicsLayer);

    return jMap;
  }
  
  private String wrap(String str) {
    // create a HTML string that wraps text when longer
    return "<html><p style='width:200px;'>" + str + "</html>";
  }
}
