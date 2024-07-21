package com.nitramite.apcupsdmonitor;

import android.content.Context;
import android.util.Log;

// UPS Object
@SuppressWarnings("WeakerAccess")
public class UPS {

    // Logging
    private static final String TAG = UPS.class.getSimpleName();

    // Statuses
    public static final String UPS_NOT_REACHABLE = "0";
    public static final String UPS_REACHABLE = "1";

    // Variables | for connection
    public String UPS_ID = null;
    public String UPS_CONNECTION_TYPE = null;
    public String UPS_SERVER_ADDRESS = null;
    public String UPS_SERVER_PORT = null;
    public String UPS_SERVER_USERNAME = null;
    public String UPS_SERVER_PASSWORD = null;
    public String UPS_USE_PRIVATE_KEY_AUTH = null;
    public String UPS_PRIVATE_KEY_PASSWORD = null;
    public String UPS_PRIVATE_KEY_PATH = null;
    public String UPS_SERVER_SSH_STRICT_HOST_KEY_CHECKING = null;
    public String UPS_SERVER_STATUS_COMMAND = null;
    public String UPS_SERVER_EVENTS_LOCATION = null;
    public String UPS_SERVER_HOST_NAME = null;
    public String UPS_SERVER_HOST_FINGER_PRINT = null;
    public String UPS_SERVER_HOST_KEY = null;
    public String UPS_LOAD_EVENTS = null;
    public boolean UPS_IS_APC_NMC = false;
    public String UPS_NODE_ID = null; // IPM this is serial number of your device
    public boolean UPS_ENABLED = true; // cab disable ups momentarily if user wants so
    public boolean UPS_USE_HTTPS = true;
    public String UPS_DISPLAY_NAME;

    // Variables | status and event strings
    private String UPS_STATUS_STR = null;
    private String UPS_EVENT_STRING = null;
    private boolean UPS_IS_REACHABLE = true;

    // Variables | know names
    private String UPS_NAME = null;                     // UPSNAME
    private String START_TIME = null;                   // STARTTIME
    private String MODEL = null;                        // MODEL
    private String STATUS = null;                       // STATUS
    private String LINE_VOLTAGE = null;                 // LINEV
    private String LOAD_PERCENT = null;                 // LOADPCT
    private String BATTERY_CHARGE_LEVEL = null;         // BCHARGE
    private String BATTERY_TIME_LEFT = null;            // TIMELEFT
    private String MINIMUM_BATTERY_CHARGE = null;       // MBATTCHG
    private String MINIMUM_TIME_LEFT = null;            // MINTIMEL
    private String BATTERY_VOLTAGE = null;              // BATTV
    private String LAST_TRANSFER_REASON = null;         // LASTXFER
    private String LAST_SECONDS_ON_BATTERY = null;      // TONBATT
    private String LAST_TIME_DATE_ON_BATTERY = null;    // XOFFBATT
    private String BATTERY_DATE = null;                 // BATTDATE
    private String FIRMWARE = null;                     // FIRMWARE
    private String ITEMP = null;                        // TEMPERATURE (Int. Temp / Internal temp)


    // Constructor
    UPS() {
    }


    // ---------------------------------------------------------------------------------------------
    // Setters

    public void setUPS_NAME(String ups_name) {
        this.UPS_NAME = ups_name;
    }

    public void setSTART_TIME(String start_time) {
        this.START_TIME = start_time;
    }

    public void setMODEL(String model) {
        this.MODEL = model;
    }

    public void setSTATUS(String status) {
        this.STATUS = status;
    }

    public void setLINE_VOLTAGE(String line_voltage) {
        this.LINE_VOLTAGE = line_voltage;
    }

    public void setLOAD_PERCENT(String load_percent) {
        this.LOAD_PERCENT = load_percent;
    }

    public void setBATTERY_CHARGE_LEVEL(String battery_charge_level) {
        this.BATTERY_CHARGE_LEVEL = battery_charge_level;
    }

    public void setBATTERY_TIME_LEFT(String battery_time_left) {
        this.BATTERY_TIME_LEFT = battery_time_left;
    }

    public void setMINIMUM_BATTERY_CHARGE(String minimum_battery_charge) {
        this.MINIMUM_BATTERY_CHARGE = minimum_battery_charge;
    }

    public void setMINIMUM_TIME_LEFT(String minimum_time_left) {
        this.MINIMUM_TIME_LEFT = minimum_time_left;
    }

    public void setBATTERY_VOLTAGE(String BATTERY_VOLTAGE) {
        this.BATTERY_VOLTAGE = BATTERY_VOLTAGE;
    }

    public void setLAST_TRANSFER_REASON(String last_transfer_reason) {
        this.LAST_TRANSFER_REASON = last_transfer_reason;
    }

    public void setLAST_SECONDS_ON_BATTERY(String last_seconds_on_battery) {
        this.LAST_SECONDS_ON_BATTERY = last_seconds_on_battery;
    }

    public void setLAST_TIME_DATE_ON_BATTERY(String last_time_date_on_battery) {
        this.LAST_TIME_DATE_ON_BATTERY = last_time_date_on_battery;
    }

    public void setBATTERY_DATE(String BATTERY_DATE) {
        this.BATTERY_DATE = BATTERY_DATE;
    }

    public void setFIRMWARE(String FIRMWARE) {
        this.FIRMWARE = FIRMWARE;
    }

    public void setITEMP(String ITEMP) {
        this.ITEMP = ITEMP;
    }

    public void setUPS_STATUS_STR(String UPS_STATUS_STR) {
        this.UPS_STATUS_STR = UPS_STATUS_STR;
        this.statusParser();
    }

