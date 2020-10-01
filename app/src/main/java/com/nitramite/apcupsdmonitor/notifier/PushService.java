package com.nitramite.apcupsdmonitor.notifier;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nitramite.apcupsdmonitor.Constants;
import com.nitramite.apcupsdmonitor.StatusService;

import org.joda.time.Duration;
import org.joda.time.Interval;

import java.util.Date;
import java.util.Objects;

@SuppressWarnings("HardCodedStringLiteral")
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
public class PushService extends FirebaseMessagingService {

    private String TAG = this.getClass().getSimpleName();


    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.i(TAG, "onMessageReceived " + remoteMessage.getFrom());
        if (Objects.equals(remoteMessage.getFrom(), PushUtils.TOPIC_UPDATE)) {
            try {
                Log.i(TAG, "Push Update triggered");
                if (permittedToUpdate()) {
                    Log.i(TAG, "Update permitted");
                    startCheck();
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
        super.onMessageReceived(remoteMessage);
    }

    /**
     * Checking if user-defined interval is over. If it is, returning true and saving current time
     *
     * @return boolean Is the interval over?
     */
    private boolean permittedToUpdate() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        double parcelInterval = Double.parseDouble(sharedPreferences.getString(Constants.SP_UPDATE_INTERVAL, "1.0")
                .replace(",", "."));
        Date currentDateObject = new Date();
        Date lastUpdate = new Date(sharedPreferences.getLong(Constants.SP_LAST_PUSH_UPDATE, currentDateObject.getTime()));
        Duration period = new Interval(lastUpdate.getTime(), currentDateObject.getTime()).toDuration();
        long intervalAsMinutes = (long) (parcelInterval * 60);
        boolean permitted = period.getStandardMinutes() >= intervalAsMinutes;
        if (permitted)
            sharedPreferences.edit().putLong(Constants.SP_LAST_PUSH_UPDATE, currentDateObject.getTime()).apply();
        return permitted;
    }

    /**
     * Doing so we defend the Google's Doze, because it doesn't allow anyone to start a service here
     */
    public void startCheck() {
        StatusService statusService = new StatusService();
        statusService.getUpdaterThread(this).start();
    }


}
