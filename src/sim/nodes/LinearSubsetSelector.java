package sim.nodes;

import java.util.Arrays;
import java.util.Comparator;

import Jama.Matrix;

public class LinearSubsetSelector implements SubsetSelector {
    Model model;

    public LinearSubsetSelector(Model m) {
        model = m;
    }

    @Override
    public int[] select(Matrix mean, Matrix cov, Matrix sentValues,
            Matrix currentValues, double e) {
        int subsetSize = 0;
        int m = mean.getRowDimension() / 2;
        int[] sentIndex = new int[m];
        int[] predictIndex = new int[m];
        Helper.reset(sentIndex, 0);
        Helper.reset(predictIndex, m);

        // sort by predicted error
        Matrix p = model.predict(mean, cov, sentIndex, predictIndex, sentValues);
        p.minusEquals(currentValues);
        double[] pp = p.getColumnPackedCopy();
        Entry[] entries = new Entry[m];
        for (int i = 0; i < m; i++) {
            entries[i] = new Entry(i, Math.abs(pp[i]));
        }
        Arrays.sort(entries, new Comparator<Entry>() {
            @Override
            public int compare(Entry o1, Entry o2) {
                if (o1.val < o2.val)
                    return -1;
                else if (o1.val == o2.val)
                    return 0;
                else
                    return 1;
            }
        });

        for (int i = m - 1; i > -1; i--) {
            int toSend = entries[i].key;
            sentIndex[toSend] = predictIndex[toSend];
            predictIndex[toSend] = -1;
            sentValues.set(toSend, 0, currentValues.get(toSend, 0));
            p = model.predict(mean, cov, sentIndex, predictIndex, sentValues);
            int count = Helper
                    .countUnbounded(p, currentValues, predictIndex, e);
            subsetSize++;
            if (count == 0)
                break;
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
    
    class Entry {
        int key;
        double val;
        
        public Entry(int k, double v) {
            key = k;
            val = v;
        }
    }
}
