package chap13;

public interface HeapMemory {
	Object read(int index);

	void write(int index, Object v);
}