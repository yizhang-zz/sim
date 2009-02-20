package sim.nodes;
import coding.*;

public class Network {
	private boolean allocate;

	public int timeSteps;
	public int clusterCount;
	public int nodeCount;
	public double epsilon1;
	public double epsilon2;
	public int maxTry;
	public int nodeRedundancy;
	public int nodeRedundancyFromHeadToBase;
	public int headRedundancy;
	//public int headHistorySize;
	
	public BaseStation baseStation;
	public Cluster[] clusters;
	public int[] inverseClusterTable;
	
	public double failureRate1;
	public double failureRate2;
	public DataProvider dataProvider;

	public EncoderConfiguration encoderConfiguration;
	public boolean coding = false;
	
	public Network() {
//		for (Cluster c : clusters) {
//			c.init(epsilon1, epsilon2, allocate);
//		}
	}
	
	public BaseStation createBaseStation() {
		return (baseStation = new BaseStation(this, clusterCount));
	}
	
	public void startSimulation() {
		for (int i=0; i<timeSteps; i++) {
				baseStation.receive();
		}
		// cleanup: sort the cluster intervals if coding is used
		baseStation.cleanup();
	}

}
