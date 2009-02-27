package sim.constraints;

import sim.nodes.TransmissionRecord;
public class Interval {
    public enum Type {GOOD, BAD, UNKNOWN};
	public int begin;
	public int end;
	public Type type;
	public int seq;
//	public static final int GOOD = 1;
//	public static final int BAD = 0;
//	public static final int UNKNOWN = -1;
	
	public TransmissionRecord txRecord;

	public Interval(int b, int e, Type t, int s) {
		begin = b;
		end = e;
		type = t;
		seq = s;
	}

	@Override
	public String toString() {
		return (type == Type.BAD ? "!" : "") + begin + "-" + end;
	}
}
