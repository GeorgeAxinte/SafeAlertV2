package com.example.safealertv2;

import android.content.Context;
import android.os.Handler;
import android.telephony.SmsManager;
import android.widget.Toast;

public class ScheduledMessage {
    private static ScheduledMessage instance;
    private final Context context;
    private final Handler handler;
    private Runnable messageRunnable;

    private ScheduledMessage(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new Handler();
    }

    public static ScheduledMessage getInstance(Context context) {
        if (instance == null) {
            instance = new ScheduledMessage(context);
        }
        return instance;
    }

    public void scheduleMessage(String phoneNumber, String message, long delayMillis) {
        cancelScheduledMessage();
        messageRunnable = () -> {
            sendMessage(phoneNumber, message);
            messageRunnable = null;
        };
        handler.postDelayed(messageRunnable, delayMillis);
        Toast.makeText(context, "Message scheduled. It will be sent automatically if not canceled.", Toast.LENGTH_SHORT).show();
    }

    public void cancelScheduledMessage() {
        if (messageRunnable != null) {
            handler.removeCallbacks(messageRunnable);
            messageRunnable = null;
            Toast.makeText(context, "Scheduled message canceled!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Toast.makeText(context, "Message sent: " + message, Toast.LENGTH_SHORT).show();
    }
}
