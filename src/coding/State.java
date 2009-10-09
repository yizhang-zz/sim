package coding;

import java.util.Collection;
import java.util.Hashtable;
import coding.Encoder;
import coding.EncoderConfiguration;
import coding.Symbol;

public class State {
    // static int FIELD_SIZE = 4;
    // static int MEM_SIZE = 3;
    // int id;
    public Symbol[] symbols;
    private Hashtable<State, Edge> edges = new Hashtable<State, Edge>();
    private EncoderConfiguration conf;

    @Override
    public int hashCode() {
        int n = 0;
        for (int i = 0; i < symbols.length; i++)
            n |= (symbols[i].toInt() << (i * conf.fieldsize));
        return n;
    }
    
    public static int toInt(Symbol[] symbols, EncoderConfiguration conf) {
        int n = 0;
        for (int i = 0; i < symbols.length; i++)
            n |= (symbols[i].toInt() << (i * conf.fieldsize));
        return n;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof State)
            return hashCode() == obj.hashCode();
        else
            return false;
    }

    // public State(int id) {
    // setId(id);
    // }

    public State(Symbol[] s, EncoderConfiguration conf) {
        this.conf = conf;
        setSymbols(s);
    }

    private void buildEdges() {
        Encoder encoder = new Encoder(conf);
        int inputSize = (int) Math.pow(2, conf.fieldsize);
        // for each state, there're 2^fieldsize possible inputs; generate
        // all these edges
        for (int j = 0; j < inputSize; j++) {
            Symbol in = new Symbol(j);
            encoder.setMem(symbols);
            Symbol[] out = encoder.encode(in);
            State to = new State(encoder.mem, conf);
            edges.put(new State(out, conf), new Edge(to, in, out));
        }
    }

    // public int getId() {
    // return id;
    // }

    public void setSymbols(Symbol[] s) {
        symbols = s;
        int n = 0;
        for (int i = 0; i < symbols.length; i++)
            n |= (symbols[i].data << (i * conf.fieldsize));
        // id = n;
    }

     public State(int id, EncoderConfiguration conf) {
         this.conf = conf;
         symbols = new Symbol[conf.memsize];
         for (int i = 0; i < conf.memsize; i++) {
             symbols[i] = new Symbol((id >>> (i * conf.fieldsize))
                     & ((1 << conf.fieldsize) - 1));
         }
     }

    public String toString() {
        String s = symbols[0].toString();
        for (int i = 1; i < symbols.length; i++)
            s += "," + symbols[i];
        return s;
    }

    private Collection<Edge> getEdges() {
        if (edges.size() == 0)
            buildEdges();
        return edges.values();
    }

    private Edge getEdge(Symbol[] output) {
        Encoder encoder = new Encoder(conf);
        int inputSize = (int) Math.pow(2, conf.fieldsize);
        // for each state, there're 2^fieldsize possible inputs; generate
        // all these edges
        for (int j = 0; j < inputSize; j++) {
            Symbol in = new Symbol(j);
            encoder.setMem(symbols);
            Symbol[] out = encoder.encode(in);
            boolean found = true;
            for (int i = 0; i < output.length; i++)
                if (!output[i].equals(out[i])) {
                    found = false;
                    break;
                }
            if (found)
                return new Edge(new State(encoder.mem, conf), in, out);
        }
        return null;
    }
}