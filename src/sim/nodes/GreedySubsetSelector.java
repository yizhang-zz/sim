package sim.nodes;

import Jama.Matrix;

public class GreedySubsetSelector implements SubsetSelector {

    Model model;

    public GreedySubsetSelector(Model m) {
        model = m;
    }

    /**
     * Greedily selects a minimal subset of readings, so that the predictions
     * for others are bounded by e.
     * 
     * @param mean
     * @param cov
     * @param sentValues
     *            sent values from last time (before selecting a subset)
     * @param currentValues
     *            real values for current readings
     * @param e
     *            the epsilon bound
     * @return index of selected elements
     */
    @Override
    public int[] select(Matrix mean, Matrix cov, Matrix sentValues,
            Matrix currentValues, double e) {
        int subsetSize = 0;
        int unboundedSize = 1;
        int m = mean.getRowDimension() / 2;
        int[] sentIndex = new int[m];
        int[] predictIndex = new int[m];
        Helper.reset(sentIndex, 0);
        Helper.reset(predictIndex, m);

        while (unboundedSize != 0) {
            int min = Integer.MAX_VALUE;
            int minIndex = 0;
            for (int i = 0; i < predictIndex.length; i++) {
                if (predictIndex[i] == -1) {
                    continue;
                    // backup
                }
                int tmpIndex = sentIndex[i];
                double tmpValue = sentValues.get(i, 0);

                sentIndex[i] = predictIndex[i];
                predictIndex[i] = -1; // needn't predict this one

                sentValues.set(i, 0, currentValues.get(i, 0));
                // we don't kick the ith component out of predictIndex cause
                // that doesn't affect prediction for others
                Matrix p = model.predict(mean, cov, sentIndex, predictIndex,
                        sentValues);
                int count = Helper.countUnbounded(p, currentValues,
                        predictIndex, e);
                if (count < min) {
                    min = count;
                    minIndex = i;
                }

                // restore
                predictIndex[i] = sentIndex[i];
                sentIndex[i] = tmpIndex;
                sentValues.set(i, 0, tmpValue);
            }
            unboundedSize = min;
            // update indices and values
            sentIndex[minIndex] = predictIndex[minIndex];
            sentValues.set(minIndex, 0, currentValues.get(minIndex, 0));
            predictIndex[minIndex] = -1;
            subsetSize++;
        }

        int[] result = new int[subsetSize];
        int k = 0;
        for (int i = 0; i < m; i++) {
            if (predictIndex[i] == -1) {
                result[k++] = i;
            }
        }
        return result;
    }
}
