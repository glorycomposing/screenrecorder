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
    private Button testButton;
    private TextView statusText;
    private TextView progressText;
    private boolean isRecording = false;
    
    // Screen recording variables
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;
    private String recordingPath;
    private SupabaseService supabaseService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide keyboard to prevent IME issues
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        
        // Additional IME configuration to prevent errors
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        
        // Create a simple layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        // Add title
        TextView title = new TextView(this);
        title.setText("Screen Recorder App");
        title.setTextSize(24);
        title.setGravity(android.view.Gravity.CENTER);
        layout.addView(title);
        
        // Add status text
        statusText = new TextView(this);
        statusText.setText("Ready to record");
        statusText.setTextSize(16);
        statusText.setGravity(android.view.Gravity.CENTER);
        statusText.setPadding(0, 20, 0, 10);
        layout.addView(statusText);
        
        // Add progress text
        progressText = new TextView(this);
        progressText.setText("");
        progressText.setTextSize(14);
        progressText.setGravity(android.view.Gravity.CENTER);
        progressText.setPadding(0, 0, 0, 20);
        layout.addView(progressText);
        
        // Add start button
        startButton = new Button(this);
        startButton.setText("Start Recording");
        startButton.setTextSize(18);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Clear focus to prevent IME issues
                    v.clearFocus();
                    startRecording();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error starting recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        layout.addView(startButton);
        
        // Add stop button
        stopButton = new Button(this);
        stopButton.setText("Stop Recording");
        stopButton.setTextSize(18);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Clear focus to prevent IME issues
                    v.clearFocus();
                    stopRecording();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error stopping recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        layout.addView(stopButton);
        
        // Add test button
        testButton = new Button(this);
        testButton.setText("Test Supabase Connection");
        testButton.setTextSize(16);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testSupabaseConnection();
            }
        });
        layout.addView(testButton);
        
        setContentView(layout);
        
        // Initialize Supabase service
        supabaseService = new SupabaseService(this);
        
        updateUI();
    }
    
    private void startRecording() {
        if (!isRecording) {
            try {
                // Hide keyboard before starting recording
                android.view.inputmethod.InputMethodManager imm = 
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    try {
                        imm.hideSoftInputFromWindow(getCurrentFocus() != null ? getCurrentFocus().getWindowToken() : null, 0);
                    } catch (Exception imeError) {
                        // Ignore IME errors - they're often harmless
                        System.out.println("IME warning (expected): " + imeError.getMessage());
                    }
                }
                
                // Request screen capture permission
                MediaProjectionManager mediaProjectionManager = 
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                startActivityForResult(captureIntent, REQUEST_SCREEN_CAPTURE);
            } catch (Exception e) {
                Toast.makeText(this, "Error starting recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void stopRecording() {
        if (isRecording) {
            // Set recording to false first to prevent multiple calls
            isRecording = false;
            updateUI();
            
            // Hide keyboard before stopping recording
            try {
                android.view.inputmethod.InputMethodManager imm = 
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    try {
                        imm.hideSoftInputFromWindow(getCurrentFocus() != null ? getCurrentFocus().getWindowToken() : null, 0);
                    } catch (Exception imeError) {
                        // Ignore IME errors - they're often harmless
                        System.out.println("IME warning (expected): " + imeError.getMessage());
                    }
                }
            } catch (Exception e) {
                // Ignore IME errors
            }
            
            // Use a background thread for cleanup to avoid blocking UI
            new Thread(() -> {
                    try {
                        // Stop MediaRecorder first
                        if (mediaRecorder != null) {
                            try {
                                mediaRecorder.stop();
                            } catch (Exception e) {
                                // Ignore stop errors
                            }
                            try {
                                mediaRecorder.release();
                            } catch (Exception e) {
                                // Ignore release errors
                            }
                            mediaRecorder = null;
                        }
                        
                        // Wait before releasing VirtualDisplay
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                        
                        // Release VirtualDisplay
                        if (virtualDisplay != null) {
                            try {
                                virtualDisplay.release();
                            } catch (Exception e) {
                                // Ignore release errors
                            }
                            virtualDisplay = null;
                        }
                        
                        // Wait before stopping MediaProjection
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                        
                        // Stop MediaProjection last
                        if (mediaProjection != null) {
                            try {
                                mediaProjection.stop();
                            } catch (Exception e) {
                                // PHASE_CLIENT_ALREADY_HIDDEN is expected and can be safely ignored
                                // This happens when the system has already stopped the projection
                                System.out.println("MediaProjection stop warning (expected): " + e.getMessage());
                            }
                            mediaProjection = null;
                        }
                        
                                // Check if recording file was created successfully
                                final boolean fileExists = new File(recordingPath).exists();
                                final long fileSize = fileExists ? new File(recordingPath).length() : 0;
                                
                                // Upload to Supabase if file exists and has content
                                if (fileExists && fileSize > 0) {
                                    final File recordingFile = new File(recordingPath);
                                    final String fileName = "recording_" + System.currentTimeMillis() + ".mp4";
                                    
                                    // Upload to Supabase
                                    supabaseService.uploadRecording(recordingFile, fileName, new SupabaseService.UploadCallback() {
                                        @Override
                                        public void onSuccess(String fileUrl, String recordingId) {
                                            runOnUiThread(() -> {
                                                Toast.makeText(MainActivity.this,
                                                        "Recording uploaded successfully!\nURL: " + fileUrl + "\nID: " + recordingId,
                                                        Toast.LENGTH_LONG).show();
                                                
                                                // Update status text
                                                statusText.setText("Recording uploaded to cloud!");
                                            });
                                            
                                            // Delete local file after successful upload
                                            try {
                                                if (recordingFile.delete()) {
                                                    System.out.println("Local file deleted after upload");
                                                }
                                            } catch (Exception e) {
                                                System.out.println("Could not delete local file: " + e.getMessage());
                                            }
                                        }
                                        
                                        @Override
                                        public void onError(String error) {
                                            runOnUiThread(() -> {
                                                Toast.makeText(MainActivity.this,
                                                        "Upload failed: " + error + "\nFile saved locally: " + recordingPath,
                                                        Toast.LENGTH_LONG).show();
                                                
                                                // Update status text
                                                statusText.setText("Recording saved locally (upload failed)");
                                            });
                                        }
                                        
                                        @Override
                                        public void onProgress(int progress) {
                                            runOnUiThread(() -> {
                                                progressText.setText("Uploading... " + progress + "%");
                                            });
                                        }
                                    });
                                } else {
                                    // Show error message on UI thread
                                    runOnUiThread(() -> {
                                        Toast.makeText(MainActivity.this,
                                                "Recording stopped (file may be empty or missing)",
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }
                        
                    } catch (Exception e) {
                        // Show error message on UI thread
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Recording stopped (with warnings)", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }).start();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
                try {
                    // Start actual screen recording
                    startScreenRecording(data);
                } catch (Exception e) {
                    Toast.makeText(this, "Error starting recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Screen recording permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void startScreenRecording(Intent data) throws IOException {
        // Clean up any existing resources first
        try {
            if (mediaRecorder != null) {
                mediaRecorder.release();
                mediaRecorder = null;
            }
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        
        // Create recording file path
        File recordingsDir = new File(getExternalFilesDir(null), "recordings");
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs();
        }
        
        recordingPath = new File(recordingsDir, "recording_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();
        
        // Setup MediaProjection
        MediaProjectionManager mediaProjectionManager = 
            (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mediaProjectionManager.getMediaProjection(-1, data);
        
        // Setup MediaRecorder with error handling
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(recordingPath);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setVideoSize(720, 1280);
            
            // Use the correct method for newer Android versions
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaRecorder.setVideoEncodingBitRate(6000000);
            }
            
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.prepare();
        } catch (Exception e) {
            Toast.makeText(this, "Error setting up MediaRecorder: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        
        // Create VirtualDisplay with error handling
        try {
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            int density = metrics.densityDpi;
            
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenRecorder",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(),
                null, null
            );
        } catch (Exception e) {
            Toast.makeText(this, "Error creating VirtualDisplay: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        
        // Small delay before starting recording
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Start recording with error handling
        try {
            mediaRecorder.start();
            isRecording = true;
            updateUI();
            Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error starting MediaRecorder: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Clean up on error
            try {
                if (mediaRecorder != null) {
                    mediaRecorder.release();
                    mediaRecorder = null;
                }
                if (virtualDisplay != null) {
                    virtualDisplay.release();
                    virtualDisplay = null;
                }
                if (mediaProjection != null) {
                    mediaProjection.stop();
                    mediaProjection = null;
                }
            } catch (Exception cleanupError) {
                // Ignore cleanup errors
            }
        }
    }
    
    private void updateUI() {
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
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources when activity is destroyed
        if (isRecording) {
            stopRecording();
        }
        
        // Clean up all resources to prevent channel errors
        try {
            if (mediaRecorder != null) {
                mediaRecorder.release();
                mediaRecorder = null;
            }
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Don't stop recording when activity is paused
        // This allows recording to continue in background
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Ensure UI is updated when returning to the app
        updateUI();
    }
    
    private void testSupabaseConnection() {
        progressText.setText("Testing Supabase connection...");
        
        // Test database connection
        SupabaseTest.testConnection(this, new SupabaseTest.TestCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    progressText.setText("Database: " + message);
                });
                
                // Test storage connection
                SupabaseTest.testStorage(MainActivity.this, new SupabaseTest.TestCallback() {
                    @Override
                    public void onSuccess(String message) {
                        runOnUiThread(() -> {
                            progressText.setText("Database: OK, Storage: " + message);
                            Toast.makeText(MainActivity.this, "Supabase connection successful!", Toast.LENGTH_SHORT).show();
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressText.setText("Database: OK, Storage: " + error);
                            Toast.makeText(MainActivity.this, "Storage connection failed: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressText.setText("Database: " + error);
                    Toast.makeText(MainActivity.this, "Database connection failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}