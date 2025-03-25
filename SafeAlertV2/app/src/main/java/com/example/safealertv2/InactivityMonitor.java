package com.example.safealertv2;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.util.Log;

public class InactivityMonitor implements SensorEventListener {

    private static final long INACTIVITY_TIMEOUT = 600000;
    private static final float MOVEMENT_THRESHOLD = 1.0f;
    private Context context;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private float lastAccelerometerValue = 0;
    private long lastActivityTime = 0;
    private CountDownTimer inactivityTimer;

    public InactivityMonitor(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
    }

    public void startMonitoring() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
        startInactivityTimer();
    }

    public void stopMonitoring() {
        sensorManager.unregisterListener(this);
        if (inactivityTimer != null) {
            inactivityTimer.cancel();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float acceleration = (float) Math.sqrt(Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2));
            if (acceleration > MOVEMENT_THRESHOLD) {
                lastActivityTime = System.currentTimeMillis();
                resetInactivityTimer();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void startInactivityTimer() {
        inactivityTimer = new CountDownTimer(INACTIVITY_TIMEOUT, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long timeElapsed = System.currentTimeMillis() - lastActivityTime;
                if (timeElapsed > INACTIVITY_TIMEOUT) {
                    sendSMS();
                }
            }

            @Override
            public void onFinish() {
            }
        };
        inactivityTimer.start();
    }

    private void resetInactivityTimer() {
        if (inactivityTimer != null) {
            inactivityTimer.cancel();
        }
        startInactivityTimer();
    }

    private void sendSMS() {
        String phoneNumber = "+40770203394";
        String message = "I have not been active for 10 minutes.";

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.d("InactivityMonitor", "SMS trimis");
        } catch (Exception e) {
            Log.e("InactivityMonitor", "Error", e);
        }
    }
}
