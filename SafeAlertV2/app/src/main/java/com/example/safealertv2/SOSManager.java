package com.example.safealertv2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.ArrayList;

import android.os.Handler;
import android.os.Looper;

public class SOSManager {
    private final Context context;
    private final FavoriteContactsHelper contactsHelper;

    public SOSManager(Context context) {
        this.context = context;
        this.contactsHelper = new FavoriteContactsHelper(context);
    }

    public void sendSOS(double lat, double lon) {
        SmsManager smsManager = SmsManager.getDefault();
        String message = (lat == 0 && lon == 0)
                ? "SOS! My location could not be obtained."
                : "SOS! My location: https://maps.google.com/?q=" + lat + "," + lon;

        List<String> favoriteContacts = contactsHelper.getFavoriteContacts();
        if (favoriteContacts.isEmpty()) {
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "No favorite contacts found!", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        for (String contact : favoriteContacts) {
            smsManager.sendTextMessage(contact, null, message, null, null);
            Log.d("SOS", "Message sent to: " + contact);
        }

        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, "SOS message sent to favorite contacts!", Toast.LENGTH_SHORT).show()
        );

        callFirstFavoriteContact(favoriteContacts.get(0));
    }

    public static void sendSOS2(Context context) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:112"));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callIntent);
    }

    public void callFirstFavoriteContact(String phoneNumber) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callIntent);
    }

    public void stopSOS() {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, "SOS Action stopped!", Toast.LENGTH_SHORT).show()
        );
    }
}
