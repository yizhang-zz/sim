/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sim;
import sim.nodes.*;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
	String configFile = "4_config.xml";
    	if (args.length > 0)
    		configFile = args[0];
    	Network net = NetworkConfiguration.createNetwork(configFile);
    	net.startSimulation();

    	System.out.println(MVNModel.stat_sentCount);
    	System.out.println(((double)MVNModel.stat_sentCount)/(net.nodeCount * net.timeSteps));
        //conf.startSimulation(conf.clusters[0], 1, "sim.nodes.LinearModel");
    	System.out.println(configFile);
    }
}
