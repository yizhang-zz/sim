package coding;

import java.util.*;
import java.math.*;

class Edge {
	State from;
	State to;
	Symbol input;
	Symbol[] output;

	public Edge(State to, Symbol input, Symbol[] output) {
		this.to = to;
		this.input = input;
		this.output = output;
	}
}

public class StateDiagram {
	int size;
	int fieldSize, memSize;
	State[] states;

	private StateDiagram(int fieldSize, int memSize) {
		this.fieldSize = fieldSize;
		this.memSize = memSize;
		this.size = (int) Math.pow(2, fieldSize * memSize);
		states = new State[size];
		for (int i = 0; i < size; i++)
			states[i] = new State(i);
	}

	public static StateDiagram construct(int fieldSize, int memsize,
			Encoder encoder) {
		StateDiagram diag = new StateDiagram(fieldSize, memsize);

		for (int i = 0; i < diag.size; i++) {
			State state = diag.states[i];
			int inputSize = (int) Math.pow(2, fieldSize);
			// for each state, there're 2^fieldsize possible inputs; generate all these edges
			for (int j = 0; j < inputSize; j++) {
				Symbol in = new Symbol(j);
				Symbol[] all = new Symbol[memsize];
				all[0] = in;
				for (int k=1; k<memsize; k++)
					all[k] = state.symbols[k-1];
				Symbol[] out = Encoder.encode(all);
				State to = diag.states[State.outputToId(all)];
				state.edges.put(State.outputToId(out), new Edge(to,in,out));
			}
		}
		return diag;
	}
	
	public void output() {
		//StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++) {
			State state = states[i];
			System.out.println("** From state "+state);
			for (Edge e:state.edges.values()) {				
				System.out.println("in:"+e.input+"\tout:"+new State(e.output)+"\tnew:"+e.to);
			}
		}
	}
}