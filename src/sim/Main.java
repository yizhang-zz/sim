/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sim;

import java.util.Enumeration;

import sim.nodes.MVNModel;
import sim.nodes.Network;
import sim.nodes.NetworkConfiguration;
import org.apache.log4j.*;

public class Main {

    public static void main(String[] args) {
        String configFile = "4_config.xml";
        if (args.length != 2) {
            System.out.println("args: conf.xml runid");
            System.exit(1);
        }
        configFile = args[0];
        String runid = args[1];
        String[] loggerNames = new String[] { "Node", "Cluster", "BaseStation" };
        for (int i = 0; i < loggerNames.length; i++) {
            Logger logger = Logger.getLogger(loggerNames[i]);
            Enumeration appenders = logger.getAllAppenders();
            while (appenders.hasMoreElements()) {
                Appender apdr = (Appender) appenders.nextElement();
                if (apdr instanceof FileAppender) {
                    FileAppender fa = (FileAppender) apdr;
                    fa.setFile(fa.getFile() + "." + runid);
                    fa.activateOptions();
                }
            }
        }
        try {
            Network net = NetworkConfiguration.createNetwork(configFile);
            net.startSimulation();

            System.out.println(MVNModel.stat_sentCount);
            System.out.println(MVNModel.stat_tx);
            System.out.println(((double) MVNModel.stat_sentCount)
                    / (net.nodeCount * net.timeSteps));
            // conf.startSimulation(conf.clusters[0], 1,
            // "sim.nodes.LinearModel");
            System.out.println(new java.io.File(configFile).getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
