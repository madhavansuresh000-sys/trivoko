// Apply the saved theme before the page paints (avoids a white flash in dark mode). Loaded from index.html.
try {
  const saved = localStorage.getItem('theme')
  const dark = saved ? saved === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches
  document.documentElement.classList.toggle('dark', dark)
} catch {
  /* storage blocked: keep the default light theme */
}
