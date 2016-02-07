/* Copyright 2014 Esri

All rights reserved under the copyright laws of the United States
and applicable international laws, treaties, and conventions.

You may freely redistribute and use this sample code, with or
without modification, provided you include the original copyright
notice and use restrictions.

See the use restrictions.*/


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
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.RouteTask;
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
public class Client {

  private JMap map;
  private JComponent contentPane;
  private NAFeaturesAsFeature stops = new NAFeaturesAsFeature();
  private GraphicsLayer graphicsLayer;
  private DrawingOverlay myDrawingOverlay;
  private int numStops = 0;
  private int count = 0;
  private Graphic[] graphArray =  new Graphic[2];
  private SpatialReference sparticalReferencePass;
  private int n;
  private int time;
  private String setoutTime;

  
  private boolean preserveOrder = false;
  private JButton stopButton;

  // ------------------------------------------------------------------------
  // Constructor
  // ------------------------------------------------------------------------
  public Client(int option, int waitTime, String passtime) {
	n = option;
	time = waitTime;
	setoutTime = passtime;
  }

  // ------------------------------------------------------------------------
  // Core functionality
  // ------------------------------------------------------------------------

  private void sendServer() {
    try {
      sparticalReferencePass = map.getSpatialReference();
      ParamData params = new ParamData(graphArray, n, time, sparticalReferencePass,setoutTime);
      InfoSend sendObj = new InfoSend();
      sendObj.sendParams("172.20.10.4", 1543, params);
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(contentPane,
          wrap("An error has occured. "+ e.getLocalizedMessage()), "", JOptionPane.WARNING_MESSAGE);
    }
  }


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
	            numStops++;
	            if(count >=2) 
	        	  stopButton.setEnabled(false);         
	          
	        	else{
	        		stops.addFeature(graphic); //graphic is a stop
	        		graphArray[count] = graphic;
	          	          
	        		graphicsLayer.addGraphic(new Graphic(graphic.getGeometry(), new TextSymbol(12, String
	              .valueOf(numStops), Color.WHITE)));}
	        	}
	        	count++;
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
  
  public static String SetTime(){
		//dropdown list for month
	  String timeString = ""; //has every time information
	  String[] input1 = { "1", "2", "3", "4", "5", "6","7", "8", "9", "10", "11", "12" };
	  String timeMonth = (String) JOptionPane.showInputDialog(null, "Choose a month now",
		        "The Choice of a Month", JOptionPane.QUESTION_MESSAGE, null, 
		        input1, // Array of choices
		        input1[0]); // Initial choice
		    
	  timeString = timeString + timeMonth + ",";
	  System.out.println(timeString);
	//dropdown list for date
	  String[] input2 = { "1", "2", "3", "4", "5", "6","7", "8", "9", "10", "11", "12",
			  "13", "14", "15", "16", "17", "18","19", "20", "21", "22", "23", "24", "25",
			  "26", "27", "28", "29", "30", "31"};
	  String timeDate = (String) JOptionPane.showInputDialog(null, "Choose a date now",
		        "The Choice of a Date", JOptionPane.QUESTION_MESSAGE, null, 
		        input2, // Array of choices
		        input2[0]); // Initial choice
	  timeString = timeString + timeDate + ",";
	  System.out.println(timeString);
	//dropdown list for hour
	  String[] input3 = { "1am", "2am", "3am", "4am", "5am", "6am","7am", "8am", "9am", "10am", "11am", "12pm",
			  "1pm", "2pm", "3pm", "4pm", "5pm", "6pm","7pm", "8pm", "9pm", "10pm", "11pm", "12am"};
	  String timeHour = (String) JOptionPane.showInputDialog(null, "Choose a hour now",
		        "The Choice of a Hour", JOptionPane.QUESTION_MESSAGE, null, 
		        input3, // Array of choices
		        input3[0]); // Initial choice
	  timeString = timeString + timeHour + ",";
	  System.out.println(timeString);
	//dropdown list for minutes
	  
	  String[] input4 = {"0","10","20","30","40","50"};
	  String timeMinute = (String) JOptionPane.showInputDialog(null, "Choose a minute now",
		        "The Choice of a minute", JOptionPane.QUESTION_MESSAGE, null, 
		        input4, // Array of choices
		        input4[0]); // Initial choice
	  timeString = timeString + timeMinute;
	  
	  System.out.println(timeString);
	  return timeString;
	  
  }
  
  
  public static void main(String[] args) {
	  /* Hannah*/
	  
	  Object[] options = {"Driver",
              "Rider"};
	  final int waitTime;
	  final int option = JOptionPane.showOptionDialog(null,
			  "Are you a driver or a rider?",
			  "Who are you?",
			  JOptionPane.YES_NO_OPTION,
			  JOptionPane.QUESTION_MESSAGE,
			  null,
			  options, null);
	  if(option==0){
	  String userIn = JOptionPane.showInputDialog(null, "Tolerence time in minutes");
	  waitTime = Integer.parseInt(userIn);
	  	  
	  } else {
		waitTime = 0;
	}
  
	  //send parameter
	  //n is the client type
  
	  /* Hannah*/
	  
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          // instance of this application
          Client routingApp = new Client(option,waitTime, SetTime());

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

    // solve
    JButton routeButton = new JButton(" Send route ");
    routeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        sendServer();
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
        count = 0;
        numStops = 0;
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
