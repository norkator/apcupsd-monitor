package com.nitramite.apcupsdmonitor;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class StatusService implements ConnectorInterface {

    // Logging
    private static final String TAG = StatusService.class.getSimpleName();

    // Variables
    private DatabaseHelper databaseHelper = null;
    private Context context;


    public Thread getUpdaterThread(Context context_) {
        return new Thread(() -> {
            Log.i(TAG, "Run getUpdaterThread");
            context = context_;
            databaseHelper = new DatabaseHelper(context);
            new ConnectorTask(this, context, TaskMode.MODE_SERVICE, null);
        });
    }

    @Override
    public void noUpsConfigured() {
        Log.i(TAG, "noUpsConfigured");
    }

    @Override
    public void onAskToTrustKey(String upsId, String hostName, String hostFingerPrint, String hostKey) {
        Log.i(TAG, "onAskToTrustKey");
    }

    @Override
    public void onRefreshList() {
    }

    @Override
    public void onTaskCompleted() {
        Log.i(TAG, "onTaskCompleted");
        try {
            if (databaseHelper.isAnyUpsDown()) {
                showNotification(context, context.getString(R.string.warning), context.getString(R.string.non_online_status_change_detected));
            }
        } catch (NullPointerException ignored) {
        }
    }

    @Override
    public void onTaskError(String exception) {
        Log.e(TAG, exception);
    }

    @Override
    public void onMissingPreferences() {
        Log.i(TAG, "onMissingPreferences");
    }

    @Override
    public void onConnectionError(final String upsId) {
        Log.e(TAG, "onConnectionError");
    }

    @Override
    public void onCommandError(String errorStr) {
        Log.e(TAG, "onCommandError " + errorStr);
    }

    // Creates notification
    private static void showNotification(Context context, final String title, final String statusText) {
        Intent intent = new Intent(context, MainMenu.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);
            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }
        // Init notification and show it
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        notification.setSmallIcon(R.drawable.ic_error_small);
        notification.setContentTitle(context.getString(R.string.app_name));

        String content = title + " - " + statusText;
        notification.setContentText(content);
        notification.setStyle(new NotificationCompat.BigTextStyle().bigText(content).setSummaryText(content));

        notification.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.logo));
        notification.setContentIntent(pendingIntent);
        notification.setAutoCancel(true);
        assert notificationManager != null;
        notificationManager.notify(1, notification.build());
    }


}