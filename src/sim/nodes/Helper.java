package sim.nodes;

import java.util.AbstractList;

public class Helper {
	public static <T> String toString(Iterable<T> l) {
		StringBuffer buf = new StringBuffer("[ ");
		if (l != null)
			for (T i : l)
				buf.append(i.toString() + " ");
		buf.append("]");
		return buf.toString();
	}

}

/*
class CircularList<E> extends AbstractList<E> {

	private Object[] array;
	private int size;
	private int capacity;
	private int tail; // current position

	public CircularList(int capacity) {
		super();
		if (capacity < 0)
			throw new IllegalArgumentException("Illegal capacity " + capacity);

		this.capacity = capacity;
		array = new Object[capacity];
		tail = -1;

	}

	@Override
	public E get(int index) {
		return (E) array[index];
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean add(E element) {
		tail = (tail + 1) % capacity;
		array[(tail)] = element;
		return true;
	}
	
//	@Override
//	public void add(int i, E element) {
//		if (i >= capacity || i <0)
//			throw new ArrayIndexOutOfBoundsException("Illegal index "+i);
//		int count = capacity
//	}
}
*/