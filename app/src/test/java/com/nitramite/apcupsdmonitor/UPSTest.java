package com.nitramite.apcupsdmonitor;


import android.content.Context;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UPSTest extends TestCase {
    @org.mockito.Mock
    private Context mockContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testApcUpsdStatusParse() {
        when(this.mockContext.getString(R.string.ups_start_time)).thenReturn("Start time");
        when(this.mockContext.getString(R.string.ups_volts_line_voltage)).thenReturn("Volts line voltage");
        when(this.mockContext.getString(R.string.ups_of_load)).thenReturn("of load");
        when(this.mockContext.getString(R.string.ups_battery_charge)).thenReturn("battery charge");
        when(this.mockContext.getString(R.string.ups_battery_time_left)).thenReturn("Battery time left");
        when(this.mockContext.getString(R.string.ups_last)).thenReturn("Last");
        when(this.mockContext.getString(R.string.ups_battery_date)).thenReturn("Battery date");

        UPS ups = new UPS();
        ups.setUPS_STATUS_STR(Mock.APCUPSD_MOCK_DATA);
        assertEquals("APCUPS", ups.getUPS_NAME());
        assertEquals("Start time: 2021-02-16 15:06:17 +0100", ups.getSTART_TIME(this.mockContext));
        assertEquals("Smart-UPS 1000", ups.getMODEL());
        assertEquals("UPS ONLINE", ups.getSTATUS());
        assertEquals("233.0V", ups.getLineVoltageOnlyStr(this.mockContext));
        assertEquals("18.0 Percent of load", ups.getLoadPercentStr(this.mockContext));
        assertEquals("100.0 Percent battery charge", ups.getBatteryChargeLevelStr(this.mockContext));
        assertEquals("Battery time left: 47.0 Minutes", ups.getBATTERY_TIME_LEFT(this.mockContext));
        assertEquals("40 Percent", ups.getMINIMUM_BATTERY_CHARGE());
        assertEquals("5 Minutes", ups.getMINIMUM_TIME_LEFT());
        assertEquals("27.0V", ups.getBatteryVoltageOnlyStr(this.mockContext));
        assertEquals("Last: Automatic or explicit self test", ups.getLastTransferReasonStr(this.mockContext));
        assertEquals("0 Seconds", ups.getLAST_SECONDS_ON_BATTERY());
        assertEquals("N/A", ups.getLAST_TIME_DATE_ON_BATTERY());
        assertEquals("Battery date 08/15/2014", ups.getBATTERY_DATE(this.mockContext));
        assertEquals("UPS 08.8 (ID18)", ups.getFIRMWARE());
        assertEquals("30.0 C", ups.getITEMP());
    }

    @Test
    public void testNetworkUpsToolsStatusParse() {
        when(this.mockContext.getString(R.string.ups_battery_charge)).thenReturn("battery charge");
        when(this.mockContext.getString(R.string.ups_volts_line_voltage)).thenReturn("Volts line voltage");
        when(this.mockContext.getString(R.string.ups_of_load)).thenReturn("of load");
        when(this.mockContext.getString(R.string.ups_battery_time_left)).thenReturn("Battery time left");

        UPS ups = new UPS();
        ups.setUPS_STATUS_STR(Mock.NUT_MOCK_DATA);
        assertEquals("CPS", ups.getUPS_NAME());
        assertEquals("100 battery charge", ups.getBatteryChargeLevelStr(this.mockContext));
        assertEquals("16.0V", ups.getBatteryVoltageOnlyStr(this.mockContext));
        assertEquals("CP900EPFCLCD", ups.getMODEL());
        assertEquals("UPS OL", ups.getSTATUS());
        assertEquals("DSM6-2-25364-191230", ups.getFIRMWARE());
        assertEquals("228.0V", ups.getLineVoltageOnlyStr(this.mockContext));
        assertEquals("24 of load", ups.getLoadPercentStr(this.mockContext));
        assertEquals("Battery time left: 27 minutes", ups.getBATTERY_TIME_LEFT(this.mockContext));
    }

    @Test
    public void testApcNMCStatusParse() {
        when(this.mockContext.getString(R.string.ups_last)).thenReturn("Last");
        when(this.mockContext.getString(R.string.ups_battery_time_left)).thenReturn("Battery time left");
        when(this.mockContext.getString(R.string.ups_battery_charge)).thenReturn("battery charge");
        when(this.mockContext.getString(R.string.ups_of_load)).thenReturn("of load");
        when(this.mockContext.getString(R.string.ups_volts_line_voltage)).thenReturn("Volts line voltage");

        UPS ups = new UPS();
        ups.setUPS_STATUS_STR(Mock.APC_NMC_MOCK_DATA);
        assertEquals("N/A", ups.getUPS_NAME());
        assertEquals("UPS OnLine,NoAlarmsPresent", ups.getSTATUS());
        assertEquals("Last: Due to software command or UPS's test control", ups.getLastTransferReasonStr(this.mockContext));
        assertEquals("Battery time left: 2 hr 0 min", ups.getBATTERY_TIME_LEFT(this.mockContext));
        assertEquals("100.0 battery charge", ups.getBatteryChargeLevelStr(this.mockContext));
        assertEquals("8.0 of load", ups.getLoadPercentStr(this.mockContext));
        assertEquals("226.0V", ups.getLineVoltageOnlyStr(this.mockContext));
        assertEquals("54.8V", ups.getBatteryVoltageOnlyStr(this.mockContext));
        assertEquals("16.2 C, 61.1 F", ups.getITEMP());
    }

    @Test
    public void testApcNMCFullStatusParse() {
        when(this.mockContext.getString(R.string.ups_last)).thenReturn("Last");
        when(this.mockContext.getString(R.string.ups_battery_time_left)).thenReturn("Battery time left");
        when(this.mockContext.getString(R.string.ups_battery_charge)).thenReturn("battery charge");
        when(this.mockContext.getString(R.string.ups_of_load)).thenReturn("of load");
        when(this.mockContext.getString(R.string.ups_volts_line_voltage)).thenReturn("Volts line voltage");

        UPS ups = new UPS();
        ups.setUPS_STATUS_STR(Mock.APC_NMC_MOCK_DATA_FULL);
        assertEquals("apc9B4234", ups.getUPS_NAME());
        assertEquals("UPS OnLine,NoAlarmsPresent", ups.getSTATUS());
        assertEquals("Last: Due to software command or UPS's test control", ups.getLastTransferReasonStr(this.mockContext));
        assertEquals("Battery time left: 2 hr 0 min", ups.getBATTERY_TIME_LEFT(this.mockContext));
        assertEquals("100.0 battery charge", ups.getBatteryChargeLevelStr(this.mockContext));
        assertEquals("8.0 of load", ups.getLoadPercentStr(this.mockContext));
        assertEquals("226.0V", ups.getLineVoltageOnlyStr(this.mockContext));
        assertEquals("54.8V", ups.getBatteryVoltageOnlyStr(this.mockContext));
        assertEquals("16.2 C, 61.1 F", ups.getITEMP());
    }
}