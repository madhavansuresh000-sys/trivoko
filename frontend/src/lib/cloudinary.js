import api from '../api/client'

/**
 * Uploads ONE product photo straight from the browser to Cloudinary (the photo never passes through our server):
 *   1) POST /api/uploads/signature -> a signature made with the secret key (the key itself stays on the server)
 *   2) POST the file + signature to Cloudinary -> the photo's URL (secure_url)
 * The server later accepts only URLs of OUR Cloudinary account (PUT .../images).
 */
export async function uploadPhoto(file) {
  const { data: s } = await api.post('/uploads/signature')
  if (file.size > s.maxFileSize) {
    throw new Error(`"${file.name}" is too big: at most ${Math.round(s.maxFileSize / 1024 / 1024)} MB per photo.`)
  }
  const form = new FormData()
  form.append('file', file)
  form.append('api_key', s.apiKey)
  form.append('timestamp', String(s.timestamp))
  form.append('signature', s.signature)
  form.append('folder', s.folder)
  form.append('allowed_formats', s.allowedFormats)
  const response = await fetch(s.uploadUrl, { method: 'POST', body: form }) // not our API: no cookie, no CSRF
  const body = await response.json()
  if (!response.ok) throw new Error(body?.error?.message ?? 'The photo could not be uploaded.')
  return body.secure_url
}
