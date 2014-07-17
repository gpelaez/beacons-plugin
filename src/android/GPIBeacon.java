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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.util.Log;
import android.os.Bundle;
import android.os.RemoteException;
import android.content.ServiceConnection;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconData;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconDataNotifier;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.client.DataProviderException;
import com.radiusnetworks.ibeacon.MonitorNotifier;
import com.radiusnetworks.ibeacon.Region;
import com.radiusnetworks.ibeacon.RangeNotifier;

public class GPIBeacon extends CordovaPlugin implements IBeaconConsumer, MonitorNotifier, RangeNotifier, IBeaconDataNotifier {
    public static final int REQUEST_CODE = 0x0ba7c0de;
    
    public static final int NOTIFICATION_ID = 137;
    
    private static final String TAG = "GPIBeacon";

    private static final String ACTION_ADDREGION = "addRegion";
    
    private static final int NEAR_FAR_FREQUENCY = 1 * 60 * 1000;
    private static final int NIGH_RSSI = -30;
    private static final int NIGH_FREQUENCY = 6 * 1000;
    private static final int PROXIMITY_NIGH = 100;

    private static Activity activity;
    private boolean gForeground = false;
    private static CordovaWebView gWebView;
    private static Hashtable<String,JSONObject> beaconsData;

    private IBeaconManager iBeaconManager;

    private Time lastNigh;
    private Time lastFar;

    private Boolean firstNearFar;

