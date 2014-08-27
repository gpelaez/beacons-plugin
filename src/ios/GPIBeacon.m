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

#import "GPIBeacon.h"
#import <Cordova/CDVJSON.h>

#pragma mark -
#pragma mark GPIBeacon

@implementation GPIBeacon

static int NIGH_PROXIMITY = -30;

@synthesize locationManager;

- (CDVPlugin*)initWithWebView:(UIWebView*)theWebView
{
    if (self) {
        self.locationManager = [[CLLocationManager alloc] init];
        self.locationManager.delegate = self; // Tells the location manager to send updates to this object
        
        self.lastNigh = [[NSDate alloc] init];
        self.lastFar = [[NSDate alloc] init];
        
        self.regionDict = [[NSMutableDictionary alloc] init];
        self.data = [[NSDictionary alloc] initWithObjectsAndKeys:
                     [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleDisplayName"], @"title",
                     @"", @"message",
                     NO, @"rangeBeacons",
                     @"Ver", @"action",
                     UILocalNotificationDefaultSoundName, @"sound",
                     nil];
    }
    return self;
}
- (void)addBeacon:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        
        NSString* callbackId = command.callbackId;
        @try {
            NSArray* arguments = command.arguments;
            NSDictionary* dictionary = [arguments objectAtIndex:0];
            NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:[dictionary objectForKey:@"uuid"]];
            int major =  [[NSString stringWithString:[dictionary objectForKey:@"major"]] intValue];
            int minor =  [[NSString stringWithString:[dictionary objectForKey:@"minor"]] intValue];
            NSString* identifier = [dictionary objectForKey:@"identifier"];
            
            CLBeaconRegion *myRegion = [[CLBeaconRegion alloc] initWithProximityUUID:uuid major:major minor:minor identifier:identifier];
            if(myRegion) {
                [self.regionDict setObject:dictionary forKey:identifier];
                [self.locationManager startMonitoringForRegion: myRegion];
                
                CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                [self.commandDelegate sendPluginResult:result callbackId:callbackId];
                
                NSLog(@"Region added: %@", [myRegion description]);
            }
        }
        @catch(NSException * e) {
            CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
            [self.commandDelegate sendPluginResult:result callbackId:callbackId];
            
        }
    }];
}
- (void)addGeofence:(CDVInvokedUrlCommand *)command
{
    [self.commandDelegate runInBackground:^{
        NSString* callbackId = command.callbackId;
        @try {
            NSArray* arguments = command.arguments;
            NSDictionary* dictionary = [arguments objectAtIndex:0];
            
            NSString *identifier = [dictionary objectForKey:@"identifier"];
            if(!identifier) identifier = @"default";
            double lat = [[NSString stringWithString:[dictionary objectForKey:@"lat"]] doubleValue];
            double lon = [[NSString stringWithString:[dictionary objectForKey:@"lon"]] doubleValue];
            int radius = [[NSString stringWithString:[dictionary objectForKey:@"radius"]] intValue];
            
            CLLocationCoordinate2D coord = CLLocationCoordinate2DMake(lat, lon);
            
            CLLocationDistance rad = radius;
            
            if(CLLocationCoordinate2DIsValid(coord)) {
                CLCircularRegion *myRegion = [[CLCircularRegion alloc] initWithCenter: coord radius: rad identifier: identifier];
                if(myRegion) {
                    [self.regionDict setObject:dictionary forKey:identifier];
                    [self.locationManager startMonitoringForRegion: myRegion];
                    
                    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
                    
                    NSLog(@"Region added: %@", [myRegion description]);
                }
            } else {
                CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
                [self.commandDelegate sendPluginResult:result callbackId:callbackId];
            }
        }
        @catch(NSException * e) {
            CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
            [self.commandDelegate sendPluginResult:result callbackId:callbackId];
        }
    }];
}
- (void)setDefaults:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        NSString* callbackId = command.callbackId;
        @try {
            NSArray* arguments = command.arguments;
            NSDictionary* dictionary = [arguments objectAtIndex:0];
            NSMutableDictionary *aux = [dictionary mutableCopy];
            [aux addEntriesFromDictionary:self.data];
            self.data = [NSDictionary dictionaryWithDictionary:aux];
        } @catch(NSException * e) {
            CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
            [self.commandDelegate sendPluginResult:result callbackId:callbackId];
        }
    }];
}
- (void)addRegion:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        
        NSString* callbackId = command.callbackId;
        
        @try {
            NSArray* arguments = command.arguments;
            NSDictionary* dictionary = [arguments objectAtIndex:0];
            //        NSString* strUUID = [dictionary objectForKey:@"uuid"];
            NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:[dictionary objectForKey:@"uuid"]];
            int major =  [[NSString stringWithString:[dictionary objectForKey:@"major"]] intValue];
            int minor =  [[NSString stringWithString:[dictionary objectForKey:@"minor"]] intValue];
            NSString* identifier = [dictionary objectForKey:@"identifier"];
            
            CLBeaconRegion *myRegion = [[CLBeaconRegion alloc] initWithProximityUUID:uuid major:major minor:minor identifier:identifier];
            if(myRegion) {
                [self.regionDict setObject:dictionary forKey:identifier];
                [self.locationManager startMonitoringForRegion: myRegion];
                
                CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                [self.commandDelegate sendPluginResult:result callbackId:callbackId];
                
                NSLog(@"Region added: %@", [myRegion description]);
            }
        }
        @catch (NSException * e) {
            CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
            [self.commandDelegate sendPluginResult:result callbackId:callbackId];
            
            //        NSLog(@"Error adding region: %@", [myRegion description]);
        }
    }];
}


