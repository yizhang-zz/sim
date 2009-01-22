/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.nodes;

import java.util.*;

import org.apache.log4j.Logger;

import sim.constraints.IntervalList;

/**
 * 
 * @author Yi
 */
public class Head {
	private static Logger logger = Logger.getLogger(Head.class);
	private int nodeCount;
	public int time = 0;
	double[] lastReceived;
	// double[] lastSent;
	Model model;
	// transmission history (success/failure) for all child nodes
	IntervalList[] childHistory;

	List<ClusterMessage> history;

	private boolean hasNewFailure;

	// how many past msgs (to basestation) do we keep as redundancies
	int MAX_HISTORY_SIZE = 4;

	// how many past msgs (from children) do we keep as redundancies (and to discover
	// failures)
	// int MAX_CHILD_HISTORY_SIZE = 8;

	public Head() {
		history = new ArrayList<ClusterMessage>();
		// spatialModel = new MVNModel();
		// spatialModel.setCluster(this);
		// params = new Hashtable<String, Object>();
	}

	/**
	 * Sends a message to the base station for the whole cluster.
	 * 
	 * @return ClusterMessage
	 */
	public ClusterMessage send() {
		// approximate current values for children by lastReceived
		// because they are bounded
		time++;
		int[] sentIndex = model.send(lastReceived);

		ClusterMessage msg = new ClusterMessage();
		msg.time = time - 1;
		// nothing to send
		if (sentIndex == null && !hasNewFailure) {
			return null;
		} else if (sentIndex != null && !hasNewFailure) {
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

		// add readings content to the message
		if (sentIndex != null) {
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
		msg.childHistory = childHistory;

		// evict old history item if necessary
		if (history.size() > MAX_HISTORY_SIZE) {
			history.remove(0);
		}
		history.add(msg);

		// add redundancy info
		msg.redundancy = history;
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
		for (NodeMessage msg : msgs) {
			if (msg == null) {
				// message suppressed
			} else {
				if (msg.protocol == NodeMessage.Protocol.TS) {
					lastReceived[msg.from] = msg.value;
					updateNodeHistory(msg.from, msg.history);
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
				}
			} else { // append newly discovered failures
				// all transmission before this time should have been correctly identified
				// transmission at lastTime is always a SUCCESS
				// last entry in h must be [x,x] with GOOD type
				int lastTime = h.get(h.size() - 1).begin;
				q.add(time);
				for (int i=0; i<q.size()-1; i++) {
					if (q.get(i) == lastTime) {
						// extend and complete the last one
						h.get(h.size() - 1).end = q.get(i+1) - 1;
					}
					if (q.get(i) > lastTime) {
						h.add(q.get(i), q.get(i+1)-1, sim.constraints.Interval.BAD);
					}
				}
			}
		}
		h.add(time, time, sim.constraints.Interval.GOOD);
		//h.beginKnown = h.get(0).time;
		//h.endKnown = time;
	}

	public int getNodeCount() {
		return nodeCount;
	}

	public void setNodeCount(int n) {
		if (nodeCount != n && n > 0) {
			nodeCount = n;
			childHistory = new IntervalList[n];
			lastReceived = new double[n];
			for (int i = 0; i < n; i++)
				childHistory[i] = new IntervalList(IntervalList.MAX_HISTORY_SIZE);
		}
	}

	public void setModel(Model model) {
		this.model = model;
	}

}

class Pair {
	public int time;
	// whether received or not. 1 - success; 0 - failure; -1 - newly discovered
	// failure
	public int received;

	public Pair(int e, int r) {
		time = e;
		received = r;
	}

	@Override
	public String toString() {
		if (received == 1)
			return String.valueOf(time);
		return String.valueOf(time) + "*";
	}
}
