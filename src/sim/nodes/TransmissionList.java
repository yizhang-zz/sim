package sim.nodes;

import java.util.ArrayList;

public class TransmissionList extends ArrayList<TransmissionRecord> {
	private int cur = 0; // current position in the list
	
	/**
	 * The list is sorted according to time so adds rec to proper position. 
	 */
	@Override
	public boolean add(TransmissionRecord rec) {
		if (size() == 0) {
			super.add(rec);
			return true;
		}
		for (int i=super.size()-1; i>=0; i--) {
			if (super.get(i).time > rec.time)
				continue;
			super.add(i+1, rec);
			break;
		}		
		return true;
	}
	
	public void append(TransmissionRecord rec) {
		super.add(size(), rec);
	}
}