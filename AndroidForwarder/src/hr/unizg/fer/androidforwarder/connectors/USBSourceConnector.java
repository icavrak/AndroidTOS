package hr.unizg.fer.androidforwarder.connectors;

import hr.unizg.fer.androidforwarder.AndroidForwarderUtil;
import hr.unizg.fer.androidforwarder.ForwarderService;
import hr.unizg.fer.tinyosusb.TinyOSUSBManager;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class USBSourceConnector implements PacketSourceConnector {

	private TinyOSUSBManager mManager = null;
	private Handler			mHandler = null;
	
	@Override
	public void openConnector(Context context, Handler handler) {
		
		UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		
		mHandler = handler;
		
		mManager = new TinyOSUSBManager(usbManager) {
			
			@Override
			public void updateNewData(byte[] data) {
				
				Message message = Message.obtain();
				message.getData().putByteArray("rawPacket", data);
				message.what = ForwarderService.message.MESSAGE_PACKET_RECEIVED;
				mHandler.sendMessage(message);	
			}
		};
		
		mManager.open();
	}

	@Override
	public void closeConnector() {
		
		mManager.close();
	}

	@Override
	public void send(Message message) {
		
		//Log.i("USBSourceConnector", AndroidForwarderUtil.bytesToHexDelimited(message.getData().getByteArray("rawPacket")));
		mManager.write(message.getData().getByteArray("rawPacket"));
	}

}
