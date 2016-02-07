
public class ParamData {
	private String stops;
	private String clientType;
	private int time_Tolerance;
	public ParamData(String stops, String clientType, int time_Tolerance){
		this.clientType=clientType;
		this.time_Tolerance=time_Tolerance;
		this.stops=stops;
		   
	}
	public String getclientType(){
		return clientType;
	}
	public String getstops(){
		return stops;
	}
	public int gettime_Tolerance(){
		return time_Tolerance;
	}
}
