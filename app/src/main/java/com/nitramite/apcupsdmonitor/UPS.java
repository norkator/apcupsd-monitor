package com.nitramite.apcupsdmonitor;

import android.content.Context;
import android.util.Log;

// UPS Object
@SuppressWarnings("WeakerAccess")
public class UPS {

    private static final String TAG = "UPS";

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

    // Variables | status and event strings
    private String UPS_STATUS_STR = null;
    private String UPS_EVENT_STRING = null;

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
                this.getSTATUS().contains("OnLine,NoAlarmsPresent"); // APC NETWORK CARD
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

    // ---------------------------------------------------------------------------------------------


    // Parse status string
    private void statusParser() {
        if (UPS_STATUS_STR == null) {
            return;
        }
        String[] lines = UPS_STATUS_STR.split("\n");
        for (String line : lines) {

            /* APCUPSD */
            if (line.contains("UPSNAME")) {
                setUPS_NAME(this.getCleanLine(line, "UPSNAME"));
            } else if (line.contains("STARTTIME")) {
                setSTART_TIME(this.getCleanLine(line, "STARTTIME"));
            } else if (line.contains("MODEL")) {
                setMODEL(this.getCleanLine(line, "MODEL"));
            } else if (line.contains("STATUS")) {
                setSTATUS(this.getCleanLine(line, "STATUS"));
            } else if (line.contains("LINEV")) {
                setLINE_VOLTAGE(this.getCleanLine(line, "LINEV"));
            } else if (line.contains("LOADPCT")) {
                setLOAD_PERCENT(this.getCleanLine(line, "LOADPCT"));
            } else if (line.contains("BCHARGE")) {
                setBATTERY_CHARGE_LEVEL(this.getCleanLine(line, "BCHARGE"));
            } else if (line.contains("TIMELEFT")) {
                setBATTERY_TIME_LEFT(this.getCleanLine(line, "TIMELEFT"));
            } else if (line.contains("MBATTCHG")) {
                setMINIMUM_BATTERY_CHARGE(this.getCleanLine(line, "MBATTCHG"));
            } else if (line.contains("MINTIMEL")) {
                setMINIMUM_TIME_LEFT(this.getCleanLine(line, "MINTIMEL"));
            } else if (line.contains("BATTV") && !line.contains("NOMBATTV")) {
                setBATTERY_VOLTAGE(this.getCleanLine(line, "BATTV"));
            } else if (line.contains("LASTXFER")) {
                setLAST_TRANSFER_REASON(this.getCleanLine(line, "LASTXFER"));
            } else if (line.contains("TONBATT")) {
                setLAST_SECONDS_ON_BATTERY(this.getCleanLine(line, "TONBATT"));
            } else if (line.contains("XOFFBATT")) {
                setLAST_TIME_DATE_ON_BATTERY(this.getCleanLine(line, "XOFFBATT"));
            } else if (line.contains("BATTDATE")) {
                setBATTERY_DATE(this.getCleanLine(line, "BATTDATE"));
            } else if (line.contains("FIRMWARE")) {
                setFIRMWARE(this.getCleanLine(line, "FIRMWARE"));
            } else if (line.contains("ITEMP")) {
                setITEMP(this.getCleanLine(line, "ITEMP"));
            }

            /* SYNOLOGY UPSC UPS */
            else if (line.contains("battery.charge") && !line.contains("battery.charge.")) {
                setBATTERY_CHARGE_LEVEL(this.getCleanLine(line, "battery.charge"));
            } else if (line.contains("battery.voltage") && !line.contains("battery.voltage.")) {
                setBATTERY_VOLTAGE(this.getCleanLine(line, "battery.voltage"));
            } else if (line.contains("device.mfr")) {
                setUPS_NAME(this.getCleanLine(line, "device.mfr"));
            } else if (line.contains("device.model")) {
                setMODEL(this.getCleanLine(line, "device.model"));
            } else if (line.contains("ups.firmware")) {
                setFIRMWARE(this.getCleanLine(line, "ups.firmware"));
            } else if (line.contains("ups.status")) {
                setSTATUS(this.getCleanLine(line, "ups.status"));
            }

            /* APC NETWORK MANAGEMENT CARD */
            else if (line.contains("Status of UPS")) {
                setSTATUS(this.getCleanLine(line, ""));
                setUPS_NAME("APC NMC AOS");
            } else if (line.contains("Last Transfer")) {
                setLAST_TRANSFER_REASON(this.getCleanLine(line, ""));
            } else if (line.contains("Runtime Remaining")) {
                setBATTERY_TIME_LEFT(this.getCleanLine(line, ""));
            } else if (line.contains("Battery State Of Charge")) {
                setBATTERY_CHARGE_LEVEL(this.getCleanLine(line, "").replace("%", ""));
            } else if (line.contains("Output Watts Percent")) {
                setLOAD_PERCENT(this.getCleanLine(line, "").replace("%", ""));
            } else if (line.contains("Input Voltage")) {
                setLINE_VOLTAGE(this.getCleanLine(line, "").replace("VAC", ""));
            } else if (line.contains("Battery Voltage")) {
                setBATTERY_VOLTAGE(this.getCleanLine(line, "").replace("VDC", ""));
            } else if (line.contains("Internal Temperature")) {
                setITEMP(this.getCleanLine(line, ""));
            }

        }
    }


    // Helper to clean results
    private String getCleanLine(final String line, final String containing) {
        String[] split = line.split(": "); // See : and space, important
        return split.length > 0 ? split[1] : "";
    }


} // End of class