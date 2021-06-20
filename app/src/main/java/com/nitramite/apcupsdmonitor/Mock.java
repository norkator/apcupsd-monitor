package com.nitramite.apcupsdmonitor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/* Provides mock data */
public class Mock {

    public static final String APCUPSD_MOCK_DATA = "APC : 001,044,1035\n" +
            "DATE : 2021-02-16 19:18:03 +0100\n" +
            "HOSTNAME : proxmox\n" +
            "VERSION : 3.14.14 (31 May 2016) debian\n" +
            "UPSNAME : APCUPS\n" +
            "CABLE : Ethernet Link\n" +
            "DRIVER : SNMP UPS Driver\n" +
            "UPSMODE : Stand Alone\n" +
            "STARTTIME: 2021-02-16 15:06:17 +0100\n" +
            "MODEL : Smart-UPS 1000\n" +
            "STATUS : ONLINE\n" +
            "LINEV : 233.0 Volts\n" +
            "LOADPCT : 18.0 Percent\n" +
            "BCHARGE : 100.0 Percent\n" +
            "TIMELEFT : 47.0 Minutes\n" +
            "MBATTCHG : 40 Percent\n" +
            "MINTIMEL : 5 Minutes\n" +
            "MAXTIME : 0 Seconds\n" +
            "MAXLINEV : 233.0 Volts\n" +
            "MINLINEV : 230.0 Volts\n" +
            "OUTPUTV : 233.0 Volts\n" +
            "SENSE : High\n" +
            "DWAKE : 1000 Seconds\n" +
            "DSHUTD : 480 Seconds\n" +
            "DLOWBATT : 2 Minutes\n" +
            "LOTRANS : 207.0 Volts\n" +
            "HITRANS : 253.0 Volts\n" +
            "ITEMP : 30.0 C\n" +
            "ALARMDEL : 30 Seconds\n" +
            "BATTV : 27.0 Volts\n" +
            "LINEFREQ : 50.0 Hz\n" +
            "LASTXFER : Automatic or explicit self test\n" +
            "NUMXFERS : 0\n" +
            "TONBATT : 0 Seconds\n" +
            "CUMONBATT: 0 Seconds\n" +
            "XOFFBATT : N/A\n" +
            "SELFTEST : NG\n" +
            "STESTI : OFF\n" +
            "STATFLAG : 0x05000008\n" +
            "MANDATE : 11/06/2013\n" +
            "SERIALNO : AS1345222226\n" +
            "BATTDATE : 08/15/2014\n" +
            "NOMOUTV : 230 Volts\n" +
            "FIRMWARE : UPS 08.8 (ID18)\n" +
            "END APC : 2021-02-16 19:18:32 +0100\n";
    public static final String NUT_MOCK_DATA =
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
    public static final String APC_NMC_MOCK_DATA = "E000: Success\n" +
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
    public static final String APC_NMC_9630_MOCK_DATA = "E000: Success\n" +
            "Status of UPS: Online - Green Mode\n" +
            "Last Transfer: None\n" +
            "Input Status: Acceptable\n" +
            "Next Battery Replacement Date: 11/11/2026\n" +
            "Runtime Remaining: 1 hr 58 min 0 sec\n" +
            "Battery State Of Charge: 100.0 %\n" +
            "Output Voltage: 247.6 VAC\n" +
            "Output Frequency: 50.1 Hz\n" +
            "Output Watts Percent: 9.1 %\n" +
            "Output VA Percent: 10.4 %\n" +
            "Output Current: 0.66 A\n" +
            "Output Efficiency: 90.0 %\n" +
            "Output Energy: 4631.618 kWh\n" +
            "Input Voltage: 247.6 VAC\n" +
            "Input Frequency: 50.1 Hz\n" +
            "Battery Voltage: 27.3 VDC\n" +
            "Battery Temperature: 20.7 C, 69.2 F\n" +
            "Self-Test Result: Refused via internal operation due to internal fault o\n" +
            "Self-Test Date: Unknown\n" +
            "Calibration Result: Unknown\n" +
            "Calibration Date: Unknown";

    /**
     * APCUPSD Mock data
     */
    public static InputStream ApcupsdMockData() {
        /*
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
             */
        return new ByteArrayInputStream(APCUPSD_MOCK_DATA.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Synology NAS Mock data
     * command: upsc ups
     */
    public static InputStream SynologyMockData() {
        return new ByteArrayInputStream(NUT_MOCK_DATA.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * APC Network Card Mock data
     */
    public static InputStream APCNetworkCardMockData() {
        return new ByteArrayInputStream(APC_NMC_MOCK_DATA.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * APC Network Card Mock data #2
     */
    public static InputStream APCNetworkCardMockDataAP9630() {
        return new ByteArrayInputStream(APC_NMC_9630_MOCK_DATA.getBytes(StandardCharsets.UTF_8));
    }


}
