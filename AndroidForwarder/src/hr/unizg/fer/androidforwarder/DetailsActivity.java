package hr.unizg.fer.androidforwarder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class DetailsActivity extends Activity {

	
	private BroadcastReceiver mMessageInfoReceiver = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_details);
		// Show the Up button in the action bar.
		setupActionBar();
		
		//Log.i("DetailsActivity", "onCreate");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		//register BroadcastListener for MESSAGE_INFO Intents
		mMessageInfoReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
							
				//Log.i("Details activity", intent.getAction());
				
				if( intent.getAction().equals(ForwarderService.action.MESSAGE_INFO)) {
					boolean incoming = intent.getBooleanExtra(ForwarderService.extra.EXTRA_MESSAGE_INCOMING, false);
					byte[] content = intent.getByteArrayExtra(ForwarderService.extra.EXTRA_MESSAGE_CONTENT);
					
					EditText edit = (EditText) findViewById(R.id.textArea);
					SimpleDateFormat now = new SimpleDateFormat("HH.mm.ss");
					String timer = now.format(new Date());
		
					if( incoming )
						edit.append("-> ");
					else
						edit.append("<- ");
					edit.append(timer + "\t");
					edit.append(AndroidForwarderUtil.bytesToHexDelimited(content));
					edit.append("\n");
				}			
			}
		};
		
		IntentFilter messageInfoFilter = new IntentFilter();
		messageInfoFilter.addAction(ForwarderService.action.MESSAGE_INFO);
		
		//register broadcast receiver
		this.registerReceiver(mMessageInfoReceiver, messageInfoFilter);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		unregisterReceiver(mMessageInfoReceiver);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		//Log.i("DetailsActivity", "onDestroy");
	}
	
	

}
