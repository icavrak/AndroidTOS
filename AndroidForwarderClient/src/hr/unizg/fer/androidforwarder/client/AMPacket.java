package hr.unizg.fer.androidforwarder.client;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

public class AMPacket implements Parcelable {

	protected static final int		MAX_PACKET_LENGTH		= 36;
	protected static final int		MAX_PAYLOAD_LENGTH		= 28;
	protected static final int		PAYLOAD_OFFSET			= 8;
	protected static final int		PAYLOAD_SIZE_OFFSET		= 5;
	
	//packet examples
	//00 FF FF 00 00 04 22 06 00 02 00 01
	//00 FF FF 00 00 04 22 06 00 02 00 02
	//00 FF FF 00 00 04 22 06 00 02 00 03
	
	protected byte[] contents = new byte[MAX_PACKET_LENGTH];
	
	public AMPacket() {
		
	}
	
	protected static final AMPacket reconstructAMPacket(byte[] rawPacket) {
		
		//check raw packet vailidity
		if( rawPacket.length == 0 ) return null;
		if( rawPacket.length  > MAX_PACKET_LENGTH) return null;
		if( rawPacket[0] != 0 ) return null;						//first byte == 0 -> AM type packet!
		
		AMPacket res = new AMPacket();
		System.arraycopy(rawPacket, 0, res.contents, 0, rawPacket.length);
		
		return res;
	}
	
	
	//copy constructor
	public AMPacket(AMPacket packet) throws PayloadSizeException {
		
		if( contents[PAYLOAD_SIZE_OFFSET] > MAX_PAYLOAD_LENGTH )
			throw new PayloadSizeException();
		
		System.arraycopy(packet.contents, 0, contents, 0, MAX_PACKET_LENGTH);
	}
	
	public AMPacket(byte[] payload) throws PayloadSizeException {
		
		if( payload.length <= MAX_PAYLOAD_LENGTH ) {
			System.arraycopy(payload, 0, contents, PAYLOAD_OFFSET, payload.length);
			contents[PAYLOAD_SIZE_OFFSET] = (byte) payload.length;
		}
		else
			throw new PayloadSizeException();
	}
	
	public int getDestinationAddress() {
		
		return contents[1] *256 + contents[2];
	}
	
	public void setDestinationAddress(int address) {
		
		contents[1] = (byte) ( (address >> 8) & 0xff);
		contents[2] = (byte) (address & 0xff);
	}
	
	public int getLinkSourceAddress() {
		
		return contents[3] *256 + contents[4];
	}
	
	public void setLinkSourceAddress(int address) {
		
		contents[3] = (byte) ( (address >> 8) & 0xff);
		contents[4] = (byte) (address & 0xff);
	}
	
	public int getMessageLength() {
		
		return contents[PAYLOAD_SIZE_OFFSET];
	}
	
	public int getGroupId() {
		
		return contents[6];
	}
	
	public void setGroupId(int id) {
		
		contents[6] = (byte) (id & 0xff);
	}
	
	public int getHandlerId() {
		
		return contents[7];
	}
	
	public void setHandlerId(int id) {
		
		contents[7] = (byte) (id & 0xff);
	}
	
	public byte[] getPayload() throws PayloadSizeException {
		
		if( contents[PAYLOAD_SIZE_OFFSET] > MAX_PAYLOAD_LENGTH )
			throw new PayloadSizeException();
		
		byte[] pload = new byte[contents[PAYLOAD_SIZE_OFFSET]];
		
		System.arraycopy(contents, PAYLOAD_OFFSET, pload, 0, contents[PAYLOAD_SIZE_OFFSET]);
		
		return pload;
	}
	
	public void setPayload(byte[] payload) throws PayloadSizeException {
		
		if( payload.length > MAX_PAYLOAD_LENGTH)
			throw new PayloadSizeException();
		
		System.arraycopy(payload, 0, contents, PAYLOAD_OFFSET, payload.length);
		contents[PAYLOAD_SIZE_OFFSET] = (byte) payload.length;
	}
	
	@Override
	public String toString() {
		
		return bytesToHexDelimited(getBytes());
	}
	
	public byte[] getBytes() {
		
		byte[] rawPacket = new byte[getMessageLength()+PAYLOAD_OFFSET];
		System.arraycopy(contents, 0, rawPacket, 0, getMessageLength()+PAYLOAD_OFFSET);
		
		return rawPacket;
	}
	
	//
	// Parcelable methods
	//
	
	public int describeContents() {
     
		return 0;
    }
	
	public void writeToParcel(Parcel out, int flags) {
	
		out.writeByteArray(contents);
	}
	
	public AMPacket(Parcel in) {
		
		contents = new byte[MAX_PACKET_LENGTH];
		in.readByteArray(contents);
	}
	
	public static final Parcelable.Creator<AMPacket> CREATOR = new Parcelable.Creator<AMPacket>() {
		
			public AMPacket createFromParcel(Parcel in) {
				
				return new AMPacket(in);
			}

			public AMPacket[] newArray(int size) {
				
				return new AMPacket[size];
			}
	};
	
	
	final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	protected static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static String bytesToHexDelimited(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 3 - 1];
	    int v;
	    int pos=0;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[pos] = hexArray[v >>> 4];
	        hexChars[pos+1] = hexArray[v & 0x0F];
	        
	        if(j < (bytes.length-1))
	        	hexChars[pos+2] = ' ';
	        pos = pos+3;
	    }
	    return new String(hexChars);
	}
}
