package com.nitramite.apcupsdmonitor;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

public class StatusService extends Service implements ConnectorInterface {

    // Logging
    private static final String TAG = "StatusService";

    // Variables
    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;
    private Boolean startAsForegroundService = false;
    private int counter;
    private Timer timer;
    private TimerTask timerTask;

    public StatusService() {
    }

    public StatusService(Context applicationContext) {
        super();
        Log.i(TAG, "StatusService class");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "onStartCommand()");

        Bundle extras = intent.getExtras();
        if (extras != null) {
            startAsForegroundService = extras.getBoolean("START_AS_FOREGROUND_SERVICE");
        }

        if (startAsForegroundService) {
            startServiceAsForeground();
        }

        databaseHelper = new DatabaseHelper(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        final int minutes = Integer.parseInt(sharedPreferences.getString(Constants.SP_SERVICE_REFRESH_INTERVAL, "60"));

        int counter = 0;
        startConnector(getBaseContext(), counter, minutes);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "serviceOnDestroy()");
        Intent broadcastIntent = new Intent("com.nitramite.apcupsdmonitor.RestartService");
        sendBroadcast(broadcastIntent);
        stopConnectorTask();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "serviceOnTaskRemoved()");

        Intent intent = new Intent(getApplicationContext(), StatusService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 5000, pendingIntent);
        super.onTaskRemoved(rootIntent);

        Intent broadcastIntent = new Intent("com.nitramite.apcupsdmonitor.RestartService");
        sendBroadcast(broadcastIntent);

        Log.i(TAG, "RestartUpsStatusService at onTaskRemoved()");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(TAG, "onLowMemory()");
    }


    @Override
    public void noUpsConfigured() {

    }

    @Override
    public void onAskToTrustKey(String upsId, String hostName, String hostFingerPrint, String hostKey) {

    }

    @Override
    public void onRefreshList() {

    }

    @Override
    public void onTaskCompleted() {
        try {
            if (databaseHelper.isAnyUpsDown()) {
                showNotification("Warning", "Power failure on one of the UPS devices.");
            }
        } catch (NullPointerException ignored) {
        }
    }

    @Override
    public void onMissingPreferences() {

    }

    @Override
    public void onConnectionError() {

    }

    @Override
    public void onCommandError(String errorStr) {

    }


    void startConnector(Context context, int counter, int minutes) {
        this.counter = counter;

        // set a new timer
        timer = new Timer();

        // initialize the timer task's job
        initializeConnectorTask(context);

        // schedule the timer, to wake up every X second
        timer.schedule(timerTask, 1000, (minutes * (60 * 1000)));
    }

    // it sets the timer to print the counter every x seconds
    private void initializeConnectorTask(Context context) {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.i("StatusService in timer", "in timer ++++ " + (counter++));
                new ConnectorTask(StatusService.this, context, TaskMode.MODE_ACTIVITY, null); // Update all
            }
        };
    }

    void stopConnectorTask() {
        // stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    /**
     * Service foreground problem solutions for android O and up
     * https://stackoverflow.com/questions/47531742/startforeground-fail-after-upgrade-to-android-8-1
     */
    private void startServiceAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";
            String channelName = "StatusService";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.mipmap.logo_round)
                    .setContentTitle("Updating UPS statuses in foreground.")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(2, notification);
        }
    }


    // Creates notification
    private void showNotification(final String title, final String statusText) {
        Intent intent = new Intent(StatusService.this, MainMenu.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(StatusService.this, 0, intent, 0);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
        NotificationCompat.Builder notification = new NotificationCompat.Builder(StatusService.this, NOTIFICATION_CHANNEL_ID);
        notification.setSmallIcon(R.drawable.ic_error_small);
        notification.setContentTitle(title);
        notification.setContentText(statusText);
        notification.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.logo));
        notification.setContentIntent(pendingIntent);
        notification.setAutoCancel(true);
        assert notificationManager != null;
        notificationManager.notify(1, notification.build());
    }


} // End of class