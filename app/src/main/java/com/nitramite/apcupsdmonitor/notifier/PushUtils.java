package com.nitramite.apcupsdmonitor.notifier;

import com.google.firebase.messaging.FirebaseMessaging;

@SuppressWarnings("HardCodedStringLiteral")
public class PushUtils {

    public static String TOPIC_UPDATE = "/topics/update_statuses";

    public static void subscribeToTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic);
    }

    public static void unsubscribeFromTopic(String topic) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
    }
}
