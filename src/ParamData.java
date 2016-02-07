import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;


public class ParamData implements
java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private Graphic[] stops;
	private int clientType;
	private int timeTolerance;
	private SpatialReference sPf;
	
	public ParamData(){
	}
	
	public ParamData(Graphic[] stops, int clientType, int timeTolerance, SpatialReference sPf){
		this.clientType=clientType;
		this.timeTolerance=timeTolerance;
		this.stops=stops;
		this.sPf=sPf;   
	}
	public int getClientType(){
		return clientType;
	}
	public Graphic[] getStops(){
		return stops;
	}
	public int gettimeTolerance(){
		return timeTolerance;
	}
	public SpatialReference getsPf(){
		return sPf;
	}
}
