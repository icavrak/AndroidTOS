package hr.unizg.fer.androidforwarder;

import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

public final class AndroidForwarderUtil {

	
	
	public static UsbDevice getUSBAttachedDevice(Context context) {
		
		UsbManager mgr = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		
		HashMap<String, UsbDevice> deviceMap = mgr.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceMap.values().iterator();
		
		if( deviceIterator.hasNext()) {
		
			UsbDevice dev = deviceIterator.next();

			//TODO: filtering according to vendor and product id-s! Data should not be hard-coded!
			
			/*
			name.setText("" + dev.getDeviceName());
			vendor.setText("" + dev.getVendorId());
			product.setText("" + dev.getProductId());
			classInfo.setText("" + dev.getDeviceClass());
			subclass.setText("" + dev.getDeviceSubclass());
			protocol.setText("" + dev.getDeviceProtocol());
			*/
			return dev;
		}
		
		//no forwarder devices attached to USB
		else
			
			return null;
	}
	
	public static void serviceActive(Context context) {
		
		Intent intent = new Intent(context, ForwarderService.class);
		intent.setAction(ForwarderService.action.SERVICE_STATUS);
		context.startService(intent);
	}
	
	public static void startService(Context context) {
		
		Intent intent = new Intent(context, ForwarderService.class);
		intent.setAction(ForwarderService.action.START_SERVICE);
		context.startService(intent);
	}
	
	public static void stopService(Context context) {
		
		Intent intent = new Intent(context, ForwarderService.class);
		intent.setAction(ForwarderService.action.STOP_SERVICE);
		context.startService(intent);
	}
	
	
	final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	public static String bytesToHex(byte[] bytes) {
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
