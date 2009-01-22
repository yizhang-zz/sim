package sim.nodes;

import java.util.List;

import sim.constraints.IntervalList;

/**
 * Class of messages to be sent from cluster heads to the base station.
 * @author Yi Zhang
 *
 */
public class ClusterMessage {

	/**
	 * Type of this message: only data from the head, or only discovered failures from children,
	 * or both
	 */
		public static final int ONLYDATA = 1;
		public static final int ONLYFAILURE = 2;
		public static final int DATAFAILURE = 3;
	
	/**
	 * Uses timestamps or values as redundancy
	 *
	 */
	public static enum Protocol {
		TS,
		VALUE
	}
	
	public int type;
	public Protocol protocol;
	public int from;
	public int time;
	public List<IndexValuePair> content;
	public IntervalList[] childHistory;
	public List<ClusterMessage> clusterHistory;
	
	//public int[] beginKnowns;
	//public int[] endKnowns;
	//public static final ClusterMessage NONE = new ClusterMessage();
}

class IndexValuePair {
	int index;
	double value;
	public IndexValuePair(int i, double v) {
		index = i;
		value = v;
	}
	@Override
	public String toString() {
		return String.valueOf(index)+":"+value;
	}
}
