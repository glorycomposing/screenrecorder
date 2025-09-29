# Screen Recorder Dashboard

A Next.js web dashboard for managing screen recordings from the mobile app.

## Setup

1. Install dependencies:
```bash
npm install
```

2. Create a `.env.local` file with your Supabase credentials:
```
NEXT_PUBLIC_SUPABASE_URL=your_supabase_url_here
NEXT_PUBLIC_SUPABASE_ANON_KEY=your_supabase_anon_key_here
```

3. Run the development server:
```bash
npm run dev
```

## Features

- View all screen recordings
- Download recordings
- Delete recordings
- Real-time updates
- Responsive design

## Database Schema

The dashboard expects a `recordings` table with the following structure:

```sql
CREATE TABLE recordings (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  file_name TEXT NOT NULL,
  file_path TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  file_size BIGINT DEFAULT 0
);
```

## Storage

The dashboard expects a Supabase storage bucket named `screen-recordings` to store the video files.





