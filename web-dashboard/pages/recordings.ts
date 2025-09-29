import { NextApiRequest, NextApiResponse } from 'next'
import { supabase } from '../_app'

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method === 'GET') {
    try {
      const { data, error } = await supabase
        .from('recordings')
        .select('*')
        .order('created_at', { ascending: false })

      if (error) throw error

      res.status(200).json(data)
    } catch (error) {
      res.status(500).json({ error: error instanceof Error ? error.message : 'An error occurred' })
    }
  } else if (req.method === 'DELETE') {
    try {
      const { id, filePath } = req.body

      if (!id || !filePath) {
        return res.status(400).json({ error: 'Missing required fields' })
      }

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

      res.status(200).json({ success: true })
    } catch (error) {
      res.status(500).json({ error: error instanceof Error ? error.message : 'An error occurred' })
    }
  } else {
    res.setHeader('Allow', ['GET', 'DELETE'])
    res.status(405).end(`Method ${req.method} Not Allowed`)
  }
}





