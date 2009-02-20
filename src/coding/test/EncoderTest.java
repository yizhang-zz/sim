package coding.test;

import static org.junit.Assert.*;

import org.junit.Test;
import coding.*;
public class EncoderTest {

	@Test
	public void testEncode() {
	    EncoderConfiguration conf = new EncoderConfiguration();
	    conf.denominator = new int[]{1,0,0};
	    conf.fieldsize = 4;
	    conf.inputsize = 1;
	    conf.memsize = 2;
	    conf.outputs = new int[][]{{1,0,0},{1,1,1}};
	    conf.outputsize = 2;
	    
		Encoder en = new Encoder(conf);
		Symbol[] out = en.encode(new Symbol(5));//0101
		assertEquals(out[0].toString(), "101");
		assertEquals(out[1].toString(), "101");
		out = en.encode(new Symbol(12));//1100
		assertEquals(out[0].toString(), "1100");
		assertEquals(out[1].toString(), "1001");
		out = en.encode(new Symbol(3));
		assertEquals(out[0].toString(), "11");
		assertEquals(out[1].toString(), "1010");


	}

}
