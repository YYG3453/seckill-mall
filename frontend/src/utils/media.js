/**
 * 商品图、头像等：支持外链或本地上传路径 /uploads/...
 */
export function mediaUrl(path) {
  if (!path) return ''
  const p = String(path).trim()
  if (p.startsWith('http://') || p.startsWith('https://')) return p
  return p.startsWith('/') ? p : `/${p}`
}

/**
 * 同源 /uploads 或代理下的图片下载（带 Cookie）
 */
export async function downloadMediaUrl(path, filename = 'image') {
  const u = mediaUrl(path)
  if (!u) return
  const res = await fetch(u, { credentials: 'include' })
  if (!res.ok) throw new Error('下载失败 ' + res.status)
  const blob = await res.blob()
  const href = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = href
  a.download = filename
  a.rel = 'noopener'
  a.click()
  URL.revokeObjectURL(href)
}
