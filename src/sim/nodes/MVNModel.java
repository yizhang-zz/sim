package sim.nodes;

import java.util.Hashtable;
import java.util.*;
import java.util.Vector;
import org.apache.log4j.Logger;
import Jama.Matrix;
import sim.constraints.*;

public class MVNModel implements Model {
    //private static Logger logger = Logger.getLogger(Model.class);
    // public int timeSteps;
    // public int clusterCount;

    public int m; // # of nodes

    public static int stat_sentCount = 0;

    // public Cluster[] clusters;
    // public int[] inverseClusterTable;
    public Matrix c;
    public Matrix a;
    public Matrix sigma;
    double epsilon;
    public double epsilon1;
    Matrix sentValues;
    /** if each (child) component of head-to-base transmission is known*/
    boolean[] known;
    int dim; // total dimension of the distribution; last m dimensions are always for
    // latest epoch

    Matrix mean, cov;
    int[] sentIndex;
    int[] lastIndex;
    int ts = 0;
    /** time of last transmission from child to head */
    int[] lastTs;
    /** type of transmission from child to head last time */
    int[] lastTypes;
    boolean stateSynced = true;

    // boolean hasUnknown = false;
    public MVNModel(double epsilon, Matrix c, Matrix a, Matrix sigma) {
        // c = (Matrix) (cluster.params.get("c"));
        // a = (Matrix) (cluster.params.get("a"));
        // sigma = (Matrix) (cluster.params.get("sigma"));
        this.c = c;
        this.a = a;
        this.sigma = sigma;
        m = c.getRowDimension();
        this.epsilon = epsilon;
    }

    // @Override
    // public void setParams(Hashtable<String, Object> params) {
    // c = (Matrix) (params.get("c"));
    // a = (Matrix) (params.get("a"));
    // sigma = (Matrix) (params.get("sigma"));
    // m = cluster.getNodeCount();
    // }
    private int[] remainingIndex(int total, int[] state) {
        int count = 0;
        for (int s : state) {
            if (s != 0) {
                count++;
            }
        }
        int[] y = new int[total - count];
        int k = 0;
        for (int i = 0; i < state.length; i++) {
            if (state[i] == 0) {
                y[k++] = i;
            }
        }
        for (int i = state.length; i < total; i++) {
            y[k++] = i;
        }
        return y;
    }

    /**
     * When a new subset is transmitted, they should overwrite previous values in the
     * model. Drop those old elements and marginalize on the latest sent+unknown elements.
     */
    public void marginalize(int[] state) {
        int[] remain = remainingIndex(dim, state);
        mean = mean.getMatrix(remain, 0, 0);
        cov = cov.getMatrix(remain, remain);
        reset(lastIndex, remain.length - m);
        int j = 0, x = 0;
        for (int i = 0; i < m; i++) {
            if (state[i] != 0) {
                sentIndex[i] = remain.length - m + i;
                x++;
                j++;
            } else {
                sentIndex[i] -= x;
            }
        }
        dim = remain.length;
    }

