package sim.nodes;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;

import sim.constraints.IntervalList;
import coding.Encoder;
import coding.Symbol;


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

	private int nodeCount;
	private int time;
	private int maxTry = NetworkConfiguration.getGlobalNetwork().maxTry2;
	private FailureGenerator failureGenerator;
	private static Logger logger = Logger.getLogger(Cluster.class);
	
	public IntervalList[] intervalLists;
	//public List<Integer>[] failureLists;
	public TransmissionList transmissionList = new TransmissionList();
	
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

	Encoder encoder;
	private int seq = 0;
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
		
		Network net = NetworkConfiguration.getGlobalNetwork();
		if (net.coding) {
		    encoder = new Encoder(net.encoderConfiguration);
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
			msg.seq = seq++;
		} else if (sentIndex != null && hasNewFailure) {
			msg.type = ClusterMessage.DATAFAILURE;
			msg.seq = seq++;
		} else {
			msg.type = ClusterMessage.ONLYFAILURE;
			msg.seq = -1;
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
		    if (encoder != null)
		        msg.codedMsg = encoder.encode(new Symbol(sentIndex));
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
		
		boolean failed = true;
		int count = 0;
		if (maxTry == -1) { // no retry, rely on redundancies
			failed = failureGenerator.isFailure();
			count++;
		}
		else if (maxTry == 0) { // try until success
			while (failed) {
				failed = failureGenerator.isFailure();
				count++;
			}
		}
		else { // try maxTry times
			while(failed & count < maxTry) {
				failed = failureGenerator.isFailure();
				count++;				
			}
		}
		
		msg.success = !failed;
		msg.tryCount = count;
		if (failed) {
			logger.info(String.format("T %d C %d failure, transmitting %s, tried %d times", time, id, Helper.toString(msg.content), count));
		}
		else {
			logger.info(String.format("T %d C %d success, transmitting %s, tried %d times", time, id, Helper.toString(msg.content), count));
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
		//int[] bcast = new int[nodeCount];
		Hashtable<Integer,Integer> bcast = new Hashtable<Integer,Integer>();
		for (int i=0; i<nodeCount; i++) {
			NodeMessage msg = msgs[i];
			if (msg == null) {
			} 
			else {
				// If a child tried n times and succeeded on the last time, then head should ack on that try
				// currently we assume acks are reliable
				if (bcast.containsKey(msg.tryCount))
					bcast.put(msg.tryCount, bcast.get(msg.tryCount)+1);
				else
					bcast.put(msg.tryCount, 1);
				if (msg.protocol == NodeMessage.Protocol.TS) {
					lastReceived[msg.from] = msg.value;
					updateNodeHistory(msg);
					logger.info(String.format("T %d N %d value %f",time,msg.from,msg.value));
					logger.info(String.format("T %d N %d intervals %s", time, msg.from, childHistory[msg.from]));
					/*logger.info(String.format(
							"T %d N %d redundancy %s recovered history %s known interval %d to %d", time,
							msg.from, Helper.list2string(msg.history), Helper
									.list2string(childHistory[msg.from]), childHistory[msg.from].beginKnown, childHistory[msg.from].endKnown));
				*/}
			}
		}
		/* Log ACK activity
		 * ACK x1 x2 ... xn means n broadcast ACK messages, each ACKing xi child nodes 
		 */
		if (NetworkConfiguration.getGlobalNetwork().maxTry1 != -1) {
		String s = "";
		Enumeration<Integer> keys = bcast.keys();
		while (keys.hasMoreElements()) {
			Integer key = keys.nextElement();
			Integer val = bcast.get(key);
			s += " "+val;
		}
		logger.info(String.format("T %d C %d ACK"+s, time, id));
		}
	}

	private void updateNodeHistory(NodeMessage msg) {
		/* If assuming no failures could happen, then all intervals are GOOD */
		if (NetworkConfiguration.getGlobalNetwork().assumeNoFailures==1) {
			IntervalList h = childHistory[msg.from];
			if (h.size() > 0) {
				h.get(h.size()-1).end = msg.epoch-1;
			}
			h.add(msg.epoch, msg.epoch, sim.constraints.Interval.Type.GOOD, msg.seq);
			return;
		}
		
        IntervalList h = childHistory[msg.from];
        List<NodeMessage> q = msg.history;
        if (q != null && q.size() > 0) {
            if (h.size() == 0) {
                 /* Cluster has received nothing, so all timestamps in node
                 redundancy are failures
                 Note: this actually won't happen because the first message
                 is always assumed to succeed. */
                 for (int i = 0; i < q.size() - 1; i++) {
                    h.add(q.get(i).epoch, q.get(i + 1).epoch - 1,
                            sim.constraints.Interval.Type.BAD, q.get(i).seq);
                    hasNewFailure = true;
                }
            } else {
                int lastTime = h.get(h.size() - 1).begin;
                // transmission at lastTime is always a SUCCESS
                // what can be recovered has already been recovered at lastTime
                // so we start processing from lastTime
                for (int i = 0; i < q.size() - 1; i++) {
                    if (q.get(i).epoch == lastTime) {
                        // extend and complete the last one
                        h.get(h.size() - 1).end = q.get(i + 1).epoch - 1;
                    }
                    if (q.get(i).epoch > lastTime) {
                        h.add(q.get(i).epoch, q.get(i + 1).epoch - 1,
                                sim.constraints.Interval.Type.BAD, q.get(i).seq);
                        hasNewFailure = true;
                    }
                }
            }
        }
        // the last entry in h must be a point interval with current time --
        // [x,x] with GOOD type
        h.add(msg.epoch, msg.epoch, sim.constraints.Interval.Type.GOOD, msg.seq);
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
