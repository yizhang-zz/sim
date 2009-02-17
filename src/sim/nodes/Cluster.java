package sim.nodes;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import sim.constraints.*;

import org.apache.log4j.Logger;

import sim.constraints.*;
import coding.*;


public class Cluster {

	//Head head;
	Node[] nodes;

	public int id;
	public int[] nodeGlobalIDs; // real node Ids in index order
	//public double[][] data; // [node][time]
	public Hashtable<String, Object> params;

	// suppression parameters
	public double epsilon1;
	public double epsilon2;
	public int redundancyLevel = 4;

	private int nodeCount;
	private int time;

	private FailureGenerator failureGenerator;
	private static Logger logger = Logger.getLogger(Cluster.class);
	
	public IntervalList[] intervalLists;
	//public List<Integer>[] failureLists;
	public TransmissionList transmissionList = new TransmissionList();
	
	private BaseStation parent;
	
	double[] lastReceived;
	// double[] lastSent;
	private Model model;
	// transmission history (success/failure) for all child nodes
	IntervalList[] childHistory;

	List<ClusterMessage> history;

	private boolean hasNewFailure;

	// how many past msgs (to basestation) do we keep as redundancies
	//int MAX_HISTORY_SIZE = 4;

	// how many past msgs (from children) do we keep as redundancies (and to discover
	// failures)
	// int MAX_CHILD_HISTORY_SIZE = 8;

	Encoder encoder = new Encoder();
	public Cluster(int nodeCount, double epsilon1, double epsilon2) {
		this.nodeCount = nodeCount;
		this.epsilon1  = epsilon1;
		this.epsilon2 = epsilon2;
		
		failureGenerator = new SimpleFailureGenerator(NetworkConfiguration.getGlobalNetwork().failureRate2, id);

		createNodes();
		params = new Hashtable<String, Object>();
		//head = new Head();
		//head.setModel(new MVNModel(this));
		//model = new MVNModel(this);
		time = -1;
		history = new ArrayList<ClusterMessage>();
		childHistory = new IntervalList[nodeCount];
		lastReceived = new double[nodeCount];
		for (int i = 0; i < nodeCount; i++) {
			childHistory[i] = new IntervalList(NetworkConfiguration.getGlobalNetwork().nodeRedundancyFromHeadToBase);
		}
	}

	/*public void init(double epsilon1, double epsilon2, boolean allocate) {
		this.epsilon1  = epsilon1;
		this.epsilon2 = epsilon2;
		epoch = -1;
		if (allocate)
			createNodes();
		head.setNodeCount(nodeCount);
		head.setModel(new MVNModel(this));
	}
	*/
	public void initIntervalLists() {
		intervalLists = new IntervalList[nodeCount];
		for (int i=0; i< nodeCount; i++) {
			intervalLists[i] = new IntervalList();
		}
	}
	
	/*public void initFailureLists() {
		failureLists = new ArrayList[nodeCount];
		for (int i=0; i< nodeCount; i++) {
			failureLists[i] = new ArrayList<Integer>();
		}
	}*/

	/**
	 * Sends a message to the base station for the whole cluster. A wrapper for
	 * the message produced by the cluster head.
	 * @return ClusterMessage
	 */
	public ClusterMessage send() {
		time++;
		
		// receive messages from children
		NodeMessage[] msgs = new NodeMessage[nodeCount];
		int k = 0;
		for (Node node : nodes) {
			msgs[k++] = node.send();
		}
		receive(msgs);
		
		// approximate current values for children by lastReceived
		// because they are bounded
		int[] sentIndex = model.send(lastReceived);

		// nothing to send: empty subset and no child failures
		if (sentIndex == null && !hasNewFailure) {
			logger.info(String.format("T %d C %d suppression", time, id));
			return null;
		}
		
		
		ClusterMessage msg = new ClusterMessage();

		msg.time = time;	
		msg.from = id;
		if (sentIndex != null && !hasNewFailure) {
			msg.type = ClusterMessage.ONLYDATA;
		} else if (sentIndex != null && hasNewFailure) {
			msg.type = ClusterMessage.DATAFAILURE;
		} else {
			msg.type = ClusterMessage.ONLYFAILURE;
		}
		
		// add known intervals to the message
//		msg.beginKnowns = new int[nodeCount];
//		msg.endKnowns = new int[nodeCount];
//		for (int i=0; i<nodeCount; i++) {
//			//msg.beginKnowns[i] = childHistory[i].beginKnown;
//			msg.endKnowns[i] = childHistory[i].endKnown;
//		}
		
		// attach known intervals of children to the outgoing msg
		msg.childHistory = childHistory;

		// add readings content to the message
		if (sentIndex != null) {
			msg.codedMsg = encoder.encode(sentIndex);
			msg.content = new ArrayList<IndexValuePair>(sentIndex.length);
			for (int i = 0; i < sentIndex.length; i++) {
				msg.content.add(i, new IndexValuePair(sentIndex[i],
						lastReceived[sentIndex[i]]));
			}
		}

		// add -newly- discovered failures from children
		// CORRECTION: NOT newly discovered, but a fixed length of history
		/*msg.failures = new ArrayList<FailureList<Integer>>();
		for (int i = 0; i < childHistory.length; i++) {
			IntervalList pl = childHistory[i];
			boolean found = false;
			FailureList<Integer> fl = null;
			int k = pl.size() - 1;
			for (;k>=0; k--) {
				if (NodeHistory.Record.SUCCESS != pl.get(k).status) {					
					if (!found) {
						found = true;
						msg.failures.add((fl = new FailureList<Integer>(i)));
						// fl.node = i;
						// fl.failureEpochs = new ArrayList<Integer>();
					}
					pl.get(k).status = NodeHistory.Record.FAILURE; // set to 0 after being
															// discovered
					fl.add(0, pl.get(k).time);
					msg.beginKnowns[i] = pl.get(k).time;					
				}
			}
		}*/


		// evict old history item if necessary
		if (history.size() > NetworkConfiguration.getGlobalNetwork().headRedundancy) {
			history.remove(0);
		}
		history.add(msg);

		// add redundancy info
		msg.clusterHistory = history;
		

		if (!failureGenerator.isFailure()){
			logger.info(String.format("T %d C %d success, transmitting %s", time, id, Helper.toString(msg.content)));
			msg.success = true;
		}
		else {
			logger.info(String.format("T %d C %d failure, transmitting %s", time, id, Helper.toString(msg.content)));
			msg.success = false;
			//return null;
                }
		return msg;
	}

