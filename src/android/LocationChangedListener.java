package me.gpelaez.cordova.plugins.ibeacon;

import android.location.Location;

public interface LocationChangedListener {

    void onLocationChanged(Location location);
}