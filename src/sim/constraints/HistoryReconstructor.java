package sim.constraints;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sim.constraints.Interval.Type;
import sim.nodes.Cluster;
import sim.nodes.MVNModel;
import sim.nodes.Network;
import sim.nodes.NetworkConfiguration;
import sim.nodes.TransmissionRecord;

public class HistoryReconstructor {
	private String logFile;
	private String confFile;
	private Network net;

	public HistoryReconstructor(String confFile, String logFile) {
		this.logFile = logFile;
		this.confFile = confFile;
	}

	public void reconstruct() throws FileNotFoundException {

		// don't allocate space for raw data
		net = NetworkConfiguration.createNetwork(confFile, false);

		// initialize intervallists for each cluster
		for (Cluster c : net.baseStation.clusters) {
			c.initIntervalLists();
			//c.initFailureLists();
		}

		Pattern pattern1 = Pattern.compile("T (\\d+) C (\\d+) N (\\d+) intervals (.+)");
		// with capital letters INTERVALS, all intervals are sorted and cleaned after simulation
		Pattern pattern2 = Pattern
				.compile("T (\\d+) C (\\d+) INTERVALS (.+)");
		Pattern pattern3 = Pattern
				.compile("T (\\d+) C (\\d+) type (\\d+) \\[ ((.+ )+)\\]");
		Pattern pattern4 = Pattern
				.compile("T (\\d+) C (\\d+) failure found @ T (\\d+) type (\\d+) \\[ ((.+ )+)\\]");
		BufferedReader in = new BufferedReader(new FileReader(logFile));
		String str;
		try {
			while ((str = in.readLine()) != null) {
				Matcher matcher = pattern1.matcher(str);
				if (matcher.find()) {
					// int time = Integer.parseInt(matcher.group(1));
					int cluster = Integer.parseInt(matcher.group(2));
					int node = Integer.parseInt(matcher.group(3));
					parseIntervals(net.baseStation.clusters[cluster].intervalLists[node], matcher.group(4));
					// int begin = Integer.parseInt(matcher.group(4));
					// int end = Integer.parseInt(matcher.group(5));
					// conf.clusters[cluster].intervalLists[node].add(begin, end);
					continue;
				}
				matcher = pattern2.matcher(str);
				if (matcher.find()) {
					int cluster = Integer.parseInt(matcher.group(2));
					parseIntervals(net.baseStation.clusterHistory[cluster], matcher.group(3));
					continue;
				}
				matcher = pattern3.matcher(str);
				if (matcher.find()) {
					int time = Integer.parseInt(matcher.group(1));
					int cluster = Integer.parseInt(matcher.group(2));
					int type = Integer.parseInt(matcher.group(3));
					double[] values = new double[net.baseStation.clusters[cluster].getNodeCount()];
					int[] status = new int[net.baseStation.clusters[cluster].getNodeCount()];
					extractValues(matcher.group(4), values, status, true);
					net.baseStation.clusters[cluster].transmissionList.add(new TransmissionRecord(
							time, type, values, status, true));
					continue;
				}

				
				matcher = pattern4.matcher(str);
				if (matcher.find()) {
					int cluster = Integer.parseInt(matcher.group(2));
					int time = Integer.parseInt(matcher.group(3));
					int type = Integer.parseInt(matcher.group(4));
					double[] values = new double[net.baseStation.clusters[cluster].getNodeCount()];
					int[] status = new int[net.baseStation.clusters[cluster].getNodeCount()];
					extractValues(matcher.group(5), values, status, false);
					net.baseStation.clusters[cluster].transmissionList.add(new TransmissionRecord(
							time, type, values, status, false));
					continue;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// print intervals
		//for (IntervalList il : net.baseStation.clusters[0].intervalLists) {
		//	System.out.println("node " + il);
		//}
		//for (List<Integer> il : net.clusters[0].failureLists) {
		//	System.out.println("failures " + sim.nodes.Helper.toString(il));
		//}
		//System.out.println("cluster tx "
		//		+ sim.nodes.Helper.toString(net.baseStation.clusters[0].transmissionList));
		
		//System.out.println("cluster history "
		//				+ sim.nodes.Helper.toString(net.baseStation.clusterHistory[0]));
		//System.out.println("cluster tx "+sim.nodes.Helper.toString(net.baseStation.clusters[0].transmissionList));
	}
        
        public void writeNodeHistory(String file) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                for (Cluster cluster : net.baseStation.clusters) {
                    IntervalList[] nhis = net.baseStation.clusters[cluster.id].intervalLists;
                    for (int i=0; i<nhis.length; i++) {
                        out.write(i+":"+nhis[i].toString()+"\n");
                    }
                }        
                out.close();
        } catch (IOException ex) {
            //Logger.getLogger(HistoryReconstructor.class.getName()).log(Level.SEVERE, null, ex);
        }
        }

	public void generateConstraints() {
        Interval unknownInterval = new Interval(0,0,Type.UNKNOWN,-1);
		for (Cluster cluster : net.baseStation.clusters) {
			MVNModel model = (MVNModel)(cluster.getModel());
			model.epsilon1 = net.epsilon1;
			// first epoch is always equality constraint
			outputFirstEpoch(cluster.transmissionList.get(0));
			TransmissionRecord txrec = cluster.transmissionList.get(0);
			
			model.makePrediction(Interval.Type.GOOD, null, txrec.values, txrec.status);
			
			//boolean shouldEnd = false;
			int i = 1;
			int ptx = 0;
			int phis = 0;
			int[] pint = new int[cluster.getNodeCount()];
			IntervalList chis = net.baseStation.clusterHistory[cluster.id];
			IntervalList[] nhis = cluster.intervalLists;
			Type ctype; // cluster interval type
			Interval[] ntype = new Interval[cluster.getNodeCount()];
			while (i < net.timeSteps) {
				if (phis == chis.size()-1 && i > chis.get(phis).end)
					break;
				// find right cluster interval
				while (phis < chis.size() && chis.get(phis).end < i)
					phis++;

				if (chis.get(phis).begin <= i) {
					ctype = chis.get(phis).type;
				}
//				else if (chis.get(phis).begin < i)
//					ctype = Type.GOOD; // suppression
				else
					ctype = Type.UNKNOWN;
				
				// find right child intervals
				for (int j=0; j<nhis.length; j++) {
					while (pint[j] < nhis[j].size() && nhis[j].get(pint[j]).end < i) {
						//System.out.println(""+i+" "+nhis[j].get(pint[j]).end);
						pint[j]++;
					}
					if (pint[j] < nhis[j].size()) {
						if (nhis[j].get(pint[j]).begin <= i)
							ntype[j] = nhis[j].get(pint[j]);
						//else if (nhis[j].get(pint[j]).begin < i)
						//	ntype[j] = Interval.GOOD; //suppression
						else
							ntype[j] = unknownInterval;
					}
					else
						ntype[j] = unknownInterval;
					//System.out.println(i+" "+ntype[j]);
				}
				
				// find head -> base station message
				while (ptx < cluster.transmissionList.size() && cluster.transmissionList.get(ptx).time < i)
					ptx++;
				if (ptx == cluster.transmissionList.size())
					model.makePrediction(ctype, ntype, null, new int[cluster.getNodeCount()]);
				else if (cluster.transmissionList.get(ptx).time == i) {
					model.makePrediction(ctype, ntype, cluster.transmissionList.get(ptx).values, cluster.transmissionList.get(ptx).status);
				}
				else {
					model.makePrediction(ctype, ntype, null, new int[cluster.getNodeCount()]);
				}
				i++;
			}
		}
	}

	private void outputFirstEpoch(TransmissionRecord rec) {
		for (int i = 0; i < rec.values.length; i++)
			System.out.println(String.format("0:x[%d,%d] = %f",  0+1,i+1, rec.values[i]));
	}

/*	private int[] convert2Int(String[] str) {
		int[] d = new int[str.length];
		for (int i = 0; i < str.length; i++) {
			d[i] = Integer.parseInt(str[i]);
		}
		return d;
	}
*/
	private void extractValues(String str, double[] values, int[] status, boolean success) {
		//Hashtable<Integer, Double> ht = new Hashtable<Integer, Double>();
		String[] ss = str.split(" ");
		for (String s : ss) {
			String[] pair = s.split(":");
			int i = Integer.parseInt(pair[0]);
			//if (pair[1].equals("?"))
			if (success) {
				status[i] = 1;
				values[i] = Double.parseDouble(pair[1]);
			}
			else {
				status[i] = -1;
			}
		}
	}

	private IntervalList parseIntervals(IntervalList list, String s) {
		Interval lastInterval;
		if (list.size() == 0) {
			lastInterval = new Interval(-1,-1,Interval.Type.GOOD, -1);
		}
		else {
		lastInterval = list.get(list.size() - 1);
		}
		String[] sIntervals = s.split(",");
		//Interval[] ints = new Interval[sIntervals.length];
		for (int i = 0; i < sIntervals.length; i++) {
			String sInterval = sIntervals[i];
			Interval.Type type = Interval.Type.GOOD;
			if (sInterval.startsWith("!")) {
				type = Interval.Type.BAD;
				sInterval = sInterval.substring(1);
			}
			String[] nums = sInterval.split("-");
			int begin = Integer.parseInt(nums[0]);
			int end = Integer.parseInt(nums[1]);
			if (begin < lastInterval.begin)
				continue;
			if (begin == lastInterval.begin) {
				if (end > lastInterval.end) {
					lastInterval.end = end;
				}
			}
			// the sequence number here doesn't really matter, so set to -1
			list.add(begin, end, type, -1);
		}
		return list;
	}
}
