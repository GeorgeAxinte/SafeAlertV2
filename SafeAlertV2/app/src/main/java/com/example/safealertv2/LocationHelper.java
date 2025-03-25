package com.example.safealertv2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationHelper {
    public static final int PERMISSION_REQUEST_LOCATION = 1;
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final LocationCallback locationCallback;
    private final LocationListener locationListener;

    public interface LocationListener {
        void onLocationRetrieved(double latitude, double longitude);
    }

    public LocationHelper(Context context, LocationListener listener) {
        this.context = context;
        this.locationListener = listener;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        locationListener.onLocationRetrieved(location.getLatitude(), location.getLongitude());
                    }
                }
            }
        };
    }

    public void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                locationListener.onLocationRetrieved(location.getLatitude(), location.getLongitude());
            }
        });
    }
}
