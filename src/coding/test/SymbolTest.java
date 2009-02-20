package coding.test;

import static org.junit.Assert.*;

import org.junit.Test;
import coding.*;
public class SymbolTest {


	@Test
	public void testToString() {
		Symbol s = new Symbol(new int[]{0,2,4});
		assertEquals(s.toString(),"10101");
		System.out.println(5^6);
}

	@Test
	public void testAdd() {
		Symbol s= new Symbol(new int[]{0,1,3,5});
		Symbol t= new Symbol(new int[]{0,2,4,5});
		assertEquals(s.add(t).toString(),"11110");
	}
}
