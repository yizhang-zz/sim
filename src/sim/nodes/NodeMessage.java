package sim.nodes;

import java.util.List;

/**
 * This class describes what kind of message a node sends to its parent in each
 * epoch. A static instance NONE is used when suppression occurs.
 * 
 * @author YZ
 * 
 */
class NodeMessage {
	// protocol version of the message
	public static enum Protocol{
		TS,	// timestamp version
		VAL	// value version
		}
	public Protocol protocol;
	public int from;
	public int epoch;
	public double value;
	public List<Integer> history;
	public int tryCount;
	//public static final NodeMessage NONE = new NodeMessage();
}