# Screen Recorder App

A complete screen recording solution with a React Native mobile app and Next.js web dashboard. The app automatically records screen activity and uploads recordings to Supabase for management through a web interface.

## 🚀 Live Demo

- **Web Dashboard**: [Deployed on Vercel](https://your-app-name.vercel.app)
- **Mobile App**: Available for Android devices

## ✨ Features

### Mobile App (React Native)
- 📱 Automatic screen recording when app is active
- 🔄 Background recording support
- ☁️ Automatic upload to cloud storage
- 🔐 Permission management
- ⚠️ Error handling and user feedback

### Web Dashboard (Next.js)
- 📊 View all recordings
- ⬇️ Download recordings
- 🗑️ Delete recordings
- 🔄 Real-time updates
- 📱 Responsive design

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Mobile App    │    │   Supabase      │    │  Web Dashboard  │
│  (React Native) │───▶│   (Database +   │◀───│   (Next.js)     │
│                 │    │    Storage)     │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🚀 Quick Start

### 1. Clone and Install

```bash
git clone https://github.com/glorycomposing/screenrecorder.git
cd screenrecorder
npm run install:all
```

### 2. Set up Supabase

1. Create a new Supabase project at [supabase.com](https://supabase.com)
2. Run the SQL scripts from `supabase-setup.sql` and `supabase-setup-updated.sql`
3. Get your API keys from Supabase dashboard

### 3. Configure Environment

#### Web Dashboard

Create `web-dashboard/.env.local`:

```env
NEXT_PUBLIC_SUPABASE_URL=your_supabase_url_here
NEXT_PUBLIC_SUPABASE_ANON_KEY=your_supabase_anon_key_here
```

#### Mobile App

Update `mobile-app/src/config/supabase.ts`:

```typescript
export const supabaseConfig = {
  url: 'YOUR_SUPABASE_URL',
  anonKey: 'YOUR_SUPABASE_ANON_KEY',
};
```

### 4. Run the Applications

```bash
# Start both mobile and web apps
npm run dev

# Or start individually
npm run dev:web     # Next.js dashboard
npm run dev:mobile  # React Native app
```

## 📁 Project Structure

```
screen-recorder-app/
├── mobile-app/                 # React Native mobile app
│   ├── android/               # Android native code
│   ├── src/
│   │   ├── components/        # React components
│   │   ├── config/           # Configuration files
│   │   ├── services/         # API services
│   │   └── utils/            # Utility functions
│   └── package.json
├── web-dashboard/             # Next.js web dashboard
│   ├── pages/                # Next.js pages
│   ├── styles/               # CSS styles
│   └── package.json
├── supabase-setup.sql        # Database setup script
└── package.json              # Root package.json
```

## 🌐 Deployment

### Vercel (Web Dashboard)

1. Push your code to GitHub
2. Connect your repository to Vercel
3. Set environment variables in Vercel dashboard:
   - `NEXT_PUBLIC_SUPABASE_URL`
   - `NEXT_PUBLIC_SUPABASE_ANON_KEY`
4. Deploy!

### Android (Mobile App)

1. Build the Android APK:
   ```bash
   cd mobile-app/android
   ./gradlew assembleRelease
   ```
2. Install on Android device or publish to Google Play Store

## 🛠️ Development

### Mobile App

```bash
cd mobile-app
npm start
# In another terminal
npm run android
```

### Web Dashboard

```bash
cd web-dashboard
npm run dev
```

## 🗄️ Database Schema

```sql
CREATE TABLE recordings (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  file_name TEXT NOT NULL,
  file_url TEXT NOT NULL,
  file_size BIGINT DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

## 🔧 Troubleshooting

### Common Issues

1. **Screen recording not working**
   - Check Android permissions
   - Ensure device supports screen recording
   - Verify native module is properly linked

2. **Upload failures**
   - Check Supabase configuration
   - Verify network connectivity
   - Check file permissions

3. **Web dashboard not loading recordings**
   - Verify Supabase API keys
   - Check database connection
   - Ensure RLS policies are correct

## 🔒 Security Considerations

- All recordings are stored privately
- Row Level Security (RLS) enabled
- API keys should be kept secure
- Consider implementing user authentication for production

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📞 Support

For issues and questions:

1. Check the troubleshooting section
2. Review the Supabase setup instructions
3. Check the React Native and Next.js documentation
4. Open an issue on GitHub

---

Made with ❤️ by [Glory Composing](https://github.com/glorycomposing)