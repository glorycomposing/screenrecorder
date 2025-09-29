package com.screenrecorderapp;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SupabaseTest {
    private static final String TAG = "SupabaseTest";
    private static final String SUPABASE_URL = "https://lvbrdodgvwnglhikrcnj.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx2YnJkb2RndnduZ2xoaWtyY25qIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTg1Mjc4NTAsImV4cCI6MjA3NDEwMzg1MH0.hD0bV9PLHsoserBm1arHvrB4DTL_FfA76waWjqLHymQ";
    
    public interface TestCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public static void testConnection(Context context, TestCallback callback) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        
        // Test database connection
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/recordings?select=count")
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .get()
                .build();
        
        new Thread(() -> {
            try {
                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Database connection successful: " + responseBody);
                    callback.onSuccess("Database connection successful");
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Database connection failed: " + response.code() + " - " + errorBody);
                    callback.onError("Database connection failed: " + response.code());
                }
            } catch (IOException e) {
                Log.e(TAG, "Database connection error", e);
                callback.onError("Database connection error: " + e.getMessage());
            }
        }).start();
    }
    
    public static void testStorage(Context context, TestCallback callback) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        
        // Test storage connection
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/storage/v1/bucket/screen-recordings")
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .get()
                .build();
        
        new Thread(() -> {
            try {
                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Storage connection successful: " + responseBody);
                    callback.onSuccess("Storage connection successful");
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Storage connection failed: " + response.code() + " - " + errorBody);
                    callback.onError("Storage connection failed: " + response.code());
                }
            } catch (IOException e) {
                Log.e(TAG, "Storage connection error", e);
                callback.onError("Storage connection error: " + e.getMessage());
            }
        }).start();
    }
}
