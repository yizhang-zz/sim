package sim.nodes;


//public class TransmissionList extends LinkedList<TransmissionRecord> {
	
//}

public class TransmissionRecord {
	//public Hashtable<Integer, Double> values;
	public double[] values;
	public int[] status;
	public boolean success;
	public int time;
	public int type;
	
	public TransmissionRecord(int time, int type, double[] values, int[] status, /*Hashtable<Integer, Double> ht,*/ boolean success) {
		this.values = values;
		this.status = status;
		this.success = success;
		this.time = time;
		this.type = type;
	}
	
	public String toString() {
		return String.valueOf(time)+(success?"":"*");
	}
}

