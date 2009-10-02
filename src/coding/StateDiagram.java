package coding;

import java.util.Hashtable;

public class StateDiagram {
	int size;
	public State[] states;
	EncoderConfiguration conf;
	//int fieldsize;
	//int memsize;

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
				State to = diag.states[diag.outputToId(encoder.mem)];
				state.edges.put(diag.outputToId(out), diag.new Edge(to,in,out));
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
	
    public int outputToId(Symbol[] s)
    {
        int n = 0;
        for (int i = 0; i < s.length; i++)
            n |= (s[i].data << (i * conf.fieldsize));
        return n;
    }
	
	public class State {

	    //static int FIELD_SIZE = 4;
	    //static int MEM_SIZE = 3;
	    int id;
	    public Symbol[] symbols;
	    Hashtable<Integer, Edge> edges = new Hashtable<Integer, Edge>();

	    public State(int id) {
	        setId(id);
	    }
	    
	    public State(Symbol[] s) {
	        setSymbols(s);
	    }
	    
	    public int getId() {
	        return id;
	    }
	    
	    public void setSymbols(Symbol[] s) {
	        symbols = s;
	        int n = 0;
	        for (int i = 0; i < symbols.length; i++)
	            n |= (symbols[i].data << (i * conf.fieldsize));
	        id = n;
	    }
	    
	    public void setId(int id) {
	        this.id = id;
	        symbols = new Symbol[conf.memsize];
	        for (int i=0; i<conf.memsize; i++){
	            symbols[i] = new Symbol((id>>>(i*conf.fieldsize))& ((1<<conf.fieldsize)-1));
	        }
	    }
	    
	    public String toString() {
	        String s = symbols[0].toString();
	        for (int i=1; i<symbols.length; i++)
	            s += ","+symbols[i];
	        return s;
	    }
	}
	
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
}