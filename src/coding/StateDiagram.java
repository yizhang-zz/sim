package coding;

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
	public State[] states;
	EncoderConfiguration conf;

	private StateDiagram(EncoderConfiguration conf) {
	    this.conf = conf;
		this.size = (int) Math.pow(2, conf.fieldsize * conf.memsize);
		states = new State[size];
		for (int i = 0; i < size; i++)
			states[i] = new State(i);
	}

	public static StateDiagram construct(EncoderConfiguration conf) {
		StateDiagram diag = new StateDiagram(conf);
		Encoder encoder = new Encoder(conf);
		for (int i = 0; i < diag.size; i++) {
			State state = diag.states[i];
			int inputSize = (int) Math.pow(2, conf.fieldsize);
			// for each state, there're 2^fieldsize possible inputs; generate all these edges
			for (int j = 0; j < inputSize; j++) {
				Symbol in = new Symbol(j);
				encoder.setMem(state.symbols);
				Symbol[] out = encoder.encode(in);
				State to = diag.states[State.outputToId(encoder.mem)];
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