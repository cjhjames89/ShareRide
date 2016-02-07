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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import com.esri.core.gps.FileGPSWatcher;
import com.esri.core.gps.IGPSWatcher;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.GPSLayer;
import com.esri.map.GPSLayer.Mode;
import com.esri.map.JMap;
import com.esri.map.LayerList;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.runtime.ArcGISRuntime;

/**
 * This sample shows how to use the GPS modes of the {@link GPSLayer}, including 
 * Auto-pan and Navigation modes. The mode is set on the GPSLayer using 
 * {@link GPSLayer#setMode(Mode)}. For simplicity, the GPS layer is created using 
 * raw NMEA data stored in a text file.
 */
public class GPSLayerNavigation {

  private static final String FSP = System.getProperty("file.separator");
  private JMap map;
  private GPSLayer gpsLayer;
  private IGPSWatcher gpsWatcher;
  private final JTextField txtFactor = new JTextField(4);
  private final JTextField txtAutoFocusBoundary = new JTextField(4);

  // ------------------------------------------------------------------------
  // Constructor
  // ------------------------------------------------------------------------
  public GPSLayerNavigation() {
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
    gpsWatcher = new FileGPSWatcher(getPathSampleData() + "gps"
        + FSP + "campus.txt", 500, true);
    // create the GPS layer and add to map
    gpsLayer = new GPSLayer(gpsWatcher);
    gpsLayer.setMode(Mode.NAVIGATION);
    gpsLayer.setNavigationPointHeightFactor(0.3);
    txtFactor.setText("0.3");
    // option to customize the GPS layer symbology, such as setting the
    // track point symbol
    gpsLayer.setTrackPointSymbol(new SimpleMarkerSymbol(new Color(200, 0,
        0, 200), 10, SimpleMarkerSymbol.Style.CIRCLE));
    layers.add(gpsLayer);

    return jMap;
  }

