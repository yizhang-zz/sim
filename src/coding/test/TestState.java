package coding.test;

import static org.junit.Assert.*;

import org.junit.Test;
import coding.*;
public class TestState {

	@Test
	public void testToId() {
		/*State s = new State();
		s.symbols = new Symbol[3];
		s.symbols[0] = new Symbol(new int[]{0,2,3});
		s.symbols[1] = new Symbol(new int[]{1,2,3});
		s.symbols[2] = new Symbol(new int[]{0,1});
		assertEquals(s.toId(),0x3ED);
		*/
	}
	
	@Test
	public void testSetId() {
		State s = new State(0x3ED);
		assertEquals(s.symbols[0].toString(),"1101");
		assertEquals(s.symbols[1].toString(),"1110");
		assertEquals(s.symbols[2].toString(),"11");
	}

}
