package com.screenrecorderapp;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SupabaseService {
    private static final String TAG = "SupabaseService";
    private static final String SUPABASE_URL = "https://lvbrdodgvwnglhikrcnj.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx2YnJkb2RndnduZ2xoaWtyY25qIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTg1Mjc4NTAsImV4cCI6MjA3NDEwMzg1MH0.hD0bV9PLHsoserBm1arHvrB4DTL_FfA76waWjqLHymQ";
    private static final String STORAGE_BUCKET = "screen-recordings";
    
    private OkHttpClient httpClient;
    private Gson gson;
    private Context context;
    
    public SupabaseService(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }
    
    public interface UploadCallback {
        void onSuccess(String fileUrl, String recordingId);
        void onError(String error);
        void onProgress(int progress);
    }
    
    public void uploadRecording(File recordingFile, String fileName, UploadCallback callback) {
        if (recordingFile == null || !recordingFile.exists()) {
            callback.onError("Recording file not found");
            return;
        }
        
        // First, get the file size for progress tracking
        long fileSize = recordingFile.length();
        if (fileSize == 0) {
            callback.onError("Recording file is empty");
            return;
        }
        
        Log.d(TAG, "Starting upload: " + fileName + " (Size: " + fileSize + " bytes)");
        
        // Create request body with the file
        RequestBody fileBody = RequestBody.create(recordingFile, MediaType.parse("video/mp4"));
        
        // Create multipart request
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .build();
        
        // Create request
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/storage/v1/object/" + STORAGE_BUCKET + "/" + fileName)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .post(requestBody)
                .build();
        
        // Execute request in background thread
        new Thread(() -> {
            try {
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    // File uploaded successfully, now save metadata to database
                    saveRecordingMetadata(fileName, fileSize, callback);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Upload failed: " + response.code() + " - " + errorBody);
                    callback.onError("Upload failed: " + response.code() + " - " + errorBody);
                }
            } catch (IOException e) {
                Log.e(TAG, "Upload error", e);
                callback.onError("Upload error: " + e.getMessage());
            }
        }).start();
    }
    
    private void saveRecordingMetadata(String fileName, long fileSize, UploadCallback callback) {
        try {
            // Create metadata object
            JsonObject metadata = new JsonObject();
            metadata.addProperty("filename", fileName);
            metadata.addProperty("file_size", fileSize);
            metadata.addProperty("created_at", System.currentTimeMillis());
            metadata.addProperty("duration", 0); // We'll calculate this later if needed
            
            // Create request body
            RequestBody body = RequestBody.create(
                gson.toJson(metadata), 
                MediaType.parse("application/json")
            );
            
            // Create request
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/recordings")
                    .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .post(body)
                    .build();
            
            // Execute request
            Response response = httpClient.newCall(request).execute();
            
            if (response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Metadata saved: " + responseBody);
                
                // Extract recording ID from response
                String recordingId = extractRecordingId(responseBody);
                String fileUrl = SUPABASE_URL + "/storage/v1/object/public/" + STORAGE_BUCKET + "/" + fileName;
                
                callback.onSuccess(fileUrl, recordingId);
            } else {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                Log.e(TAG, "Metadata save failed: " + response.code() + " - " + errorBody);
                callback.onError("Failed to save metadata: " + response.code());
            }
        } catch (Exception e) {
            Log.e(TAG, "Metadata save error", e);
            callback.onError("Failed to save metadata: " + e.getMessage());
        }
    }
    
    private String extractRecordingId(String responseBody) {
        try {
            // Parse the response to extract the recording ID
            // This assumes the response contains the inserted record with an ID
            JsonObject response = gson.fromJson(responseBody, JsonObject.class);
            if (response.has("id")) {
                return response.get("id").getAsString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting recording ID", e);
        }
        return "unknown";
    }
    
    public void deleteRecording(String fileName, UploadCallback callback) {
        try {
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/storage/v1/object/" + STORAGE_BUCKET + "/" + fileName)
                    .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .delete()
                    .build();
            
            new Thread(() -> {
                try {
                    Response response = httpClient.newCall(request).execute();
                    if (response.isSuccessful()) {
                        callback.onSuccess("Deleted", "success");
                    } else {
                        callback.onError("Delete failed: " + response.code());
                    }
                } catch (IOException e) {
                    callback.onError("Delete error: " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            callback.onError("Delete error: " + e.getMessage());
        }
    }
}
