package sim.nodes;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class Node {
	private static Logger log = Logger.getLogger(Node.class);
	private int id;
	//private double[] data;
	private double epsilon;
	private double state; // state variable for suppression: last transmitted
							// value
	private int redundancyLevel;
	//private int historySize;
	private int epoch = 0;
	private List<NodeMessage> history;
	
	private FailureGenerator failureGenerator;
	private int seq = 0;

	// private boolean bufferFull;

	private Cluster parent;
	private static int maxTry = NetworkConfiguration.getGlobalNetwork().maxTry;
	
	public Node(Cluster c, int id, double epsilon) {
		this.id = id;
		this.parent = c;
		this.epsilon = epsilon;
		redundancyLevel = NetworkConfiguration.getGlobalNetwork().nodeRedundancy;
		failureGenerator = new SimpleFailureGenerator(NetworkConfiguration.getGlobalNetwork().failureRate1, id);
		//historySize = NetworkConfiguration.getGlobalNetwork().nodeHistorySize;
		history = new LinkedList<NodeMessage>();
	}
	
	public double getData(int time) {
		return NetworkConfiguration.getGlobalDataProvider().getData(this, time);
	}

	public int getGlobalID () {
		return parent.nodeGlobalIDs[id];
	}
	
	public double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

/*	public int getRedundancyLevel() {
		return redundancyLevel;
	}

	public void setRedundancyLevel(int redundancyLevel) {
		this.redundancyLevel = redundancyLevel;
	}
*/
	public NodeMessage send() {
		// first time: always send 
		if (history.size() == 0) {
			state = getData(0);
			epoch = 1;
			NodeMessage msg = new NodeMessage();
			msg.protocol = NodeMessage.Protocol.TS;
			msg.from = id;
			msg.value = getData(0);
			msg.epoch = 0;
			msg.tryCount = 1;
			msg.seq = seq++;
			history.add(msg);
            msg.history = history;
			log.info(String.format("T %d N %d success, transmitting %f, tried 1 times", (epoch-1), id, msg.value));
			return msg;
		}
		
		NodeMessage msg = null;
		// bound violated, send
		double curData = getData(epoch);
		if (Math.abs(curData - state) > epsilon) {
			msg = new NodeMessage();
			state = curData;
			msg.protocol = NodeMessage.Protocol.TS;
			msg.from = id;
			msg.epoch = epoch;
			msg.value = curData;
			msg.seq = seq++;
			if (history.size() > redundancyLevel) {
				history.remove(0); // evict earliest one
			}
			history.add(msg);
			msg.history = history;
		}
		epoch++;
		
		// manually inject failure
		boolean failed = true;
		int count = 0;
		
		if (msg == null)
			log.info(String.format("T %d N %d suppression", (epoch-1), id));
		else {
			if (maxTry <= 0) {
				failed = failureGenerator.isFailure();
				count = 1;
			}
			else {
				while (count < maxTry && failed) {
					failed = failureGenerator.isFailure();
					count++;
				}
			}
			
			if (failed) {
				log.info(String.format("T %d N %d failure, tried %d times", (epoch-1), id, count));
				msg = null;
			}
			else {
				msg.tryCount = count;
				log.info(String.format("T %d N %d success, transmitting %f, tried %d times", (epoch-1), id, msg.value, count));
			}
		}
		return msg;
	}


}
