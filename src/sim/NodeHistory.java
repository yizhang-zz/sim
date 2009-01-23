package sim;

import java.io.FileNotFoundException;
import org.apache.log4j.*;
import sim.constraints.*;
public class NodeHistory {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String configFile, traceFile;
		if (args.length != 3) {
			System.err.println("Missing arguments: config file, trace file, output file");
			return;
		}
		else {
			configFile = args[0];
			traceFile = args[1];
		}
		try {
			HistoryReconstructor reconstructor = new HistoryReconstructor(configFile, traceFile);
			reconstructor.reconstruct();
			reconstructor.writeNodeHistory(args[2]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
