import com.esri.core.geometry.SpatialReference;

public class ParamData {
	private String stops;
	private int clientType;
	private int time_Tolerance;
	private SpatialReference sPf;
	
	public ParamData(){
	}
	
	public ParamData(String stops, int clientType, int time_Tolerance, SpatialReference sPf){
		this.clientType=clientType;
		this.time_Tolerance=time_Tolerance;
		this.stops=stops;
		this.sPf=sPf;   
	}
	public int getclientType(){
		return clientType;
	}
	public String getstops(){
		return stops;
	}
	public int gettime_Tolerance(){
		return time_Tolerance;
	}
	public SpatialReference getsPf(){
		return sPf;
	}
}
