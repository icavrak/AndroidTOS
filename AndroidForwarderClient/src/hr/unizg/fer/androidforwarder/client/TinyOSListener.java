package hr.unizg.fer.androidforwarder.client;



public interface TinyOSListener {

	
	public void packetReceived(AMPacket packet);
	public void connectionLost();
	public void connectionEstablished();

}
