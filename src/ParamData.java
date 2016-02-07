import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;


public class ParamData implements
java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private Graphic[] stops;
	private int clientType;
	private int timeTolerance;
	private String date;
	private SpatialReference sPf;
	private double drivingTime;
	
	public ParamData(){
	}
	
	public ParamData(Graphic[] stops, int clientType, int timeTolerance, SpatialReference sPf, String date){
		this.clientType=clientType;
		this.timeTolerance=timeTolerance;
		this.stops=stops;
		this.sPf=sPf;
		this.date=date;
		this.drivingTime = -1;
	}
	public int getClientType(){
		return clientType;
	}
	public Graphic[] getStops(){
		return stops;
	}
	public int getTimeTolerance(){
		return timeTolerance;
	}
	public SpatialReference getsPf(){
		return sPf;
	}
	public String getDate(){
		return date;
	}
	public void setDrivingTime(double dtime){
		this.drivingTime=dtime;
	}
	public double  getDrivingTime(){
		return drivingTime;
	}
}
