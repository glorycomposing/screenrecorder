import React, { useEffect, useState } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  View,
  Alert,
  AppState,
  AppStateStatus,
} from 'react-native';
import { check, request, PERMISSIONS, RESULTS } from 'react-native-permissions';
import { createClient } from '@supabase/supabase-js';
import ScreenRecorder from './src/components/ScreenRecorder';
import { supabaseConfig } from './src/config/supabase';

const supabase = createClient(supabaseConfig.url, supabaseConfig.anonKey);

const App: React.FC = () => {
  const [isRecording, setIsRecording] = useState(false);
  const [permissionsGranted, setPermissionsGranted] = useState(false);

  useEffect(() => {
    requestPermissions();
    setupAppStateListener();
  }, []);

  const requestPermissions = async () => {
    try {
      // Request audio recording permission
      const audioResult = await request(PERMISSIONS.ANDROID.RECORD_AUDIO);
      
      // Request storage permission
      const storageResult = await request(PERMISSIONS.ANDROID.WRITE_EXTERNAL_STORAGE);
      
      if (audioResult === RESULTS.GRANTED && storageResult === RESULTS.GRANTED) {
        setPermissionsGranted(true);
      } else {
        Alert.alert(
          'Permission Required',
          'Audio recording and storage permissions are required for this app to function.',
          [{ text: 'OK' }]
        );
      }
    } catch (error) {
      console.error('Permission request error:', error);
      Alert.alert(
        'Error',
        'Failed to request permissions. Please check your device settings.',
        [{ text: 'OK' }]
      );
    }
  };

  const setupAppStateListener = () => {
    const handleAppStateChange = (nextAppState: AppStateStatus) => {
      if (nextAppState === 'active' && permissionsGranted) {
        setIsRecording(true);
      } else if (nextAppState === 'background' || nextAppState === 'inactive') {
        setIsRecording(false);
      }
    };

    AppState.addEventListener('change', handleAppStateChange);
    
    // Set initial state
    if (AppState.currentState === 'active' && permissionsGranted) {
      setIsRecording(true);
    }
  };

  if (!permissionsGranted) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.permissionContainer}>
          <Text style={styles.permissionText}>
            Please grant screen recording permission to continue.
          </Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Screen Recorder</Text>
        <Text style={styles.status}>
          Status: {isRecording ? 'Recording' : 'Stopped'}
        </Text>
      </View>
      
      <ScreenRecorder 
        isRecording={isRecording}
        onRecordingChange={setIsRecording}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    padding: 20,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
    textAlign: 'center',
  },
  status: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
    marginTop: 10,
  },
  permissionContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  permissionText: {
    fontSize: 18,
    color: '#666',
    textAlign: 'center',
  },
});

export default App;
