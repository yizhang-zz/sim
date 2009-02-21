package sim.nodes;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class DataProvider {

	private double[][] data;
	//private int nodeCount;
	private int timeSteps;
	
	public DataProvider(int nodeCount, int timeSteps) {
		if (nodeCount <=0 || timeSteps <= 0) {
			throw new IllegalArgumentException("arguments should be positive integers");
		}
		//this.nodeCount = nodeCount;
		this.timeSteps = timeSteps;
		
		data = new double[nodeCount][];
		for (int i=0; i< nodeCount; i++) {
			data[i] = new double[timeSteps];
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
				for (String w : words) {
					data[k++][time] = Double.parseDouble(w);
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
		return data[node.getGlobalID()][time];
	}
	
	public double[] getData(Cluster cluster, int time) {
		double[] d = new double[cluster.getNodeCount()];
		for (int i=0; i< cluster.nodeGlobalIDs.length; i++) {
			d[i] = data[cluster.nodeGlobalIDs[i]][time];
		}
		return d;
	}
}
