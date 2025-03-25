package com.example.safealertv2;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EarthquakeHelper {
    private static final String EARTHQUAKE_API_URL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson";

    public interface EarthquakeListener {
        void onEarthquakeDataReceived(double magnitude, String place);
    }

    private final Context context;
    private final EarthquakeListener listener;

    public EarthquakeHelper(Context context, EarthquakeListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void getEarthquakeData() {
        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, EARTHQUAKE_API_URL, null,
                response -> {

                        JSONArray features = response.optJSONArray("features");
                        if (features != null && features.length() > 0) {
                            JSONObject firstFeature = features.optJSONObject(0);
                            JSONObject properties = firstFeature != null ? firstFeature.optJSONObject("properties") : null;

                            double magnitude = properties != null ? properties.optDouble("mag", 0.0) : 0.0;
                            String place = properties != null ? properties.optString("place", "Unknown Location") : "Unknown Location";

                            listener.onEarthquakeDataReceived(magnitude, place);
                        }

                },
                error -> Log.e("EarthquakeAPI", "Error fetching earthquake data", error)
        );

        queue.add(jsonObjectRequest);
    }
}
