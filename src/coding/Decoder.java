package coding;

import java.util.*;

import sim.nodes.ClusterMessage;

//import sim.nodes.IndexValuePair;
//import sim.nodes.NetworkConfiguration;

class StateTrace {
	public Vector<StateTrace> parents;
	public Vector<StateTrace> children;
	public State cur;
	public Vector<Symbol> inputs;
	public int time;
    // whether the transmission succeeded or not
	public boolean success;
	public int seq;
	
	public StateTrace(State current, StateTrace parent, Symbol input, int time, boolean success, int seq) {
		this.cur = current;
		this.parents = new Vector<StateTrace>();
		this.children = new Vector<StateTrace>();
		parents.add(parent);
		this.inputs = new Vector<Symbol>();
		inputs.add(input);
		this.time = time;
		this.success = success;
		this.seq = seq;
	}
}

public class Decoder {
	LinkedList<StateTrace> curStates; // list is sorted by state for easy
	// identification of duplicates
	StateDiagram diag;
	int lastGood = -1;
	boolean bad = false;
	int fieldSize;
        LinkedList<LinkedList<StateTrace>> history =  new LinkedList<LinkedList<StateTrace>>();
	
	public Decoder(EncoderConfiguration conf) {
		diag = StateDiagram.construct(conf);
		this.fieldSize = conf.fieldsize;
		// set initial memory state
		curStates = new LinkedList<StateTrace>();
		curStates.add(new StateTrace(diag.states[0], null, null, -1, true, -1));
                history.add(curStates);
	}

	private void insertNewState(LinkedList<StateTrace> list, State to,
			StateTrace parent, Symbol input, int time, boolean success, int seq) {
		int len = list.size();
		if (len ==0) {
                    StateTrace st = new StateTrace(to, parent, input, time, success, seq);
			list.add(st);
			parent.children.add(st);
			return;
		}
		// change sequential search to binary search
		int p = 0;
		int q = len-1;
		while(p<=q) {
			int mid = p+(q-p)/2;
			if (list.get(mid).cur.getId() < to.getId()) {
				p = mid+1;
			}
			else if (list.get(mid).cur.getId() > to.getId()) {
				q = mid-1;
			}
			else {
				list.get(mid).parents.add(parent);
				list.get(mid).inputs.add(input);
                                parent.children.add(list.get(mid));
				break;
			}
		}
		if (p>q) { // not found
                    StateTrace st = new StateTrace(to, parent, input, time, success, seq);
			list.add(p, st);
                        parent.children.add(st);
		}/*
		int i=0;
		while (i<len)
		 {
			if (list.get(i).cur.getId() < to.getId()) {
				i++;
				continue;
			}
			else if (list.get(i).cur.getId() == to.getId()) {
				list.get(i).parents.add(parent);
				list.get(i).inputs.add(input);
				break;
			}
			else{ // should insert it here
				list.add(i, new StateTrace(to, parent, input, time, success));
				break;
			}
		}
		if (i==len) { // haven't found
			list.add(i, new StateTrace(to, parent, input, time, success));
		}
			*/
	}

