package me.gpelaez.cordova.plugins.ibeacon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static me.gpelaez.cordova.plugins.ibeacon.LocationManagerService.TAG;

public class ProximityReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    String id = (String) intent.getExtras().get("id");
    Log.d(TAG, "received proximity alert for region " + id);

    GPIBeacon.getInstance().fireRegionChangedEvent(intent);
  }
}
