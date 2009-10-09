package coding;

import java.util.*;

class StateTrace {
    public Vector<StateTrace> parents;
    public Vector<StateTrace> children;
    public State state;
    public Vector<Symbol> inputs;
//    public int time;
    // whether the transmission succeeded or not
//    public boolean success;
//    public int seq;

    public StateTrace(State current, StateTrace parent, Symbol input) {
        this.state = current;
        this.parents = new Vector<StateTrace>();
        this.children = new Vector<StateTrace>();
        this.inputs = new Vector<Symbol>();
        if (parent != null) {
            parents.add(parent);
            inputs.add(input);
        }
//        this.time = time;
//        this.success = success;
//        this.seq = seq;
    }
}

class StateCollection extends LinkedList<StateTrace>{
    /*
     * List is sorted by state for easy identification of duplicates.
     */
    //public LinkedList<StateTrace> states;
    public boolean success;
    public int seq;
    public int time;
    public Symbol[] output;
    
    /**
     * Whether states in this collection has been connected with states in next time step.
     */
    public boolean connectedWithNext;
    
    public StateCollection(boolean success, int time, int seq, Symbol[] output) {
        super();
        this.success = success;
        this.seq = seq;
        this.time = time;
        this.output = output;
    }
    
    /**
     * Searches for a state with stateId.
     * 
     * @param stateId
     *            ID of state to be searched for
     * @return position of state if found; -(insertionPos+1) if not found, where
     *         insertionPos is the position of state if it is to be inserted.
     */
    public int search(int stateId) {
        int len = this.size();
        int p = 0;
        int q = len - 1;
        int mid = 0;
        while (p <= q) {
            mid = p + (q - p) / 2;
            if (this.get(mid).state.hashCode() < stateId) {
                p = mid + 1;
            } else if (this.get(mid).state.hashCode() > stateId) {
                q = mid - 1;
            } else {
                return mid;
            }
        }
        return -p-1;
    }
    
    /**
     * Inserts a new state into this collection of possible states for a particular time step.
     * The new state is (doubly) linked to its parent state in the previous time step.
     * @param to the New state.
     * @param parent The parent state in the previous time step; can be null.
     * @param input The input that leads from parent to the current state; should be null if parent is null.
     */
    public StateTrace insert(State to, StateTrace parent, Symbol input) {
        int len = this.size();
        if (len == 0) {
            StateTrace st = new StateTrace(to, parent, input);
            this.add(st);
            if (parent != null)
                parent.children.add(st);
            return st;
        }
        int pos = search(to.hashCode());
        StateTrace st = null;
        if (pos >= 0) {     // found
            if (parent != null) {
                st = this.get(pos);
                st.inputs.add(input);
                st.parents.add(parent);
                parent.children.add(st);
            }           
        }
        else {
            pos = -pos-1;
            st = new StateTrace(to, parent, input);
            this.add(pos, st);
            if (parent != null) {
                parent.children.add(st);
            }            
        }
        return st;
        
        // change sequential search to binary search
//        int p = 0;
//        int q = len - 1;
//        int mid = 0;
//        while (p <= q) {
//            mid = p + (q - p) / 2;
//            if (this.get(mid).state.hashCode() < to.hashCode()) {
//                p = mid + 1;
//            } else if (this.get(mid).state.hashCode() > to.hashCode()) {
//                q = mid - 1;
//            } else {
//                if (parent != null) {
//                    this.get(mid).inputs.add(input);
//                    this.get(mid).parents.add(parent);
//                    parent.children.add(this.get(mid));
//                }
//            }
//        }
//        if (p > q) { // not found
//            StateTrace st = new StateTrace(to, parent, input);
//            this.add(p, st);
//            if (parent != null) {
//                parent.children.add(st);
//            }
//            return st;
//        }
//        return this.get(mid);
        /*
          * int i=0; while (i<len) { if (list.get(i).cur.getId() < to.getId()) {
          * i++; continue; } else if (list.get(i).cur.getId() == to.getId()) {
          * list.get(i).parents.add(parent); list.get(i).inputs.add(input);
          * break; } else{ // should insert it here list.add(i, new
          * StateTrace(to, parent, input, time, success)); break; } } if
          * (i==len) { // haven't found list.add(i, new StateTrace(to, parent,
          * input, time, success)); }
          */
    }
}

public class Decoder {
    //LinkedList<StateTrace> curStates; // list is sorted by state for easy
    // 
    // StateDiagram diag;
    int lastGood = -1;
    //boolean bad = false;
    int fieldSize;
    LinkedList<StateCollection> history = new LinkedList<StateCollection>();
    EncoderConfiguration conf;
    CodingTable table;

