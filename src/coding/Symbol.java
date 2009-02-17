package coding;

public class Symbol {
	int data = 0;

	public Symbol(Symbol s) {
		data = s.data;
	}
	
	public Symbol() {}
	
	public boolean equals(Object o) {
		return (o!=null) && (o instanceof Symbol) && (((Symbol)o).data==this.data);
	}
	public Symbol(int n) {
		data = n;
	}

	public Symbol(int[] sentIndex) {
		data = 0;
		for (int i = 0; i < sentIndex.length; i++) {
			data |= (1 << sentIndex[i]);
		}
	}

	public String toString() {
		return Integer.toBinaryString(data);
	}
	
	public int hashCode(){
		return data;
	}

	public static String id(Symbol[] s) {
		String str = "";
		for (int i=0; i<s.length; i++)
			str += s[i].data;
		return str;
	}
	
	public static Symbol add(Symbol ...symbols) {
		Symbol res = new Symbol(symbols[0]);
		for (int i=1; i<symbols.length; i++) {
			res.data = res.data ^ symbols[i].data;
		}
		return res;
	}
}
