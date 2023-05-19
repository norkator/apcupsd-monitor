package com.nitramite.apcupsdmonitor;

import android.util.Log;

import androidx.core.util.Consumer;

import java.util.Arrays;
import java.util.List;

public class StatusParser {
    public static class ParseField {
        private final String label;
        private final Consumer<String> setter;
        private final List<String> trim;

        public ParseField(String label, Consumer<String> setter, String... trim) {
            this.label = label;
            this.setter = setter;
            this.trim = Arrays.asList(trim);
        }

        public String getLabel() {
            return label;
        }

        public List<String> getTrim() {
            return trim;
        }

        public Consumer<String> getSetter() {
            return setter;
        }

        public void parseLine(String line) {
            if (this.matchesLine(line)) {
                String cleaned = this.clean(line);
                this.setter.accept(cleaned);
            }
        }

        private boolean matchesLine(String line) {
            return line.startsWith(label);
        }

        protected String clean(final String line) {
            try {
                String[] split = line.split(": "); // See : and space, important
                String cleaned = split.length > 0 ? split[1] : "";
                for (String s : this.trim) {
                    cleaned = cleaned.replace(s, "");
                }
                return cleaned.trim();
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    //This overrides clean() to add parsing of the runtime field from seconds -> minutes
    public static class NUTRuntimeParseField extends ParseField {
        public NUTRuntimeParseField(String label, Consumer<String> setter, String... trim) {
            super(label, setter, trim);
        }

        @Override
        protected String clean(String line) {
            String cleaned = super.clean(line);
            try {
                int value = Integer.parseInt(cleaned);
                return value / 60 + " " + "minutes";
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), e.toString());
                return "N/A (Parsing failed)";
            }
        }
    }

    public static List<ParseField> getParseFields(UPS ups) {
        return Arrays.asList(
                //Name
                new ParseField("UPSNAME", ups::setUPS_NAME),
                new ParseField("device.mfr", ups::setUPS_NAME),
                new ParseField("Name", ups::setUPS_NAME, "Date"),

                //Status
                new ParseField("STATUS", ups::setSTATUS),
                new ParseField("ups.status", ups::setSTATUS),
                new ParseField("Status of UPS", ups::setSTATUS),

                //Model
                new ParseField("MODEL", ups::setMODEL),
                new ParseField("device.model", ups::setMODEL),
                new ParseField("Model", ups::setMODEL),

                //Line voltage
                new ParseField("LINEV", ups::setLINE_VOLTAGE),
                new ParseField("input.voltage:", ups::setLINE_VOLTAGE),
                new ParseField("Input Voltage", ups::setLINE_VOLTAGE, "VAC"),

                //Load percent
                new ParseField("LOADPCT", ups::setLOAD_PERCENT),
                new ParseField("ups.load", ups::setLOAD_PERCENT),
                new ParseField("Output Watts Percent", ups::setLOAD_PERCENT, "%"),

                //Battery charge level
                new ParseField("BCHARGE", ups::setBATTERY_CHARGE_LEVEL),
                new ParseField("battery.charge:", ups::setBATTERY_CHARGE_LEVEL),
                new ParseField("Battery State Of Charge", ups::setBATTERY_CHARGE_LEVEL, "%"),

                //Battery time left
                new ParseField("TIMELEFT", ups::setBATTERY_TIME_LEFT),
                new ParseField("Runtime Remaining", ups::setBATTERY_TIME_LEFT),
                new NUTRuntimeParseField("battery.runtime:", ups::setBATTERY_TIME_LEFT),

                //Firmware version
                new ParseField("FIRMWARE", ups::setFIRMWARE),
                new ParseField("driver.version:", ups::setFIRMWARE),
                new ParseField("Firmware Revision", ups::setFIRMWARE),

                //Temperature
                new ParseField("ITEMP", ups::setITEMP),
                new ParseField("Internal Temperature", ups::setITEMP),
                new ParseField("Battery Temperature", ups::setITEMP),

                //Battery voltage
                new ParseField("BATTV", ups::setBATTERY_VOLTAGE),
                new ParseField("battery.voltage:", ups::setBATTERY_VOLTAGE),
                new ParseField("Battery Voltage", ups::setBATTERY_VOLTAGE, "VDC"),

                //Last transfer reason
                new ParseField("LASTXFER", ups::setLAST_TRANSFER_REASON),
                new ParseField("Last Transfer", ups::setLAST_TRANSFER_REASON),

                //Minimum battery charge
                new ParseField("MBATTCHG", ups::setMINIMUM_BATTERY_CHARGE),

                //Minimum time left
                new ParseField("MINTIMEL", ups::setMINIMUM_TIME_LEFT),

                //Time on battery
                new ParseField("TONBATT", ups::setLAST_SECONDS_ON_BATTERY),

                //Off battery since
                new ParseField("XOFFBATT", ups::setLAST_TIME_DATE_ON_BATTERY),

                //Battery install date
                new ParseField("BATTDATE", ups::setBATTERY_DATE),

                //Start time
                new ParseField("STARTTIME", ups::setSTART_TIME)
        );
    }

    public static UPS parseStatus(String statusString, UPS ups) {
        String[] lines = statusString.split("\n");
        List<ParseField> parseFields = getParseFields(ups);
        for (String line : lines) {
            for (ParseField parseField : parseFields) {
                parseField.parseLine(line);
            }
        }
        return ups;
    }
}
