// Runs before every test file (vite.config.js -> test.setupFiles)
import '@testing-library/jest-dom/vitest' // adds expect(...).toBeInTheDocument(), toHaveTextContent() ...
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(cleanup) // remove what the last test drew, so tests never see each other's screens
