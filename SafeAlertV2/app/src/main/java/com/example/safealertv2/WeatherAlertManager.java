package com.example.safealertv2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherAlertManager {
    private static final String API_KEY = "163c1a932040679442d27435a41cc40b";
    private static final String WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=" + API_KEY + "&units=metric&lang=en";
    private final Context context;
    private final FavoriteContactsHelper contactsHelper;
    private final SmsManager smsManager;
    private final ExecutorService executorService;

    public WeatherAlertManager(Context context, FavoriteContactsHelper contactsHelper) {
        this.context = context;
        this.contactsHelper = contactsHelper;
        this.smsManager = SmsManager.getDefault();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void checkWeatherAndNotify(double latitude, double longitude) {
        executorService.execute(() -> {
            try {
                URL url = new URL(String.format(WEATHER_URL, latitude, longitude));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                parseWeatherResponse(response.toString());
            } catch (Exception e) {
                Log.e("WeatherAlert", "Error fetching weather data", e);
            }
        });
    }

    private void parseWeatherResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject weather = jsonObject.getJSONArray("weather").getJSONObject(0);
            String condition = weather.getString("main");

            if (isSevereWeather(condition)) {
                sendAlert(condition);
            }
        } catch (Exception e) {
            Log.e("WeatherAlert", "Error parsing weather data", e);
        }
    }

    private boolean isSevereWeather(String condition) {
        return condition.equalsIgnoreCase("Thunderstorm") ||
                condition.equalsIgnoreCase("Tornado") ||
                condition.equalsIgnoreCase("Extreme") ||
                condition.equalsIgnoreCase("Snow") ||
                condition.equalsIgnoreCase("Flood") ||
                condition.equalsIgnoreCase("Fire") ||
                condition.equalsIgnoreCase("Earthquake");
    }

    private void sendAlert(String condition) {
        String message = "ALERT: Severe weather detected! Condition: " + condition + ". Stay safe!";

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
    }
}