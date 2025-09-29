-- Create the recordings table
CREATE TABLE IF NOT EXISTS recordings (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  file_name TEXT NOT NULL,
  file_path TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  file_size BIGINT DEFAULT 0
);

-- Create an index on created_at for better query performance
CREATE INDEX IF NOT EXISTS idx_recordings_created_at ON recordings(created_at DESC);

-- Create a storage bucket for screen recordings
INSERT INTO storage.buckets (id, name, public)
VALUES ('screen-recordings', 'screen-recordings', false)
ON CONFLICT (id) DO NOTHING;

-- Set up Row Level Security (RLS) for the recordings table
ALTER TABLE recordings ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist, then create new ones
DROP POLICY IF EXISTS "Allow all operations for authenticated users" ON recordings;
DROP POLICY IF EXISTS "Allow authenticated users to upload recordings" ON storage.objects;
DROP POLICY IF EXISTS "Allow authenticated users to view recordings" ON storage.objects;
DROP POLICY IF EXISTS "Allow authenticated users to delete recordings" ON storage.objects;

-- Create a policy that allows all operations for authenticated users
CREATE POLICY "Allow all operations for authenticated users" ON recordings
  FOR ALL USING (auth.role() = 'authenticated');

-- Create policies for storage bucket access
CREATE POLICY "Allow authenticated users to upload recordings" ON storage.objects
  FOR INSERT WITH CHECK (bucket_id = 'screen-recordings' AND auth.role() = 'authenticated');

CREATE POLICY "Allow authenticated users to view recordings" ON storage.objects
  FOR SELECT USING (bucket_id = 'screen-recordings' AND auth.role() = 'authenticated');

CREATE POLICY "Allow authenticated users to delete recordings" ON storage.objects
  FOR DELETE USING (bucket_id = 'screen-recordings' AND auth.role() = 'authenticated');
