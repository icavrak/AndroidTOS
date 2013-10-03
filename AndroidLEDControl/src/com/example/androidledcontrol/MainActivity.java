package com.example.androidledcontrol;

import com.example.androidledcontrol.packet.LEDPacket;

import hr.unizg.fer.androidforwarder.client.AMPacket;
import hr.unizg.fer.androidforwarder.client.PayloadSizeException;
import hr.unizg.fer.androidforwarder.client.TinyOSListener;
import hr.unizg.fer.androidforwarder.client.TinyOSService;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements TinyOSListener {

	TinyOSService mService = null;
	
	Button redButton = null;
	Button yellowButton = null;
	Button greenButton = null;
	
	ToggleButton redToggle = null;
	ToggleButton yellowToggle = null;
	ToggleButton greenToggle = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mService = TinyOSService.getService(this);
		mService.registerListener(this);
		
		
		
		redButton = (Button) findViewById(R.id.button1);
		redButton.setEnabled(false);
		
		yellowButton = (Button) findViewById(R.id.button2);
		yellowButton.setEnabled(false);
		
		greenButton = (Button) findViewById(R.id.button3);
		greenButton.setEnabled(false);
		
		
		
		redToggle = (ToggleButton) findViewById(R.id.toggleButton1);
		redToggle.setSelected(false);
		
		yellowToggle = (ToggleButton) findViewById(R.id.toggleButton2);
		yellowToggle.setSelected(false);
		
		greenToggle = (ToggleButton) findViewById(R.id.toggleButton3);
		greenToggle.setSelected(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void packetReceived(AMPacket packet) {
		
		//Toast.makeText(this, "packet received", Toast.LENGTH_SHORT).show();
		Log.i("client", "packet payload " + packet.getMessageLength());
		
		LEDPacket p = new LEDPacket(packet);
		
		redToggle.setSelected(p.isRed());
		yellowToggle.setSelected(p.isYellow());
		greenToggle.setSelected(p.isGreen());
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		mService.unregisterListener(this);
	}

	@Override
	public void connectionLost() {
		
		redButton.setEnabled(false);
		yellowButton.setEnabled(false);
		greenButton.setEnabled(false);
	}

	@Override
	public void connectionEstablished() {
		
		redButton.setEnabled(true);
		yellowButton.setEnabled(true);
		greenButton.setEnabled(true);
	}
	
	public void clickedRed(View view) {
		
		LEDPacket packet = new LEDPacket(true, false, false);
		sendPacket(packet);
	}
	
	public void clickedYellow(View view) {
		
		LEDPacket packet = new LEDPacket(false, true, false);
		sendPacket(packet);
	}
	
	public void clickedGreen(View view) {
		
		LEDPacket packet = new LEDPacket(false, false, true);
		sendPacket(packet);
	}
	
	private void sendPacket(AMPacket packet) {
		
		packet.setHandlerId(0xb);
		packet.setDestinationAddress(0x01);
		
		//Toast.makeText(this, packet.toString(), Toast.LENGTH_LONG).show();
		mService.sendPacket(packet);
	}
}
