export function formatDateTime(s) {
  if (!s) return ''
  return s.replace('T', ' ').substring(0, 19)
}
