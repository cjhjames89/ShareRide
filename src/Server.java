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
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapOptions;
import com.esri.map.MapOptions.MapType;

public class Server extends Thread{

	private ServerSocket socket;
	private ObjectInputStream oin;
	private ParamData driver;
	private double driverTime;
	private RouteParameters parameters;
	private RouteResult result;
	private RouteTask task;
  
	public Server(int port) throws IOException, Exception {
		task = RouteTask.createOnlineRouteTask("http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route", null);
		driverTime = 0;
		socket = new ServerSocket(port);
	    socket.setSoTimeout(50000);
	}
	
	public static void main(String [] args) {
		int port = 1543;
		try {
			Thread thread = new Server(port);
			thread.start();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(Exception e) {
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
				ParamData data = (ParamData)oin.readObject();
				if(data.getClientType() == 0) { //Driver
					System.out.println("Driver data received!!!");
					parameters = buildDriverParams(data);
					driver = data;
				} else { //Rider
					System.out.println("Rider data received!!!");
					parameters = buildParams(data);
				}
				//Do routing
				result = task.solve(parameters);
				//Compare time and print result
				if(data.getClientType() == 1) {
					double totalTime = getTotalTime(result);
					printResult(totalTime);
				} else {
					driverTime = getTotalTime(result);
					System.out.println("Driver's driving time = " + driverTime);
				}
				client.close();
			} catch(SocketTimeoutException s) {
				System.out.println("Server timed out!");
				break;
			} catch(IOException e) {
				e.printStackTrace();
				break;
			} catch(ClassNotFoundException ce) {
				ce.printStackTrace();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private RouteParameters buildDriverParams(ParamData data) throws Exception {
		driverTime = data.gettimeTolerance();
		return buildParams(data);
	}
	
	private RouteParameters buildParams(ParamData data) throws Exception {
		RouteTask task = RouteTask.createOnlineRouteTask(
		          "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route", null);
	    parameters = task.retrieveDefaultRouteTaskParameters();
	    parameters.setOutSpatialReference(data.getsPf());
	    NAFeaturesAsFeature stops = new NAFeaturesAsFeature();
	    for(Graphic stop : data.getStops()) {
	    	stops.addFeature(stop);
	    	System.out.println(stop);
	    }
	    parameters.setStops(stops);
	    parameters.setFindBestSequence(false);
		return parameters;
	}
	
	private double getTotalTime(RouteResult result) {
		String str = result.toString().split("Minutes=")[1];
		System.out.println(str);
		return Double.valueOf(str.split("]")[0]);
	}
	
	private void printResult(double totalTime) {
		System.out.println("************************************************************");
		if(totalTime - driverTime < driver.gettimeTolerance())
			System.out.println("\tThe driver can share ride!");
		else
			System.out.println();
		System.out.println("************************************************************");
	}
}
