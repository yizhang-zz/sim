package coding;

public class Encoder {
    EncoderConfiguration conf;
	Symbol[] mem;

	public Encoder(EncoderConfiguration conf) {
	    this.conf = conf;
		mem = new Symbol[conf.memsize];
		for (int i=0; i<conf.memsize; i++)
			mem[i] = new Symbol();
	}
	
	private void shift(Symbol s) {
		for (int i=conf.memsize-1; i>0; i--) {
			mem[i] = mem[i-1];
		}
		mem[0] = s;
	}
	
	public void setMem(Symbol[] s) {
	    mem = s.clone();
	}
	
	public Symbol[] encode(Symbol in) {
	    // denominator always starts with constant term 1
	    // so the input symbol is always included
	    Symbol feedback = in;
	    for (int i=1 ;i <= conf.memsize; i++) {
	        if (conf.denominator[i]!=0)
	            feedback = feedback.add(mem[i-1]);
	    }
	    // conf.outputs[i] specifies the selection of the i-th output as:
	    // 0        1      2      ...
	    // feedback mem[0] mem[1] ...
		Symbol[] output = new Symbol[conf.outputsize];
		for (int i=0; i<conf.outputsize; i++) {
			output[i] = Symbol.ZERO;
			if (conf.outputs[i][0]!=0)
			    output[i] = output[i].add(feedback);
			for (int j=1; j <= conf.memsize; j++)
				if (conf.outputs[i][j]!=0)
					output[i] = output[i].add(mem[j-1]);
		}
		shift(feedback);
		return output;
	}
}
