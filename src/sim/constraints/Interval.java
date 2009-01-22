package sim.constraints;

import java.util.*;
import sim.nodes.*;
public class Interval {
	public int begin;
	public int end;
	public int type;
	public static final int GOOD = 1;
	public static final int BAD = 0;
	public static final int UNKNOWN = -1;
	
	public TransmissionRecord txRecord;

	public Interval(int b, int e, int t) {
		begin = b;
		end = e;
		type = t;
	}

	@Override
	public String toString() {
		return (type == BAD ? "!" : "") + begin + "-" + end;
	}
}