#pragma mark -
#pragma mark CLLocationManagerDelegate

-(void)notifyWithTitle:(NSString *) title andMessage:(NSString *) message andData:(NSDictionary *) data {
    UILocalNotification *notification = [[UILocalNotification alloc] init];
    
    // mensaje que saldrá en la alerta
    notification.alertBody = [NSString stringWithFormat:@"%@\n%@", title, message];
    // sonido por defecto
    notification.soundName = UILocalNotificationDefaultSoundName;
    // título del botón
    notification.alertAction = [self.data objectForKey:@"action"];
    notification.hasAction = YES;
    // notification sound
    notification.soundName = [self.data objectForKey:@"sound"];
    notification.applicationIconBadgeNumber = 1;
    
    notification.fireDate = [NSDate date];
    notification.timeZone = [NSTimeZone defaultTimeZone];
    notification.userInfo = data;
    

    
    // activa la notificación
    [[UIApplication sharedApplication] scheduleLocalNotification:notification];
}
- (void)locationManager:(CLLocationManager*)manager didEnterRegion:(CLRegion*)region
{
    if([region isKindOfClass:[CLBeaconRegion class]] || [region isKindOfClass:[CLCircularRegion class]]) {
        //        [self.locationManager startRangingBeaconsInRegion:(CLBeaconRegion *)region];
        NSLog(@"Entered region..%@", region.identifier);
        NSDictionary* dict = [self.regionDict objectForKey:region.identifier];
        
        NSString *title = [dict objectForKey:@"title"];
        if(!title) {
            title = [self.data objectForKey:@"title"];
        }
        NSString *msg = [dict objectForKey:@"message"];
        if(msg) {
            msg = [self.data objectForKey:@"message"];
        }
        
        [self notifyWithTitle:title andMessage:msg andData:dict];
        
//        UIAlertView* alert =[[UIAlertView alloc] initWithTitle:title message:msg delegate:self cancelButtonTitle:@"Cancelar" otherButtonTitles:nil];
//        [alert show];
        NSDictionary *result = [NSDictionary dictionaryWithObjectsAndKeys:dict,@"region", nil];
        
        NSString *jsStatement = [NSString stringWithFormat:@"cordova.fireDocumentEvent('ibeaconenter', %@);", [result JSONString]];
        [self.commandDelegate evalJs:jsStatement];
        
        // Start Ranging Beacon
        if([region isKindOfClass:[CLBeaconRegion class]] && [self.data objectForKey:@"rangeBeacons"]) {
            [self.locationManager startRangingBeaconsInRegion: region];
        }
    }
}

-(void)locationManager:(CLLocationManager*)manager didExitRegion:(CLRegion*)region
{
    //    Do not range beacons... just notify when ibeacon found. Battery consumption issues.
    if([region isKindOfClass:[CLBeaconRegion class]] || [region isKindOfClass:[CLCircularRegion class]]) {
        NSLog(@"Exited region..%@", region.identifier);
        NSDictionary* beacon = [self.regionDict objectForKey:region.identifier];
        // [self.locationManager stopRangingBeaconsInRegion: self.beaconDict[region.identifier]];
        
        NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
        [result setObject:beacon forKey:@"ibeacon"];
        
        NSString *jsStatement = [NSString stringWithFormat:@"cordova.fireDocumentEvent('ibeaconexit', %@);", [result JSONString]];
        [self.commandDelegate evalJs:jsStatement];
        
        //Stop ranging beacon
        if([region isKindOfClass:[CLBeaconRegion class]] && [self.data objectForKey:@"rangeBeacons"]) {
            [self.locationManager stopRangingBeaconsInRegion:(CLBeaconRegion *) region];
        }
    }
}

