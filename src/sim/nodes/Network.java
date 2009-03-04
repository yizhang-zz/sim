package sim.nodes;
import coding.EncoderConfiguration;

public class Network {
	//private boolean allocate;

	public int timeSteps;
	public int clusterCount;
	public int nodeCount;
	public double epsilon1;
	public double epsilon2;
	public int maxTry1;
	public int maxTry2;
	public int nodeRedundancy;
	public int nodeRedundancyFromHeadToBase;
	public int headRedundancy;
	public int assumeNoFailures;
	
	public BaseStation baseStation;
	public Cluster[] clusters;
	public int[] inverseClusterTable;
	
	public double failureRate1;
	public double failureRate2;
	public DataProvider dataProvider;

	public EncoderConfiguration encoderConfiguration;
	public boolean coding = false;
	public boolean codeValue = false; // Value or bitmap being coded
	
	public Network() {
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
