package coding;

public class Edge {
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