package hr.unizg.fer.androidforwarder.connectors;

import hr.unizg.fer.androidforwarder.ForwarderService.message;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SimSourceConnector implements PacketSourceConnector {

	private Thread 		demoMessageThread 				= null;
	private boolean 	stopMessageServiceFlag 			= false;
	private Handler		mHandler 						= null;
	
	byte[] demoContent = new byte[] {0x21, 0x22, 0x20, 0x3, 0x17, 0x00};
	byte demoCounter = 0;
	
	@Override
	public void openConnector(Context context, Handler handler) {
		
		mHandler = handler;
		
		demoMessageThread = new Thread( new Runnable() {

			@Override
			public void run() {
				
				for(int i=0 ; i<100 ; i++) {
					
					if( stopMessageServiceFlag == true )
						return;
					long endTime = System.currentTimeMillis() + 5*1000;
			          while (System.currentTimeMillis() < endTime) {
			              synchronized (this) {
			                  try {
			                      wait(endTime - System.currentTimeMillis());
			                  } catch (Exception e) {
			                  }
			              }
			          }
			          Message m = Message.obtain();
			          m.what = message.MESSAGE_PACKET_RECEIVED;
			          demoContent[5] = demoCounter++;
			          m.getData().putByteArray("rawPacket", demoContent);
			          mHandler.sendMessage(m);
				}
			}
			
			
		});
		
		//start simulated packet reception
		demoMessageThread.start();
		
	}

	@Override
	public void closeConnector() {
		
		stopMessageServiceFlag = true;

	}

	@Override
	public void send(Message message) {
		
		//byte[] data = message.getData().getByteArray("rawPacket");
		Log.i("SimSourceConnector", "msg sent");
		
	}

}
