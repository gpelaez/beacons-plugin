/*
The MIT License (MIT)

Copyright (c) 2014

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package me.gpelaez.cordova.plugins.ibeacon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconData;
import com.radiusnetworks.ibeacon.IBeaconDataNotifier;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.MonitorNotifier;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;
import com.radiusnetworks.ibeacon.client.DataProviderException;

public class GPIBeacon extends CordovaPlugin implements IBeaconConsumer,
		MonitorNotifier, RangeNotifier, IBeaconDataNotifier {
	public static final int REQUEST_CODE = 0x0ba7c0de;

	public static final int NOTIFICATION_ID = 137;

	private static final String TAG = GPIBeacon.class.getSimpleName();

	private static final String ACTION_ADDREGION = "addRegion";
	private static final String ACTION_ADDBEACON = "addBeacon";
	private static final String ACTION_ADDGEOFENCE = "addGeofence";
	private static final String ACTION_SETDEFAULT = "setDefaults";

	private static final int NEAR_FAR_FREQUENCY = 1 * 60 * 1000;
	private static final int NIGH_RSSI = -30;
	private static final int NIGH_FREQUENCY = 6 * 1000;
	private static final int PROXIMITY_NIGH = 100;

	private static Activity activity;
	private boolean gForeground = false;
	private static CordovaWebView gWebView;
	private static Hashtable<String, JSONObject> beaconsData;
	private static Hashtable<String, String> defaults = new Hashtable<String, String>();
	private static ArrayList<String> itemsNotification = new ArrayList<String>();

	private static GPIBeacon instance;

	private IBeaconManager iBeaconManager;

	private Time lastNigh;
	private Time lastFar;

	private Boolean firstNearFar;

	// --- Geofence ---
	private static Hashtable<String, JSONObject> geofenceData;
	public LocationManagerService service;
	private BroadcastReceiver receiver;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);

		activity = cordova.getActivity();
		final GPIBeacon that = this;

		this.gForeground = true;

		this.lastNigh = new Time();
		this.lastNigh.setToNow();

		this.lastFar = new Time();
		this.lastFar.setToNow();
		this.firstNearFar = true;
		beaconsData = new Hashtable<String, JSONObject>();
		geofenceData = new Hashtable<String, JSONObject>();

		cordova.getThreadPool().execute(new Runnable() {
			@Override
			public void run() {

				iBeaconManager = IBeaconManager
						.getInstanceForApplication(activity);
				iBeaconManager.bind(that);

				// Geofence
				service = new LocationManagerService(activity);
			}
		});
	}

	/**
	 * Executes the request.
	 * 
	 * This method is called from the WebView thread. To do a non-trivial amount
	 * of work, use: cordova.getThreadPool().execute(runnable);
	 * 
	 * To run on the UI thread, use:
	 * cordova.getActivity().runOnUiThread(runnable);
	 * 
	 * @param action
	 *            The action to execute.
	 * @param data
	 *            The exec() arguments.
	 * @param callbackContext
	 *            The callback context used when calling back into JavaScript.
	 * @return Whether the action was valid.
	 * 
	 * @sa 
	 *     https://github.com/apache/cordova-android/blob/master/framework/src/org
	 *     /apache/cordova/CordovaPlugin.java
	 */
	@Override
	public boolean execute(String action, JSONArray data,
			CallbackContext callbackContext) {
		
		Log.d(TAG, "execute " + action);

		if (gWebView == null)
			gWebView = this.webView;

		if (action.equals(ACTION_ADDREGION)) {
			try {
				addRegion(data, callbackContext);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (action.equals(ACTION_ADDBEACON)) {
			try {
				addBeacon(data, callbackContext);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (action.equals(ACTION_ADDGEOFENCE)) {
			try {
				addGeofence(data, callbackContext);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (action.equals(ACTION_SETDEFAULT)) {
			try {
				setDefaults(data, callbackContext);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			return false;
		}
		return true;
	}

	private void addBeacon(JSONArray data, CallbackContext callbackContext)
			throws JSONException {
		final JSONObject data2 = data.getJSONObject(0);
		final CallbackContext callbackContext2 = callbackContext;
		cordova.getThreadPool().execute(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					String iden = data2.getString("identifier");
					String uuid = data2.getString("uuid");
					Integer major = data2.getInt("major");
					Integer minor = data2.getInt("minor");
					beaconsData.put(iden, data2);

					Region region = new Region(iden, uuid, major, minor);
					iBeaconManager.startMonitoringBeaconsInRegion(region);
					// iBeaconManager.startMonitoringBeaconsInRegion(new
					// Region("", null, null, null));
					callbackContext2.success();

				} catch (RemoteException e) {
					callbackContext2.error("Could not add region");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});
	}

	private void addGeofence(JSONArray data, CallbackContext callbackContext)
			throws JSONException {
		final JSONObject data2 = data.getJSONObject(0);
		final CallbackContext callbackContext2 = callbackContext;
		cordova.getThreadPool().execute(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				String iden;
				try {
					iden = data2.getString("identifier");
					Double lat = data2.getDouble("lat");
					Double lon = data2.getDouble("lon");
					Float radius = (float) data2.getInt("radius");
					geofenceData.put(iden, data2);
					Log.d(TAG, "Adding Geofence(" + iden + ", " + lat + ", "
							+ lon + ", " + radius + ")");
					service.addRegion(iden, lat, lon, radius);

					registerListener();
					callbackContext2.success("Geofence " + iden
							+ " Added Successfully.");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					callbackContext2.error("Geofence add error.");
				}

			}

		});
	}

	private void setDefaults(JSONArray data, CallbackContext callbackContext)
			throws JSONException {
		final JSONObject data2 = data.getJSONObject(0);
		final CallbackContext callbackContext2 = callbackContext;
		cordova.getThreadPool().execute(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				callbackContext2.error("Geofence still in beta phase");
			}

		});
	}

	private void addRegion(JSONArray data, CallbackContext callbackContext)
			throws JSONException {

		final JSONObject data2 = data.getJSONObject(0);

		final CallbackContext callbackContext2 = callbackContext;

		cordova.getThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				try {
					String iden = data2.getString("identifier");
					String uuid = data2.getString("uuid");
					Integer major = data2.getInt("major");
					Integer minor = data2.getInt("minor");
					beaconsData.put(iden, data2);

					Region region = new Region(iden, uuid, major, minor);
					iBeaconManager.startMonitoringBeaconsInRegion(region);
					// iBeaconManager.startMonitoringBeaconsInRegion(new
					// Region("", null, null, null));
					callbackContext2.success();

				} catch (RemoteException e) {
					callbackContext2.error("Could not add region");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	private void init(CallbackContext callbackContext) {
		Log.d(TAG, "Enabling plugin");

		callbackContext.success();
	}

	private void registerListener() {
		IntentFilter filter = new IntentFilter(
				LocationManagerService.PROXIMITY_ALERT_INTENT);
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, final Intent intent) {
				fireRegionChangedEvent(intent);
			}
		};
		cordova.getActivity().registerReceiver(receiver, filter);
	}

	/**
	 * Método que se ejecuta al ingresar a un geofence.
	 * 
	 * @param intent
	 *            El intent debe traer unos extras.
	 */
	void fireRegionChangedEvent(final Intent intent) {

		String event = intent.getBooleanExtra(
				LocationManager.KEY_PROXIMITY_ENTERING, false) ? "geofenceenter"
				: "geofenceleave";
		try {
			String iden = (String) intent.getExtras().get("id");
			JSONObject obj = geofenceData.get(iden);

			JSONObject result = new JSONObject();
			result.put("geofence", obj);
			result.put("event", event);
			result.put("foreground", this.gForeground);

			if (this.gForeground) {
				performJSEvent(result.getString("event"), result);
			} else if (event.equalsIgnoreCase("geofenceenter")) {
				result.put("title", obj.getString("title"));
				result.put("message", obj.getString("message"));
				itemsNotification.add(obj.getString("identifier"));
				createNotification(this.getApplicationContext(), result);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		gForeground = true;
		iBeaconManager.unBind(this);
		if (receiver != null) {
			cordova.getActivity().unregisterReceiver(receiver);
		}
	}

	@Override
	public void onPause(boolean multitasking) {
		super.onPause(multitasking);
		gForeground = false;
		if (iBeaconManager.isBound(this))
			iBeaconManager.setBackgroundMode(this, true);
	}

	@Override
	public void onResume(boolean multitasking) {
		super.onResume(multitasking);
		gForeground = true;
		if (iBeaconManager.isBound(this))
			iBeaconManager.setBackgroundMode(this, false);
	}

	public Context getApplicationContext() {
		return cordova.getActivity();
	}

	public void unbindService(ServiceConnection connection) {
		cordova.getActivity().unbindService(connection);
	}

	public boolean bindService(Intent intent, ServiceConnection connection,
			int mode) {
		return cordova.getActivity().bindService(intent, connection, mode);
	}

	@Override
	public void onIBeaconServiceConnect() {
		iBeaconManager.setMonitorNotifier(this);
	}

	@Override
	public void didEnterRegion(Region region) {
		Log.d(TAG, "I just saw an iBeacon for the first time!");

		try {
			// iBeaconManager.startRangingBeaconsInRegion(region);
			// iBeaconManager.setRangeNotifier(this);

			JSONObject obj = beaconsData.get(region.getUniqueId());

			JSONObject result = new JSONObject();
			result.put("ibeacon", obj);
			result.put("event", "ibeaconenter");
			result.put("foreground", this.gForeground);

			if (this.gForeground) {

				performJSEvent(result.getString("event"), result);
			} else {
				result.put("title", obj.getString("title"));
				result.put("message", obj.getString("message"));
				itemsNotification.add(obj.getString("identifier"));
				createNotification(this.getApplicationContext(), result);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public static void performJSEvent(String eventName, JSONObject result)
			throws JSONException {
		if (result == null) {
			JSONArray inner = new JSONArray();
			for (int i = 0; i < itemsNotification.size(); i++) {
				inner.put(beaconsData.get(itemsNotification.get(i)));
			}
			result = new JSONObject();
			result.put("ibeacons", inner);
		}
		final String jsStatement = String.format(
				"cordova.fireDocumentEvent(\"%s\", %s);", eventName,
				result.toString());

		Log.v(TAG, "sendJavascript: " + jsStatement);
		String _d = "javascript:" + jsStatement;

		if (gWebView != null) {
			gWebView.sendJavascript(_d);

		}

		/*
		 * cordova.getActivity().runOnUiThread( new Runnable() {
		 * 
		 * @Override public void run() { webView.loadUrl("javascript:" +
		 * jsStatement); } } );
		 */

		// String _d = "javascript:" + eventName + "_callback(" +
		// result.toString() + ")";
		// Log.v(TAG, "sendJavascript: " + _d);

		// if (gWebView != null) {
		// gWebView.sendJavascript(_d);
		// }
	}

	/**
	 * + * Issues a notification to inform the user that server has sent a
	 * message. +
	 * 
	 * @throws JSONException
	 */
	@SuppressLint("InlinedApi")
	private static void createNotification(Context context, JSONObject json)
			throws JSONException {
		Bundle extra = new Bundle();
		extra.putString("json", json.toString());

		Intent notificationIntent = new Intent(activity,
				BeaconNotificationHandler.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("beacon", extra);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context).setDefaults(Notification.DEFAULT_ALL)
				.setWhen(System.currentTimeMillis())
				.setTicker(json.getString("message"))
				.setContentTitle(json.getString("title"))
				.setContentText(json.getString("message"))
				.setSmallIcon(context.getApplicationInfo().icon)
				.setContentIntent(contentIntent);

		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

		// Sets a title for the Inbox style big view
		inboxStyle.setBigContentTitle("Beacons");

		// Moves events into the big view
		for (int i = 0; i < itemsNotification.size(); i++) {
			JSONObject beacon = beaconsData.get(itemsNotification.get(i));
			inboxStyle.addLine(beacon.getString("title"));
		}
		// Moves the big view style object into the notification object.
		mBuilder.setStyle(inboxStyle);

		String message = json.getString("message");
		if (message != null) {
			mBuilder.setContentText(message);
		} else {
			mBuilder.setContentText("<missing message content>");
		}
		mBuilder.addAction(context.getApplicationInfo().icon, "Ver mas",
				contentIntent);

		((NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE))
				.notify((String) getAppName(context), NOTIFICATION_ID,
						mBuilder.build());

	}

	@Override
	public void didExitRegion(Region region) {
		Log.d(TAG, "I no longer see an iBeacon");

		try {
			iBeaconManager.startRangingBeaconsInRegion(region);

			JSONObject obj = new JSONObject();
			obj.put("identifier", region.getUniqueId());

			JSONObject result = new JSONObject();
			result.put("ibeacon", obj);

			final String jsStatement = String.format(
					"cordova.fireDocumentEvent('ibeaconexit', %s);",
					result.toString());

			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					webView.loadUrl("javascript:" + jsStatement);
				}
			});

		} catch (JSONException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void didDetermineStateForRegion(int state, Region region) {
		// logToDisplay("I have just switched from seeing/not seeing iBeacons: "+state);
		Log.d(TAG, "I have just switched from seeing/not seeing iBeacons: "
				+ state);
	}

	@Override
	public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons,
			Region region) {

		for (IBeacon iBeacon : iBeacons) {
			Integer proximity = iBeacon.getProximity();
			Integer rssi = iBeacon.getRssi();

			// custom check for nigh proximity,
			if (rssi > NIGH_RSSI && rssi < 0) {
				Log.d(TAG,
						"Found One: " + rssi + " " + iBeacon.getProximityUuid()
								+ " " + iBeacon.getMajor() + " "
								+ iBeacon.getMinor());

				Time now = new Time();
				now.setToNow();

				if ((now.toMillis(false) - this.lastNigh.toMillis(false)) > NIGH_FREQUENCY) {
					sendIbeaconEvent(iBeacon, region, PROXIMITY_NIGH);
					this.lastNigh.setToNow();
				}

			} else if (proximity == IBeacon.PROXIMITY_FAR
					|| proximity == IBeacon.PROXIMITY_NEAR) {

				Time now = new Time();
				now.setToNow();

				if (this.firstNearFar
						|| (now.toMillis(false) - this.lastFar.toMillis(false)) > NEAR_FAR_FREQUENCY) {
					Log.d(TAG, "Found One Near/Far: " + iBeacon.getAccuracy());

					sendIbeaconEvent(iBeacon, region, proximity);
					this.lastFar.setToNow();
					this.firstNearFar = false;
				}
			}
		}

	}

	@Override
	public void iBeaconDataUpdate(IBeacon iBeacon, IBeaconData iBeaconData,
			DataProviderException e) {
		if (e != null) {
			Log.d(TAG, "data fetch error:" + e);
		}
		if (iBeaconData != null) {
			String displayString = iBeacon.getProximityUuid() + " "
					+ iBeacon.getMajor() + " " + iBeacon.getMinor() + "\n"
					+ "Welcome message:" + iBeaconData.get("welcomeMessage");
			
			Log.d(TAG,"" + displayString);

		}
	}

	private void sendIbeaconEvent(IBeacon iBeacon, Region region, Integer range) {
		try {
			Log.d(TAG, "Firing Event");

			JSONObject obj = new JSONObject();
			obj.put("uuid", iBeacon.getProximityUuid());
			obj.put("major", iBeacon.getMajor());
			obj.put("minor", iBeacon.getMinor());
			obj.put("range", proximityText(range));
			obj.put("identifier", region.getUniqueId());

			JSONObject result = new JSONObject();
			result.put("ibeacon", obj);

			final String jsStatement = String.format(
					"cordova.fireDocumentEvent('ibeacon', %s);",
					result.toString());

			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					webView.loadUrl("javascript:" + jsStatement);
				}
			});

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private String proximityText(Integer proximity) {
		switch (proximity) {
		case PROXIMITY_NIGH:
			return "nigh";
		case IBeacon.PROXIMITY_NEAR:
			return "near";
		case IBeacon.PROXIMITY_FAR:
			return "far";
		case IBeacon.PROXIMITY_IMMEDIATE:
			return "immediate";
		default:
			return "unknown";
		}
	}

	public static void cancelNotification(Context context) {
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel((String) getAppName(context),
				NOTIFICATION_ID);
		itemsNotification.clear();
	}

	private static String getAppName(Context context) {
		CharSequence appName = context.getPackageManager().getApplicationLabel(
				context.getApplicationInfo());

		return (String) appName;
	}

	public static boolean isActive() {
		// TODO Auto-generated method stub
		return gWebView != null;
	}

	public static GPIBeacon getInstance() {
		return instance;
	}

	public GPIBeacon() {
		instance = this;
	}
}