package sim.constraints;

import java.util.LinkedList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


public class IntervalList extends LinkedList<Interval>{
	//public static final int MAX_HISTORY_SIZE = 4;
	//List<Interval> list = new ArrayList<Interval>();
	int capacity = 0;
	
	public IntervalList(int cap) {
		capacity = cap;
	}
	
	public IntervalList() {
		
	}
	
	//@Override
//	public Iterator<Interval> iterator() {
//		return list.iterator();
//	}	
	
	public void add(int begin, int end, Interval.Type type, int seq) {
	    /* need sort? */
		if (capacity > 0 && size() == capacity) {
			remove(0);
		}
		if (size() ==0) {
			super.add(new Interval(begin, end, type, seq));
			return;
		}
		int i = size() - 1;
		while (get(i).begin > begin) i--;
		if (begin > get(i).end) {
			add(i+1, new Interval(begin, end, type, seq));
		}
		else if (end > get(i).end)
			get(i).end = end;
		// [begin,end] can only overlap with the last one
//		if (list.size() > 0 && list.get(list.size()-1).end >= begin
//				/*&& list.get(list.size()-1).begin <= begin*/) {
//			list.get(list.size()-1).end = end;
//		}
		//else {
//			list.add(new Interval(begin, end, type, seq));
		//}
	}
	
	@Override
	public String toString() {
		if (size() == 0)
			return "";
		StringBuffer buf = new StringBuffer(get(0).toString());
		for (int i=1; i< size(); i++) {
			buf.append("," + get(i).toString());
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
	public Interval.Type contains(int i) {
		Iterator<Interval> it = this.iterator();
		while (it.hasNext()) {
			Interval interval = it.next();
			if (interval.end >= i && interval.begin <= i)
				return interval.type;
		}
		// If can't be found, return unknown
		return Interval.Type.UNKNOWN;
	}
	
	public void sort() {
	    Collections.sort(this, new IntervalComparator());
	    int size = size();
	    for (int i=0; i<size-1; i++) {
	        if (get(i).seq == get(i+1).seq - 1) {
	            get(i).end = get(i+1).begin - 1;
	        }
	    }
	}
}

class IntervalComparator implements Comparator<Interval> {
    public int compare(Interval i1, Interval i2) {
        if (i1.begin < i2.begin)
            return -1;
        else if (i1.begin == i2.begin)
            return 0;
        else
            return 1;
    }    
}