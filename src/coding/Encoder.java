package coding;

import java.util.Vector;

public class Encoder {
	static int memsize = 3;
	static int inputsize = 1;
	static int outputsize = 2;
	static int[][] scheme = { { 1, 0, 0 }, { 1, 1, 1 } };
	//static int[][] scheme = { { 1, 0, 0 }, { 1, 1, 0 }, { 1, 1, 1 } };
	Symbol[] mem;

	public Encoder() {
		mem = new Symbol[memsize];
		for (int i=0; i<memsize; i++)
			mem[i] = new Symbol();
	}
	
	private void push(Symbol s) {
		for (int i=memsize-1; i>0; i--) {
			mem[i] = mem[i-1];
		}
		mem[0] = s;
	}
	
	public Symbol[] encode(int[] sentIndex) {
		Symbol input = new Symbol(sentIndex);
		push(input);
		return encode(mem);
	}
	
	public static Symbol[] encode(Symbol[] mem) {
		Symbol[] output = new Symbol[outputsize];
		for (int i=0; i<outputsize; i++) {
			Vector<Symbol> v = new Vector<Symbol>();
			for (int j=0; j<memsize; j++)
				if (scheme[i][j]==1)
					v.add(mem[j]);
			Symbol[] operands = new Symbol[v.size()];
			v.toArray(operands);
			output[i] = Symbol.add(operands);
		}
		return output;
	}
}
