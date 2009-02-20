package sim.constraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import sim.nodes.TransmissionRecord;


public class IntervalList implements Iterable<Interval>{
	//public static final int MAX_HISTORY_SIZE = 4;
	List<Interval> list = new ArrayList<Interval>();
	int capacity = 0;
	
	public IntervalList(int cap) {
		capacity = cap;
	}
	
	public IntervalList() {
		
	}
	
	//@Override
	public Iterator<Interval> iterator() {
		return list.iterator();
	}	
	
	public void add(int begin, int end, int type, int seq) {
		if (capacity > 0 && list.size() == capacity) {
			list.remove(0);
		}
		// [begin,end] can only overlap with the last one
		if (list.size() > 0 && list.get(list.size()-1).end >= begin) {
			list.get(list.size()-1).end = end;
		}
		else {
			list.add(new Interval(begin, end, type, seq));
		}
	}
	
	public int size() {
		return list.size();
	}
	
	public Interval get(int i) {
		return list.get(i);
	}
	
	@Override
	public String toString() {
		if (list.size() == 0)
			return "";
		StringBuffer buf = new StringBuffer(list.get(0).toString());
		for (int i=1; i< list.size(); i++) {
			buf.append("," + list.get(i).toString());
		}
		return buf.toString();
	}
	
	/**
	 * Tests if the list contains an integer (representing an epoch).
	 * @param i integer
	 * @return Interval.GOOD if i is contained and is either a successful transmission or suppression following;
	 * Interval.BAD if i is contained but is either a failure or suppression following
	 * -1 if not contained.
	 */
	public int contains(int i) {
		for(Interval interval: list) {
			if (interval.end >= i && interval.begin <= i)
				return interval.type;
		}
		return -1;
	}
	
	public void sort() {
	    Collections.sort(list, new IntervalComparator());
	    int size = list.size();
	    for (int i=0; i<size-1; i++) {
	        if (list.get(i).seq == list.get(i+1).seq - 1) {
	            list.get(i).end = list.get(i+1).begin - 1;
	        }
	    }
	}
}

class IntervalComparator implements Comparator<Interval> {
    @Override
    public int compare(Interval i1, Interval i2) {
        if (i1.begin < i2.begin)
            return -1;
        else if (i1.begin == i2.begin)
            return 0;
        else
            return 1;
    }    
}