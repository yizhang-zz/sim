package sim.nodes;

import java.util.*;

import org.apache.log4j.Logger;

import sim.constraints.*;
import coding.*;

public class BaseStation {
	private static final int FAIL_BUF_SIZE_PER_CLUSTER = 4;
	private static final int HIST_BUF_SIZE_PER_CLUSTER = 4;
	private static Logger logger = Logger.getLogger(BaseStation.class);
	private int time = 0;
	
	public Cluster[] clusters;
	//MVNModel[] models;
	//private ClusterHistory[] clusterHistory;
	public IntervalList[] clusterHistory;

	List<FailureList<Integer>> recentFailures;
	
	private Network net;
	private Decoder decoder;

	public Cluster createCluster(int id, int nodeCount) {
		clusters[id] = new Cluster(nodeCount, net.epsilon1, net.epsilon2);
		//models[id] = new MVNModel(clusters[id]);
		recentFailures.add(new FailureList<Integer>(id, FAIL_BUF_SIZE_PER_CLUSTER));
		//clusterHistory[id] = new ClusterHistory();
		clusterHistory[id] = new IntervalList();
		return clusters[id];
	}
	
	public BaseStation(Network net, int clusterCount) {
		this.net = net;
		clusters = new Cluster[clusterCount];
		//models = new MVNModel[clusterCount];
		recentFailures = new ArrayList<FailureList<Integer>>(clusterCount);
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
			//models[id].update(time, msg.content);

			/*
			 * Add current message to history buffer. The redundancy in msg includes
			 * itself -- the current message, so we should first add this to our history
			 * buffer to make comparison easy when looking for failures
			 */
			// clusterHistory[id].add(time, ClusterHistory.Record.SUCCESS, msg);
			processRedundancy(msg);
			// add current msg to history
			clusterHistory[msg.from].add(time, time, Interval.GOOD);
			logger.info(String.format("T %d C %d type %d %s", time, id, msg.type, Helper.toString(msg.content)));
			logger.info(String.format("T %d C %d intervals %s", time, id, clusterHistory[msg.from]));
		}
		time++;
	}
	
	public void receive1(ClusterMessage msg) {
		if (msg == null) {			
		}
		else if (msg.success) {
			int id = msg.from;
			//models[id].update(time, msg.content);

			/*
			 * Add current message to history buffer. The redundancy in msg includes
			 * itself -- the current message, so we should first add this to our history
			 * buffer to make comparison easy when looking for failures
			 */
			// clusterHistory[id].add(time, ClusterHistory.Record.SUCCESS, msg);
			processRedundancy1(msg);
			
			// add current msg to history
			clusterHistory[msg.from].add(time, time, Interval.GOOD);
			logger.info(String.format("T %d C %d type %d %s", time, id, msg.type, Helper.toString(msg.content)));
			logger.info(String.format("T %d C %d intervals %s", time, id, clusterHistory[msg.from]));
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
			ArrayList<DecodeResult> res = decoder.decode(msg.codedMsg, time);
			if (res != null)
			for (int i=res.size()-1; i>=0; i--) {
				int t = res.get(i).time;
				if (!res.get(i).success) {
					lq.add(t, res.get(i-1).time-1, Interval.BAD);
					logger.warn(String.format("T %d C %d failure found @ T %d type %d %s", time, msg.from, t, 3, Helper.toString(res.get(i).list)));
				}
			}
			}			
		}
		else {
			if (msg.type!=ClusterMessage.ONLYFAILURE)
				decoder.decode(null, time);
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
		List<ClusterMessage> lp = msg.clusterHistory;
		IntervalList lq = clusterHistory[msg.from];
		// int p = lp.size() - 1;
		// int q = lq.size() - 1;
		if (lp != null && lp.size() > 1) {
			// size of lp must be > 1 because the last element is current msg
			int lastTime = -1;
			if (lq.size() > 0) {
				lastTime = lq.get(lq.size()-1).begin;
			}
			ClusterMessage m;
			for (int i = 0; i < lp.size() - 1; i++) {
				m = lp.get(i);
				if (m.time == lastTime) {
					// extend last one to current time
					lq.get(lq.size()-1).end =  lp.get(i+1).time -1;
				}
				if (m.time > lastTime) {
					lq.add(m.time, lp.get(i+1).time-1, Interval.BAD);
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
}
