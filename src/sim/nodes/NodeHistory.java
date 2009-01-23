package sim.nodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class records the latest transmission history for a child node.
 * The capacity is limited.
 * @author Yi Zhang
 *
 */
public class NodeHistory implements Iterable<NodeHistory.Record>{
	// how many past msgs (from children) do we keep as redundancies (and to discover failures)
	private static final int MAX_HISTORY_SIZE = 6;
	/**
	 * from when the known interval starts (inclusive)
	 */
	public int beginKnown;
	/**
	 * till when the known interval ends (inclusive)
	 */
	public int endKnown;
	private List<Record> records;
	private int capacity;
	
	public NodeHistory(int cap) {
		capacity = cap;
		records = new ArrayList<Record>(cap);
	}
	
	public NodeHistory() {
		this(MAX_HISTORY_SIZE);
	}
	
	public Record get(int i) {
		return records.get(i);
	}
	
	public int size() {
		return records.size();
	}
	
	public void add(int time, int status){
		Record rec = new Record(time, status);
		if (records.size()==capacity) {
			records.remove(0);
		}
		records.add(rec);
	}
	
	//@Override
	public Iterator<Record> iterator() {
		return records.iterator();
	}	
	class Record {
		public static final int SUCCESS = 1;
		public static final int FAILURE = 0;
		public static final int NEW_FAILURE = -1;
		public int time;
		public int status;

		public Record(int e, int r) {
			time = e;
			status = r;
		}

		@Override
		public String toString() {
			if (status == SUCCESS)
				return String.valueOf(time);
			if (status == FAILURE)
				return String.valueOf(time) + "*";
			return String.valueOf(time) + "^";
		}
	}


}
