package sim.nodes;

import java.util.Random;

public class SimpleFailureGenerator implements FailureGenerator {

	private double probability;
	private Random rand = new Random();
	/**
	 * This class generates samples of failure given some failure probability
	 * 
	 * @param probability
	 *            of failure
	 */
	public SimpleFailureGenerator(double probability, int seed) {
		this.probability = probability;
		rand.setSeed(seed*23+345219);
	}

	//@Override
	public boolean isFailure() {
		
		if (rand.nextDouble() < probability)
			return true;
		return false;
	}

}
