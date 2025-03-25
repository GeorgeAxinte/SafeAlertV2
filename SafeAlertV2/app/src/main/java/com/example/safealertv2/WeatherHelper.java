package com.example.safealertv2;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherHelper {
    private static final String API_KEY = "163c1a932040679442d27435a41cc40b";
    private static final String WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=" + API_KEY + "&units=metric&lang=en";

    public interface WeatherListener {
        void onWeatherDataReceived(String location, double temperature, String weatherDescription, int weatherIcon);
    }

    private final Context context;
    private final WeatherListener listener;

    public WeatherHelper(Context context, WeatherListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void fetchWeatherData(double latitude, double longitude) {
        String url = String.format(WEATHER_URL, latitude, longitude);
        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {

                        String locationName = response.optString("name", "Unknown Location");
                        JSONObject main = response.optJSONObject("main");
                        double temperature = main != null ? main.optDouble("temp", 0) : 0;

                        JSONArray weatherArray = response.optJSONArray("weather");
                        String weatherDescription = weatherArray != null && weatherArray.length() > 0
                                ? weatherArray.optJSONObject(0).optString("description", "No description available")
                                : "No description available";

                        int weatherIcon = R.drawable.ic_sun;
                        listener.onWeatherDataReceived(locationName, temperature, weatherDescription, weatherIcon);

                },
                error -> Log.e("WeatherAPI", "Error fetching weather data", error)
        );

        queue.add(jsonObjectRequest);
    }
}
