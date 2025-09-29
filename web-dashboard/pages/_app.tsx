import type { AppProps } from 'next/app'
import { createClient } from '@supabase/supabase-js'
import { createContext, useContext } from 'react'
import '../styles/globals.css'

const supabaseUrl = 'https://lvbrdodgvwnglhikrcnj.supabase.co'
const supabaseAnonKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx2YnJkb2RndnduZ2xoaWtyY25qIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTg1Mjc4NTAsImV4cCI6MjA3NDEwMzg1MH0.hD0bV9PLHsoserBm1arHvrB4DTL_FfA76waWjqLHymQ'

export const supabase = createClient(supabaseUrl, supabaseAnonKey)

export const SupabaseContext = createContext(supabase)

export const useSupabase = () => {
  const context = useContext(SupabaseContext)
  if (!context) {
    throw new Error('useSupabase must be used within a SupabaseProvider')
  }
  return context
}

export default function App({ Component, pageProps }: AppProps) {
  return (
    <SupabaseContext.Provider value={supabase}>
      <Component {...pageProps} />
    </SupabaseContext.Provider>
  )
}
