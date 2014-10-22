/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

var argscheck = require('cordova/argscheck'),
    exec = require('cordova/exec'),
    Constants = {
      Proximity:{
        IMMEDIATE: 0,
        NEAR: 1,
        FAR: 2,
        UNKNOWN: 2
      }
    };
    // Constants = require('./iBeaconConstants');
    // XXX: commented out
    //CameraPopoverHandle = require('./CameraPopoverHandle');

var iBeaconExports = {};

// Tack on the iBeacon Constants to the base plugin.
for (var key in Constants) {
    iBeaconExports[key] = Constants[key];
}

iBeaconExports.setDefaults = function(successCallback, errorCallback, options) {
    argscheck.checkArgs('fFO', 'iBeacon.setDefaults', arguments);
    options = options || {};
    var getValue = argscheck.getValue;

    options.title = getValue(options.title, 'Smart Savings');
    options.message = getValue(options.message, 'Encontramos promociones para tí.');

    exec(successCallback, errorCallback, "GPIBeacon", "setDefaults", [options]);
};
iBeaconExports.addRegion = function(successCallback, errorCallback, options) {
    argscheck.checkArgs('fFO', 'iBeacon.addRegion', arguments);
    options = options || {};
    var getValue = argscheck.getValue;

    options.identifier = getValue(options.identifier, 'defaultRegion');
    options.uuid = getValue(options.uuid, '');
    options.major = getValue(options.major, null);
    options.minor = getValue(options.minor, null);
    options.title = getValue(options.title, null);
    options.message = getValue(options.message, null);
    options.range = getValue(options.range, null);

    exec(successCallback, errorCallback, "GPIBeacon", "addRegion", [options]);
};
iBeaconExports.addBeacon = function(successCallback, errorCallback, options) {
    argscheck.checkArgs('fFO', 'iBeacon.addBeacon', arguments);
    options = options || {};
    var getValue = argscheck.getValue;

    options.identifier = getValue(options.identifier, 'defaultRegion');
    options.uuid = getValue(options.uuid, '');
    options.major = getValue(options.major, null);
    options.minor = getValue(options.minor, null);
    options.range = getValue(options.range, null);
    options.title = getValue(options.title, null);
    options.message = getValue(options.message, null);
    options.range = getValue(options.range, null);

    exec(successCallback, errorCallback, "GPIBeacon", "addBeacon", [options]);
};
iBeaconExports.addGeofence = function(successCallback, errorCallback, options) {
    argscheck.checkArgs('fFO', 'iBeacon.addGeofence', arguments);
    options = options || {};
    var getValue = argscheck.getValue;

    options.identifier = getValue(options.identifier, 'defaultRegion');
    options.lat = options.lat;
    options.lon = options.lon;
    options.radius = getValue(options.minor, 12);
    options.title = getValue(options.title, null);
    options.message = getValue(options.message, null);

    exec(successCallback, errorCallback, "GPIBeacon", "addGeofence", [options]);
};


// cameraExport.cleanup = function(successCallback, errorCallback) {
//     exec(successCallback, errorCallback, "Camera", "cleanup", []);
// };

module.exports = iBeaconExports;