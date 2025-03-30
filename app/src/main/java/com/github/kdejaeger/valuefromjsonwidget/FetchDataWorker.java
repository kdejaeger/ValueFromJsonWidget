package com.github.kdejaeger.valuefromjsonwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchDataWorker extends Worker {

    private static final String TAG = "FetchDataWorker";
    public static final String PREFS_NAME = "widget_prefs";

    public FetchDataWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Load settings from SharedPreferences
        var prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String apiUrlString = prefs.getString("apiUrl", "https://api.airgradient.com/public/api/v1/world/locations/154803/measures/current");
        String jsonKey = prefs.getString("json_key", "pm02");

        try {
            var response = fetchData(apiUrlString);
            if (response != null) {
                String value = parseJson(response, jsonKey);

                // Save the fetched value to SharedPreferences
                SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putString("last_value", value);
                editor.apply();

                ValueFromJsonWidget.updateWidget(getApplicationContext(), value);
                return Result.success();
            } else {
                return Result.failure();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching data", e);
            return Result.failure();
        }
    }

    private String fetchData(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching data from " + urlString, e);
            return null;
        }
    }

    private String parseJson(String response, String jsonKey) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject.optString(jsonKey, "?");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
            return "Error";
        }
    }

}
