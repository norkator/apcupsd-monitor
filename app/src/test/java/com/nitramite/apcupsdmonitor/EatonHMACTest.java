package com.nitramite.apcupsdmonitor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EatonHMACTest {

    @Test
    public void eatonHMAC() {
        assertEquals("9abe937b97c3a3b258e0c8b5300db06a018446e7", EatonHMAC.GetEatonHMAC("sample", "sample-data"));
    }

    @Test
    public void eatonHexToBin() {
        assertEquals("apcupsdmonitor", EatonHMAC.hexToBin("617063757073646d6f6e69746f72"));
    }

}
