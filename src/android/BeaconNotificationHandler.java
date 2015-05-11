package com.ingeneo.cordova.plugins.ibeacon;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class BeaconNotificationHandler extends Activity{

	private static String TAG = "BeaconNotificationHandler"; 

	/*
	 * this activity will be started if the user touches a notification that we own. 
	 * We send it's data off to the push plugin for processing.
	 * If needed, we boot up the main activity to kickstart the application. 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.v(TAG, "onCreate");

		boolean isBeaconPluginActive = GPIBeacon.isActive();
		try {
			processBeaconNotification(isBeaconPluginActive);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		finish();

		if (!isBeaconPluginActive) {
			forceMainActivityReload();
		}
	}
	/**
	 * Takes the pushBundle extras from the intent, 
	 * and sends it through to the PushPlugin for processing.
	 * @throws JSONException 
	 */
	private void processBeaconNotification(boolean isBeaconPluginActive) throws JSONException
	{
		Bundle extras = getIntent().getExtras();

		if (extras != null)	{
			JSONObject beacon = new JSONObject(extras.getBundle("beacon").getString("json"));
			GPIBeacon.performJSEvent(beacon.getString("event"),beacon);
			GPIBeacon.cancelNotification(this);
		}
	}
	/**
	 * Forces the main activity to re-launch if it's unloaded.
	 */
	private void forceMainActivityReload()
	{
		PackageManager pm = getPackageManager();
		Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());    		
		startActivity(launchIntent);
	}

}
