package sim.nodes;

public class IndexValuePair {
	int index;
	double value;
	public IndexValuePair(int i, double v) {
		index = i;
		value = v;
	}
	@Override
	public String toString() {
		return String.valueOf(index)+":"+value;
	}
}
