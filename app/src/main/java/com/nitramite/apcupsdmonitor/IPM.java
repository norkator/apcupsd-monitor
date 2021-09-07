package com.nitramite.apcupsdmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IPM {

    // Logging
    private final static String TAG = IPM.class.getSimpleName();

    private final OkHttpClient client = getUnsafeOkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public static final MediaType FORM = MediaType.parse("multipart/form-data");


    IPM(Context context, String baseUrl, String port) {
        try {
            String challenge = getChallenge(baseUrl, port);
            Log.i(TAG, "Challenge: " + challenge);
            String sessionId = getLoginSessionId(context, baseUrl, port, "admin", "admin", challenge);
            Log.i(TAG, sessionId);
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


    private String getLoginSessionId(
            Context context, String baseUrl, String port, String userName,
            String password, String challenge
    ) throws Exception {
        String hmac = EatonHMAC(context, password, challenge);
        Log.i(TAG, "HMAC: " + hmac);
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
            return response.body().string();
            // JSONObject jsonObject = new JSONObject(Objects.requireNonNull(response.body()).string());
            // return jsonObject.optString("challenge");
        }
    }


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
     * @param context of app
     * @param key which is user
     * @param data which is challenge
     * @return hash
     * @throws IOException
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
        runtime.release();
        return hash;
    }


}
