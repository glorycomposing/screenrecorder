package com.screenrecorderapp;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.hardware.display.VirtualDisplay;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    
    private static final int REQUEST_SCREEN_CAPTURE = 1001;
    
    private Button startButton;
    private Button stopButton;
    private TextView statusText;
    private boolean isRecording = false;
    
    // Screen recording variables
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;
    private String recordingPath;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configure keyboard behavior to completely prevent IME interactions
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | 
                                   android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        
        // Disable IME completely for this activity
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, 
                           android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        
        // Create a simple layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        // Add title
        TextView title = new TextView(this);
        title.setText("Screen Recorder App");
        title.setTextSize(24);
        title.setPadding(0, 0, 0, 30);
        layout.addView(title);
        
        // Add status text
        statusText = new TextView(this);
        statusText.setText("Ready to record");
        statusText.setTextSize(16);
        statusText.setPadding(0, 0, 0, 20);
        layout.addView(statusText);
        
        // Add start button
        startButton = new Button(this);
        startButton.setText("Start Recording");
        startButton.setOnClickListener(v -> startRecording());
        layout.addView(startButton);
        
        // Add stop button
        stopButton = new Button(this);
        stopButton.setText("Stop Recording");
        stopButton.setEnabled(false);
        stopButton.setOnClickListener(v -> stopRecording());
        layout.addView(stopButton);
        
        setContentView(layout);
        
        // Disable IME for all views to prevent keyboard interactions
        disableIMEForView(layout);
        
        updateUI();
    }
    
    private void startRecording() {
        if (!isRecording) {
            // Hide keyboard before starting recording
            hideKeyboardSafely();
                
            // Request screen capture permission
            MediaProjectionManager mediaProjectionManager = 
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, REQUEST_SCREEN_CAPTURE);
        }
    }
    
    private void stopRecording() {
        if (isRecording) {
            // Set recording to false first to prevent multiple calls
            isRecording = false;
            updateUI();
            
            // Hide keyboard before stopping recording
            hideKeyboardSafely();
            
            // Use a background thread for cleanup to avoid blocking UI
            new Thread(() -> {
                try {
                    // Stop MediaRecorder first
                    if (mediaRecorder != null) {
                        try {
                            mediaRecorder.stop();
                        } catch (Exception e) {
                            System.out.println("MediaRecorder stop error: " + e.getMessage());
                        }
                    }
                    
                    // Wait a bit before cleaning up other resources
                    Thread.sleep(500);
                    
                    // Clean up VirtualDisplay
                    if (virtualDisplay != null) {
                        try {
                            virtualDisplay.release();
                        } catch (Exception e) {
                            System.out.println("VirtualDisplay release error: " + e.getMessage());
                        }
                    }
                    
                    // Wait a bit more
                    Thread.sleep(500);
                    
                    // Clean up MediaProjection
                    if (mediaProjection != null) {
                        try {
                            mediaProjection.stop();
                        } catch (Exception e) {
                            System.out.println("MediaProjection stop error: " + e.getMessage());
                        }
                    }
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Recording stopped and saved locally", Toast.LENGTH_SHORT).show();
                    });
                    
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Recording stopped (with warnings)", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
                startScreenRecording(data);
            } else {
                Toast.makeText(this, "Screen recording permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void startScreenRecording(Intent data) {
        try {
            // Create MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            
            // Set video properties
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int screenWidth = metrics.widthPixels;
            int screenHeight = metrics.heightPixels;
            int screenDensity = metrics.densityDpi;
            
            mediaRecorder.setVideoSize(screenWidth, screenHeight);
            mediaRecorder.setVideoFrameRate(30);
            
            // Use setVideoEncodingBitRate for modern Android
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaRecorder.setVideoEncodingBitRate(6000000);
            }
            
            // Create output file
            File outputDir = new File(getExternalFilesDir(null), "recordings");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            String fileName = "recording_" + System.currentTimeMillis() + ".mp4";
            File outputFile = new File(outputDir, fileName);
            recordingPath = outputFile.getAbsolutePath();
            
            mediaRecorder.setOutputFile(recordingPath);
            
            // Prepare MediaRecorder
            mediaRecorder.prepare();
            
            // Start recording
            mediaRecorder.start();
            
            // Create MediaProjection
            MediaProjectionManager mediaProjectionManager = 
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data);
            
            // Create VirtualDisplay
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenRecorder",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(),
                null, null
            );
            
            // Update UI
            isRecording = true;
            updateUI();
            
            Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
            System.out.println("Recording start error: " + e.getMessage());
        }
    }
    
    private void updateUI() {
        // Ensure UI updates happen on the main thread
        runOnUiThread(() -> {
            try {
                if (isRecording) {
                    statusText.setText("Recording in progress...");
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                } else {
                    statusText.setText("Ready to record");
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                }
            } catch (Exception e) {
                // Handle any UI update errors gracefully
                System.out.println("UI update error: " + e.getMessage());
            }
        });
    }
    
    private void hideKeyboardSafely() {
        try {
            // Check if IME is even available before trying to hide it
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && imm.isActive()) {
                View currentFocus = getCurrentFocus();
                if (currentFocus != null && currentFocus.getWindowToken() != null) {
                    // Use a more gentle approach to hide keyboard
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 
                        android.view.inputmethod.InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
            }
        } catch (Exception e) {
            // Silently ignore IME errors - they're expected in this context
            // No logging to reduce noise in logs
        }
    }
    
    private void disableIMEForView(View view) {
        try {
            // Disable IME for this view
            view.setFocusable(false);
            view.setFocusableInTouchMode(false);
            
            // If it's a ViewGroup, recursively disable IME for all children
            if (view instanceof android.view.ViewGroup) {
                android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    disableIMEForView(viewGroup.getChildAt(i));
                }
            }
        } catch (Exception e) {
            // Ignore any errors in IME disabling
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources when activity is destroyed
        if (isRecording) {
            stopRecording();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Update UI when returning to the app
        updateUI();
    }
}