    public void setUPS_REACHABLE_STATUS(String UPS_REACHABLE_STATUS) {
        if (UPS_REACHABLE_STATUS != null) {
            this.UPS_IS_REACHABLE = UPS_REACHABLE_STATUS.equals(UPS_REACHABLE);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Getters

    public Boolean getUpsLoadEvents() {
        if (this.UPS_LOAD_EVENTS == null) {
            return false;
        } else {
            return this.UPS_LOAD_EVENTS.equals("1");
        }
    }

    public String getUPS_NAME() {
        return (this.UPS_NAME == null ? "N/A" : this.UPS_NAME);
    }

    public String getMODEL() {
        return (this.MODEL == null ? "N/A" : this.MODEL);
    }

    public String getSTATUS() {
        return (this.STATUS == null ? "N/A" : "UPS " + this.STATUS.replace(" ", ""));
    }

    public Boolean isOnline() {
        Log.i(TAG, this.getSTATUS());
        return this.getSTATUS().contains("ONLINE") || // APCUPSD
                this.getSTATUS().equals("UPS OL") || // UPSC
                this.getSTATUS().equals("UPS OLCHRG") || // UPSC
                this.getSTATUS().contains("OnLine,NoAlarmsPresent") || // APC NETWORK CARD
                this.getSTATUS().contains("Online-GreenMode"); // APC NETWORK CARD AP9630
    }

    public String getLineVoltageStr(Context context) {
        return (this.LINE_VOLTAGE == null ? "-" : this.LINE_VOLTAGE + " " + context.getString(R.string.ups_line_voltage));
    }

    public String getLineVoltageOnlyStr(Context context) {
        return (this.LINE_VOLTAGE == null ? "-" : this.LINE_VOLTAGE.replace(context.getString(R.string.ups_volts_line_voltage), "").replace("Volts", "").replace(" ", "") + "V");
    }


    public String getLoadPercentStr(Context context) {
        return (this.LOAD_PERCENT == null ? context.getString(R.string.ups_not_available) : LOAD_PERCENT + " " + context.getString(R.string.ups_of_load));
    }

    public Integer getLoadPercentInteger() {
        return (this.LOAD_PERCENT == null ? null : Integer.parseInt(LOAD_PERCENT.replace(" ", "").split("\\.")[0]));
    }


    public String getBatteryChargeLevelStr(Context context) {
        return (this.BATTERY_CHARGE_LEVEL == null ? context.getString(R.string.ups_na_charge_level) : BATTERY_CHARGE_LEVEL + " " + context.getString(R.string.ups_battery_charge));
    }

    public Integer getBatteryChargeLevelInteger() {
        return (this.BATTERY_CHARGE_LEVEL == null ? -1 : Integer.parseInt(BATTERY_CHARGE_LEVEL.replace(" ", "").split("\\.")[0]));
    }


    public String getBATTERY_TIME_LEFT(Context context) {
        return (this.BATTERY_TIME_LEFT == null ? context.getString(R.string.ups_battery_time_left_na) : context.getString(R.string.ups_battery_time_left) + ": " + this.BATTERY_TIME_LEFT);
    }


    public String getLastTransferReasonStr(Context context) {
        return (this.LAST_TRANSFER_REASON == null ? context.getString(R.string.ups_last_transfer_reason_na) : context.getString(R.string.ups_last) + ": " + this.LAST_TRANSFER_REASON);
    }


    public String getBatteryVoltageOnlyStr(Context context) {
        return (BATTERY_VOLTAGE == null ? "N/A" : BATTERY_VOLTAGE.replace("Volts", "").replace(" ", "") + "V");
    }

    public String getBATTERY_DATE(Context context) {
        return (BATTERY_DATE == null ? context.getString(R.string.ups_battery_date_na) : context.getString(R.string.ups_battery_date) + " " + BATTERY_DATE);
    }

    public String getFIRMWARE() {
        return (this.FIRMWARE == null ? "N/A" : this.FIRMWARE);
    }

    public String getSTART_TIME(Context context) {
        return (this.START_TIME == null ? context.getString(R.string.ups_start_time_na) : context.getString(R.string.ups_start_time) + ": " + this.START_TIME);
    }

    public String getLAST_SECONDS_ON_BATTERY() {
        return this.LAST_SECONDS_ON_BATTERY == null ? "N/A" : this.LAST_SECONDS_ON_BATTERY;
    }

    public String getLAST_TIME_DATE_ON_BATTERY() {
        return (this.LAST_TIME_DATE_ON_BATTERY == null ? "N/A" : this.LAST_TIME_DATE_ON_BATTERY);
    }

    public String getMINIMUM_BATTERY_CHARGE() {
        return (this.MINIMUM_BATTERY_CHARGE == null ? "N/A" : this.MINIMUM_BATTERY_CHARGE);
    }

    public String getMINIMUM_TIME_LEFT() {
        return (this.MINIMUM_TIME_LEFT == null ? "N/A" : this.MINIMUM_TIME_LEFT);
    }

    public String getITEMP() {
        return (this.ITEMP == null ? "N/A" : this.ITEMP);
    }

    public String getUPS_STATUS_STR() {
        return UPS_STATUS_STR;
    }

    public boolean upsIsReachable() {
        return this.UPS_IS_REACHABLE;
    }

    // ---------------------------------------------------------------------------------------------


    // Parse status string
    private void statusParser() {
        if (UPS_STATUS_STR == null) {
            return;
        }
        StatusParser.parseStatus(UPS_STATUS_STR, this);
    }


}