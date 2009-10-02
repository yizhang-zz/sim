package sim.nodes;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class DataProvider {

    private int nodeCount;
	private double[][] data;
	private int timeSteps;
	private HashMap<Integer,Integer> nodeIDs;
	
	public DataProvider(int nodeCount, int timeSteps, int[] IDs) {
		if (nodeCount <=0 || timeSteps <= 0) {
			throw new IllegalArgumentException("arguments should be positive integers");
		}
		
		this.nodeCount = nodeCount;
		this.timeSteps = timeSteps;
		
		data = new double[nodeCount][];
		for (int i=0; i< nodeCount; i++) {
			data[i] = new double[timeSteps];
		}
		
		nodeIDs = new HashMap<Integer, Integer>();
		for (int i=0; i<nodeCount; i++) {
		    nodeIDs.put(IDs[i], i);
		}
	}
	
	public void read(String dataFile) {
		int time = 0;
		String s;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(dataFile));
			while (time < timeSteps && (s = reader.readLine()) != null) {
				if (s.trim().length() == 0 || s.trim().startsWith("#")) {
					continue;
				}

				String[] words = s.split(" ");
				int k = 0;
				for (int j=0; j<nodeCount; j++) {
					data[k++][time] = Double.parseDouble(words[j]);
				}
				time++;
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public double getData(Node node, int time) {
	    // Global ID of node should be translated into the index range of data
		return data[nodeIDs.get(node.getGlobalID())][time];
	}
	
	public double[] getData(Cluster cluster, int time) {
	    int n = cluster.getNodeCount();
		double[] d = new double[n];
		for (int i=0; i< n; i++) {
			d[i] = data[nodeIDs.get(cluster.getNodeGlobalID(i))][time];
		}
		return d;
	}
}
