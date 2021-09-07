package com.nitramite.apcupsdmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IPM {

    // Logging
    private final static String TAG = IPM.class.getSimpleName();

    private String nodeStatus = null;
    private final ArrayList<String> events = new ArrayList<>();
    private final OkHttpClient client = getUnsafeOkHttpClient();


    IPM(Context context, String baseUrl, String port, String username, String password, String upsNodeId) {
        try {
            String challenge = getChallenge(baseUrl, port);
            Log.i(TAG, "Challenge: " + challenge);
            String sessionId = getLoginSessionId(context, baseUrl, port, username, password, challenge);
            Log.i(TAG, sessionId);

            boolean statusLoaded = loadNodeStatus(baseUrl, port, sessionId, upsNodeId);
            Log.i(TAG, "Status loaded: " + statusLoaded);

            boolean eventsLoaded = loadEvents(baseUrl, port, sessionId, upsNodeId);
            Log.i(TAG, "Events loaded: " + eventsLoaded);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }


    /**
     * Returns challenge from IPM required for login request
     *
     * @param baseUrl of IPM server
     * @param port    of IPM server
     * @return challenge, example a6e3d6d65b072af181dd40658f65051ace864235
     * @throws IOException   if http call fails
     * @throws JSONException if parsing challenge fails
     */
    private String getChallenge(String baseUrl, String port) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url("https://" + baseUrl + ":" + port + "/server/user_srv.js?action=queryLoginChallenge")
                .build();
        try (Response response = client.newCall(request).execute()) {
            JSONObject jsonObject = new JSONObject(Objects.requireNonNull(response.body()).string());
            return jsonObject.optString("challenge");
        }
    }


    /**
     * Login to IPM to get sessionId
     *
     * @param context   of app
     * @param baseUrl   of IPM server
     * @param port      of IPM server
     * @param userName  of IPM server
     * @param password  of IPM server
     * @param challenge from challenge request
     * @return sessionId like: 96fbb852021cf0c8f7c6b5a10c9d0467ffc509f1
     * @throws Exception if login fails
     */
    private String getLoginSessionId(
            Context context, String baseUrl, String port, String userName,
            String password, String challenge
    ) throws Exception {
        String hmac = EatonHMAC(context, password, challenge);
        if (hmac == null) {
            throw new Exception("HMAC generation has failed. Cannot proceed with login.");
        }
        RequestBody formBody = new FormBody.Builder()
                .add("login", userName)
                .add("password", hmac)
                .build();
        Request request = new Request.Builder()
                .url("https://" + baseUrl + ":" + port + "/server/user_srv.js?action=loginUser")
                .post(formBody)
                .build();
        try (Response response = client.newCall(request).execute()) {
            // sample: {"success":true,"sessionID":"96fbb852021cf0c8f7c6b5a10c9d0467ffc509f1","maxAge":900}
            JSONObject jsonObject = new JSONObject(Objects.requireNonNull(response.body()).string());

            String success = jsonObject.optString("success");
            if (success.equals("true")) {
                return jsonObject.optString("sessionID");
            } else {
                throw new Exception("IPM login failed!");
            }
        }
    }


    @SuppressWarnings("ConstantConditions")
    private boolean loadNodeStatus(
            String baseUrl, String port, String sessionId, String upsNodeId
    ) throws Exception {
        events.clear();
        RequestBody formBody = new FormBody.Builder()
                .add("sessionID", sessionId)
                .add("nodes", "[\"" + upsNodeId + "\"]")
                .build();
        Request request = new Request.Builder()
                .url("https://" + baseUrl + ":" + port + "/server/data_srv.js?action=loadNodeData")
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Cookie", "mc2LastLogin=admin; sessionID=" + sessionId)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build();
        try (Response response = client.newCall(request).execute()) {
            StringBuilder sb = new StringBuilder();
            JSONObject dataObject = new JSONObject(Objects.requireNonNull(response.body()).string());
            JSONObject node = dataObject.optJSONObject("nodeData").optJSONObject(upsNodeId);

            // using apcupsd template as base
            sb.append("DATE : ").append(epochToDateString(node.optLong("System.CreationDate"))).append("\n");
            sb.append("VERSION : ").append(node.optString("System.UID")).append("\n");
            sb.append("UPSNAME : ").append(node.optString("System.Name")).append("\n");
            sb.append("CABLE : ").append(node.optString("System.CommunicationDescription")).append("\n");
            sb.append("DRIVER : ").append(node.optString("System.Mode")).append("\n");
            sb.append("UPSMODE : ").append(node.optString("System.Mode")).append("\n");
            sb.append("MODEL : ").append(node.optString("System.Name")).append("\n");

            // most obvious bit is here
            sb.append("STATUS : ").append(node.optInt("System.PresentStatus.ACPresent") == 1 ? "ONLINE" : "OFFLINE").append("\n");

            sb.append("LINEV : ").append(node.optString("UPS.PowerConverter.Input[1].Voltage")).append(" Volts").append("\n");
            sb.append("LOADPCT : ").append(node.optString("System.PercentLoad")).append("\n");
            sb.append("BCHARGE : ").append(node.optString("UPS.PowerSummary.RemainingCapacity")).append("\n");
            sb.append("TIMELEFT : ").append(node.optInt("UPS.PowerSummary.RunTimeToEmpty") / 60).append(" Minutes").append("\n");

            sb.append("SERIALNO : ").append(node.optString("System.SerialNumber")).append("\n");
            sb.append("OUTPUTV : ").append(node.optString("UPS.PowerConverter.Output.Voltage")).append(" Volts").append("\n");
            sb.append("ITEMP : ").append(node.optInt("UPS.PowerSummary.Temperature") / 10).append(" C").append("\n"); // Todo.. verify this, divided by 10 is guess
            sb.append("BATTV : ").append(node.optString("UPS.PowerSummary.Voltage")).append(" Volts").append("\n");
            sb.append("LINEFREQ : ").append(node.optString("UPS.PowerConverter.Input[1].Frequency")).append(" Hz").append("\n");

            Log.i(TAG, "Status string: " + sb.toString());
            nodeStatus = sb.toString();
            return true;
        }
    }


    /**
     * IPM UPS events for specified UPS node id => your UPS serial number
     *
     * @param baseUrl   to query events from
     * @param port      to use
     * @param sessionId for request
     * @param upsNodeId which events are requested
     * @return success result
     * @throws Exception if loading fails
     */
    private Boolean loadEvents(
            String baseUrl, String port, String sessionId, String upsNodeId
    ) throws Exception {
        events.clear();
        RequestBody formBody = new FormBody.Builder()
                .add("sessionID", sessionId)
                .add("nodeID", upsNodeId)
                .build();
        Request request = new Request.Builder()
                .url("https://" + baseUrl + ":" + port + "/server/events_srv.js?action=loadNodeEvents")
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Cookie", "mc2LastLogin=admin; sessionID=" + sessionId)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build();
        try (Response response = client.newCall(request).execute()) {
            // sample: {"date":1631031250263,"count":4,"data":[{"id":"92","nodeID":"UW336A0412","name":"PW5115 750i","date":"1631025141789","status":"1","message":"Communication with device is restored","ack":"0"},{"id":"90","nodeID":"UW336A0412","name":"PW5115 750i","date":"1631024907166","status":"4","message":"Communication with device has failed","ack":"0"},{"id":"66","nodeID":"UW336A0412","name":"PW5115 750i","date":"1631002878631","status":"1","message":"Communication with device is restored","ack":"0"},{"id":"61","nodeID":"UW336A0412","name":"PW5115 750i","date":"1631000568102","status":"4","message":"Communication with device has failed","ack":"0"}]}
            JSONObject eventsObject = new JSONObject(Objects.requireNonNull(response.body()).string());
            JSONArray eventsArray = eventsObject.optJSONArray("data");

            for (int i = 0; i < eventsArray.length(); i++) {
                // example: {"id":"92","nodeID":"UW336A0412","name":"PW5115 750i","date":"1631025141789","status":"1","message":"Communication with device is restored","ack":"0"}
                StringBuilder sb = new StringBuilder();
                JSONObject eventObject = eventsArray.optJSONObject(i);

                // Datetime
                sb.append(epochToDateString(eventObject.optLong("date"))).append(" ");

                sb.append(eventObject.optString("message"));
                Log.i(TAG, "IPM node " + upsNodeId + " event: " + sb.toString());
                events.add(sb.toString());
            }
            return true;
        }
    }


    // ---------------------------------------------------------------------------------------------

    /**
     * Unsecure okHttp client to make request to local network IPM server without valid cert
     *
     * @return okHttp instance
     */
    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @SuppressLint("TrustAllX509TrustManager")
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) {
                        }

                        @SuppressLint("TrustAllX509TrustManager")
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true).build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Get hash for login
     *
     * @param context of app
     * @param key     which is user
     * @param data    which is challenge
     * @return hash
     * @throws IOException if calculating hmac fails
     */
    private String EatonHMAC(Context context, String key, String data) throws IOException {
        V8 runtime = V8.createV8Runtime();
        InputStream inputStream = context.getResources().openRawResource(R.raw.ipm);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder ipmScript = new StringBuilder();
        for (String line; (line = bufferedReader.readLine()) != null; ) {
            ipmScript.append(line).append('\n');
        }

        runtime.executeScript(ipmScript.toString());
        String hash = runtime.executeStringFunction("hmac", new V8Array(runtime).push(key).push(data));
        Log.i(TAG, "Hash from V8: " + hash);
        // runtime.release();
        return hash;
    }


    /**
     * @param date_ in epoch format like 1630851955392
     * @return string date in format
     * Todo.. add settings formats here if added any others
     */
    private String epochToDateString(long date_) {
        Date date = new Date(date_);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return jdf.format(date);
    }


    /**
     * @return parsed events
     */
    public ArrayList<String> getEvents() {
        return events;
    }

    /**
     * @return all status details string containing all information
     */
    public String getNodeStatus() {
        return nodeStatus;
    }

}
