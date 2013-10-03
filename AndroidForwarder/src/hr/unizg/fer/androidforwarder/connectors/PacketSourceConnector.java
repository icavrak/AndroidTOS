package hr.unizg.fer.androidforwarder.connectors;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

public interface PacketSourceConnector {

	public void openConnector(Context context, Handler handler);
	public void send(Message message);
	public void closeConnector();
	
}
