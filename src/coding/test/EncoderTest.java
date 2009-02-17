package coding.test;

import static org.junit.Assert.*;

import org.junit.Test;
import coding.*;
public class EncoderTest {

	@Test
	public void testEncode() {
		Encoder en = new Encoder();
		Symbol[] out = en.encode(new int[]{0,2});
		assertEquals(out[0].toString(), "101");
		assertEquals(out[1].toString(), "101");
		assertEquals(out[2].toString(), "101");
		out = en.encode(new int[]{2,3});
		assertEquals(out[0].toString(), "1100");
		assertEquals(out[1].toString(), "1001");
		assertEquals(out[2].toString(), "1001");
		out = en.encode(new int[]{0,1});
		assertEquals(out[0].toString(), "11");
		assertEquals(out[1].toString(), "1111");
		assertEquals(out[2].toString(), "1010");


	}

}
