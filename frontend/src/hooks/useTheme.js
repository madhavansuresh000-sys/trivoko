import { useCallback, useEffect, useState } from 'react'

function readInitialTheme() {
  return document.documentElement.classList.contains('dark') ? 'dark' : 'light'
}

/** Light / dark theme. Saved in localStorage; the first visit follows the device setting. */
export default function useTheme() {
  const [theme, setTheme] = useState(readInitialTheme)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
    try {
      localStorage.setItem('theme', theme)
    } catch {
      /* storage blocked (private window): the theme still works for this visit */
    }
  }, [theme])

  const toggleTheme = useCallback(() => setTheme((t) => (t === 'dark' ? 'light' : 'dark')), [])

  return { theme, toggleTheme }
}
