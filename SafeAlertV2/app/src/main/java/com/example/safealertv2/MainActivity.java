package com.example.safealertv2;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements WeatherHelper.WeatherListener, EarthquakeHelper.EarthquakeListener {

    private static final int PERMISSION_REQUEST_LOCATION = 1;
    private static final int PERMISSION_REQUEST_SMS = 2;
    private TextView locationTextView, temperatureTextView, weatherDescriptionTextView, coordinatesTextView, earthquakeTextView, emergencyTextView;
    private ImageView weatherIconImageView;
    private Button sosButton, stopButton, openMapsButton, sosButtonLarge, scheduleMessageButton, safeZoneButton, cancelButton;

    private FusedLocationProviderClient fusedLocationClient;
    private WeatherHelper weatherHelper;
    private EarthquakeHelper earthquakeHelper;
    private double currentLatitude, currentLongitude;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private SOSManager sosManager;
    private SafeZoneManager safeZoneManager;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ExecutorService executorService;
    private static final int MAX_VOLUME_PRESS_COUNT = 3;
    private static final long VOLUME_PRESS_TIMEOUT = 2000;
    private int volumePressCount = 0;
    private InactivityMonitor inactivityMonitor;
    private long lastVolumePressTime = 0;
    private BatteryAlert batteryAlert;
    private ScheduledMessage scheduledMessageSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTextView = findViewById(R.id.locationTextView);
        temperatureTextView = findViewById(R.id.temperatureTextView);
        weatherDescriptionTextView = findViewById(R.id.weatherDescriptionTextView);
        coordinatesTextView = findViewById(R.id.coordinatesTextView);
        earthquakeTextView = findViewById(R.id.earthquakeTextView);
        emergencyTextView = findViewById(R.id.emergencyTextView);
        weatherIconImageView = findViewById(R.id.weatherIconImageView);
        sosButtonLarge = findViewById(R.id.sosButtonLarge);
        sosButton = findViewById(R.id.sosButton);
        stopButton = findViewById(R.id.stopButton);
        cancelButton = findViewById(R.id.cancelButton);
        openMapsButton = findViewById(R.id.openMapsButton);
        scheduleMessageButton = findViewById(R.id.scheduleMessageButton);
        safeZoneButton = findViewById(R.id.safeZoneButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        weatherHelper = new WeatherHelper(this, this);
        earthquakeHelper = new EarthquakeHelper(this, this);
        sosManager = new SOSManager(this);
        scheduledMessageSender = new ScheduledMessage(this);


        executorService = Executors.newSingleThreadExecutor();

        checkLocationPermission();
        checkSmsPermission();

        sosButton.setOnClickListener(view -> {
            startCountdown();
            emergencyTextView.setVisibility(View.VISIBLE);
        });

        sosButtonLarge.setOnClickListener(view -> {
            callSOS();
        });

        stopButton.setOnClickListener(view -> {
            stopCountdown();
            sosManager.stopSOS();
            emergencyTextView.setText("SOS Action stopped!");
        });

        openMapsButton.setOnClickListener(view -> openGoogleMaps());

        safeZoneButton.setOnClickListener(view -> {
            if (currentLatitude != 0 && currentLongitude != 0) {
                safeZoneManager.addSafeZone("home", currentLatitude, currentLongitude, 200);
                Toast.makeText(MainActivity.this, "Safe zone has been set!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Location not available yet!", Toast.LENGTH_SHORT).show();
            }
        });

        safeZoneManager = new SafeZoneManager(this);

        inactivityMonitor = new InactivityMonitor(this);
        inactivityMonitor.startMonitoring();

        batteryAlert = new BatteryAlert();
        IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryAlert, batteryIntentFilter);

        Button scheduleMessageButton = findViewById(R.id.scheduleMessageButton);
        scheduleMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ScheduleMessageActivity.class));
            }
        });
        cancelButton.setOnClickListener(view -> scheduledMessageSender.cancelScheduledMessage());
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
        } else {
            getLocation();
        }
    }

    private void checkSmsPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_SMS);
        }
    }

    public void callSOS() {
        SOSManager.sendSOS2(this);
    }

    private void getLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
                coordinatesTextView.setText(String.format("📍 Lat: %.4f, Lng: %.4f", currentLatitude, currentLongitude));

                weatherHelper.fetchWeatherData(currentLatitude, currentLongitude);
                earthquakeHelper.getEarthquakeData();
            }
        });
    }

    @Override
    public void onWeatherDataReceived(String location, double temperature, String weatherDescription, int weatherIcon) {
        runOnUiThread(() -> {
            locationTextView.setText(location);
            temperatureTextView.setText(String.format("%.1f°C", temperature));
            weatherDescriptionTextView.setText(weatherDescription);
            weatherIconImageView.setImageResource(weatherIcon);
        });
    }

    @Override
    public void onEarthquakeDataReceived(double magnitude, String place) {
        runOnUiThread(() -> earthquakeTextView.setText(String.format("⚠️ Last earthquake: %.1f - %s", magnitude, place)));
    }

    private void startCountdown() {
        if (!isTimerRunning) {
            countDownTimer = new CountDownTimer(10000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    emergencyTextView.setText("Automatic emergency call in " + millisUntilFinished / 1000 + " seconds");
                }

                @Override
                public void onFinish() {
                    emergencyTextView.setText("Emergency call done!");
                    isTimerRunning = false;

                    String locationMessage = String.format("SOS! I need help! Location: Lat: %.4f, Lng: %.4f", currentLatitude, currentLongitude);
                    sendSOSInBackground(locationMessage);
                }
            }.start();
            isTimerRunning = true;
        }
    }

    private void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            emergencyTextView.setText("Action stopped.");
            isTimerRunning = false;
        }
    }

    private void sendSOSInBackground(final String message) {
        executorService.execute(() -> {
            try {
                double lat = currentLatitude;
                double lon = currentLongitude;
                sosManager.sendSOS(lat, lon);
            } catch (Exception e) {
                Log.e("SOS", "Failed to send SOS", e);
            }
        });
    }

    private void openGoogleMaps() {
        String uri = String.format("https://www.google.com/maps/search/?api=1&query=%f,%f", currentLatitude, currentLongitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Location permission not granted!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission not granted!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastVolumePressTime > VOLUME_PRESS_TIMEOUT) {
                volumePressCount = 0;
            }
            lastVolumePressTime = currentTime;
            volumePressCount++;

            if (volumePressCount >= MAX_VOLUME_PRESS_COUNT) {
                callSOS();
                volumePressCount = 0;
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        inactivityMonitor.stopMonitoring();
    }

    @Override
    protected void onResume() {
        super.onResume();
        inactivityMonitor.startMonitoring();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        inactivityMonitor.stopMonitoring();
        if (batteryAlert != null) {
            unregisterReceiver(batteryAlert);
        }
    }
}
