import java.awt.EventQueue;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.esri.runtime.ArcGISRuntime;
import com.esri.core.internal.tasks.ags.r;
import com.esri.core.internal.tasks.ags.t;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapOptions;
import com.esri.map.MapOptions.MapType;

public class Server extends Thread{

	private ServerSocket socket;
	private ObjectInputStream oin;
	private ParamData data;
  
	public Server(int port) throws IOException {
	      socket = new ServerSocket(port);
	      socket.setSoTimeout(50000);
	      data = new ParamData();
	}
	
	public static void main(String [] args) {
		int port = 1543;
		try {
			Thread thread = new Server(port);
			thread.start();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				System.out.println("Waiting for client...");
				Socket client = socket.accept();
				System.out.println("Client " + client.getRemoteSocketAddress() + " connected!");
				oin = new ObjectInputStream(client.getInputStream());
				data = (ParamData)oin.readObject();
				//TODO: Read ParamData Object
				//TODO: Build parameters by client type
				//TODO: Do routing
				//TODO: Compare time
				//TODO: print result
				
				client.close();
			} catch(SocketTimeoutException s) {
				System.out.println("Server timed out!");
				break;
			} catch(IOException e) {
				e.printStackTrace();
				break;
			} catch(ClassNotFoundException ce) {
				ce.printStackTrace();
			}
		}
	}
	
	private RouteParameters buildDriverParams() {
		
		return null;
	}
	
	private RouteParameters buildRiderParams() {
		
		return null;
	}
	
	
}
