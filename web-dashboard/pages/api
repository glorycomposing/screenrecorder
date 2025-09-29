import { NextApiRequest, NextApiResponse } from 'next'
import { supabase } from '../_app'

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', ['GET'])
    res.status(405).end(`Method ${req.method} Not Allowed`)
    return
  }

  try {
    const { filePath } = req.query

    if (!filePath || typeof filePath !== 'string') {
      return res.status(400).json({ error: 'Missing file path' })
    }

    const { data, error } = await supabase.storage
      .from('screen-recordings')
      .download(filePath)

    if (error) throw error

    res.setHeader('Content-Type', 'video/mp4')
    res.setHeader('Content-Disposition', `attachment; filename="${filePath}"`)
    res.status(200).send(data)
  } catch (error) {
    res.status(500).json({ error: error instanceof Error ? error.message : 'An error occurred' })
  }
}





