package com.nitramite.apcupsdmonitor;

class OutageEventObj {


    // Variables
    private String day = null;
    private Integer outageSeconds = 0;


    // Constructor
    OutageEventObj(String d, Integer os) {
        this.day = d;
        this.outageSeconds = os;
    }


    String getDay() {
        return day;
    }

    String getYearMonthPart() {
        return day.substring(0, 7);
    }

    Integer getOutageSeconds() {
        return outageSeconds;
    }


} // End of class
