import React, { useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Alert,
  NativeModules,
  Platform,
} from 'react-native';
import RNFS from 'react-native-fs';
import { uploadRecording } from '../services/uploadService';
import { generateFileName } from '../utils/fileUtils';

interface ScreenRecorderProps {
  isRecording: boolean;
  onRecordingChange: (recording: boolean) => void;
}

const ScreenRecorder: React.FC<ScreenRecorderProps> = ({
  isRecording,
  onRecordingChange,
}) => {
  const [isProcessing, setIsProcessing] = useState(false);
  const recordingRef = useRef<any>(null);

  useEffect(() => {
    if (isRecording) {
      startRecording();
    } else {
      stopRecording();
    }
  }, [isRecording]);

  const startRecording = async () => {
    try {
      if (Platform.OS === 'android') {
        const { ScreenRecorderModule } = NativeModules;
        
        if (ScreenRecorderModule) {
          const fileName = generateFileName();
          const filePath = `${RNFS.DocumentDirectoryPath}/${fileName}.mp4`;
          
          console.log('Starting recording to:', filePath);
          await ScreenRecorderModule.startRecording(filePath);
          recordingRef.current = { filePath, fileName };
          console.log('Recording started successfully');
        } else {
          console.warn('Screen recording not available on this device');
          Alert.alert(
            'Error',
            'Screen recording is not available on this device. Please ensure you have the latest version of the app.',
            [{ text: 'OK' }]
          );
        }
      } else {
        Alert.alert(
          'Error',
          'Screen recording is only supported on Android devices.',
          [{ text: 'OK' }]
        );
      }
    } catch (error) {
      console.error('Error starting recording:', error);
      Alert.alert(
        'Error',
        `Failed to start screen recording: ${error instanceof Error ? error.message : 'Unknown error'}`,
        [{ text: 'OK' }]
      );
    }
  };

  const stopRecording = async () => {
    try {
      if (recordingRef.current && Platform.OS === 'android') {
        const { ScreenRecorderModule } = NativeModules;
        
        if (ScreenRecorderModule) {
          console.log('Stopping recording...');
          await ScreenRecorderModule.stopRecording();
          
          // Check if file exists before uploading
          const fileExists = await RNFS.exists(recordingRef.current.filePath);
          if (!fileExists) {
            console.warn('Recording file does not exist:', recordingRef.current.filePath);
            Alert.alert(
              'Warning',
              'Recording file was not found. The recording may have failed.',
              [{ text: 'OK' }]
            );
            recordingRef.current = null;
            return;
          }
          
          // Upload the recording
          setIsProcessing(true);
          console.log('Uploading recording...');
          await uploadRecording(recordingRef.current.filePath, recordingRef.current.fileName);
          console.log('Recording uploaded successfully');
          setIsProcessing(false);
          
          recordingRef.current = null;
        }
      }
    } catch (error) {
      console.error('Error stopping recording:', error);
      setIsProcessing(false);
      Alert.alert(
        'Error',
        `Failed to stop recording: ${error instanceof Error ? error.message : 'Unknown error'}`,
        [{ text: 'OK' }]
      );
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.statusContainer}>
        <View style={[styles.indicator, { backgroundColor: isRecording ? '#ff4444' : '#44ff44' }]} />
        <Text style={styles.statusText}>
          {isProcessing ? 'Processing...' : isRecording ? 'Recording' : 'Stopped'}
        </Text>
      </View>
      
      {isRecording && (
        <View style={styles.recordingInfo}>
          <Text style={styles.infoText}>
            Screen recording is active. The app will automatically record when the screen is on.
          </Text>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
  },
  statusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 20,
  },
  indicator: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: 10,
  },
  statusText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  recordingInfo: {
    backgroundColor: '#e3f2fd',
    padding: 15,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#2196f3',
  },
  infoText: {
    fontSize: 14,
    color: '#1976d2',
    lineHeight: 20,
  },
});

export default ScreenRecorder;
