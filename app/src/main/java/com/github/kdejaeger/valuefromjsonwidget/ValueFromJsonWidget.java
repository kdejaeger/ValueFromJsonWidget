package com.github.kdejaeger.valuefromjsonwidget;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import static androidx.work.PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS;

import static com.github.kdejaeger.valuefromjsonwidget.FetchDataWorker.PREFS_NAME;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class ValueFromJsonWidget extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.github.kdejaeger.valuefromjsonwidget.REFRESH";

    @Override
    public void onEnabled(Context context) {
        var periodicRequest = new PeriodicWorkRequest.Builder(FetchDataWorker.class, MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("PeriodicUpdate", ExistingPeriodicWorkPolicy.KEEP, periodicRequest);

        // Trigger an immediate fetch to populate the widget after it's enabled
        var workRequest = new OneTimeWorkRequest.Builder(FetchDataWorker.class).build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }

    @Override
    public void onDisabled(Context context) {
        // Cancel periodic updates when the widget is removed
        WorkManager.getInstance(context).cancelUniqueWork("PeriodicUpdate");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // Load the last known value from SharedPreferences
            var prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            views.setTextViewText(R.id.widget_text_small, prefs.getString("json_key", "pm02"));
            String lastValue = prefs.getString("last_value", null);
            if (lastValue != null) {
                views.setTextViewText(R.id.widget_text, lastValue);
            } else {
                views.setTextViewText(R.id.widget_text, "..."); // Show placeholder if no saved value
            }

            // Set up refresh icon to trigger manual refresh
            var refreshIntent = new Intent(context, ValueFromJsonWidget.class);
            refreshIntent.setAction(ACTION_REFRESH);
            var refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.refresh_icon, refreshPendingIntent);

            // Set up text click to open browser
            var clickUrlString = prefs.getString("clickUrl", "https://app.airgradient.com");
            var browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickUrlString));
            var browserPendingIntent = PendingIntent.getActivity(context, 0, browserIntent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_text, browserPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            // Manual refresh: trigger a one-time work request to fetch new data
            var workRequest = new OneTimeWorkRequest.Builder(FetchDataWorker.class).build();
            WorkManager.getInstance(context).enqueue(workRequest);
        }
    }

    public static void updateWidget(Context context, String value) {
        var prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        RemoteViews views = new RemoteViews(context.getPackageName(), com.github.kdejaeger.valuefromjsonwidget.R.layout.widget_layout);
        views.setTextViewText(R.id.widget_text_small, prefs.getString("json_key", "pm02"));
        views.setTextViewText(R.id.widget_text, value);

        // Reapply the click listeners:
        var refreshIntent = new Intent(context, ValueFromJsonWidget.class);
        refreshIntent.setAction(ACTION_REFRESH);
        var refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.refresh_icon, refreshPendingIntent);

        var clickUrlString = prefs.getString("clickUrl", "https://app.airgradient.com");
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickUrlString));
        var browserPendingIntent = PendingIntent.getActivity(context, 0, browserIntent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_text, browserPendingIntent);

        var appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, ValueFromJsonWidget.class);
        appWidgetManager.updateAppWidget(widget, views);
    }

}
