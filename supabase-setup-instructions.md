# Supabase Setup Instructions

## 1. Create a Supabase Project

1. Go to [supabase.com](https://supabase.com)
2. Sign up or log in to your account
3. Click "New Project"
4. Choose your organization
5. Enter project details:
   - Name: `screen-recorder-app`
   - Database Password: (choose a strong password)
   - Region: (choose the closest to your users)
6. Click "Create new project"

## 2. Set up the Database

1. Go to the SQL Editor in your Supabase dashboard
2. Copy and paste the contents of `supabase-setup.sql`
3. Run the SQL script to create the necessary tables and policies

## 3. Set up Storage

1. Go to Storage in your Supabase dashboard
2. The `screen-recordings` bucket should be created automatically by the SQL script
3. If not, create it manually:
   - Click "New bucket"
   - Name: `screen-recordings`
   - Public: No (private bucket)

## 4. Get your API Keys

1. Go to Settings > API in your Supabase dashboard
2. Copy the following values:
   - Project URL
   - Anon public key

## 5. Update Configuration Files

### Mobile App
Update `mobile-app/src/config/supabase.ts`:
```typescript
export const supabaseConfig = {
  url: lvbrdodgvwnglhikrcnj',
  anonKey: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx2YnJkb2RndnduZ2xoaWtyY25qIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTg1Mjc4NTAsImV4cCI6MjA3NDEwMzg1MH0.hD0bV9PLHsoserBm1arHvrB4DTL_FfA76waWjqLHymQ,
};
```

### Web Dashboard
Create `web-dashboard/.env.local`:
```
NEXT_PUBLIC_SUPABASE_URL=lvbrdodgvwnglhikrcnj
NEXT_PUBLIC_SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx2YnJkb2RndnduZ2xoaWtyY25qIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTg1Mjc4NTAsImV4cCI6MjA3NDEwMzg1MH0.hD0bV9PLHsoserBm1arHvrB4DTL_FfA76waWjqLHymQ
```

## 6. Test the Setup

1. Start the mobile app and record a screen
2. Check the web dashboard to see if the recording appears
3. Try downloading and deleting recordings

## Security Notes

- The current setup allows all authenticated users to access all recordings
- For production, consider implementing user-specific access controls
- Review the RLS policies and adjust based on your security requirements
