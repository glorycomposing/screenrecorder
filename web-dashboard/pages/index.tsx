import { useState, useEffect } from 'react'
import Head from 'next/head'
import dynamic from 'next/dynamic'
import { useSupabase } from './_app'
import { Play, Pause, Download, Trash2, Calendar, Clock, HardDrive } from 'lucide-react'

interface Recording {
  id: string
  file_name: string
  file_path: string
  created_at: string
  file_size: number
}

function DashboardContent() {
  const [recordings, setRecordings] = useState<Recording[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const supabase = useSupabase()

  // Custom date formatting to avoid hydration issues
  const formatDate = (dateString: string): string => {
    try {
      const date = new Date(dateString)
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    } catch {
      return 'Invalid date'
    }
  }

  const formatDateShort = (dateString: string): string => {
    try {
      const date = new Date(dateString)
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric'
      })
    } catch {
      return 'Invalid date'
    }
  }

  useEffect(() => {
    fetchRecordings()
  }, [])

  const fetchRecordings = async () => {
    try {
      setLoading(true)
      const { data, error } = await supabase
        .from('recordings')
        .select('*')
        .order('created_at', { ascending: false })

      if (error) throw error
      setRecordings(data || [])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    } finally {
      setLoading(false)
    }
  }

  const deleteRecording = async (id: string, filePath: string) => {
    try {
      // Delete from storage
      const { error: storageError } = await supabase.storage
        .from('screen-recordings')
        .remove([filePath])

      if (storageError) throw storageError

      // Delete from database
      const { error: dbError } = await supabase
        .from('recordings')
        .delete()
        .eq('id', id)

      if (dbError) throw dbError

      // Update local state
      setRecordings(recordings.filter(recording => recording.id !== id))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete recording')
    }
  }

  const playRecording = async (filePath: string, fileName: string) => {
    try {
      // Clean the file path - extract just the filename if it contains a full URL
      const cleanFilePath = filePath.includes('/') ? filePath.split('/').pop() : filePath;
      
      // Construct the public URL for direct playback
      const publicUrl = `https://lvbrdodgvwnglhikrcnj.supabase.co/storage/v1/object/public/screen-recordings/${cleanFilePath}`;
      
      // Create a modal with video player
      showVideoModal(publicUrl, fileName);
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to play recording')
    }
  }

  const showVideoModal = (videoUrl: string, fileName: string) => {
    // Create modal overlay
    const modal = document.createElement('div');
    modal.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.8);
      z-index: 1000;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
    `;
    
    // Create video container
    const videoContainer = document.createElement('div');
    videoContainer.style.cssText = `
      background: white;
      border-radius: 8px;
      padding: 20px;
      max-width: 90%;
      max-height: 90%;
      position: relative;
    `;
    
    // Create video element
    const video = document.createElement('video');
    video.src = videoUrl;
    video.controls = true;
    video.autoplay = true;
    video.style.cssText = `
      width: 100%;
      max-width: 800px;
      height: auto;
      border-radius: 4px;
    `;
    
    // Create close button
    const closeBtn = document.createElement('button');
    closeBtn.innerHTML = '✕';
    closeBtn.style.cssText = `
      position: absolute;
      top: 10px;
      right: 10px;
      background: #ff4444;
      color: white;
      border: none;
      border-radius: 50%;
      width: 30px;
      height: 30px;
      cursor: pointer;
      font-size: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
    `;
    
    // Create title
    const title = document.createElement('h3');
    title.textContent = fileName;
    title.style.cssText = `
      margin: 0 0 15px 0;
      color: #333;
      font-size: 1.2rem;
    `;
    
    // Assemble modal
    videoContainer.appendChild(closeBtn);
    videoContainer.appendChild(title);
    videoContainer.appendChild(video);
    modal.appendChild(videoContainer);
    document.body.appendChild(modal);
    
    // Close modal handlers
    const closeModal = () => {
      document.body.removeChild(modal);
    };
    
    closeBtn.onclick = closeModal;
    modal.onclick = (e) => {
      if (e.target === modal) closeModal();
    };
    
    // Escape key to close
    const handleKeyPress = (e) => {
      if (e.key === 'Escape') {
        closeModal();
        document.removeEventListener('keydown', handleKeyPress);
      }
    };
    document.addEventListener('keydown', handleKeyPress);
  }

  const downloadRecording = async (filePath: string, fileName: string) => {
    try {
      // Clean the file path - extract just the filename if it contains a full URL
      const cleanFilePath = filePath.includes('/') ? filePath.split('/').pop() : filePath;
      
      const { data, error } = await supabase.storage
        .from('screen-recordings')
        .download(cleanFilePath)

      if (error) {
        // Fallback: use direct public URL
        const publicUrl = `https://lvbrdodgvwnglhikrcnj.supabase.co/storage/v1/object/public/screen-recordings/${cleanFilePath}`;
        const a = document.createElement('a');
        a.href = publicUrl;
        a.download = fileName;
        a.target = '_blank';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        return;
      }

      const url = URL.createObjectURL(data)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to download recording')
    }
  }

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary-600"></div>
      </div>
    )
  }

  return (
    <>
      <Head>
        <title>🎬 NEXT.JS APP - VERCEL IS SERVING THIS FILE</title>
        <meta name="description" content="Manage your screen recordings" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="icon" href="/favicon.ico" />
      </Head>

      <div className="min-h-screen bg-gray-50">
        <header className="bg-white shadow-sm border-b">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between items-center py-6">
              <div className="flex items-center">
                <Play className="h-8 w-8 text-primary-600 mr-3" />
                <h1 className="text-3xl font-bold text-gray-900">🎬 NEXT.JS APP - VERCEL IS SERVING THIS FILE</h1>
              </div>
              <div className="flex items-center space-x-4">
                <span className="text-sm text-gray-500">
                  {recordings.length} recording{recordings.length !== 1 ? 's' : ''}
                </span>
                <span className="text-xs bg-green-100 text-green-800 px-2 py-1 rounded">
                  UPDATED
                </span>
              </div>
            </div>
          </div>
        </header>

        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {error && (
            <div className="mb-6 bg-red-50 border border-red-200 rounded-md p-4">
              <div className="flex">
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-red-800">Error</h3>
                  <div className="mt-2 text-sm text-red-700">{error}</div>
                </div>
              </div>
            </div>
          )}

          {recordings.length === 0 ? (
            <div className="text-center py-12">
              <Play className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">No recordings</h3>
              <p className="mt-1 text-sm text-gray-500">
                Start recording on your mobile device to see recordings here.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {recordings.map((recording) => (
                <div key={recording.id} className="bg-white overflow-hidden shadow rounded-lg">
                  <div className="p-6">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center">
                        <div className="flex-shrink-0">
                          <Play className="h-8 w-8 text-primary-600" />
                        </div>
                        <div className="ml-4">
                          <h3 className="text-lg font-medium text-gray-900 truncate">
                            {recording.file_name}
                          </h3>
                          <p className="text-sm text-gray-500">
                            {formatDate(recording.created_at)}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center space-x-2">
                        <button
                          onClick={() => playRecording(recording.file_path, recording.file_name)}
                          className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-green-700 bg-green-100 hover:bg-green-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                        >
                          <Play className="h-4 w-4 mr-1" />
                          Play
                        </button>
                        <button
                          onClick={() => downloadRecording(recording.file_path, recording.file_name)}
                          className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-primary-700 bg-primary-100 hover:bg-primary-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                        >
                          <Download className="h-4 w-4 mr-1" />
                          Download
                        </button>
                        <button
                          onClick={() => deleteRecording(recording.id, recording.file_path)}
                          className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-red-700 bg-red-100 hover:bg-red-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                    
                    <div className="mt-4 flex items-center justify-between text-sm text-gray-500">
                      <div className="flex items-center">
                        <HardDrive className="h-4 w-4 mr-1" />
                        {formatFileSize(recording.file_size)}
                      </div>
                      <div className="flex items-center">
                        <Calendar className="h-4 w-4 mr-1" />
                        {formatDateShort(recording.created_at)}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </main>
      </div>
    </>
  )
}

// Export as dynamic component to prevent hydration issues
export default dynamic(() => Promise.resolve(DashboardContent), {
  ssr: false,
  loading: () => (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary-600"></div>
    </div>
  )
})





