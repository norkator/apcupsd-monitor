package com.nitramite.apcupsdmonitor;

public interface ConnectorInterface {

    void noUpsConfigured();

    void onAskToTrustKey(final String upsId, final String hostName, final String hostFingerPrint, final String hostKey);

    void onRefreshList(); // For list view update between ups devices

    void onTaskCompleted(); // Dismiss loading indicator

    void onMissingPreferences(); // Navigate to preferences page

    void onConnectionError(final String upsId);

    void onCommandError(final String errorStr);

} // End of interface()