	/**
	 * Receives a list of messages from child nodes. This happens once every epoch.
	 * 
	 * @param msgs
	 *            Array of NodeMessage
	 */
	public void receive(NodeMessage[] msgs) {
		hasNewFailure = false;
		//int[] indicators = new int[nodeCount];
		for (int i=0; i<nodeCount; i++) {
			NodeMessage msg = msgs[i];
			if (msg == null) {
				// message suppressed
				//indicators[i] = 0;
			} else {
				//indicators[i] = 1;
				if (msg.protocol == NodeMessage.Protocol.TS) {
					lastReceived[msg.from] = msg.value;
					updateNodeHistory(msg.from, msg.history);
                                        logger.info(String.format("T %d N %d value %f",time,msg.from,msg.value));
                                        logger.info(String.format("T %d N %d intervals %s", time, msg.from, childHistory[msg.from]));
					/*logger.info(String.format(
							"T %d N %d redundancy %s recovered history %s known interval %d to %d", time,
							msg.from, Helper.list2string(msg.history), Helper
									.list2string(childHistory[msg.from]), childHistory[msg.from].beginKnown, childHistory[msg.from].endKnown));
				*/}
			}
		}
	}

	private void updateNodeHistory(int n, List<Integer> q) {
		IntervalList h = childHistory[n];
		if (q != null && q.size() > 0) {
			if (h.size() == 0) { // empty, so add all as failures
				//for (Integer i : q) {
				//	h.add(i, NodeHistory.Record.NEW_FAILURE);
				q.add(time);
				for (int i=0; i<q.size()-1; i++) {
					h.add(q.get(i), q.get(i+1)-1, sim.constraints.Interval.BAD);
					hasNewFailure = true;
				}
			} else {
				// transmission at lastTime is always a SUCCESS
				// what can be recovered has already been recovered at lastTime
				// so we start processing from lastTime
				int lastTime = h.get(h.size() - 1).begin;
				q.add(time);
				for (int i=0; i<q.size()-1; i++) {
					if (q.get(i) == lastTime) {
						// extend and complete the last one
						h.get(h.size() - 1).end = q.get(i+1) - 1;
					}
					if (q.get(i) > lastTime) {
						h.add(q.get(i), q.get(i+1)-1, sim.constraints.Interval.BAD);
						hasNewFailure = true;
					}
				}
			}
		}
		// the last entry in h must be a point interval with current time -- [x,x] with GOOD type
		h.add(time, time, sim.constraints.Interval.GOOD);
		System.out.println("***** "+h.toString());
	}

	public int getNodeCount() {
		return nodeCount;
	}

	public void setNodeCount(int n) {
		if (nodeCount != n && n > 0) {
			nodeCount = n;
			// let head properly allocate space
			// head.setNodeCount(n);
		}
	}

	public double[] getData(int time) {
		return NetworkConfiguration.getGlobalDataProvider().getData(this, time);
	}
	
	private void createNodes() {
		nodes = new Node[nodeCount];
		for (int i = 0; i < nodeCount; i++) {
			nodes[i] = new Node(this, i, epsilon1);
			//nodes[i].setData(data[i]);
			//nodes[i].setEpsilon(epsilon1);
			//nodes[i].setRedundancyLevel(redundancyLevel);
		}
	}

	public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}

}


class FailureList<E> extends ArrayList<E> {
	public static final int MAX_CHILD_FAILURE_REPORT_SIZE  = 4;
	public int node;
	private int maxSize;
	//public List<Integer> failureEpochs;
	
	public FailureList(int nodeId) {
		this(nodeId, MAX_CHILD_FAILURE_REPORT_SIZE);
	}
	public FailureList(int nodeId, int maxSize) {
		super();
		node = nodeId;
		this.maxSize = maxSize;
	}
	
	@Override
	public void add(int index, E element) {
		super.add(index, element);
		if (this.size() > maxSize) {
			this.remove(0);
		}
	}
	
	@Override
	public boolean add(E element) {
		boolean b = super.add(element);
		if (this.size() > maxSize) {
			this.remove(0);
		}
		return b;
	}
}
