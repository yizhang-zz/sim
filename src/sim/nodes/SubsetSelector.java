package sim.nodes;

import Jama.Matrix;

public interface SubsetSelector {
    public int[] select(Matrix mean, Matrix cov, Matrix sentValues,
            Matrix currentValues, double e);
}
