package sim.nodes;

import Jama.Matrix;
public class NetworkConfigurationTest {

	//@Test
	public void testCountUnbounded() {
		
		double[] a = {1,2,3,4};
		Matrix dd  =new Matrix(a,2);
		dd.print(0, 7);
		
		Matrix ma = new Matrix(a,3);
		double[] b = {1.5,2,3,4,5,3.6};
		Matrix mb = new Matrix(b,6);
		int[] index = {0,-1,2,-1,1};
		//int n =NetworkConfiguration.countUnbounded(ma, mb, index, 1.6);
		//assertEquals(0, n);
	}

}
