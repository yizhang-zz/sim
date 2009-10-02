package sim.nodes;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;

import sim.constraints.Interval;
import sim.constraints.IntervalList;
import coding.DecodeResult;
import coding.Decoder;

public class BaseStation {
	private static Logger logger = Logger.getLogger(BaseStation.class.getSimpleName());
	private int time = 0;
	
	public Cluster[] clusters;
	public IntervalList[] clusterHistory;
	public Hashtable<Integer,List<IndexValuePair>>[] clusterMsgs;
	
	private Network net;
	private Decoder[] decoders;
	
	private int getNodeGlobalID(int node, int cluster) {
		return clusters[cluster].getNodeGlobalID(node);
	}

	public Cluster createCluster(int id, int nodeCount, int[] nodeGlobalIDs) {
		clusters[id] = new Cluster(id, nodeCount, nodeGlobalIDs, net.epsilon1, net.epsilon2);
		clusterHistory[id] = new IntervalList();
		clusterMsgs[id] = new Hashtable<Integer,List<IndexValuePair>>();
		return clusters[id];
	}
	
	public BaseStation(Network net, int clusterCount) {
		this.net = net;
		clusters = new Cluster[clusterCount];
		clusterHistory = new IntervalList[clusterCount];
		clusterMsgs = new Hashtable[clusterCount];
		if (net.coding) {
		    decoders = new Decoder[clusterCount];
		    for (int i=0; i<clusterCount; i++)
		        decoders[i] = new Decoder(net.encoderConfiguration);
		}
	}
	
	public void receive() {
	    // Receive messages from each cluster
		for(Cluster c: clusters) {
		    if (net.coding)
		        receive1(c.send());
		    else
		        receive(c.send());
		}
		
		time++;
	}
	
	public void receive(ClusterMessage msg) {
		if (msg != null && msg.success) {
			int id = msg.from;
			if (msg.content != null)
				clusterMsgs[id].put(time, msg.content);
			if (NetworkConfiguration.getGlobalNetwork().assumeNoFailures == 1) {
				int k;
				// Extend previous interval
				if ((k = clusterHistory[id].size()) > 0)
					clusterHistory[id].get(k-1).end = time -1;
				for (int i = 0; i < msg.childHistory.length; i++) {
					logger.info(String.format("T %d C %d N %d intervals %s", time,
							id, getNodeGlobalID(i,id), msg.childHistory[i]));
				}
			}
			else
				processRedundancy(msg);
			
			// add current msg to history
			clusterHistory[id].add(time, time, Interval.Type.GOOD, msg.seq);
			logger.info(String.format("T %d C %d type %d %s", time, id, msg.type, Helper.toString(msg.content)));
			logger.info(String.format("T %d C %d intervals %s", time, id, clusterHistory[msg.from]));
			if (NetworkConfiguration.getGlobalNetwork().maxTry2 != -1) {
				logger.info(String.format("T %d BS ACK 1", time));
			}
		}
	}
	
	/**
	 * Process received cluster head message when coding is used.
	 */
	public void receive1(ClusterMessage msg) {
		if (msg == null) {			
		}
		else {
			int id = msg.from;
			if (msg.content != null)
				clusterMsgs[id].put(time, msg.content);
			if (msg.success) {
				if (NetworkConfiguration.getGlobalNetwork().assumeNoFailures == 1) {
					int k;
					// Extend previous interval
					if ((k = clusterHistory[id].size()) > 0)
						clusterHistory[id].get(k-1).end = time -1;
					for (int i = 0; i < msg.childHistory.length; i++) {
						logger.info(String.format("T %d C %d N %d intervals %s", time,
								id, getNodeGlobalID(i,id), msg.childHistory[i]));
					}
				}
				else
					processRedundancy1(msg);
				
				// add current msg to history
				clusterHistory[id].add(time, time, Interval.Type.GOOD, msg.seq);
				logger.info(String.format("T %d C %d type %d %s", time, id, msg.type, Helper.toString(msg.content)));
				logger.info(String.format("T %d C %d intervals %s", time, id, clusterHistory[id]));
				if (NetworkConfiguration.getGlobalNetwork().maxTry2 != -1) {
					logger.info(String.format("T %d BS ACK 1", time));
				}
			}
			else {
				processRedundancy1(msg);
			}
		}
	}

