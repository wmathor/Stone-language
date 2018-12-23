package chap13;
import chap11.ResizableArrayEnv;

public class StoneVMEnv extends ResizableArrayEnv implements HeapMemory {
    protected StoneVM svm;
    protected Code code;
    public StoneVMEnv(int codeSize, int stackSize, int stringsSize) {
        svm = new StoneVM(codeSize, stackSize, stringsSize, this);
        code = new Code(svm);
    }
    public StoneVM stoneVM() { return svm; }
    public Code code() { return code; }
    public Object read(int index) { return values[index]; }
    public void write(int index, Object v) { values[index] = v; }
}