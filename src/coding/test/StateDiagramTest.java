package coding.test;
import coding.*;
import static org.junit.Assert.*;
import org.junit.Test;

public class StateDiagramTest {
	@Test
	public void testGenDiag(){
		Encoder encoder = new Encoder();
		StateDiagram diag = StateDiagram.construct(4, 3, encoder);
		diag.output();
	}
}