    public Decoder(EncoderConfiguration conf) {
        this.conf = conf;
        this.fieldSize = conf.fieldsize;
        this.table = new CodingTable(conf);
        // set initial memory state
        //curStates = new LinkedList<StateTrace>();
        Symbol[] sym = new Symbol[conf.memsize];
        for (int i = 0; i < conf.memsize; i++)
            sym[i] = new Symbol(0);
        State initialState = new State(sym, conf);
        StateCollection initial = new StateCollection(true, -1, -1, null);
        initial.add(new StateTrace(initialState, null, null));
        //curStates.add(new StateTrace(initialState, null, null, -1, true, -1));
        history.add(initial);
    }


    public ArrayList<DecodeResult> decode(boolean success, Symbol[] output,
            int time, int seq) {
        System.out.println("time "+time);
        StateCollection last = history.getLast();
        
        if (!success) {
            StateCollection current = new StateCollection(false, time, seq, null);

            // for a failed transmission, nothing is received, so enumerate all
            // possible successive states as the next state
            
            if (last.size() == 0 
                    || last.size() == Math.pow(2,conf.fieldsize*(conf.memsize-conf.inputsize))) {
                // The new curStates will be all possible states,
                // so no need to enumerate all states.
            }
            else {
                for (StateTrace s : last) {
                    List<StatePair> temp = table.lookup4(s.state.symbols);
                    for (StatePair pair : temp) {
                        current.insert(new State(pair.after, conf), s, pair.input);
                    }
                }
                last.connectedWithNext = true;
            }
            history.add(current);
            return null;
        }

        // if we received anything, it is guaranteed to be error-free
        // so we just do a search in the Trellis diagram
        boolean prune = false;
        // int outid = diag.outputToId(output);
        //State out = new State(output, conf);
        
        // Range of state at last time is the whole universe.
        StateCollection current = new StateCollection(true, time, seq, output);
        if (last.size() == 0) {
            List<StatePair> pairs = table.lookup(output);
            int len = pairs.size();
            // possible current states
            for (int i=0; i<len; i++) {
                StatePair pair = pairs.get(i);
                //StateTrace st = new StateTrace(new State(pair.after, conf), null, pair.input);
                StateTrace st = last.insert(new State(pair.before, conf), null, null);
                current.insert(new State(pair.after, conf), st, pair.input);
            }
            history.add(current);
            prune = true;
        }
        else {
            for (StateTrace s : last) {
                StatePair pair;
                if ((pair = table.lookup2(output, s.state.symbols)) != null) {
                    current.insert(new State(pair.after, conf), s, pair.input);
                } else
                    prune = true;
                // if ((e = s.state.getEdge(output)) != null) {
                // insertNewState(newStates, e.to, s, e.input, time, true, seq);
                // } else
                // prune = true;
            }
            history.add(current);
        }
        last.connectedWithNext = true;

        ArrayList<DecodeResult> res = new ArrayList<DecodeResult>();
        int k = history.size() - 1;
        StateCollection st = history.get(k);
        // test if all states in st have same input
        Symbol sym = null;
        int count = 0;
        for (StateTrace s : st) {
            for (Symbol symbol : s.inputs)
                if (!symbol.equals(sym)) {
                    count++;
                    sym = symbol;
                }
        }
        if (count == 1) {
            int sent = sym.data;
            ArrayList<sim.nodes.IndexValuePair> list = new ArrayList<sim.nodes.IndexValuePair>();
            for (int i = 0; i < fieldSize; i++) {
                if ((sent & (1 << i)) != 0) {
                    list.add(new sim.nodes.IndexValuePair(i, -1));
                }
            }
            res.add(new DecodeResult(st.time, list, st.success, st.seq));
        }

        //
        // prune = true;
        while (prune) {
            prune = false;
            k--;
            st = history.get(k);
            System.out.println("back to time" + st.time);
            /*
             * If no states have been pre-generated yet, then no coded messages has been received
             * for next time step. Not worth doing a prunning search at this time.
             */
            if (st.size() == 0) {
                break;
            }
            
            StateCollection pre = history.get(k-1);
            boolean preEmpty = (pre.size() == 0);
            Hashtable<Integer,StateTrace> preStates = new Hashtable<Integer,StateTrace>();
            for (int i = 0; i < st.size();) {
                /*
                 * Parents of elements in st may be empty because states for
                 * time k-1 may have not been generated yet.
                 */
                if (st.get(i).children.size() == 0) {
                    prune = true;
                    if (!preEmpty && pre.connectedWithNext) {
                    for (StateTrace t : st.get(i).parents)
                        t.children.remove(st.get(i));
                    }
                    st.remove(i);
                } else {
                    if (preEmpty) {
                        //prune = true;
                        // add parents
                        StateTrace s = st.get(i);
                        List<StatePair> list = table.lookup3(s.state.symbols);
                        for (StatePair x : list) {
                            StateTrace par = null;
                            int parId = State.toInt(x.before, conf);
                            if (preStates.contains(parId))
                                par = preStates.get(parId);
                            else {
                                // par = new StateTrace(new State(x.before, conf), null, null);
                                par = pre.insert(new State(x.before, conf), null, null);
                                preStates.put(parId, par);
                            }
                            s.inputs.add(x.input);
                            s.parents.add(par);
                            par.children.add(s);
                        }                     
                    }
                    else if (!pre.connectedWithNext) {
                        //prune = true;
                        // Link parents with states here
                        StateTrace s = st.get(i);
                        List<StatePair> list = table.lookup3(s.state.symbols);
                        for (StatePair x : list) {
                            int parId = State.toInt(x.before, conf);                            
                            int pos = pre.search(parId);
                            if (pos >= 0) {
                                StateTrace par = pre.get(pos);
                                par.children.add(s);
                                s.inputs.add(x.input);
                                s.parents.add(par);
                            }
                        }
                    }
                    i++;
                }
            }
            pre.connectedWithNext = true;
            
            // test if all states in st have same input
            sym = null;
            count = 0;
            for (StateTrace s : st) {
                for (Symbol symbol : s.inputs)
                    if (!symbol.equals(sym)) {
                        count++;
                        sym = symbol;
                    }
            }
            if (count == 1) {
                // determined input
                int sent = sym.data;
                ArrayList<sim.nodes.IndexValuePair> list = new ArrayList<sim.nodes.IndexValuePair>();
                for (int i = 0; i < fieldSize; i++) {
                    if ((sent & (1 << i)) != 0) {
                        list.add(new sim.nodes.IndexValuePair(i, -1));
                    }
                }

                if (res.size() == 0) {
                    StateCollection temp = history.get(k + 1);
                    res.add(new DecodeResult(temp.time, null, true, 0));
                }
                res.add(new DecodeResult(st.time, list, st.success, st.seq));
            }
            if (!prune)
                break;
        }

        if (res.size() == 0)
            return null;
        return res;

        // check if current state is unique;
        // if so, should trace back and determine all past states which now
        // become unique
        // for now don't do that
        // if (curStates.size()==1 ) {
        // // determinant now
        // if (bad) {
        // // need to trace back
        // StateTrace t = curStates.get(0);
        // while (t.time > lastGood) {
        // if (t.parents.size()==1) {
        // int sent = t.inputs.get(0).data;
        // ArrayList<sim.nodes.IndexValuePair> list = new
        // ArrayList<sim.nodes.IndexValuePair>();
        // for (int i=0; i<fieldSize; i++) {
        // if ((sent & (1<<i))!=0)
        // list.add(new sim.nodes.IndexValuePair(i,-1));
        // }
        // res.add(new DecodeResult(t.time, list, t.success, t.seq));
        // t = t.parents.get(0);
        // }
        // else {
        // /*
        // * Current state is determinant but has multiple parents.
        // * Further info won't help determine those parents.
        // * Trace back and determine previous *unique* inputs.
        // */
        // Queue<StateTrace> par = new LinkedList<StateTrace>();
        // par.add(t);
        // count = 1;
        //							
        // boolean okay = true;
        // while (okay) {
        // Symbol s = null;
        // int count1 = 0;
        // // elements with the same recursive level in the queue are processed
        // in a batch
        // StateTrace cur = par.peek();
        // for (int i=0 ;i<count; i++) {
        // StateTrace st1 = par.poll();
        // int start = 1;
        // if (s == null) s = st1.inputs.get(0);
        // else start = 0;
        // for (int j=start; j<st1.inputs.size(); j++)
        // if (!s.equals(st1.inputs.get(j))) {
        // okay = false;
        // break;
        // }
        // // append all st's parents
        // for (StateTrace temp:st1.parents)
        // par.offer(temp);
        // count1 += st1.parents.size();
        // }
        // // batch done
        // count = count1;
        // if (okay) {
        // // determined input
        // int sent = s.data;
        // ArrayList<sim.nodes.IndexValuePair> list = new
        // ArrayList<sim.nodes.IndexValuePair>();
        // for (int i=0; i<fieldSize; i++) {
        // if ((sent & (1<<i))!=0)
        // list.add(new sim.nodes.IndexValuePair(i,-1));
        // }
        // res.add(new DecodeResult(cur.time, list, cur.success, cur.seq));
        // }
        // }
        // break;
        // }
        // }
        // bad = false;
        // lastGood = time;
        // return res;
        // }
        // else {// don't trace back; just return current decoded result
        // int sent = curStates.get(0).inputs.get(0).data;
        // ArrayList<sim.nodes.IndexValuePair> list = new
        // ArrayList<sim.nodes.IndexValuePair>();
        // for (int i=0; i<fieldSize; i++) {
        // if ((sent & (1<<i))!=0)
        // list.add(new sim.nodes.IndexValuePair(i,-1));
        // }
        // res.add(new DecodeResult(time, list, true, seq));
        // lastGood = time;
        // return res;
        // }
        // }
        // else { // multiple possible states
        // bad = true;
        // return null;
        // }

    }
}
