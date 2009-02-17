package coding;

import java.util.Hashtable;

public class State {

	static int FIELD_SIZE = 4;
	static int MEM_SIZE = 3;
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
			n |= (symbols[i].data << (i * FIELD_SIZE));
		id = n;
	}
	
	public void setId(int id) {
		this.id = id;
		symbols = new Symbol[MEM_SIZE];
		for (int i=0; i<MEM_SIZE; i++){
			symbols[i] = new Symbol((id>>>(i*FIELD_SIZE))& ((1<<FIELD_SIZE)-1));
		}
	}
	
	public static int outputToId(Symbol[] s)
	{
		int n = 0;
		for (int i = 0; i < s.length; i++)
			n |= (s[i].data << (i * FIELD_SIZE));
		return n;
	}
	
	public String toString() {
		String s = symbols[0].toString();
		for (int i=1; i<symbols.length; i++)
			s += ","+symbols[i];
		return s;
	}

}
