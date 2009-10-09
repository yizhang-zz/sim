package coding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

class StatePair {
    Symbol[] before;
    Symbol[] after;
    Symbol input;
    Symbol[] output;
    

    public StatePair(Symbol[] before, Symbol[] after, Symbol input, Symbol[] output) {
        super();
        this.before = before;
        this.after = after;
        this.input = input;
        this.output = output;
    }


//    @Override
//    public int compareTo(StatePair x) {
//        for(int i=0; i<after.length; i++) {
//            int a = after[i].toInt();
//            int b = x.after[i].toInt();
//            if (a == b)
//                continue;
//            return a-b;
//        }
//        return 0;
//    }
}

public class CodingTable {
    private List<StatePair>[] table;
    private List<StatePair>[] table2;
    private List<StatePair>[] table3;
    private List<StatePair>[] table4;
    EncoderConfiguration conf;
    
    private static Comparator<StatePair> beforeComparator = new Comparator<StatePair>() {
        @Override
        public int compare(StatePair x, StatePair y) {
            for(int i=0; i<x.before.length; i++) {
                int a = x.before[i].toInt();
                int b = y.before[i].toInt();
                if (a == b)
                    continue;
                return a-b;
            }
            return 0;        
        }
    };
    
    private static Comparator<StatePair> afterComparator = new Comparator<StatePair>() {
        @Override
        public int compare(StatePair x, StatePair y) {
            for(int i=0; i<x.after.length; i++) {
                int a = x.after[i].toInt();
                int b = y.after[i].toInt();
                if (a == b)
                    continue;
                return a-b;
            }
            return 0;        
        }
    };
    
    public CodingTable(EncoderConfiguration conf) {
        this.conf = conf;
        int len = (int)Math.pow(2, conf.outputsize*conf.fieldsize);
        table = new LinkedList[len];
        table2 = new LinkedList[len];
        for (int i=0; i<len; i++) {
            table[i] = new LinkedList<StatePair>();
            table2[i] = new LinkedList<StatePair>();
        }
        len = (int)Math.pow(2, conf.memsize*conf.fieldsize);
        table3 = new LinkedList[len];
        table4 = new ArrayList[len];
        for (int i=0; i<len; i++) {
            table3[i] = new LinkedList<StatePair>();
            table4[i] = new ArrayList<StatePair>();
        }
        
        int stateCount = (int)Math.pow(2, conf.memsize*conf.fieldsize);
        int inputCount = (int)Math.pow(2, conf.inputsize*conf.fieldsize);
        Symbol[] inputs = new Symbol[inputCount];
        for (int i=0; i<inputCount; i++)
            inputs[i] = new Symbol(i);
        for (int i=0; i<stateCount; i++) {
            State before = new State(i, conf);
            for (int j=0; j<inputCount; j++) {
                Symbol[] after = new Symbol[conf.memsize];
                Symbol[] out = Encoder.encode(before.symbols, inputs[j], after, conf);
                StatePair s = new StatePair(before.symbols,after,inputs[j], out);
                int outid = State.toInt(out, conf);
                int afterid = State.toInt(after, conf);
                insert(table[outid], s, afterComparator);
                insert(table2[outid], s, beforeComparator);
                insert(table3[afterid], s, beforeComparator);
                table4[i].add(s);
            }
        }
    }
    
    private void insert(List<StatePair> list, StatePair s, Comparator<StatePair> comp) {
        int pos = Collections.binarySearch(list, s, comp);
        if (pos >= 0) { // found one with same 'after'
            list.add(pos, s);
        }
        else { // none found
            pos = -pos-1;
            list.add(pos, s);
        }
    }
    
    /**
     * Finds all transitions that end with the specified state and produce the specified output.
     * @param output The output.
     * @param before The resulting state.
     * @return
     */
    public StatePair lookup(Symbol[] output, Symbol[] after) {
        List<StatePair> list = table[State.toInt(output, conf)];
        StatePair t = new StatePair(null, after, null, null);
        int pos = Collections.binarySearch(list, t, afterComparator);
        if (pos >= 0)
            return list.get(pos);
        return null;
    }
    
    /**
     * Finds all transitions that start from the specified state and produce the specified output.
     * @param output The output.
     * @param before The original state.
     * @return A list of StatePair's.
     */
    public StatePair lookup2(Symbol[] output, Symbol[] before) {
        List<StatePair> list = table2[State.toInt(output, conf)];
        StatePair t = new StatePair(before, null, null, null);
        int pos = Collections.binarySearch(list, t, beforeComparator);
        if (pos >= 0)
            return list.get(pos);
        return null;
    }
    
    /**
     * Finds all transitions that produce the specified output.
     * @param output The output.
     * @return A list of StatePair's.
     */
    public List<StatePair> lookup(Symbol[] output) {
        return table[State.toInt(output, conf)];
    }
    
    /**
     * Finds all transitions that end with the specified state.
     * @param after Result state after transition.
     * @return A list of StatePair's.
     */
    public List<StatePair> lookup3(Symbol[] after) {
        return table3[State.toInt(after, conf)];
    }
    
    /**
     * Finds all transitions that start from the specified state.
     * @param before Starting state before transition.
     * @return A list of StatePair's.
     */    
    public List<StatePair> lookup4(Symbol[] before) {
        return table4[State.toInt(before, conf)];
    }
}
