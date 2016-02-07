
import java.awt.EventQueue;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.esri.runtime.ArcGISRuntime;
import com.esri.map.JMap;
import com.esri.map.MapOptions;
import com.esri.map.MapOptions.MapType;

public class Server {

  private JFrame window;
  private JMap map;

  public Server() {
    window = new JFrame();
    window.setSize(800, 600);
    window.setLocationRelativeTo(null); // center on screen
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.getContentPane().setLayout(new BorderLayout(0, 0));

    // dispose map just before application window is closed.
    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent windowEvent) {
        super.windowClosing(windowEvent);
        map.dispose();
      }
    });

    // Before this application is deployed you must register the application on 
    // http://developers.arcgis.com and set the Client ID in the application as shown 
    // below. This will license your application to use Basic level functionality.
    // 
    // If you need to license your application for Standard level functionality, please 
    // refer to the documentation on http://developers.arcgis.com
    //
    //ArcGISRuntime.setClientID("your Client ID");

    // Using MapOptions allows for a common online basemap to be chosen
    MapOptions mapOptions = new MapOptions(MapType.TOPO);
    map = new JMap(mapOptions);

    // If you don't use MapOptions, use the empty JMap constructor and add a tiled layer
    //map = new JMap();
    //ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
    //  "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer");
    //map.getLayers().add(tiledLayer);

    // Add the JMap to the JFrame's content pane
    window.getContentPane().add(map);

  }

  /**
   * Starting point of this application.
   * @param args
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {

      @Override
      public void run() {
        try {
          Server application = new Server();
          application.window.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
