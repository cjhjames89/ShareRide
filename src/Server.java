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
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	private List<ParamData> result;
	private RouteTask task;
	private Map<String, List<ParamData>> driverPool;
	private Map<String, List<ParamData>> riderPool;
  
	public Server(int port) throws IOException, Exception {
		task = RouteTask.createOnlineRouteTask("http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route", null);
		driverPool = new HashMap<String, List<ParamData>>();
		riderPool = new HashMap<String, List<ParamData>>();
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
				System.out.println("-----------------------------------------------------------------------------------");
				System.out.println("Waiting for client...");
				Socket client = socket.accept();
				System.out.println("Client " + client.getRemoteSocketAddress() + " connected!");
				oin = new ObjectInputStream(client.getInputStream());
				ParamData data = (ParamData)oin.readObject();
				int type = data.getClientType();
				if(data.getClientType() == 0) //Driver
					System.out.println("Driver data received!");
				else //Rider
					System.out.println("Rider data received!");
				
				System.out.println("****************************************************************************");
				result = searchMatch(type, data);
				if(result.size() == 0) {
					addToPool(type, data);
					System.out.println("\tNo match yet! But you'll be notified when there's a match.");
				} else if(data.getClientType() == 0) 
					System.out.println("\tThank you! A rider can ride your car!");
				else 
					System.out.println("\tGreat! A driver is able to share ride with you!");
				System.out.println("****************************************************************************");
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
	
	private void addToPool(int type, ParamData data) {
		Map<String, List<ParamData>> pool = type == 0 ? driverPool : riderPool;
		String time = data.getDate();
		if(!pool.containsKey(time)) {
			List<ParamData> list = new ArrayList<ParamData>();
			list.add(data);
			pool.put(time, list);
		} else
			pool.get(time).add(data);
	}
	
	public List<ParamData> searchMatch(int type, ParamData data) throws Exception {
		RouteResult result;
		RouteParameters parameters;
		ParamData driver;
		if(type == 0) {
			parameters = buildSingleParams(data);
			result = task.solve(parameters);
			data.setDrivingTime(getDrivingTime(result));
		}
		List<ParamData> otherPool = type == 0 ? riderPool.get(data.getDate()) : driverPool.get(data.getDate());
		List<ParamData> candidates = new ArrayList<ParamData>();
		if(otherPool == null)
			return candidates;
		for(ParamData p : otherPool) {
			if(type == 0) {
				parameters = buildMergedParams(data, p);
				driver = data;
			} else {
				parameters = buildMergedParams(p, data);
				driver = p;
			}
			result = task.solve(parameters);
			if(isSatisfiable(getDrivingTime(result), driver)) {
				candidates.add(p);
			}
		}
		return candidates;
	}
	
	private RouteParameters buildSingleParams(ParamData data) throws Exception {
		RouteParameters parameters = task.retrieveDefaultRouteTaskParameters();
	    parameters.setOutSpatialReference(data.getsPf());
	    NAFeaturesAsFeature stops = new NAFeaturesAsFeature();
	    for(Graphic stop : data.getStops()) {
	    	stops.addFeature(stop);
	    }
	    stops.setSpatialReference(data.getsPf());
	    parameters.setStops(stops);
	    parameters.setFindBestSequence(false);
		return parameters;
	}
	
	private RouteParameters buildMergedParams(ParamData driver, ParamData rider) throws Exception {
		RouteParameters parameters = task.retrieveDefaultRouteTaskParameters();
	    parameters.setOutSpatialReference(driver.getsPf());
	    NAFeaturesAsFeature stops = new NAFeaturesAsFeature();
	    stops.addFeature(driver.getStops()[0]);
	    for(Graphic stop : rider.getStops()) {
	    	stops.addFeature(stop);
	    }
	    stops.addFeature(driver.getStops()[1]);
	    stops.setSpatialReference(driver.getsPf());
	    parameters.setStops(stops);
	    parameters.setFindBestSequence(false);
		return parameters;
	}
	
	private double getDrivingTime(RouteResult result){
		String str = result.toString().split("Minutes=")[1];
		return Double.valueOf(str.split("]")[0]);
	}
	
	private boolean isSatisfiable(double totalDrivingTime, ParamData driver) {
		return totalDrivingTime - driver.getDrivingTime() < driver.getTimeTolerance();
	}
	
}