	/**
	 * Process redundancy by decoding when message is coded.
	 * Currently assume if a cluster message is of type ONLYFAILURE,
	 * then no coded content is sent so no need to encode or decode.
	 */
	private void processRedundancy1(ClusterMessage msg) {
		if (msg.success) {
			int cid = msg.from;
			// child-to-head historical intervals
			for (int i = 0; i < msg.childHistory.length; i++) {
				logger.info(String.format("T %d C %d N %d intervals %s", time,
						cid, getNodeGlobalID(i,cid), msg.childHistory[i]));
			}
			//List<ClusterMessage> lp = msg.clusterHistory;
			IntervalList lq = clusterHistory[msg.from];
			if (msg.type!=ClusterMessage.ONLYFAILURE) { // sentIndex is not empty
			ArrayList<DecodeResult> res = decoders[cid].decode(true, msg.codedMsg, time, msg.seq);
			if (res != null)
			for (int i=res.size()-1; i>=0; i--) {
				DecodeResult t = res.get(i);
				if (!t.success) {
					/* If only bitmap+timestamp is coded, since real values are unknown,
					 * the message is treated as a recovered failure, just as a failure
					 * discovered by using repetition redundancy. However, if transmitted
					 * values are coded, then the message is (almost) fully recovered,
					 * except the forwarded redundancies of the child nodes as we do not
					 * include that info in the coded message.
					 */
					
					if (!net.codeValue) {
						lq.add(t.time, res.get(i-1).time-1, Interval.Type.BAD, t.seq);
						logger.warn(String.format("T %d C %d failure found @ T %d type %d %s", time, cid, t.time, 3, Helper.toString(t.list)));
					}
					else {
						lq.add(t.time, res.get(i-1).time-1, Interval.Type.GOOD, t.seq);
						logger.info(String.format("T %d C %d type %d %s", t.time, cid, ClusterMessage.ONLYDATA, Helper.toString(clusterMsgs[cid].get(t.time))));
					}
				}
			}
			}			
		}
		else {
			if (msg.type!=ClusterMessage.ONLYFAILURE)
				decoders[msg.from].decode(false, msg.codedMsg, time, msg.seq);
		}
	}
	
	private void processRedundancy(ClusterMessage msg) {
		int cid = msg.from;
		// child-to-head historical intervals
		for (int i = 0; i < msg.childHistory.length; i++) {
			logger.info(String.format("T %d C %d N %d intervals %s", time,
					cid, getNodeGlobalID(i,cid), msg.childHistory[i]));
		}

		/*for (int i = 0; i < msg.beginKnowns.length; i++) {
			logger.info(String.format("T %d C %d N %d known interval %d to %d", time,
					msg.from, i, msg.beginKnowns[i], msg.endKnowns[i]));

		}*/
		
		// discover head-to-base failures and derive intervals
		List<ClusterMessage> q = msg.clusterHistory;
		IntervalList h = clusterHistory[cid];
		/* q always contains msg as its last element, so q.size() >=1 */
		if (q != null && q.size() > 1) {
			// size of lp must be > 1 because the last element is current msg
			int lastTime = -1;
			if (h.size() > 0) {
				lastTime = h.get(h.size()-1).begin;
			}
			ClusterMessage m;
			for (int i = 0; i < q.size() - 1; i++) {
				m = q.get(i);
				if (m.time == lastTime) {
					// extend and complete the last one in previous history
					h.get(h.size()-1).end =  q.get(i+1).time - 1;
				}
				if (m.time > lastTime) {
					h.add(m.time, q.get(i+1).time-1, Interval.Type.BAD, m.seq);
					logger.warn(String.format("T %d C %d failure found @ T %d type %d %s", time, cid, m.time, m.type, Helper.toString(m.content)));
				}
			}			
		}
		//logger.info(String.format("T %d C %d recovered history %s", time, msg.from,
		//		Helper.print(lq)));
		/*
		 * while (p >= 0 && q >= 0) { if (lp.get(p).time == lq.get(q).time) { // success
		 * p--; q--; continue; }
		 */
		// logger.warn(String.format("T %d C %d discovered failures %s",
		// time, msg.from, lp.get(p).time));
		// lq.add(q+1, lp.get(p));
		// p--;
		// }
		// int overflow = lq.size() - HIST_BUF_SIZE_PER_CLUSTER;
		// while (overflow > 0) {
		// //clusterHistoryBuf[msg.from] = lq.subList(overflow, lq.size());
		// lq.remove(0);
		// overflow--;
		// }
		//logger.info(String.format("T %d C %d known interval %d to %d", time, msg.from, lp
		//		.get(0).time, lp.get(lp.size() - 1).time));
	}

	/**
	 * Let each cluster clean its Interval history by sorting according to the seq no.
	 */
    public void cleanup() {
        // Time is incremented after each iteration of simulation.
        // Return to the last time epoch here.
        time--;
        for (Cluster c : clusters) {
            int id = c.id;
            clusterHistory[id].sort();
            logger.info(String.format("T %d C %d INTERVALS %s", time, id, clusterHistory[id]));
        }
    }
}
