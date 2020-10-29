package com.nitramite.apcupsdmonitor;

public class Constants {

    // In app products
    static String IAP_ITEM_SKU_DONATE_MEDIUM = "iap_donate_medium";

    // Commands
    final static String STATUS_COMMAND_APCUPSD = "sudo apcaccess status";
    final static String STATUS_COMMAND_APCUPSD_NO_SUDO = "apcaccess status";
    final static String STATUS_COMMAND_SYNOLOGY = "upsc ups";
    final static String STATUS_COMMAND_APC_NETWORK_CARD = "detstatus -all";
    final static String EVENTS_LOCATION = "/var/log/apcupsd.events";

    // Shared preferences
    public final static String SP_USE_DARK_THEME = "SP_USE_DARK_THEME";

    final static String SP_STATUS_COMMAND = "SP_STATUS_COMMAND";
    final static String SP_EVENTS_LOCATION = "SP_EVENTS_LOCATION";
    final static String SP_EVENTS_COLORING = "SP_EVENTS_COLORING";
    final static String SP_UPS_EARLIER_STATUS = "SP_UPS_EARLIER_STATUS";
    final static String SP_ACTIVITY_RUNNING = "SP_ACTIVITY_RUNNING";

    final static String SP_SET_UPS_AS_ACTIVITY_TITLE = "SP_SET_UPS_AS_ACTIVITY_TITLE";
    final static String SP_SHOW_UPS_MODEL = "SP_SHOW_UPS_MODEL";
    final static String SP_SHOW_LINE_VOLTAGE = "SP_SHOW_LINE_VOLTAGE";
    final static String SP_SHOW_BATTERY_VOLTAGE = "SP_SHOW_BATTERY_VOLTAGE";
    final static String SP_SHOW_LOAD_PERCENTAGE = "SP_SHOW_LOAD_PERCENTAGE";
    final static String SP_SHOW_PERCENT_BATTERY_CHARGE = "SP_SHOW_PERCENT_BATTERY_CHARGE";
    final static String SP_SHOW_LAST_TRANSFER_REASON = "SP_SHOW_LAST_TRANSFER_REASON";
    final static String SP_SHOW_BATTERY_TIME_LEFT = "SP_SHOW_BATTERY_TIME_LEFT";
    final static String SP_SHOW_BATTERY_DATE = "SP_SHOW_BATTERY_DATE";
    final static String SP_SHOW_FIRMWARE_VERSION = "SP_SHOW_FIRMWARE_VERSION";
    final static String SP_SHOW_START_TIME = "SP_SHOW_START_TIME";
    final static String SP_SHOW_INTERNAL_TEMPERATURE = "SP_SHOW_INTERNAL_TEMPERATURE";

    final static String SP_STATISTICS_DATE_FORMAT = "SP_STATISTICS_DATE_FORMAT";
    final static String SP_STATISTICS_TIME_FORMAT = "SP_STATISTICS_TIME_FORMAT";

    final static String SP_AUTO_OPEN_UPS_ID = "SP_AUTO_OPEN_UPS_ID";

    public final static String SP_NOTIFICATIONS_ENABLED = "SP_NOTIFICATIONS_ENABLED";
    public final static String SP_UPDATE_INTERVAL = "SP_UPDATE_INTERVAL";
    public final static String SP_LAST_PUSH_UPDATE = "SP_LAST_PUSH_UPDATE";

}