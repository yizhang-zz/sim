package coding;
import java.util.ArrayList;

import sim.nodes.*;
public class DecodeResult {
	public int time;
	public ArrayList<IndexValuePair> list;
	public boolean success;
	public int seq;
	
	public DecodeResult(int time, ArrayList<IndexValuePair> list, boolean success, int seq) {
		this.time = time;
		this.list = list;
		this.success = success;
		this.seq = seq;
	}
}
