//import com.esri.core.geometry.SpatialReference;

public class ParamData implements
java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private String Stops;
	private int ClientType;
	private int timeTolerance;
//	private SpatialReference sPf;
	
	public ParamData(){
	}
	
	public ParamData(String Stops, int ClientType, int timeTolerance){//, SpatialReference sPf){
		this.ClientType=ClientType;
		this.timeTolerance=timeTolerance;
		this.Stops=Stops;
//		this.sPf=sPf;   
	}
	public int getClientType(){
		return ClientType;
	}
	public String getStops(){
		return Stops;
	}
	public int gettimeTolerance(){
		return timeTolerance;
	}
//	public SpatialReference getsPf(){
//		return sPf;
//	}
}
