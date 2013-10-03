package hr.unizg.fer.androidforwarder.client;



import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class TinyOSService {

	//Class-level singleton
	protected static TinyOSService mSingleServiceInstance = null;
	
	
	Messenger 	mRemoteMessenger = null;
	Messenger	mLocalMessenger = null;
	boolean 	mBound = false;
	int 		mRegistrationID = 0;
	
	Context		mContext = null;
	
	List<TinyOSListener> mListeners = new Vector<TinyOSListener>();
	
	//dummy constructor
	protected TinyOSService() {
		
	}
	
	//singleton constructor
	public static TinyOSService getService(Context context) {
		
		if( mSingleServiceInstance == null)
			mSingleServiceInstance = new TinyOSService();
		
		mSingleServiceInstance.mContext = context;
		
		return mSingleServiceInstance;
	}
	
	
	public void registerListener(TinyOSListener listener) {
		
		
		//add new listener to the listener list
		if( !mListeners.contains(listener) )
			mListeners.add(listener);
		
		//if this is the first listener in the list, try to register with the background service
		if( mListeners.size() == 1 ) {
		
			Intent intent = new Intent("hr.unizg.fer.androidforwarder.SERVICE_STATUS");
			mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		} else
			listener.connectionEstablished();
	}
	
	public void unregisterListener(TinyOSListener listener) {
		
		//add new listener to the listener list
		if( mListeners.contains(listener) )
			mListeners.remove(listener);
		
		//if all listeners are removed, unbind from service
		if( mListeners.size() == 0 ) {
		
			Message unsubscribeMsg = Message.obtain();
			unsubscribeMsg.what = 0xff;
			unsubscribeMsg.arg1 = mRegistrationID;
		
			try {
				mRemoteMessenger.send(unsubscribeMsg);
				mRegistrationID = 0;
			} catch (RemoteException e) {
				Log.w("TinyOSService", "unsubscription message failed to send");
				reportDisconnection();
			}
			
			if (mBound) {
	            mContext.unbindService(mConnection);
	            mBound = false;
	        }
		}
	}
	
	public boolean sendPacket(AMPacket packet) {
		
		if( ! mBound )
			return false;
		
		// Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, 0xcc, 0, 0);
        msg.arg1 = mRegistrationID;
        msg.getData().putByteArray("rawPacket", packet.getBytes());
        try {
        	mRemoteMessenger.send(msg);
        } catch (RemoteException e) {
        	Log.w("TinyOSService", "unsubscription message failed to send");
        	return false;
        }
		
        return true;
	}
	
	
	
	
	private Handler mCallbackHandler = new Handler() {
		
		@Override
		public void handleMessage(Message message) {
			
			
			Log.i("Client", "arrived message "+message.what);
			
			//registration acknowledgment, received subscription ID
			if( message.what == 0xbb ) {
				mRegistrationID = message.arg1;	
				
				Iterator<TinyOSListener> i = mListeners.iterator();
				while(i.hasNext()) {
					 
					TinyOSListener listener = i.next();
					listener.connectionEstablished();
				}
				
			}
			
			//message arrived, distribution to all registered listeners
			else if(message.what == 0xdd) {
				
				//try to construct AMPacket from received byte array
				AMPacket sharedPacket = null;
				sharedPacket = AMPacket.reconstructAMPacket( (byte[]) message.getData().getByteArray("rawPacket"));
				
				//if packet reconstruction failed, no need to distribute it
				if( sharedPacket == null)
					return;
				
				//iterate over listeners and distribute the packet (single packet instance to all listeners - do not alter it!) 
				Iterator<TinyOSListener> i = mListeners.iterator();
				while(i.hasNext()) {
					
					TinyOSListener listener = i.next();
					listener.packetReceived(sharedPacket);
				}
			}
		}
		
	};
	
	
	
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			
			//create remote messenger for sending messages to server
			mRemoteMessenger = new Messenger(service);
			
			//set the bound flag to true
			mBound = true;
			
			//create local messenger for receiving messages from server
			mLocalMessenger = new Messenger(mCallbackHandler);
			
			Message subscribeMsg = Message.obtain();
			subscribeMsg.what = 0xee;
			subscribeMsg.replyTo = mLocalMessenger;
			
			try {
				mRemoteMessenger.send(subscribeMsg);
			} catch (RemoteException e) {
				Log.e("TinyOSService", "subscription message failed to send");
				reportDisconnection();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			
			mRemoteMessenger = null;
			mBound = false;
		}
		
	};
	
	
	protected void reportDisconnection() {
		
		Iterator<TinyOSListener> i = mListeners.iterator();
		while(i.hasNext()) {
 
			TinyOSListener listener = i.next();
			listener.connectionLost();
		}
		
		mListeners.clear();
	}
}
