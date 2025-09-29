import { createClient } from '@supabase/supabase-js';
import RNFS from 'react-native-fs';
import { supabaseConfig, STORAGE_BUCKET } from '../config/supabase';

const supabase = createClient(supabaseConfig.url, supabaseConfig.anonKey);

export const uploadRecording = async (filePath: string, fileName: string): Promise<void> => {
  try {
    // Check if file exists
    const fileExists = await RNFS.exists(filePath);
    if (!fileExists) {
      throw new Error('Recording file does not exist');
    }

    // Get file stats
    const fileStats = await RNFS.stat(filePath);
    const fileSize = fileStats.size;

    console.log(`Uploading file: ${fileName}, Size: ${fileSize} bytes`);

    // Read the file
    const fileData = await RNFS.readFile(filePath, 'base64');
    const fileBuffer = Buffer.from(fileData, 'base64');

    // Upload to Supabase Storage
    const { data, error } = await supabase.storage
      .from(STORAGE_BUCKET)
      .upload(`${fileName}.mp4`, fileBuffer, {
        contentType: 'video/mp4',
        upsert: false,
      });

    if (error) {
      console.error('Supabase storage error:', error);
      throw new Error(`Storage upload failed: ${error.message}`);
    }

    console.log('File uploaded successfully:', data);

    // Save metadata to database
    await saveRecordingMetadata(fileName, data.path, fileSize);

    // Clean up local file
    await RNFS.unlink(filePath);
    console.log('Local file cleaned up');

  } catch (error) {
    console.error('Upload error:', error);
    throw error;
  }
};

const saveRecordingMetadata = async (fileName: string, filePath: string, fileSize: number): Promise<void> => {
  try {
    const { error } = await supabase
      .from('recordings')
      .insert({
        file_name: fileName,
        file_path: filePath,
        created_at: new Date().toISOString(),
        file_size: fileSize,
      });

    if (error) {
      console.error('Database error:', error);
      throw new Error(`Database save failed: ${error.message}`);
    }

    console.log('Metadata saved successfully');
  } catch (error) {
    console.error('Error saving metadata:', error);
    throw error;
  }
};
