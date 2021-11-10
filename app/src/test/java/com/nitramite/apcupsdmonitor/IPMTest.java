package com.nitramite.apcupsdmonitor;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.io.IOException;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(BlockJUnit4ClassRunner.class)
public class IPMTest extends TestCase {


    MockWebServer server = new MockWebServer();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                System.out.println(request.getPath());
                switch (request.getPath()) {
                    case "/server/user_srv.js?action=queryLoginChallenge":
                        return new MockResponse().setResponseCode(200).setBody("{\"challenge\":\"4a501e785ee3d084bddc531352bead1cb2906765\"}");
                    case "/server/user_srv.js?action=loginUser":
                        return new MockResponse().setResponseCode(200).setBody("{\"success\":true,\"sessionID\":\"96fbb852021cf0c8f7c6b5a10c9d0467ffc509f1\",\"maxAge\":900}");
                    case "/server/data_srv.js?action=loadNodeData":
                        return new MockResponse().setResponseCode(200).setBody("{\"nodeData\":{\"UW12345678\":{\"System.CreationDate\":1631298539345,\"System.Tag\":\"DEV,UPS,SDN,MNG,PRM\",\"System.Switchable\":1,\"System.Mode\":\"XCP_USB\",\"UPS.Flow[4].ConfigVoltage\":230,\"UPS.Flow[4].ConfigFrequency\":50,\"UPS.Flow[4].ConfigApparentPower\":750,\"System.Description\":\"PW5115 750i\",\"System.CommunicationDescription\":\"USB device\",\"System.Name\":\"PW5115 750i\",\"System.SerialNumber\":\"UW12345678\",\"UPS.PowerConverter.Input[2].FlowID\":1,\"UPS.PowerConverter.ConverterType\":1,\"UPS.Flow[4].ConfigActivePower\":500,\"UPS.OutletSystem.Outlet[1].OutletID\":1,\"UPS.OutletSystem.Outlet[1].PresentStatus.Switchable\":1,\"UPS.PowerSummary.DelayBeforeShutdown\":-1,\"System.UID\":\"DEV-UPS/SN:12344234234234E\",\"System.PresentStatus.CommunicationError\":0,\"UPS.BatterySystem.Charger.PresentStatus.InternalFailure\":0,\"UPS.PowerConverter.Inverter.PresentStatus.InternalFailure\":0,\"System.PresentStatus.ACPresent2\":1,\"UPS.PowerSummary.PresentStatus.BelowRemainingCapacityLimit\":0,\"System.PresentStatus.Discharging\":0,\"UPS.PowerSummary.PresentStatus.NeedReplacement\":0,\"UPS.PowerSummary.PresentStatus.Overload\":0,\"UPS.PowerSummary.PresentStatus.ShutdownImminent\":0,\"UPS.PowerSummary.PresentStatus.WarningAlarm\":0,\"UPS.PowerConverter.Inverter.PresentStatus.Used\":0,\"UPS.PowerSummary.PresentStatus.FanFailure\":0,\"UPS.PowerSummary.PresentStatus.InternalFailure\":0,\"System.PresentStatus.ACPresent\":1,\"UPS.PowerSummary.PresentStatus.ACPresent\":1,\"System.PresentStatus.Good\":1,\"UPS.PowerSummary.PresentStatus.Good\":1,\"UPS.PowerSummary.RunTimeToEmpty\":2334,\"UPS.PowerSummary.PresentStatus.Charging\":1,\"UPS.PowerSummary.PresentStatus.Discharging\":0,\"UPS.BatterySystem.Charger.PresentStatus.Floating\":1,\"UPS.PowerConverter.Output.Current\":0,\"System.ApparentPower\":90,\"UPS.PowerConverter.Input[1].Voltage\":239,\"UPS.PowerSummary.RemainingCapacity\":100,\"UPS.PowerConverter.Output.Voltage\":239,\"System.ActivePower\":82,\"System.PercentLoad\":16,\"UPS.PowerConverter.Input[1].Frequency\":50,\"UPS.PowerConverter.Output.Frequency\":50,\"UPS.PowerSummary.Temperature\":294,\"UPS.PowerSummary.Voltage\":27.3,\"System.CommunicationErrorState\":\"\",\"System.Status\":1,\"System.CommunicationLost\":0}}}");
                    case "/server/events_srv.js?action=loadNodeEvents":
                        return new MockResponse().setResponseCode(200).setBody("{\"date\":1631031250263,\"count\":4,\"data\":[{\"id\":\"92\",\"nodeID\":\"UW12345678\",\"name\":\"PW5115 750i\",\"date\":\"1631025141789\",\"status\":\"1\",\"message\":\"Communication with device is restored\",\"ack\":\"0\"},{\"id\":\"90\",\"nodeID\":\"UW12345678\",\"name\":\"PW5115 750i\",\"date\":\"1631024907166\",\"status\":\"4\",\"message\":\"Communication with device has failed\",\"ack\":\"0\"},{\"id\":\"66\",\"nodeID\":\"UW12345678\",\"name\":\"PW5115 750i\",\"date\":\"1631002878631\",\"status\":\"1\",\"message\":\"Communication with device is restored\",\"ack\":\"0\"},{\"id\":\"61\",\"nodeID\":\"UW12345678\",\"name\":\"PW5115 750i\",\"date\":\"1631000568102\",\"status\":\"4\",\"message\":\"Communication with device has failed\",\"ack\":\"0\"}]}");
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        server.setDispatcher(dispatcher);
        server.start(4680);
    }

    @Test
    public void testIPM() {

        // Eaton IPM
        IPM ipm = new IPM(
                false, "127.0.0.1", "4680",
                "test", "test", "UW12345678"
        );

        assertNotNull(ipm.getNodeStatus());
        assertEquals(4, ipm.getEvents().size());
    }


    @After
    public void teardown() throws IOException {
        server.shutdown();
    }


}