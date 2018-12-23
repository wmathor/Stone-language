package chap13;

public class Code {
	protected StoneVM svm;
	protected int codeSize;
	protected int numOfStrings;
	protected int nextReg;
	protected int frameSize;

	public Code(StoneVM stoneVm) {
		svm = stoneVm;
		codeSize = 0;
		numOfStrings = 0;
	}

	public int position() {
		return codeSize;
	}

	public void set(short value, int pos) {
		svm.code()[pos] = (byte) (value >>> 8);
		svm.code()[pos + 1] = (byte) value;
	}

	public void add(byte b) {
		svm.code()[codeSize++] = b;
	}

	public void add(short i) {
		add((byte) (i >>> 8));
		add((byte) i);
	}

	public void add(int i) {
		add((byte) (i >>> 24));
		add((byte) (i >>> 16));
		add((byte) (i >>> 8));
		add((byte) i);
	}

	public int record(String s) {
		svm.strings()[numOfStrings] = s;
		return numOfStrings++;
	}
}