package sim.nodes;

public class Network {
	private boolean allocate;

	public int timeSteps;
	public int clusterCount;
	public int nodeCount;
	public double epsilon1;
	public double epsilon2;
	
	public int nodeRedundancy;
	public int nodeRedundancyFromHeadToBase;
	public int headRedundancy;
	//public int headHistorySize;
	
	public BaseStation baseStation;
	public Cluster[] clusters;
	public int[] inverseClusterTable;
	
	public DataProvider dataProvider;

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
			//System.out.println("********* "+i+" **********");
			//for (int j=0;j<clusters.length; j++) {
				baseStation.receive(/*clusters[j].send()*/);
			//}
		}
	}

}
