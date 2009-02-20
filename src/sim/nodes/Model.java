package sim.nodes;

import Jama.Matrix;
import java.util.*;
public interface Model {
	//public void setNetworkConfiguration(NetworkConfiguration nc);
	//public void startSimulation(Cluster c, double e);
	//public void setParams(Hashtable<String, Object> params);
	/**
	 * Decides which elements to send so that the remaining elements are bounded
	 * @param currentValues current values for each element/node
	 * @return indices of elements to be send
	 */
	public int[] send(double[] currentValues);
}
