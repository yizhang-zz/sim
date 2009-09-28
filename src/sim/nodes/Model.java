package sim.nodes;

import Jama.Matrix;

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
	
    /**
     * Predicts other readings when sending a subset.
     * 
     * @param mean
     * @param cov
     * @param sentIndex
     *            which elements to be sent
     * @param predictIndex
     *            which elements to be predicted
     * @param sentValues
     *            values of sent elements
     * @return the prediction matrix (a column vector)
     */
    public Matrix predict(Matrix mean, Matrix cov, int[] sentIndex, int[] predictIndex,
            Matrix sentValues);
}
