package hr.unizg.fer.androidforwarder;

import hr.unizg.fer.androidforwarder.connectors.NetworkSourceConnector;
import hr.unizg.fer.androidforwarder.connectors.PacketSourceConnector;
import hr.unizg.fer.androidforwarder.connectors.SimSourceConnector;
import hr.unizg.fer.androidforwarder.connectors.USBSourceConnector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class ForwarderService extends android.app.Service {

	
	//action constants
	public final class action {
		
		//activity -> service
		public static final String	START_SERVICE			=	"hr.unizg.fer.androidforwarder.START_SERVICE";
		public static final String	STOP_SERVICE			=	"hr.unizg.fer.androidforwarder.STOP_SERVICE";
		public static final String  SERVICE_STATUS			=	"hr.unizg.fer.androidforwarder.SERVICE_STATUS";
			
		//service -> activity
		public static final String	SERVICE_INFO			=	"hr.unizg.fer.androidforwarder.SERVICE_INFO";
		public static final String	MESSAGE_INFO			=	"hr.unizg.fer.androidforwarder.MESSAGE_INFO";
	}
	
	public final class extra {
		
		public static final String	EXTRA_SERVICE_ACTIVE	=	"hr.unizg.fer.androidforwarder.EXTRA_SERVICE_ACTIVE";
		public static final String	EXTRA_MSG_RECEIVED		=	"hr.unizg.fer.androidforwarder.EXTRA_MSG_RECEIVED";
		public static final String	EXTRA_MSG_SENT			=	"hr.unizg.fer.androidforwarder.EXTRA_MSG_SENT";
		public static final String	EXTRA_APP_ATTACHED		=	"hr.unizg.fer.androidforwarder.EXTRA_APP_ATTACHED";
		
		public static final String 	EXTRA_MESSAGE_INCOMING	=	"hr.unizg.fer.androidforwarder.EXTRA_MESSAGE_INCOMING";
		public static final String 	EXTRA_MESSAGE_CONTENT	=	"hr.unizg.fer.androidforwarder.EXTRA_MESSAGE_CONTENT";
	}
	
	public final class message {
		
		public static final int		MESSAGE_PACKET_SEND			= 0xcc;
		public static final int		MESSAGE_PACKET_RECEIVED		= 0xdd;
		public static final int		MESSAGE_SUBSCRIBE			= 0xee;
		public static final int		MESSAGE_UNSUBSCRIBE			= 0xff;
		
		public static final int		MESSAGE_SUBSCRIBE_RESPONSE	= 0xbb;
	}
	
	public final class mode {
		
		public static final int		MODE_USB		= 0x00;
		public static final int		MODE_SIM		= 0x01;
		public static final int		MODE_NETWORK	= 0x02;
	}
	
	//instance variables
	private boolean mServiceStarted		= false;
	
	
	private int	mMsgReceivedCounter		= 0;
	private int mMsgSentCounter			= 0;
	private int mClientsActive			= 0;
	
	private HandlerThread 	mWorker 	= null;
	private Looper			mLooper 	= null;
	private ServiceHandler	mHandler 	= null;
	private Messenger		mMessenger	= null;
	
	private boolean stopMessageServiceFlag = false;
	
	
	//forwarder mode of operation (data from usb, simulated data, network data...)
	private int				mMode = mode.MODE_USB;
	
	//client ID counter
	private int				mClientIDCounter = 1;
	
	
	//registry of subscribed clients
	Map<Integer, Messenger> subscribedClients = new HashMap<Integer, Messenger>();
	
	//Packet Source
	private PacketSourceConnector sourceConnector = null;
	
	

	private final class ServiceHandler extends Handler {
		
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		
	
		@Override
		public void handleMessage(Message message) {
			
			//Toast.makeText(ForwarderService.this, "Message " + message.what + " arrived", Toast.LENGTH_SHORT).show();
			
			if( message.what == ForwarderService.message.MESSAGE_SUBSCRIBE && (message.arg1 == 0) ) {
				
				//register new subscriber (client)
				subscribedClients.put(mClientIDCounter, message.replyTo);
				
				//increment client counter
				mClientsActive++;
				
				//create SUBSCRIBE RESPONSE message
				Message srm = Message.obtain();
				srm.what = ForwarderService.message.MESSAGE_SUBSCRIBE_RESPONSE;
				srm.arg1 = mClientIDCounter++;
				
				//send the message
				try {
					message.replyTo.send(srm);
				} catch (RemoteException e) {
					Log.i("ForwarderService", "subscribe response message failed to send to " + srm.arg1);
				}

			}
			
			else if( message.what == ForwarderService.message.MESSAGE_UNSUBSCRIBE) {
				
				
				//get subscriber ID from the message
				int subscriberID = message.arg1;
				
				//does client exist in the subscriber list?
				if( subscribedClients.containsKey(subscriberID)) {
					
					//remove the subscriber from the subscription list
					subscribedClients.remove(subscriberID);
					
					//decrement client counter
					mClientsActive--;
				}

			}
			
			else if( message.what == ForwarderService.message.MESSAGE_PACKET_SEND) {
				
				//does client exist in the subscriber list?
				if( ! subscribedClients.containsKey( message.arg1) )
					return;
				
				sendPacket(message);
			}
			
			else if( message.what == ForwarderService.message.MESSAGE_PACKET_RECEIVED) {
				
				mMsgReceivedCounter++;
				distributePacket(message);
			}
		}
	}
	
	@Override
	public void onCreate() {
		
		//Toast.makeText(this, this.getText(R.string.fs_start), Toast.LENGTH_SHORT).show();
		
		mWorker = new HandlerThread("ForwarderWorkerThread", HandlerThread.MIN_PRIORITY);
		mWorker.start();
		
		mLooper = mWorker.getLooper();
		//mLooper.loop();
		mHandler = new ServiceHandler(mLooper);
		
		mMessenger = new Messenger(mHandler);
		
		//get packet source according to source mode (mMode) 
		sourceConnector = getSourceConnector();
		
		stopMessageServiceFlag = false;
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		//Toast.makeText(this, "action " + intent.getAction() + " from " + intent.getComponent(), Toast.LENGTH_SHORT).show();
		
		if( intent.getAction().equals(action.STOP_SERVICE) && mServiceStarted ) {
			
			mServiceStarted = false;
			sourceConnector.closeConnector();
			
			stopSelf();
		}
		
		else if( intent.getAction().equals(action.SERVICE_STATUS) ) {
			
			if( !mServiceStarted )
				stopSelf();
			else 
				broadcastServiceInfoIntent();
			
		}
		else if( intent.getAction().equals(action.START_SERVICE) && !mServiceStarted ) {
			
			serviceStart();
			sourceConnector.openConnector(this, mHandler);		
		}		
		
		return START_NOT_STICKY;
	}
	
	
	


	@Override
	public void onDestroy() {
		
		//destroy working thread
		//mLooper.quit();
		//mWorker.quit();
		
		/*
		try {
			mWorker.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/
		
		stopMessageServiceFlag = true;
		
		//broadcast service stop intent
		broadcastServiceInfoIntent();
		//Intent intent = createServiceInfoIntent();
		//sendBroadcast(intent);
		
		
		
		//gui notification
		//Toast.makeText(this, this.getText(R.string.fs_stop), Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		
		Log.i("AndroidForwarder", "onBind");
		
		//if the service is not explicitly started, return null
		if( !mServiceStarted )
			return null;
		
		return mMessenger.getBinder();
		
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
	
		Log.i("AndroidForwarder", "onUnbind");
		
		mClientsActive = 0;
		return false;
	}
	
	private void serviceStart() {
		
		//change service status flag
		mServiceStarted = true;
		
		//broadcast service start intent
		broadcastServiceInfoIntent();
		
		//declare foreground service mode
		Notification notification = new Notification(R.drawable.ic_launcher, getText(R.string.app_name), System.currentTimeMillis());
		//Intent notificationIntent = createServiceInfoIntent();
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, getText(R.string.app_name), "**content**", pendingIntent);
		startForeground(1, notification);		
	}

	
	private Intent createServiceInfoIntent() {
		
		Intent intent = new Intent(action.SERVICE_INFO);
		
		intent.putExtra(extra.EXTRA_MSG_RECEIVED, mMsgReceivedCounter);
		intent.putExtra(extra.EXTRA_MSG_SENT, mMsgSentCounter);
		intent.putExtra(extra.EXTRA_APP_ATTACHED, mClientsActive);
		intent.putExtra(extra.EXTRA_SERVICE_ACTIVE, mServiceStarted);
		
		return intent;
	}
	
	private void broadcastServiceInfoIntent() {
		
		Intent intent = createServiceInfoIntent();
		sendBroadcast(intent);
	}
	
	 private void broadcastMessageInfoIntent(boolean isIncoming, byte[] content) {
		 
		 Intent intent = new Intent(action.MESSAGE_INFO);
		 
		 intent.putExtra(extra.EXTRA_MESSAGE_INCOMING, isIncoming);
		 intent.putExtra(extra.EXTRA_MESSAGE_CONTENT, content);
		 
		 sendBroadcast(intent);
	 }
	 
	 
	 private void sendPacket(Message message) {
		 
		 //TODO implement send message mechanism!
		 //Toast.makeText(this, AndroidForwarderUtil.bytesToHexDelimited(message.getData().getByteArray("rawPacket")), Toast.LENGTH_LONG).show();
		 
		 sourceConnector.send(message);
		 mMsgSentCounter++;
	 }
	 
	 private void distributePacket(Message message) {
		 
         //broadcast SERVICE_INFO
         broadcastServiceInfoIntent();
         
         //broadcast MESSAGE_INFO
         broadcastMessageInfoIntent(true, message.getData().getByteArray("rawPacket"));
 		
         //for each active subscriber
         Iterator<Integer> client = subscribedClients.keySet().iterator();
         
         while(client.hasNext()) {
        	 
        	 Integer clientID = client.next();
        	 
        	 //Message data = Message.obtain();
        	 //data.what = message.MESSAGE_PACKET_RECEIVED;				
        	 
        	 //TODO: filtering per client (define filtering mechanism!!!)
        	 
        	 Messenger m = subscribedClients.get(clientID);
        	 
        	 try {
        		 if( m != null)
        			 //m.send(data);
        			 m.send(message);
        	 } catch (RemoteException e) {
     			Log.w("ForwarderService", "data message failed to send to " + clientID);
           	 // TODO: clear inactive clients from subscribers list !
     		}
         }	
	 }
	 
	 public int getMode() {
		 
		 return mMode;
	 }
	 
	 public boolean setMode(int mode) {
		 
		 //mode cannot be started while service is running
		 if(mServiceStarted == true)
			 return false;
		 
		 mMode = mode;
		 return true;
	 }
	 
	private PacketSourceConnector getSourceConnector() {
			
		switch (mMode) {
		
			case mode.MODE_USB:			return new USBSourceConnector();
			case mode.MODE_SIM:			return new SimSourceConnector();
			case mode.MODE_NETWORK:		return new NetworkSourceConnector();
			default:					return null;
		}
	}

}
