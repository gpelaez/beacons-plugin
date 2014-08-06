iBeacon plugin.

<!-- Immediate proximity has proven to be flakey on both ios and android, so have added a new custom proximity "nigh" which is based purely on the rssi being greater than -30.  This implementation meets our current requirement however further testing is required against multiple devices/beacon types and with phone cases.  Unfortunately it's an unknown at this stage. This can not currently be configured on the JS side but support will be added in future to control/setup notifications via javascript.
 -->
 This plugin allows you to send notifications triggered by the proximity to Bluetooth LE iBeacons; this plugin works on android an ios cordova apps

# Installation
    
    cordova plugin add https://github.com/gpelaez/beacons-plugin.git

# Watch a region/UUID
    cordova.plugins.ibeacon.addRegion(
        // Success Callback
        function() {
            console.log('added successfully') ;
        }, 
        // Error Callback
        function() { 
            console.log('oh no! error');
        }, 
        // Params
        { 
            identifier: 'com.mydomain.ibeacon.myregion', 
            uuid: 'MY-UUID',
            major: <beacon-major>,
            minor: <beacon-minor>,
            title: <title-for-local-notifications>
        }
    );

# Events
All events are called on the document object. Sample code:
    document.addEventListener('ibeacon')

## ibeacon
Called when in beacon proximity

near & far once per minute,
nigh once per 6 seconds

    {
        // event data
        ibeacon: {
            uuid: 'MY-UUID',
            major: 1,
            minor: 1,
            range: 'immediate',
            identifier: 'com.mydomain.ibeacon.myregion'
        }
    }

## ibeaconenter
Called when a region is entered. 

    {
        // event data
        ibeacon: {
            identifier: 'com.mydomain.ibeacon.myregion'
        }
    }

## ibeaconexit
Called when a region is exited.

    {
        // event data
        ibeacon: {
            identifier: 'com.mydomain.ibeacon.myregion'
        }
    }

ios and android support
