package coding;

public class EncoderConfiguration {
    public int[] denominator;
    public int[][] outputs;
    public int fieldsize;
    public int memsize;
    public int inputsize;
    public int outputsize;
    
    public EncoderConfiguration(int memsize, int inputsize, int outputsize, int[] denom, int[][] out) {
        denominator = denom;
        outputs = out;
        this.memsize = memsize;
        this.inputsize = inputsize;
        this.outputsize = outputsize;
    }
    
    public EncoderConfiguration(){}

}
