package com.example.safealertv2;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.telephony.SmsManager;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class SafeZoneManager {

    private Context context;
    private GeofencingClient geofencingClient;
    private ArrayList<Geofence> geofenceList;
    private LocationManager locationManager;
    private ArrayList<String> favoriteContacts;
    private MediaPlayer mediaPlayer;
    private boolean isTracking;

    public SafeZoneManager(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
        this.geofenceList = new ArrayList<>();
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.favoriteContacts = new ArrayList<>();
        this.mediaPlayer = MediaPlayer.create(context, R.raw.alert_sound);
        this.isTracking = false;
    }

    public void addSafeZone(String id, double latitude, double longitude, float radius) {
        geofenceList.add(new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList)
                .build();

        PendingIntent geofencePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                new Intent(context, GeofenceBroadcastReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent);
    }


    private void sendAlertSMS(String message) {
        SmsManager smsManager = SmsManager.getDefault();
        for (String contact : favoriteContacts) {
            smsManager.sendTextMessage(contact, null, message, null, null);
        }
    }

    public void playAlertSound() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    public void startLiveTracking() {
        isTracking = true;
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (isTracking) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, android.os.Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void stopLiveTracking() {
        isTracking = false;
        locationManager.removeUpdates((LocationListener) context);
    }

    public void onGeofenceExit(String geofenceId) {
        sendAlertSMS("You left the safezone: " + geofenceId);
        playAlertSound();
        startLiveTracking();
    }
}

