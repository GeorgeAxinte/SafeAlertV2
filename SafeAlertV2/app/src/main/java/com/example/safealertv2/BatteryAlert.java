package com.example.safealertv2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.BatteryManager;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class BatteryAlert extends BroadcastReceiver {
    private static final String TAG = "BatteryAlert";
    private static final int TARGET_LOW_LEVEL = 10;
    private static final int TARGET_CRITICAL_LEVEL = 2;

    private boolean smsSentLow = false;
    private boolean smsSentCritical = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int batteryPct = (int) ((level / (float) scale) * 100);
        Log.d(TAG, "Battery level: " + batteryPct + "%");

        if (batteryPct <= TARGET_LOW_LEVEL && !smsSentLow) {
            smsSentLow = true;
            String toastMsg = "Battery low (" + batteryPct + "%)! Please turn on battery saver.";
            showToast(context, toastMsg);
            Log.d(TAG, "Sending SMS at " + batteryPct + "%: " + toastMsg);
            sendLocationSMS(context, batteryPct, false);
        } else if (batteryPct == TARGET_CRITICAL_LEVEL && !smsSentCritical) {
            smsSentCritical = true;
            String toastMsg = "Critical battery (" + batteryPct + "%)! Sending final location.";
            showToast(context, toastMsg);
            Log.d(TAG, "Sending SMS at " + batteryPct + "%: " + toastMsg);
            sendLocationSMS(context, batteryPct, true);
        } else if (batteryPct > TARGET_LOW_LEVEL && smsSentLow) {
            smsSentLow = false;
            Log.d(TAG, "Reset smsSentLow");
        }
    }

    private void sendLocationSMS(Context context, int batteryPct, boolean isCritical) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                String message;
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();

                    if (isCritical) {
                        message = "Critical alert: Battery " + batteryPct + "%! Final location: https://maps.google.com/?q=" + lat + "," + lon;
                    } else {
                        message = "Alert: Battery " + batteryPct + "%! My location: https://maps.google.com/?q=" + lat + "," + lon;
                    }
                    Log.d(TAG, "Location obtained: " + lat + ", " + lon);
                } else {
                    if (isCritical) {
                        message = "Critical alert: Battery " + batteryPct + "%! Could not obtain final location.";
                    } else {
                        message = "Alert: Battery " + batteryPct + "%! Location unavailable.";
                    }
                    Log.d(TAG, "Location unavailable.");
                }
                sendSMS(context, message);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error obtaining location: " + e.getMessage());
                showToast(context, "Error obtaining location.");
                String message;
                if (isCritical) {
                    message = "Critical alert: Battery " + batteryPct + "%! Could not obtain final location.";
                } else {
                    message = "Alert: Battery " + batteryPct + "%! Location unavailable.";
                }
                sendSMS(context, message);
            });
        } else {
            showToast(context, "Permission to send SMS is not granted!");
            Log.e(TAG, "Permission SEND_SMS is not granted.");
        }
    }

    private void sendSMS(Context context, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            ArrayList<String> favoriteContacts = getFavoriteContacts(context);
            if (favoriteContacts.isEmpty()) {
                Log.d(TAG, "No favorite contacts found in the phonebook.");
                showToast(context, "No favorite contacts found in the phonebook.");
                return;
            }
            for (String contact : favoriteContacts) {
                Log.d(TAG, "Sending SMS to: " + contact + " with message: " + message);
                smsManager.sendMultipartTextMessage(contact, null, parts, null, null);
            }
            showToast(context, "SMS sent to favorite contacts.");
        } catch (SecurityException e) {
            showToast(context, "SMS permission denied!");
            Log.e(TAG, "Error sending SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ArrayList<String> getFavoriteContacts(Context context) {
        ArrayList<String> favorites = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission READ_CONTACTS is not granted.");
            return favorites;
        }
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                ContactsContract.Contacts.STARRED + "=?",
                new String[]{"1"},
                null
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                @SuppressLint("Range") String hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                if (hasPhone != null && hasPhone.equals("1")) {
                    Cursor phoneCursor = context.getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{contactId},
                            null
                    );
                    if (phoneCursor != null) {
                        while (phoneCursor.moveToNext()) {
                            @SuppressLint("Range") String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            favorites.add(phoneNumber);
                        }
                        phoneCursor.close();
                    }
                }
            }
            cursor.close();
        }
        return favorites;
    }

    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
