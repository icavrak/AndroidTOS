package hr.unizg.fer.tinyosusb;

public interface TinyOSCommunicationManager {

	public void open();
	public void close();
	
	public byte[] read();
	public void updateNewData(byte[] data);
	
	public void write(byte[] data);
}
