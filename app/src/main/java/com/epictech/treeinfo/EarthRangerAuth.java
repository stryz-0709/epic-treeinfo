package com.epictech.treeinfo;

import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EarthRangerAuth {
    private static String accessToken = null;
    private static long tokenExpiryTime = 0;

    public static String getCachedToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return accessToken;
        }
        return null;
    }

    public static synchronized String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return accessToken;
        }

        try {
            OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new FormBody.Builder()
                    .add("client_id", "das_web_client")
                    .add("grant_type", "password")
                    .add("username", BuildConfig.ER_USERNAME)
                    .add("password", BuildConfig.ER_PASSWORD)
                    .build();

            Request request = new Request.Builder()
                    .url("https://epictech.pamdas.org/oauth2/token/")
                    .post(formBody)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String jsonStr = response.body().string();
                JSONObject json = new JSONObject(jsonStr);
                accessToken = json.getString("access_token");
                long expiresIn = json.getLong("expires_in");
                tokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000) - 60000;
                return accessToken;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
