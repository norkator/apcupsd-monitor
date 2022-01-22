package com.nitramite.apcupsdmonitor;

public class ConnectionType {

    // Supported connection types
    public static final String UPS_CONNECTION_TYPE_NA = "-1"; // NA (Not Available)
    public static final String UPS_CONNECTION_TYPE_SSH = "0"; // SSH (Secure Shell)
    public static final String UPS_CONNECTION_TYPE_NIS = "1"; // APCUPSD (Network Information Server)
    public static final String UPS_CONNECTION_TYPE_IPM = "2"; // Eaton IPM (Intelligent Power Manager)
    public static final String UPS_CONNECTION_TYPE_NUT = "3"; // NUT server (Network UPS Tools)

}
