package com.example.androidledcontrol.packet;

import hr.unizg.fer.androidforwarder.client.AMPacketDecorator;
import hr.unizg.fer.androidforwarder.client.AMPacket;

public class LEDPacket extends AMPacketDecorator {

	private static final byte		OFFSET_CODE		= PAYLOAD_OFFSET + 0;
	private static final byte		OFFSET_RED 		= PAYLOAD_OFFSET + 1;
	private static final byte		OFFSET_YELLOW 	= PAYLOAD_OFFSET + 2;
	private static final byte		OFFSET_GREEN 	= PAYLOAD_OFFSET + 3;
	private static final byte 		OFFSET_SENSOR	= PAYLOAD_OFFSET + 4;
	
	public LEDPacket(AMPacket decoratedAMPacket) {
		
		super(decoratedAMPacket);
		
		contents[PAYLOAD_SIZE_OFFSET] = (byte) 0x5;
		contents[OFFSET_CODE] = (byte) 0xFA;
		
	}
	
	public LEDPacket(boolean red, boolean yellow, boolean green) {
		
		this(new AMPacket());
		
		//contents[PAYLOAD_SIZE_OFFSET] = (byte) 0x5;
		//contents[OFFSET_CODE] = (byte) 0xAF;
		
		
		contents[OFFSET_RED] 	= red ? (byte)1 : (byte)0;
		contents[OFFSET_YELLOW] = yellow ? (byte)1 : (byte)0;
		contents[OFFSET_GREEN] 	= green ? (byte)1 : (byte)0;
	}
	
	
	public boolean isRed() {
		
		return  isTrue(contents[OFFSET_RED]);
	}
	
	public boolean isYellow() {
		
		return isTrue(contents[OFFSET_YELLOW]);
	}

	public boolean isGreen() {
	
		return isTrue(contents[OFFSET_GREEN]);
	}
	
	public int getSensorValue() {
		
		return contents[OFFSET_SENSOR];
	}
	
	
	private boolean isTrue(byte b) {
		
		if( b == 0 )	return false;
		else  			return true;
	}
}
