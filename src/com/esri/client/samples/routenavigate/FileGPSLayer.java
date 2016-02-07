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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import com.esri.core.gps.FileGPSWatcher;
import com.esri.core.gps.IGPSWatcher;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.GPSLayer;
import com.esri.map.JMap;
import com.esri.map.LayerList;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.runtime.ArcGISRuntime;

/**
 * This sample shows how to use a {@link GPSLayer} with a {@link FileGPSWatcher}.
 */
public class FileGPSLayer {

  private static final String FSP = System.getProperty("file.separator");
  private JMap map;
  private IGPSWatcher gpsWatcher;

  // ------------------------------------------------------------------------
  // Constructor
  // ------------------------------------------------------------------------
  public FileGPSLayer() {
  }

  // ------------------------------------------------------------------------
  // Core functionality
  // ------------------------------------------------------------------------
  private JMap createMap() {

    JMap jMap = new JMap();
    jMap.setAnimationDuration(1.0f); // lengthen the animation duration to 1 second

    // add base layer
    ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
        "http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer");
    LayerList layers = jMap.getLayers();
    layers.add(tiledLayer);

    // create the FileGPSWatcher
    gpsWatcher =
        new FileGPSWatcher(getPathSampleData() + "gps" + FSP + "campus.txt", 500, true);
    // create the GPS layer and add to map
    GPSLayer gpsLayer = new GPSLayer(gpsWatcher);
    // option to customize the GPS layer symbology, such as setting the track point symbol
    gpsLayer.setTrackPointSymbol(
        new SimpleMarkerSymbol(new Color(200, 0, 0, 200), 10, SimpleMarkerSymbol.Style.CIRCLE));
    layers.add(gpsLayer);

    return jMap;
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
          FileGPSLayer fileGPSLayerApp = new FileGPSLayer();

          // create the UI, including the map, for the application.
          JFrame appWindow = fileGPSLayerApp.createWindow();
          appWindow.add(fileGPSLayerApp.createUI());
          appWindow.setVisible(true);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
  }

  // ------------------------------------------------------------------------
  // Public methods
  // ------------------------------------------------------------------------

  public JComponent createUI() {
    // application content
    JComponent contentPane = createContentPane();

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

  /**
   * Creates a window.
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame("File GPS Layer Application");
    window.setBounds(100, 100, 1000, 700);
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.getContentPane().setLayout(new BorderLayout(0, 0));
    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent windowEvent) {
        super.windowClosing(windowEvent);
        if (map != null) map.dispose();
        if (gpsWatcher != null) gpsWatcher.dispose();
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
}