  // ------------------------------------------------------------------------
  // Static methods
  // ------------------------------------------------------------------------
  /**
   * Starting point of this application.
   *
   * @param args
   *            arguments to this application.
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          // instance of this application
          GPSLayerNavigation gpsApp = new GPSLayerNavigation();

          // create the UI, including the map, for the application.
          JFrame appWindow = gpsApp.createWindow();
          appWindow.add(gpsApp.createUI());
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
  /**
   * Creates and displays the UI, including the map, for this application.
   * Adds listeners to the buttons created in order to pan, zoom, and rotate
   * the map.
   */
  public JComponent createUI() {
    // application content
    final JComponent contentPane = createContentPane();
    Font font = new Font("Dialog", Font.PLAIN, 13);

    // map
    map = createMap();

    // radio buttons
    JRadioButton btnOff = new JRadioButton("Off", false);
    btnOff.setFont(font);
    btnOff.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          txtAutoFocusBoundary.setEnabled(false);
          txtFactor.setEnabled(false);
          gpsLayer.setMode(Mode.OFF);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
    btnOff.setForeground(Color.WHITE);
    btnOff.setBackground(Color.BLACK);

    JRadioButton btnAutoPan = new JRadioButton("Auto-pan", false);
    btnAutoPan.setFont(font);
    btnAutoPan.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          txtAutoFocusBoundary.setEnabled(true);
          txtFactor.setEnabled(false);
          gpsLayer.setMode(Mode.AUTOPAN);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
    btnAutoPan.setForeground(Color.WHITE);
    btnAutoPan.setBackground(Color.BLACK);

    JRadioButton btnNavigation = new JRadioButton("Navigation", true);
    btnNavigation.setFont(font);
    btnNavigation.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          txtAutoFocusBoundary.setEnabled(false);
          txtFactor.setEnabled(true);
          gpsLayer.setMode(Mode.NAVIGATION);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
    btnNavigation.setForeground(Color.WHITE);
    btnNavigation.setBackground(Color.BLACK);

    // logical grouping of radio buttons
    ButtonGroup btnGroup = new ButtonGroup();
    btnGroup.add(btnOff);
    btnGroup.add(btnAutoPan);
    btnGroup.add(btnNavigation);

    // auto-focus boundary
    JLabel boundaryLabel = new JLabel(" Auto-pan boundary: ");
    boundaryLabel.setFont(font);
    boundaryLabel.setForeground(Color.WHITE);
    boundaryLabel.setBackground(Color.BLACK);
    txtAutoFocusBoundary
    .setText(Integer.toString(gpsLayer.getAutoFocusBoundary()));
    txtAutoFocusBoundary.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          int boundary = Integer.parseInt(txtAutoFocusBoundary.getText());
          if (1 <= boundary && boundary <= 49) {
            gpsLayer.setAutoFocusBoundary(boundary);
          } else {
            JOptionPane.showMessageDialog(contentPane,
                "Please enter a value from 1 to 49.", "",
                JOptionPane.WARNING_MESSAGE);
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
    txtAutoFocusBoundary.setEnabled(false);

    // navigation point height
    JLabel navLabel = new JLabel(" Navigation point height: ");
    navLabel.setFont(font);
    navLabel.setForeground(Color.WHITE);
    navLabel.setBackground(Color.BLACK);
    txtFactor.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          double factor = Double.parseDouble(txtFactor.getText());
          if (0 <= factor && factor <= 1) {
            gpsLayer.setNavigationPointHeightFactor(factor);
          } else {
            JOptionPane.showMessageDialog(contentPane,
                "Please enter a value from 0 to 1.", "",
                JOptionPane.WARNING_MESSAGE);

          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });

    // rotate buttons
    JButton btnRotateClockwise = new JButton("30\u02da\u21b7");
    btnRotateClockwise.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          map.setRotation(map.getRotation() - 30);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });

    JButton btnRotateAntiClockwise = new JButton("30\u02da\u21b6");
    btnRotateAntiClockwise.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          map.setRotation(map.getRotation() + 30);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });

    JPanel navPointPanel = new JPanel();
    navPointPanel.setLayout(new BorderLayout());
    navPointPanel.add(navLabel, BorderLayout.WEST);
    navPointPanel.add(txtFactor, BorderLayout.EAST);
    navPointPanel.setBackground(Color.BLACK);

    JPanel boundaryPanel = new JPanel();
    boundaryPanel.setLayout(new BorderLayout());
    boundaryPanel.add(boundaryLabel, BorderLayout.WEST);
    boundaryPanel.add(txtAutoFocusBoundary, BorderLayout.EAST);
    boundaryPanel.setBackground(Color.BLACK);

    JLabel modeLabel = new JLabel("GPS Layer Modes", SwingConstants.CENTER);
    modeLabel.setFont(new Font("Dialog", Font.BOLD, 16));
    modeLabel.setForeground(Color.BLACK);
    modeLabel.setBackground(Color.WHITE);

    // Put the radio buttons and text boxes in a panel.
    JPanel radioPanel = new JPanel();
    radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
    radioPanel.setBackground(Color.BLACK);
    btnOff.setAlignmentX(Component.LEFT_ALIGNMENT);
    radioPanel.add(btnOff);
    radioPanel.add(Box.createVerticalStrut(8));
    btnAutoPan.setAlignmentX(Component.LEFT_ALIGNMENT);
    radioPanel.add(btnAutoPan);
    boundaryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    radioPanel.add(boundaryPanel);
    radioPanel.add(Box.createVerticalStrut(8));
    btnNavigation.setAlignmentX(Component.LEFT_ALIGNMENT);
    radioPanel.add(btnNavigation);
    navPointPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    radioPanel.add(navPointPanel);

    final JPanel rotationPanel = new JPanel();
    rotationPanel.setLayout(new BorderLayout());
    rotationPanel.setSize(40, 80);
    rotationPanel.add(btnRotateClockwise, BorderLayout.CENTER);
    rotationPanel.add(btnRotateAntiClockwise, BorderLayout.SOUTH);
    rotationPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    rotationPanel.setBackground(new Color(0, 0, 0, 100));

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BorderLayout());
    topPanel.setBackground(Color.WHITE);
    topPanel.setLocation(10, 10);
    topPanel.setSize(220, 235);
    topPanel.setBorder(new LineBorder(Color.BLACK, 3));

    topPanel.add(modeLabel, BorderLayout.NORTH);
    topPanel.add(radioPanel, BorderLayout.CENTER);
    topPanel.add(rotationPanel, BorderLayout.SOUTH);

    contentPane.add(topPanel);
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
   *
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
   *
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame("GPS Layer Modes");
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
      if (!(javaPath.endsWith("/") || javaPath.endsWith("\\"))) {
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
