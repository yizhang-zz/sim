package coding;

import java.util.*;

import sim.nodes.ClusterMessage;

//import sim.nodes.IndexValuePair;
//import sim.nodes.NetworkConfiguration;

class StateTrace {
	public Vector<StateTrace> parents;
	public State cur;
	public Vector<Symbol> inputs;
	public int time;
    // whether the transmission succeeded or not
	public boolean success;
	public int seq;
	
	public StateTrace(State current, StateTrace parent, Symbol input, int time, boolean success, int seq) {
		this.cur = current;
		this.parents = new Vector<StateTrace>();
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
	
	public Decoder(EncoderConfiguration conf) {
		diag = StateDiagram.construct(conf);
		this.fieldSize = conf.fieldsize;
		// set initial memory state
		curStates = new LinkedList<StateTrace>();
		curStates.add(new StateTrace(diag.states[0], null, null, -1, true, -1));
	}

	private void insertNewState(LinkedList<StateTrace> list, State to,
			StateTrace parent, Symbol input, int time, boolean success, int seq) {
		int len = list.size();
		if (len ==0) {
			list.add(new StateTrace(to, parent, input, time, success, seq));
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
				break;
			}
		}
		if (p>q) { // not found
			list.add(p, new StateTrace(to, parent, input, time, success, seq));
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
			bad = true;
			return null;
		}

		// if we received anything, it is guaranteed to be error-free
		// so we just do a search in the Trellis diagram
		LinkedList<StateTrace> newStates = new LinkedList<StateTrace>();
		int outid = State.outputToId(output);
		for (StateTrace s : curStates) {
			Edge e;
			if ((e = s.cur.edges.get(outid)) != null) {
				insertNewState(newStates, e.to, s, e.input, time, true, seq);
			}
		}
		curStates = newStates;
		
		ArrayList<DecodeResult> res = new ArrayList<DecodeResult>();
		// check if current state is unique;
		// if so, should trace back and determine all past states which now
		// become unique
		// for now don't do that
		if (curStates.size()==1 ) {
				// determinant now
				if (bad) {
					// need to trace back
					StateTrace t = curStates.get(0);				
					while (t.time > lastGood) {
						if (t.parents.size()==1) {
							int sent = t.inputs.get(0).data;
							ArrayList<sim.nodes.IndexValuePair> list = new ArrayList<sim.nodes.IndexValuePair>();
							for (int i=0; i<fieldSize; i++) {
								if ((sent & (1<<i))!=0)
									list.add(new sim.nodes.IndexValuePair(i,-1));
							}
							res.add(new DecodeResult(t.time, list, t.success, t.seq));
							t = t.parents.get(0);
						}
						else {
							/*
							 * Current state is determinant but has multiple parents.
							 * Further info won't help determine those parents.
							 * Trace back and determine previous *unique* inputs.
							 */
							Queue<StateTrace> par = new LinkedList<StateTrace>();
							par.add(t);
							int count = 1;
							
							boolean okay = true;
							while (okay) {
								Symbol s = null;
								int count1 = 0;
								// elements with the same recursive level in the queue are processed in a batch
								StateTrace cur = par.peek();
								for (int i=0 ;i<count; i++) {
									StateTrace st = par.poll();
									int start = 1;
									if (s == null) s = st.inputs.get(0);
									else start = 0;
									for (int j=start; j<st.inputs.size(); j++)
										if (!s.equals(st.inputs.get(j))) {
											okay = false;
											break;
										}
									// append all st's parents
									for (StateTrace temp:st.parents)
										par.offer(temp);
									count1 += st.parents.size();
								}
								// batch done
								count = count1;
								if (okay) {
									// determined input
									int sent = s.data;
									ArrayList<sim.nodes.IndexValuePair> list = new ArrayList<sim.nodes.IndexValuePair>();
									for (int i=0; i<fieldSize; i++) {
										if ((sent & (1<<i))!=0)
											list.add(new sim.nodes.IndexValuePair(i,-1));
									}
									res.add(new DecodeResult(cur.time, list, cur.success, cur.seq));
								}
							}
							break;
						}
					}
					bad = false;
					lastGood = time;
					return res;
				}
				else {// don't trace back; just return current decoded result
					int sent = curStates.get(0).inputs.get(0).data;
					ArrayList<sim.nodes.IndexValuePair> list = new ArrayList<sim.nodes.IndexValuePair>();
					for (int i=0; i<fieldSize; i++) {
						if ((sent & (1<<i))!=0)
							list.add(new sim.nodes.IndexValuePair(i,-1));
					}
					res.add(new DecodeResult(time, list, true, seq));
					lastGood = time;
					return res;
				}			
		}
		else { // multiple possible states
			bad = true;
			return null;
		}

	}
}
