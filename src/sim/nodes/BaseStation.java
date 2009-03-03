package sim.nodes;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import sim.constraints.Interval;
import sim.constraints.IntervalList;
import coding.DecodeResult;
import coding.Decoder;

public class BaseStation {
	//private static final int FAIL_BUF_SIZE_PER_CLUSTER = 4;
	//private static final int HIST_BUF_SIZE_PER_CLUSTER = 4;
	private static Logger logger = Logger.getLogger(BaseStation.class);
	private int time = 0;
	
	public Cluster[] clusters;
	//MVNModel[] models;
	//private ClusterHistory[] clusterHistory;
	public IntervalList[] clusterHistory;

	//List<FailureList<Integer>> recentFailures;
	
	private Network net;
	private Decoder decoder;

	public Cluster createCluster(int id, int nodeCount) {
		clusters[id] = new Cluster(nodeCount, net.epsilon1, net.epsilon2);
		//models[id] = new MVNModel(clusters[id]);
		//recentFailures.add(new FailureList<Integer>(id, FAIL_BUF_SIZE_PER_CLUSTER));
		//clusterHistory[id] = new ClusterHistory();
		clusterHistory[id] = new IntervalList();
		return clusters[id];
	}
	
	public BaseStation(Network net, int clusterCount) {
		this.net = net;
		clusters = new Cluster[clusterCount];
		//models = new MVNModel[clusterCount];
		//recentFailures = new ArrayList<FailureList<Integer>>(clusterCount);
		//clusterHistory = new ClusterHistory[clusterCount];
		clusterHistory = new IntervalList[clusterCount];
		if (net.coding)
		    decoder = new Decoder(net.encoderConfiguration);
	}
	
	/*@SuppressWarnings("unchecked")
	public void init(Cluster[] clusters) {
		models = new MVNModel[clusters.length];
		recentFailures = new ArrayList<FailureList<Integer>>(clusters.length);
		clusterHistory = new ClusterHistory[clusters.length];
		for (int i = 0; i < models.length; i++) {
			models[i] = new MVNModel(clusters[i]);
			recentFailures.add(new FailureList<Integer>(i, FAIL_BUF_SIZE_PER_CLUSTER));
			clusterHistory[i] = new ClusterHistory();
		}
	}
*/
	public void receive() {
		for(Cluster c: clusters)
		    if (net.coding)
		        receive1(c.send());
		    else
		        receive(c.send());
	}
	
	public void receive(ClusterMessage msg) {
		if (msg != null && msg.success) {
			int id = msg.from;
			if (NetworkConfiguration.getGlobalNetwork().assumeNoFailures == 1) {
				int k;
				// Extend previous interval
				if ((k = clusterHistory[id].size()) > 0)
					clusterHistory[id].get(k-1).end = time -1;
				for (int i = 0; i < msg.childHistory.length; i++) {
					logger.info(String.format("T %d C %d N %d intervals %s", time,
							msg.from, i, msg.childHistory[i]));
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
		time++;
	}
	
	public void receive1(ClusterMessage msg) {
		if (msg == null) {			
		}
		else if (msg.success) {
			int id = msg.from;
			if (NetworkConfiguration.getGlobalNetwork().assumeNoFailures == 1) {
				int k;
				// Extend previous interval
				if ((k = clusterHistory[id].size()) > 0)
					clusterHistory[id].get(k-1).end = time -1;
				for (int i = 0; i < msg.childHistory.length; i++) {
					logger.info(String.format("T %d C %d N %d intervals %s", time,
							msg.from, i, msg.childHistory[i]));
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
		time++;
	}

	/*
	 * Currently assume if a cluster message is of type ONLYFAILURE,
	 * then no coded content is sent so no need to encode or decode.
	 */
	private void processRedundancy1(ClusterMessage msg) {
		if (msg.success) {
			// child-to-head historical intervals
			for (int i = 0; i < msg.childHistory.length; i++) {
				logger.info(String.format("T %d C %d N %d intervals %s", time,
						msg.from, i, msg.childHistory[i]));
			}
			//List<ClusterMessage> lp = msg.clusterHistory;
			IntervalList lq = clusterHistory[msg.from];
			if (msg.type!=ClusterMessage.ONLYFAILURE) { // sentIndex is not empty
			ArrayList<DecodeResult> res = decoder.decode(msg.codedMsg, time, msg.seq);
			if (res != null)
			for (int i=res.size()-1; i>=0; i--) {
				DecodeResult t = res.get(i);
				if (!t.success) {
					lq.add(t.time, res.get(i-1).time-1, Interval.Type.BAD, t.seq);
					logger.warn(String.format("T %d C %d failure found @ T %d type %d %s", time, msg.from, t.time, 3, Helper.toString(t.list)));
				}
			}
			}			
		}
		else {
			if (msg.type!=ClusterMessage.ONLYFAILURE)
				decoder.decode(null, time, msg.seq);
		}
	}
	
	private void processRedundancy(ClusterMessage msg) {
		// child-to-head historical intervals
		for (int i = 0; i < msg.childHistory.length; i++) {
			logger.info(String.format("T %d C %d N %d intervals %s", time,
					msg.from, i, msg.childHistory[i]));
		}

		/*for (int i = 0; i < msg.beginKnowns.length; i++) {
			logger.info(String.format("T %d C %d N %d known interval %d to %d", time,
					msg.from, i, msg.beginKnowns[i], msg.endKnowns[i]));

		}*/
		
		// discover head-to-base failures and derive intervals
		List<ClusterMessage> q = msg.clusterHistory;
		IntervalList h = clusterHistory[msg.from];
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
					logger.warn(String.format("T %d C %d failure found @ T %d type %d %s", time, msg.from, m.time, m.type, Helper.toString(m.content)));
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
        for (Cluster c : clusters) {
            int id = c.id;
            clusterHistory[id].sort();
            logger.info(String.format("T %d C %d INTERVALS %s", time, id, clusterHistory[id]));
        }
    }
}