    private CallbackContext callbackContext;

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

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {

                iBeaconManager = IBeaconManager.getInstanceForApplication(activity);
                iBeaconManager.bind(that);
            }
        });
    }

    /**
     * Executes the request.
     *
     * This method is called from the WebView thread. To do a non-trivial amount of work, use:
     *     cordova.getThreadPool().execute(runnable);
     *
     * To run on the UI thread, use:
     *     cordova.getActivity().runOnUiThread(runnable);
     *
     * @param action          The action to execute.
     * @param data            The exec() arguments.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return                Whether the action was valid.
     *
     * @sa https://github.com/apache/cordova-android/blob/master/framework/src/org/apache/cordova/CordovaPlugin.java
     */
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;

        Log.d(TAG, "execute " + action);
        
        if(gWebView==null) gWebView = this.webView;

        
        if (action.equals(ACTION_ADDREGION)) {
            try {
                addRegion(data, callbackContext);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else if (action.equals("anotheraction")) {

        } else {
            return false;
        }
        return true;
    }

    private void addRegion(JSONArray data, CallbackContext callbackContext) throws JSONException {

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
//                  iBeaconManager.startMonitoringBeaconsInRegion(new Region("", null, null, null));
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        gForeground = true;
        iBeaconManager.unBind(this);
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        gForeground = false;
        if (iBeaconManager.isBound(this)) iBeaconManager.setBackgroundMode(this, true);
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        gForeground = true;
        if (iBeaconManager.isBound(this)) iBeaconManager.setBackgroundMode(this, false);
    }

    public Context getApplicationContext() {
        return cordova.getActivity();
    }

    public void unbindService(ServiceConnection connection) {
        cordova.getActivity().unbindService(connection);
    }

    public boolean bindService(Intent intent, ServiceConnection connection, int mode) {
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
//            iBeaconManager.startRangingBeaconsInRegion(region);
//            iBeaconManager.setRangeNotifier(this);
            
            JSONObject obj = beaconsData.get(region.getUniqueId());
            obj.put("foreground", this.gForeground);
            
            JSONObject result = new JSONObject();
            result.put("ibeacon", obj);
            result.put("event", "ibeaconenter");
            
            if (this.gForeground) {
                  performJSEvent(result.getString("event"),result);
            } else {
                result.put("title", obj.getString("title"));
                result.put("message", obj.getString("message"));
                // Send a notification if there is a message
                createNotification(this.getApplicationContext(), result);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public static void performJSEvent(String eventName,JSONObject result)
    {
        final String jsStatement = String.format("cordova.fireDocumentEvent(\"%s\", %s);", eventName, result.toString());
        
        Log.v(TAG, "sendJavascript: " + jsStatement);
        String _d = "javascript:" + jsStatement;

        if (gWebView != null) {
            gWebView.sendJavascript(_d); 
        }
 
/*        cordova.getActivity().runOnUiThread(
            new Runnable() {
                @Override
                 public void run() {
                     webView.loadUrl("javascript:" + jsStatement);
                 }
            }
        );
*/

        // String _d = "javascript:" + eventName + "_callback(" + result.toString() + ")";
        // Log.v(TAG, "sendJavascript: " + _d);

        // if (gWebView != null) {
        //     gWebView.sendJavascript(_d); 
        // }
    }
    /**
     * + * Issues a notification to inform the user that server has sent a
     * message. +
     * @throws JSONException 
     */
    @SuppressLint("InlinedApi")
    private static void createNotification(Context context, JSONObject json) throws JSONException {
        Bundle extra = new Bundle();
        extra.putString("json", json.toString());
        
        Intent notificationIntent = new Intent(activity, BeaconNotificationHandler.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("beacon",extra);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setSmallIcon(context.getApplicationInfo().icon)
                    .setWhen(System.currentTimeMillis())
                    .setTicker(json.getString("title"))
                    .setContentTitle(json.getString("message"))
                    .setContentIntent(contentIntent);
        
        String message = json.getString("message");
        if (message != null) {
            mBuilder.setContentText(message);
        } else {
            mBuilder.setContentText("<missing message content>");
        }
        mBuilder.addAction(context.getApplicationInfo().icon, json.getString("message"), contentIntent);
        
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
            .notify((String) getAppName(context), NOTIFICATION_ID, mBuilder.build());
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

            final String jsStatement = String.format("cordova.fireDocumentEvent('ibeaconexit', %s);", result.toString());

            cordova.getActivity().runOnUiThread(
                new Runnable() {
                    @Override
                     public void run() {
                         webView.loadUrl("javascript:" + jsStatement);
                     }
                }
            );

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        // logToDisplay("I have just switched from seeing/not seeing iBeacons: "+state);
        Log.d(TAG, "I have just switched from seeing/not seeing iBeacons: "+state);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region) {

        for (IBeacon iBeacon: iBeacons) {
            Integer proximity = iBeacon.getProximity();
            Integer rssi = iBeacon.getRssi();

            // custom check for nigh proximity,
            if (rssi > NIGH_RSSI && rssi < 0) {
                Log.d(TAG, "Found One: " + rssi + " " + iBeacon.getProximityUuid() + " " + iBeacon.getMajor() + " " + iBeacon.getMinor());

                Time now = new Time();
                now.setToNow();

                if ((now.toMillis(false) - this.lastNigh.toMillis(false)) > NIGH_FREQUENCY) {
                    sendIbeaconEvent(iBeacon, region, PROXIMITY_NIGH);
                    this.lastNigh.setToNow();
                }

            } else if (proximity == IBeacon.PROXIMITY_FAR || proximity == IBeacon.PROXIMITY_NEAR) {

                Time now = new Time();
                now.setToNow();

                if (this.firstNearFar || (now.toMillis(false) - this.lastFar.toMillis(false)) > NEAR_FAR_FREQUENCY) {
                    Log.d(TAG, "Found One Near/Far: "+iBeacon.getAccuracy());

                    sendIbeaconEvent(iBeacon, region, proximity);
                    this.lastFar.setToNow();
                    this.firstNearFar = false;
                }
            }
        }

    }

    @Override
    public void iBeaconDataUpdate(IBeacon iBeacon, IBeaconData iBeaconData, DataProviderException e) {
        if (e != null) {
            Log.d(TAG, "data fetch error:"+e);
        }
        if (iBeaconData != null) {
            String displayString = iBeacon.getProximityUuid()+" "+iBeacon.getMajor()+" "+iBeacon.getMinor()+"\n"+"Welcome message:"+iBeaconData.get("welcomeMessage");

        }
    }

    private void sendIbeaconEvent(IBeacon iBeacon, Region region, Integer range)
    {
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

            final String jsStatement = String.format("cordova.fireDocumentEvent('ibeacon', %s);", result.toString());

            cordova.getActivity().runOnUiThread(
                new Runnable() {
                    @Override
                     public void run() {
                         webView.loadUrl("javascript:" + jsStatement);
                     }
                }
            );

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
    public static void cancelNotification(Context context)
    {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel((String)getAppName(context), NOTIFICATION_ID);  
    }
    private static String getAppName(Context context)
    {
        CharSequence appName = 
                context
                    .getPackageManager()
                    .getApplicationLabel(context.getApplicationInfo());
        
        return (String)appName;
    }

    public static boolean isActive() {
        // TODO Auto-generated method stub
        return gWebView != null;
    }
}