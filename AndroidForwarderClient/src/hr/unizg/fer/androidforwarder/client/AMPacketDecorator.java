package hr.unizg.fer.androidforwarder.client;

public abstract class AMPacketDecorator extends AMPacket {

	protected AMPacket mPacket = null;
	
	protected AMPacketDecorator(AMPacket packet) {
		
		mPacket = packet;
		this.contents = mPacket.contents;
	}
	
/*
	@Override
	public int getDestinationAddress() {
		
		return mPacket.getDestinationAddress();
	}
	
	@Override
	public void setDestinationAddress(int address) {
		
		mPacket.setDestinationAddress(address);
	}
	
	@Override
	public int getLinkSourceAddress() {
		
		return mPacket.getLinkSourceAddress();
	}
	
	@Override
	public void setLinkSourceAddress(int address) {
		
		mPacket.setLinkSourceAddress(address);
	}
	
	@Override
	public int getMessageLength() {
		
		return mPacket.getMessageLength();
	}
	
	@Override
	public int getGroupId() {
		
		return mPacket.getGroupId();
	}
	
	@Override
	public void setGroupId(int id) {
		
		mPacket.setGroupId(id);
	}
	
	@Override
	public int getHandlerId() {
		
		return mPacket.getHandlerId();
	}
	
	@Override
	public void setHandlerId(int id) {
		
		mPacket.setHandlerId(id);
	}
	
	@Override
	public byte[] getPayload() throws PayloadSizeException {
		
		return mPacket.getPayload();
	}
	
	@Override
	public void setPayload(byte[] payload) throws PayloadSizeException {
		
		mPacket.setPayload(payload);
	}
	
	@Override
	public String toString() {
		
		return mPacket.toString();
	}
	
	@Override
	public byte[] getBytes() {
		
		return mPacket.getBytes();
	}
*/	
}