    private boolean allKnown() {
        for (boolean b : known) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    private boolean allGood() {
        for (int i = 0; i < m; i++) /* if a component is unkown, it must appear in symbolic constraint so it's lastType is required to be good */ {
            if (!known[i] && lastTypes[i] != Interval.GOOD) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes content of transmission and outputs constraints.
     * 
     * @param type
     *            type of head-to-bs transmission: GOOD, BAD or UNKNOWN
     * @param ntype array of types of child-to-head transmission: GOOD, BAD or UNKNOWN
     * @param val
     *            values vector contained in head-to-bs message
     * @param status
     *            for each component in val, 1 means value known, -1 means value unknown and 0
     *            means not included in the transmitted subset.
     */
    public void makePrediction(int type, Interval[] ntype, double[] val, int[] status) {
        if (sentValues == null) {
            ts = 0;
            dim = m;
            sentValues = new Matrix(m, 1); // last sent value for each node

            for (int i = 0; i < m; i++) {
                sentValues.set(i, 0, val[i]);
            }

            // END = 2 * m - 1;
            mean = new Matrix(new double[m], m);
            cov = new Matrix(new double[m][m]);
            // mean initialized to 0 automatically
            // now initialize cov
            cov.setMatrix(0, m - 1, 0, m - 1, sigma);

            sentIndex = new int[m];
            lastIndex = new int[m];
            reset(sentIndex, 0);
            reset(lastIndex, 0);

            known = new boolean[m];
            lastTs = new int[m];
            lastTypes = new int[m];
            Arrays.fill(known, true);
            Arrays.fill(lastTs, 0);
            Arrays.fill(lastTypes, Interval.GOOD);
            stateSynced = true;
            return;
        }

        /* evolve model for each time step*/
        forward();
        marginalize(status);

        /* If head-to-bs is unknown, no constraint can be generated */
        if (type == Interval.UNKNOWN) {
            System.out.println("# t " + ts + " no constraints");
            // don't know if any subset is transmitted or not
            Arrays.fill(known, false);
            stateSynced = false;
            return;
        }

        /* copy sent values */
        for (int i = 0; i < status.length; i++) {
            if (status[i] == 1) {// newly sent values

                sentValues.set(i, 0, val[i]);
                known[i] = true;
            } else if (status[i] == -1) {// unknown values

                known[i] = false;
            } else {
                continue;
            }
            lastTs[i] = ts;
            lastTypes[i] = ntype[i].type;
        }

        /* if previously not in sync, check if all components are known again */
        if (!stateSynced) {
            stateSynced = true;
            for (boolean b : known) {
                stateSynced &= b;
            // still not in sync; wait
            }
            if (!stateSynced) {
                System.out.println("# t " + ts + " no constraints: prediction state not in sync");
                return;
            }
        }


        /* which components need be predicted? */
        int[] predictIndex = lastIndex.clone();
        int subsetSize = 0;
        for (int i = 0; i < m; i++) {
            if (status[i] != 0) {
                predictIndex[i] = -1; // no need to predict

                subsetSize++;
            }
        }

        /* last transmission for any components is known, so numeric computation */
        if (allKnown()) {
            Matrix prediction = predict(mean, cov, sentIndex, predictIndex, sentValues);
            // now constraints?
            // if (type == Interval.GOOD)

            // produce constraints
            int x = 0; // how many -1s encountered

            for (int j = 0; j < m; j++) {
                if (predictIndex[j] == -1) {
                    x++;
                }
                if (ntype[j].type == Interval.GOOD) {
                    if (predictIndex[j] == -1) {
                        /* type 0: x[i,j] = a */
                        if (ntype[j].begin == ts) // both tiers transmit, equality constraint
                        {
                            System.out.println(String.format("0:x[%d,%d] = %f", ts + 1, j + 1, sentValues.get(j, 0)));
                        } else {
                            System.out.println(String.format("1:%d,%d,%f,%f", ts + 1, j + 1, sentValues.get(j, 0) - (epsilon1), sentValues.get(j, 0) + (epsilon1)));
                        }
                    } else {
                        /* type 1: a <= x[i,j] <= b*/
                        if (ntype[j].begin != ts)
                        System.out.println(String.format("1:%d,%d,%f,%f", ts + 1, j + 1, prediction.get(j - x, 0) - (epsilon1 + epsilon), prediction.get(j - x, 0) + (epsilon1 + epsilon)));
                        else
                            System.out.println(String.format("1:%d,%d,%f,%f", ts + 1, j + 1, prediction.get(j - x, 0) - epsilon, prediction.get(j - x, 0) +  epsilon));
                    }
                } else if (ntype[j].type == Interval.BAD) {
                    if (predictIndex[j] == -1) {
                        /* type 2: x[i,j] <a or x[i,j] >b */
                        System.out.println(String.format("2:%d,%d,%f,%f", ts + 1, j + 1, sentValues.get(j, 0) - epsilon1, sentValues.get(j, 0) + epsilon1));
                    } else {
                        // loose bound
                    }
                }
            }
        } else { // symbolic computation

            /* NOTE: Symbolic variable here is NOT real reading, but view of the head.
             * So unless these variables are all GOOD, we have no constraints. */
            if (!allGood()) {
                return;
            }
            int[] compactPredictIndex = pack(predictIndex); // remove -1 elements

            Matrix predictMean = mean.getMatrix(compactPredictIndex, 0, 0);
            Matrix coef = cov.getMatrix(compactPredictIndex, sentIndex).times(
                    cov.getMatrix(sentIndex, sentIndex).inverse());
            Matrix C = predictMean.minusEquals(coef.times(mean.getMatrix(sentIndex, 0, 0))); // the constant term

            /*
             * constraint will be: C-epsilon <= x- coef*sentValues <= C+epsilon
             * output format: 3:left;right;coef,time,node;coef,time,node;...
             */

            int x = 0; // how many -1s encountered

            for (int j = 0; j < m; j++) {
                if (predictIndex[j] != -1) { // to be predicted
                    if (ntype[j].type == Interval.GOOD) {
                        double left = C.get(j - x, 0);
                        double right = C.get(j - x, 0);
                        if (ntype[j].begin == ts) { // begin of a suppression interval
                            left -= (epsilon);
                            right += epsilon;
                        }
                        else {
                            left -=  (epsilon + epsilon1);
                            right +=  (epsilon + epsilon1);
                        }
                        double temp = 0, relax = 0;
                        String sVar = "";
                        for (int k = 0; k < m; k++) {
                            if (known[k]) {
                                temp += coef.get(j - x, k) * sentValues.get(k, 0);
                            } else {
                                // NOTE: Symbolic variable here is NOT real reading, but view of the head.
                                // So additional relaxation of the bounds should be considered. 
                                sVar += String.format(";%f,%d,%d", -coef.get(j - x, k), lastTs[k] + 1, k + 1);
                                relax += Math.abs(coef.get(j - x, k)) * epsilon1;
                            // relax could be more tight when view of the head *is* actually the real reading
                            }
                        }
                        left += (temp - relax);
                        right += (temp + relax);
                        StringBuffer output = new StringBuffer(String.valueOf("3:" + left + ";" + right));//= String.format("%f <=, arg1)

                        output.append(";1," + (ts + 1) + "," + (1 + j));
                        output.append(sVar);
                        //output.append("<= "+right);
                        System.out.println(output);
                    }
                    else if (ntype[j].type == Interval.BAD) {
                    }
                } else {// sent values
                    x++;
                    if (known[j] && ntype[j].type == Interval.GOOD) {
                        if (ntype[j].begin == ts)
                        System.out.println(String.format("0:x[%d,%d] = %f", ts + 1, j + 1, sentValues.get(j, 0)));
                        else
                             System.out.println(String.format("1:%d,%d,%f,%f", ts + 1, j + 1, sentValues.get(j, 0) - (epsilon1), sentValues.get(j, 0) + (epsilon1)));
                    /*System.out.println(String.format("1:%d,%d,%f,%f", ts+1, j+1, sentValues
                    .get(j, 0)
                    - (epsilon1),  sentValues.get(j, 0)
                    + (epsilon1)));*/
                    } else if (known[j] && ntype[j].type == Interval.BAD) {
                        System.out.println(String.format("2:%d,%d,%f,%f", ts + 1, j + 1, sentValues.get(j, 0) - epsilon1, sentValues.get(j, 0) + epsilon1));
                    }
                }
            }

        }
    }

    /**
     * Move forward one epoch. Update mean, cov and indices.
     */
    public void forward() {
        ts++;
        // update distribution params: mean and cov

        Matrix lastMean = mean.getMatrix(lastIndex, 0, 0);
        Matrix currentMean = c.plus(a.times(lastMean));

        // Matrix cov = cov.getMatrix(0, dim-1, 0, dim-1);
        Matrix UL = cov.copy();
        Matrix LL = a.times(cov.getMatrix(lastIndex, 0, dim - 1));
        // oldCov.print(0, 0);
        Matrix UR = cov.inverse().getMatrix(0, dim - 1, lastIndex).times(a.transpose());
        Matrix temp = new Matrix(m, dim);
        temp.setMatrix(0, m - 1, dim - m, dim - 1, a);
        Matrix LR = sigma.plus(temp.times(cov).times(temp.transpose()));

        // construct new mean and cov
        dim = 2 * m;
        int END = dim - 1;
        Matrix tempMean = new Matrix(dim, 1);
        tempMean.setMatrix(0, m - 1, 0, 0, mean.getMatrix(sentIndex, 0, 0));
        tempMean.setMatrix(m, dim - 1, 0, 0, currentMean);
        mean = tempMean;

        cov = new Matrix(dim, dim);
        cov.setMatrix(0, m - 1, 0, m - 1, UL.getMatrix(sentIndex, sentIndex));
        cov.setMatrix(0, m - 1, m, END, UR.getMatrix(sentIndex, 0, m - 1));
        cov.setMatrix(m, END, 0, m - 1, LL.getMatrix(0, m - 1, sentIndex));
        cov.setMatrix(m, END, m, END, LR);
        // cov is full now
        // lastTimeBegin = m;
        // lastTimeEnd = END;
        reset(sentIndex, 0);
        reset(lastIndex, m);
    }

    //@Override
    public int[] send(double[] currentVal) {
        // first time to send: always send out all values
        if (sentValues == null) {
            dim = m;
            sentValues = new Matrix(m, 1); // last sent value for each node

            for (int i = 0; i < m; i++) {
                sentValues.set(i, 0, currentVal[i]);
            }
            mean = new Matrix(new double[m], m);
            cov = new Matrix(new double[m][m]);
            // mean initialized to 0 automatically
            // now initialize cov
            cov.setMatrix(0, m - 1, 0, m - 1, sigma);

            sentIndex = new int[m];
            lastIndex = new int[m];
            reset(sentIndex, 0);
            reset(lastIndex, 0);

            stat_sentCount += m;

            return sentIndex;
        }

        Matrix currentValues = new Matrix(currentVal, m);

        // move forward one epoch
        forward();
        // cov.print(0, 7);

        // prediction and bound check
        Matrix prediction = predict(mean, cov, sentIndex, lastIndex, sentValues);

        if (isBounded(prediction, currentValues, epsilon)) {
            // System.out.println("suppressed");
            return null;
        }

        /*
         * subset selection
         */
        int[] newSentIndex = GreedySelectSubset(mean, cov, sentValues, currentValues,
                epsilon);
        int subsetSize = newSentIndex.length;
        // for (int j = 0; j < newSentIndex.length; j++) {
        // if (newSentIndex[j] >= m) {
        // subsetSize++;
        // }
        // }
        stat_sentCount += subsetSize;

        // compact mean and cov by dropping useless entries
        // newly selected indices overwrite previous ones
        // e.g. suppose newSentIndex=[1,2], m=4, then newIndex=[0,3,4,5,6,7]
        // and sentIndex becomes [0,3,4,1] while lastIndex=[2,3,4,5]
        int[] newIndex = new int[dim - subsetSize];
        Vector<Integer> vec = new Vector<Integer>();
        for (int j = 0; j < dim; j++) {
            vec.add(j);
        }
        for (int j = subsetSize - 1; j > -1; j--) {
            vec.remove(newSentIndex[j]);
        }
        for (int j = 0; j < vec.size(); j++) {
            newIndex[j] = vec.get(j);
        // int k = 0;
        // for (int j = 0; j < newSentIndex.length; j++) {
        // if (newSentIndex[j] < m)
        // newIndex[k++] = newSentIndex[j];
        // }
        // for (int j = m; j < dim; j++)
        // newIndex[k++] = j;
        }
        mean = mean.getMatrix(newIndex, 0, 0);
        cov = cov.getMatrix(newIndex, newIndex);
        dim -= subsetSize;
        reset(lastIndex, m - subsetSize);

        int x = 0; // how many have been dropped so far

        int k = 0;
        for (int j = 0; j < sentIndex.length; j++) {
            if (k < subsetSize && sentIndex[j] == newSentIndex[k]) { // intersect
                // of
                // sentIndex
                // and
                // predictIndex

                x++;
                k++;
                sentIndex[j] += (m - subsetSize);
                sentValues.set(j, 0, currentValues.get(j, 0));
            } else {
                sentIndex[j] -= x;
            }
        }
        // int x = 0;
        // for (int j = 0; j < newSentIndex.length; j++) {
        // if (newSentIndex[j] >= m) { // intersect of sentIndex and
        // // predictIndex
        // x++;
        // newSentIndex[j] -= subsetSize;
        // sentValues.set(j, 0, currentValues.get(j, 0));
        // } else {
        // newSentIndex[j] -= x;
        // }
        // }
        // sentIndex = newSentIndex;
        return newSentIndex;
    }

    private void reset(int[] d, int start) {
        for (int i = 0; i < d.length; i++) {
            d[i] = start + i;
        }
    }

    private boolean isBounded(Matrix a, Matrix b, double e) {
        return (a.minus(b).normInf() <= e);
    }

    /**
     * Deletes -1 from an array
     * 
     * @param a
     *            Array
     * @return Result
     */
    static int[] pack(int[] a) {
        int n = 0;
        // int count = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != -1) {
                n++;
            // no -1s in a
            }
        }
        if (n == a.length) {
            return a;
        }
        int j = 0;
        int[] l = new int[n];
        for (int i = 0; i < a.length; i++) {
            if (a[i] != -1) {
                l[j++] = a[i];
            }
        }
        return l;
    }

    /**
     * Predicts other readings when sending a subset.
     * 
     * @param mean
     * @param cov
     * @param sentIndex
     *            which elements to be sent
     * @param predictIndex
     *            which elements to be predicted
     * @param sentValues
     *            values of sent elements
     * @return the prediction matrix (a column vector)
     */
    private Matrix predict(Matrix mean, Matrix cov, int[] sentIndex, int[] predictIndex,
            Matrix sentValues) {
        predictIndex = pack(predictIndex); // remove -1 elements

        Matrix predictMean = mean.getMatrix(predictIndex, 0, 0);
        predictMean.plusEquals(cov.getMatrix(predictIndex, sentIndex).times(
                cov.getMatrix(sentIndex, sentIndex).inverse()).times(
                sentValues.minus(mean.getMatrix(sentIndex, 0, 0))));
        return predictMean;
    }

    /**
     * Greedily selects a minimal subset of readings, so that the predictions for others
     * are bounded by e.
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
    private int[] GreedySelectSubset(Matrix mean, Matrix cov, Matrix sentValues,
            Matrix currentValues, double e) {
        int subsetSize = 0;
        int unboundedSize = 1;
        int m = mean.getRowDimension() / 2;
        int[] sentIndex = new int[m];
        int[] predictIndex = new int[m];
        reset(sentIndex, 0);
        reset(predictIndex, m);

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
                Matrix p = predict(mean, cov, sentIndex, predictIndex, sentValues);
                int count = countUnbounded(p, currentValues, predictIndex, e);
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

        // print
        int[] result = new int[subsetSize];
        int k = 0;
        // System.out.print("MVN Sending nodes:");
        for (int i = 0; i < m; i++) {
            if (predictIndex[i] == -1) {
                // System.out.print(" " + i);
                result[k++] = i;
            }
        }
        // System.out.println();

        // return sentIndex;
        return result;

    }

    static int countUnbounded(Matrix a, Matrix b, int[] predictIndex, double e) {
        int count = 0;
        int n = predictIndex.length;
        int i = 0,  j = 0;
        for (; j < n; j++) {
            // skip unused ones
            if (predictIndex[j] == -1) {
                continue;
            }
            if (Math.abs(a.get(i, 0) - b.get(j, 0)) > e) {
                count++;
            }
            i++;
        }
        return count;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    /**
     * Updates the model after receiving a subset of values.
     * 
     * @param content
     */
    public void update(int epoch, List<IndexValuePair> content) {
        // all values should be transmitted at epoch 0
        if (epoch == 0) {
            dim = m;
            sentIndex = new int[m];
            lastIndex = new int[m];
            reset(sentIndex, 0);
            reset(lastIndex, 0);

            sentValues = new Matrix(m, 1); // last sent value for each node

            for (IndexValuePair p : content) {
                sentIndex[p.index] = p.index;
                sentValues.set(p.index, 0, p.value);
            }
            // END = 2 * m - 1;
            mean = new Matrix(new double[m], m);
            cov = new Matrix(new double[m][m]);
            // mean initialized to 0 automatically
            // now initialize cov
            cov.setMatrix(0, m - 1, 0, m - 1, sigma);

            return;
        }

        // move forward and update various matrices
        forward();
        // cov.print(0, 7);

        int size = content == null ? 0 : content.size();

        if (size == 0) {
            // empty message
            // sentIndex and lastIndex remain intact
        } else {
            // for (IndexValuePair p : content) {
            // predictIndex[p.index] = -1;
            // sentIndex[p.index] += m;
            // sentValues.set(p.index, 0, p.value);
            // }
            int[] newIndex = new int[dim - size];
            Vector<Integer> vec = new Vector<Integer>();
            for (int j = 0; j < dim; j++) {
                vec.add(j);
            }
            for (int j = size - 1; j > -1; j--) {
                vec.remove(content.get(j).index);
            }
            for (int j = 0; j < vec.size(); j++) {
                newIndex[j] = vec.get(j);
            }
            mean = mean.getMatrix(newIndex, 0, 0);
            cov = cov.getMatrix(newIndex, newIndex);
            dim -= size;
            reset(lastIndex, m - size);
            int x = 0; // how many have been dropped so far

            int k = 0;
            for (int j = 0; j < sentIndex.length; j++) {
                if (k < size && sentIndex[j] == content.get(k).index) { // intersect
                    // of
                    // sentIndex
                    // and
                    // predictIndex

                    sentIndex[j] += (m - size);
                    sentValues.set(j, 0, content.get(k).value);
                    x++;
                    k++;
                } else {
                    sentIndex[j] -= x;
                }
            }

        }

        // check if we have sth to predict
        int[] predictIndex = new int[m];
        reset(predictIndex, dim - m);
        if (size < m) {
            Matrix prediction = predict(mean, cov, sentIndex, predictIndex, sentValues);

            if (size != 0) {
                for (IndexValuePair p : content) {
                    prediction.set(p.index, 0, p.value);
                }
            }
        }

    // update mean and cov to drop useless terms
    // dim -=
    // System.out.println("===== prediction @ base station");
    // prediction.print(0, 7);

    }
}