-(void)locationManager:(CLLocationManager*)manager
       didRangeBeacons:(NSArray*)beacons
              inRegion:(CLBeaconRegion*)region
{
    // Beacon found!
    if (beacons.count > 0) {
        
        CLBeacon *foundBeacon = [beacons firstObject];
        
        if (foundBeacon.rssi >= NIGH_PROXIMITY && foundBeacon.rssi < 0) {
            NSLog(@"%ld - %ld", (long)foundBeacon.rssi, (long)foundBeacon.major);
            
            NSTimeInterval secs = [self.lastNigh timeIntervalSinceNow];
            
            if (secs < -6) {
                [self sendIbeaconEvent:foundBeacon forRegion:region forRange:@"nigh"];
                
                self.lastNigh = [[NSDate alloc] init];
            }
            
        } else {
            switch (foundBeacon.proximity) {
                case CLProximityNear:
                case CLProximityFar:
                {
                    NSTimeInterval secs = [self.lastFar timeIntervalSinceNow];
                    
                    if (secs < -60) {
                        [self sendIbeaconEvent:foundBeacon forRegion:region forRange:[self regionText:foundBeacon]];
                        
                        self.lastFar = [[NSDate alloc] init];
                    }
                }
                    break;
                default:
                    break;
            }
        }
    }
    
}

- (void)sendIbeaconEvent:(CLBeacon *)foundBeacon forRegion:(CLRegion *) region forRange:(NSString *) range
{
    NSLog(@"Sending event");
    
    NSDictionary* beacon = [self.regionDict objectForKey:region.identifier];
    NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
    [result setObject:beacon forKey:@"ibeacon"];
    
    NSLog(@"%@", [result JSONString]);
    
    NSString *jsStatement = [NSString stringWithFormat:@"cordova.fireDocumentEvent('ibeacon', %@);", [result JSONString]];
    
    [self.commandDelegate evalJs:jsStatement];
}


- (void)locationManager:(CLLocationManager *)manager didStartMonitoringForRegion:(CLRegion *)region
{
    //    NSLog(@"hello %@", region.identifier);
    if([region isKindOfClass:[CLBeaconRegion class]]) {
        //        [self.locationManager startRangingBeaconsInRegion:(CLBeaconRegion *)region];
    }
}

- (void)locationManager: (CLLocationManager *)manager
       didFailWithError: (NSError *)error
{
    [manager stopUpdatingLocation];
    NSLog(@"error%@",error);
}

- (void)dealloc
{
    self.locationManager.delegate = nil;
    self.regionDict = nil;
}

- (void)onReset
{
    self.regionDict = nil;
}

- (NSString *)regionText:(CLBeacon *)beacon
{
    switch (beacon.proximity) {
        case CLProximityFar:
            return @"far";
        case CLProximityImmediate:
            return @"immediate";
        case CLProximityNear:
            return @"near";
        default:
            return @"unknown";
    }
}

#pragma mark -
#pragma mark Local notifications handling

/**
 * Calls the cancel or trigger event after a local notification was received.
 * Cancels the local notification if autoCancel was set to true.
 */
- (void) didReceiveLocalNotification:(NSNotification*)localNotification
{
    UILocalNotification* notification = [localNotification object];
    
    NSDictionary* userInfo = notification.userInfo;
    
    
  // cancelar notificación
    [[UIApplication sharedApplication] cancelLocalNotification:notification];
    
    //send javascript
    NSDictionary *result = [NSDictionary dictionaryWithObjectsAndKeys:userInfo,@"region", nil];
    
    NSString *jsStatement = [NSString stringWithFormat:@"cordova.fireDocumentEvent('ibeaconenter', %@);", [result JSONString]];
    [self.commandDelegate evalJs:jsStatement];

}

/**
 * Calls the cancel or trigger event after a local notification was received.
 */
- (void) didFinishLaunchingWithOptions:(NSNotification*)notification
{
    NSDictionary* launchOptions = [notification userInfo];
    
    UILocalNotification* localNotification = [launchOptions objectForKey:
                                              UIApplicationLaunchOptionsLocalNotificationKey];
    
    if (localNotification) {
        [self didReceiveLocalNotification:
         [NSNotification notificationWithName:CDVLocalNotification
                                       object:localNotification]];
    }
}

/**
 * Registers obervers for the following events after plugin was initialized.
 *      didReceiveLocalNotification:
 *      didFinishLaunchingWithOptions:
 */
- (void) pluginInitialize
{
    NSNotificationCenter* notificationCenter = [NSNotificationCenter
                                                defaultCenter];
    [notificationCenter addObserver:self
                           selector:@selector(didReceiveLocalNotification:)
                               name:CDVLocalNotification
                             object:nil];
    [notificationCenter addObserver:self
                           selector:@selector(didFinishLaunchingWithOptions:)
                               name:UIApplicationDidFinishLaunchingNotification
                             object:nil];
}


@end