package com.nitramite.apcupsdmonitor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/* Provides mock data */
public class Mock {

    /**
     * APCUPSD Mock data
     */
    public static InputStream ApcupsdMockData() {
        String input_test =
            "APC      : 001,037,0940\n" +
            "DATE     : 2019-09-20 09:10:34 +0200\n" +
            "HOSTNAME : nebula\n" +
            "VERSION  : 3.14.14 (31 May 2016) debian\n" +
            "UPSNAME  : HomeUPS\n" +
            "CABLE    : USB Cable\n" +
            "DRIVER   : USB UPS Driver\n" +
            "UPSMODE  : Stand Alone\n" +
            "STARTTIME: 2019-09-04 21:52:07 +0200\n" +
            "MODEL    : Back-UPS XS 950U\n" +
            "STATUS   : ONLINE\n" +
            "LINEV    : 232.0 Volts\n" +
            "LOADPCT  : 12.0 Percent\n" +
            "BCHARGE  : 100.0 Percent\n" +
            "TIMELEFT : 48.0 Minutes\n" +
            "MBATTCHG : 5 Percent\n" +
            "MINTIMEL : 3 Minutes\n" +
            "MAXTIME  : 0 Seconds\n" +
            "SENSE    : Medium\n" +
            "LOTRANS  : 155.0 Volts\n" +
            "HITRANS  : 280.0 Volts\n" +
            "ALARMDEL : 30 Seconds\n" +
            "BATTV    : 13.4 Volts\n" +
            "LASTXFER : Unacceptable line voltage changes\n" +
            "NUMXFERS : 2\n" +
            "XONBATT  : 2019-09-12 09:32:19 +0200\n" +
            "TONBATT  : 0 Seconds\n" +
            "CUMONBATT: 117 Seconds\n" +
            "XOFFBATT : 2019-09-12 09:34:13 +0200\n" +
            "SELFTEST : NO\n" +
            "STATFLAG : 0x05000008\n" +
            "SERIALNO : 3B1739X28322\n" +
            "BATTDATE : 2017-10-01\n" +
            "NOMINV   : 230 Volts\n" +
            "NOMBATTV : 12.0 Volts\n" +
            "NOMPOWER : 480 Watts\n" +
            "FIRMWARE : 925.T2 .I USB FW:T2\n" +
            "END APC  : 2019-09-20 09:10:35 +0200\n";
        return new ByteArrayInputStream(input_test.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Synology NAS Mock data
     * command: upsc ups
     */
    public static InputStream SynologyMockData() {
        String input_test =
            "battery.charge: 100\n" +
            "battery.charge.low: 10\n" +
            "battery.charge.warning: 20\n" +
            "battery.mfr.date: CPS\n" +
            "battery.runtime: 1620\n" +
            "battery.runtime.low: 300\n" +
            "battery.type: PbAcid\n" +
            "battery.voltage: 16.0\n" +
            "battery.voltage.nominal: 24\n" +
            "device.mfr: CPS\n" +
            "device.model: CP900EPFCLCD\n" +
            "device.serial: 000000000000\n" +
            "device.type: ups\n" +
            "driver.name: usbhid-ups\n" +
            "driver.parameter.pollfreq: 30\n" +
            "driver.parameter.pollinterval: 5\n" +
            "driver.parameter.port: auto\n" +
            "driver.version: DSM6-2-25364-191230\n" +
            "driver.version.data: CyberPower HID 0.3\n" +
            "driver.version.internal: 0.38\n" +
            "input.transfer.high: 260\n" +
            "input.transfer.low: 170\n" +
            "input.voltage: 228.0\n" +
            "input.voltage.nominal: 230\n" +
            "output.voltage: 260.0\n" +
            "ups.beeper.status: enabled\n" +
            "ups.delay.shutdown: 20\n" +
            "ups.delay.start: 30\n" +
            "ups.load: 24\n" +
            "ups.mfr: CPS\n" +
            "ups.model: CP900EPFCLCD\n" +
            "ups.productid: 0501\n" +
            "ups.realpower.nominal: 540\n" +
            "ups.serial: 000000000000\n" +
            "ups.status: OL\n" +
            "ups.test.result: No test initiated\n" +
            "ups.timer.shutdown: -60\n" +
            "ups.timer.start: -60\n" +
            "ups.vendorid: 0764\n";
        return new ByteArrayInputStream(input_test.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * APC Network Card Mock data
     */
    public static InputStream APCNetworkCardMockData() {
        String input_test =
            "E000: Success\n" +
            "Status of UPS: On Line, No Alarms Present\n" +
            "Last Transfer: Due to software command or UPS's test control\n" +
            "Runtime Remaining: 2 hr 0 min\n" +
            "Battery State Of Charge: 100.0 %\n" +
            "Output Voltage: 229.3 VAC\n" +
            "Output Frequency: 50.0 Hz\n" +
            "Output Watts Percent: 8.0 %\n" +
            "Input Voltage: 226.0 VAC\n" +
            "Input Frequency: 50 Hz\n" +
            "Battery Voltage: 54.8 VDC\n" +
            "Internal Temperature: 16.2 C, 61.1 F\n" +
            "Self-Test Result: Passed\n" +
            "Self-Test Date: 10/25/2020\n" +
            "Calibration Result: Not Available\n" +
            "Calibration Date: Unknown\n";
        return new ByteArrayInputStream(input_test.getBytes(StandardCharsets.UTF_8));
    }


}