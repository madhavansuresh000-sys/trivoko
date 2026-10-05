/**
 * Where to go after login: only pages of THIS site. A link like /login?next=https://evil.example
 * (or //evil.example) is ignored, so nobody can use our login page to send people elsewhere.
 */
export const safeNext = (next) => (next && next.startsWith('/') && !next.startsWith('//') ? next : null)