	public ArrayList<DecodeResult> decode(boolean success, Symbol[] output, int time, int seq) {
		if (!success) {
			// for a failed transmission, nothing is received, so enumerate all
			// possible successive states as the next state
			LinkedList<StateTrace> newStates = new LinkedList<StateTrace>();
			for (StateTrace s : curStates) {
				for (Edge e : s.cur.edges.values()) {
					// insert into sorted newStates
					insertNewState(newStates, e.to, s, e.input, time, false, seq);
				}
			}
			curStates = newStates;
                        history.add(newStates);
			bad = true;
			return null;
		}

		// if we received anything, it is guaranteed to be error-free
		// so we just do a search in the Trellis diagram
		LinkedList<StateTrace> newStates = new LinkedList<StateTrace>();
		boolean prune = false;
		int outid = State.outputToId(output);
		for (StateTrace s : curStates) {
			Edge e;
			if ((e = s.cur.edges.get(outid)) != null) {
				insertNewState(newStates, e.to, s, e.input, time, true, seq);
			}
			else prune = true;
		}
		curStates = newStates;
                history.add(newStates);
		
                ArrayList<DecodeResult> res = new ArrayList<DecodeResult>();
                int k = history.size() - 1;                
                LinkedList<StateTrace> st = history.get(k);
                // test if all states in st have same input
                    Symbol sym = null;
                    int count = 0;
                    for (StateTrace s:st) {
                        for (Symbol symbol:s.inputs)
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
                        StateTrace cur = st.get(0);
                        res.add(new DecodeResult(cur.time, list, cur.success, cur.seq)); 
                        //System.out.println("time"+cur.time);
                    }

                /////
                //prune = true;
                while (prune) {
                    prune = false;
                    k--;
                    st = history.get(k);
                    System.out.println("time"+st.get(0).time);
                    for (int i=0; i<st.size();) {
                        if (st.get(i).children.size()==0) {
                            for (StateTrace t: st.get(i).parents)
                                t.children.remove(st.get(i));
                            st.remove(i);
                            prune = true;
                        }
                        else 
                            i++;
                    }
                    if (!prune) break;
                    // test if all states in st have same input
                    sym = null;
                    count = 0;
                    for (StateTrace s:st) {
                        for (Symbol symbol:s.inputs)
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
                        StateTrace cur = st.get(0);
                        if (res.size()==0) {
                            StateTrace temp = history.get(k+1).get(0);
                            res.add(new DecodeResult(temp.time,null,true,0));
                        }
                        res.add(new DecodeResult(cur.time, list, cur.success, cur.seq));                        
                    }
                }
                    
                    if (res.size()==0) return null;
                    return res;
                /////
		
		// check if current state is unique;
		// if so, should trace back and determine all past states which now
		// become unique
		// for now don't do that
//		if (curStates.size()==1 ) {
//				// determinant now
//				if (bad) {
//					// need to trace back
//					StateTrace t = curStates.get(0);				
//					while (t.time > lastGood) {
//						if (t.parents.size()==1) {
//							int sent = t.inputs.get(0).data;
//							ArrayList<sim.nodes.IndexValuePair> list = new ArrayList<sim.nodes.IndexValuePair>();
//							for (int i=0; i<fieldSize; i++) {
//								if ((sent & (1<<i))!=0)
//									list.add(new sim.nodes.IndexValuePair(i,-1));
//							}
//							res.add(new DecodeResult(t.time, list, t.success, t.seq));
//							t = t.parents.get(0);
//						}
//						else {
//							/*
//							 * Current state is determinant but has multiple parents.
//							 * Further info won't help determine those parents.
//							 * Trace back and determine previous *unique* inputs.
//							 */
//							Queue<StateTrace> par = new LinkedList<StateTrace>();
//							par.add(t);
//							count = 1;
//							
//							boolean okay = true;
//							while (okay) {
//								Symbol s = null;
//								int count1 = 0;
//								// elements with the same recursive level in the queue are processed in a batch
//								StateTrace cur = par.peek();
//								for (int i=0 ;i<count; i++) {
//									StateTrace st1 = par.poll();
//									int start = 1;
//									if (s == null) s = st1.inputs.get(0);
//									else start = 0;
//									for (int j=start; j<st1.inputs.size(); j++)
//										if (!s.equals(st1.inputs.get(j))) {
//											okay = false;
//											break;
//										}
//									// append all st's parents
//									for (StateTrace temp:st1.parents)
//										par.offer(temp);
//									count1 += st1.parents.size();
//								}
//								// batch done
//								count = count1;
//								if (okay) {
//									// determined input
//									int sent = s.data;
//									ArrayList<sim.nodes.IndexValuePair> list = new ArrayList<sim.nodes.IndexValuePair>();
//									for (int i=0; i<fieldSize; i++) {
//										if ((sent & (1<<i))!=0)
//											list.add(new sim.nodes.IndexValuePair(i,-1));
//									}
//									res.add(new DecodeResult(cur.time, list, cur.success, cur.seq));
//								}
//							}
//							break;
//						}
//					}
//					bad = false;
//					lastGood = time;
//					return res;
//				}
//				else {// don't trace back; just return current decoded result
//					int sent = curStates.get(0).inputs.get(0).data;
//					ArrayList<sim.nodes.IndexValuePair> list = new ArrayList<sim.nodes.IndexValuePair>();
//					for (int i=0; i<fieldSize; i++) {
//						if ((sent & (1<<i))!=0)
//							list.add(new sim.nodes.IndexValuePair(i,-1));
//					}
//					res.add(new DecodeResult(time, list, true, seq));
//					lastGood = time;
//					return res;
//				}			
//		}
//		else { // multiple possible states
//			bad = true;
//			return null;
//		}

	}
}
