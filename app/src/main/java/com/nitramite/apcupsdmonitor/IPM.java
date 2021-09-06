package com.nitramite.apcupsdmonitor;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class IPM {

    // Logging
    private final static String TAG = IPM.class.getSimpleName();

    private final OkHttpClient client = getUnsafeOkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");


    IPM(String baseUrl, String port) {
        try {
            String challenge = getChallenge(baseUrl, port);
            Log.i(TAG, challenge);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }


    private String getChallenge(String baseUrl, String port) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + ":" + port + "/server/user_srv.js?action=queryLoginChallenge")
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }


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

}
