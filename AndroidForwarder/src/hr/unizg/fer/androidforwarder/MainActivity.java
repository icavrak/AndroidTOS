package hr.unizg.fer.androidforwarder;

import hr.unizg.fer.androidforwarder.ForwarderService.extra;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;

public class MainActivity extends Activity {
	
	//USB broadcast receiver
	private BroadcastReceiver mUSBActionReceiver = null;
	
	//Service broadcast receiver
	private BroadcastReceiver mForwarderServiceActionReceiver = null;
	
	//USB device (forwarder device)
	UsbDevice mUSBDevice = null;
	
	//Service INFO
	private int mMsgReceived	= 0;
	private int mMsgSent		= 0; 
	private int mClientsActive	= 0;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	
		Log.i("MainActivity", "onCreate");
		
		
		//Setup Default Preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		//setup gui component states
		Switch serviceSwitch = (Switch) findViewById(R.id.serviceSwitch);
		serviceSwitch.setChecked(false);
		serviceSwitch.setEnabled(false);
		
		serviceSwitch.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				if( isChecked )
					AndroidForwarderUtil.startService(MainActivity.this);
				else 
					AndroidForwarderUtil.stopService(MainActivity.this);

				updateGUI();
			}
					
		});
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		Log.i("MainActivity", "onResume Action: " + getIntent().getAction());
		
		//Intent acquired by UP action from sub-activities does not have an action defined!
		
		//activity started with ACTION_USB_DEVICE_ATTACHED action
		if( (getIntent().getAction() != null) && (getIntent().getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)))
			mUSBDevice = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
				
		//activity started with MAIN action, check for pre-connected USB device
		else if( (getIntent().getAction() != null) && (getIntent().getAction().equals(Intent.ACTION_MAIN)) )
			mUSBDevice = AndroidForwarderUtil.getUSBAttachedDevice(this);
		
		//activity resumed with SERVICE_INFO action
		else  if( (getIntent().getAction() != null) && (getIntent().getAction().equals(ForwarderService.action.SERVICE_INFO))) {
			Switch serviceSwitch = (Switch) findViewById(R.id.serviceSwitch);
			
			boolean status = getIntent().getBooleanExtra(ForwarderService.extra.EXTRA_SERVICE_ACTIVE, false);
			serviceSwitch.setChecked(status);
		}
			
		
		//register BroadcastListener for USB ATTACH and DETACH actions
		mUSBActionReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				
				Log.i("MainActivity", "usbActionReceiver: " + intent.getAction());
				
				//get the action name (ATTACH or DETACH)
				String action = intent.getAction();
				
				//if action is DETACH
				if( action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED) ) {
					
					//Toast.makeText(MainActivity.this, "receive: DETACH", Toast.LENGTH_SHORT).show();
					mUSBDevice = null;
				}
				
				else if( action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED) ) {
				
					//Toast.makeText(MainActivity.this, "receive: ATTACH", Toast.LENGTH_SHORT).show();
					mUSBDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				}
				
				updateGUI();
			}
		};
		
		mForwarderServiceActionReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				
				String ext = "";
				if( intent.getAction().equals(ForwarderService.action.SERVICE_INFO ) ) ext = ext + intent.getBooleanExtra(ForwarderService.extra.EXTRA_SERVICE_ACTIVE, false); 
				//Log.i("MainActivity", "serviceActionReceiver: " + intent.getAction() + ", s=" + ext);
				
				if( intent.getAction().equals(ForwarderService.action.SERVICE_INFO)) {
					
					boolean status = intent.getBooleanExtra(ForwarderService.extra.EXTRA_SERVICE_ACTIVE, false);
					Switch serviceSwitch = (Switch) findViewById(R.id.serviceSwitch);
					serviceSwitch.setChecked(status);
					
					if( !status ) {
						mMsgReceived	= 0;
						mMsgSent		= 0; 
						mClientsActive	= 0;
					}
					else {
						mMsgReceived = intent.getIntExtra(ForwarderService.extra.EXTRA_MSG_RECEIVED, 0);
						mMsgSent = intent.getIntExtra(ForwarderService.extra.EXTRA_MSG_SENT, 0);
						mClientsActive = intent.getIntExtra(ForwarderService.extra.EXTRA_APP_ATTACHED, 0);
					}
					updateGUI();
						
				}
			}
		};
		
		
		
		//create IntentFilter for ATTACH and DETACH actions
		IntentFilter usbFilter = new IntentFilter();
		usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		
		//register broadcast receiver for ATTACH and DETACH actions
		this.registerReceiver(mUSBActionReceiver, usbFilter);
		
		
		//create IntentFilter for Forwarder Serivice actions
		IntentFilter serviceFilter = new IntentFilter();
		serviceFilter.addAction(ForwarderService.action.SERVICE_INFO);
		
		//register broadcast receiver for Forwarder Serivice actions
		this.registerReceiver(mForwarderServiceActionReceiver, serviceFilter);	
		
		
		//request service state
		AndroidForwarderUtil.serviceActive(this);
		
		//update GUI status
		updateGUI();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		Log.i("MainActivity", "onPause");
		
		//unregister BroadcastListener for USB ATTACH and DETACH actions
		this.unregisterReceiver(mUSBActionReceiver);
		this.unregisterReceiver(mForwarderServiceActionReceiver);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		
		Log.i("MainActivity", "onStop");
	}
	
	@Override
	protected void onDestroy() {
		
		super.onDestroy();
		
		Log.i("MainActivity", "onDestroy");
		//this.unregisterReceiver(mForwarderServiceActionReceiver);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		Log.i("MainActivity", "onOptionsItemSelected " + item.getTitle());
		
		if( item.getItemId() == R.id.action_details ) {
			
			Intent i =  new Intent(this, DetailsActivity.class);
			startActivity(i);
			
		}
		
		if( item.getItemId() == R.id.action_settings ) {
			
			Intent i =  new Intent(this, SettingsActivity.class);
			startActivity(i);
			
		}	
		
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	protected void onNewIntent(Intent intent) {
		
		setIntent(intent);
	}
	
	private void updateGUI() {
		
		//get references to gui objects
		CheckBox deviceConnectedView = (CheckBox) findViewById(R.id.usbConnected);
		Switch serviceSwitch = (Switch) findViewById(R.id.serviceSwitch);
		
		//set USB Device connected indicator
		deviceConnectedView.setChecked(mUSBDevice != null);
		
		//service INFO data
		EditText rcvView = (EditText) findViewById(R.id.editReceived);
		EditText sndView = (EditText) findViewById(R.id.editSent);
		EditText cliView = (EditText) findViewById(R.id.editClients);
		
		rcvView.setText("" + mMsgReceived);
		sndView.setText("" + mMsgSent);
		cliView.setText("" + mClientsActive);
		
		serviceSwitch.setEnabled(true);
		
		//TODO: uncomment !!! set the service state switch and state 
		//serviceSwitch.setEnabled(mUSBDevice != null);
		//if(mUSBDevice == null)
		//	serviceSwitch.setChecked(false);
		
	}
}
