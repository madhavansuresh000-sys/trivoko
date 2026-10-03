import { lazy } from 'react'
import { Route, Routes } from 'react-router-dom'

import Layout from './components/layout/Layout'
import HomePage from './pages/HomePage'
import NotFoundPage from './pages/NotFoundPage'

// loaded only when someone opens it (React.lazy = code splitting)
const StyleGuidePage = lazy(() => import('./pages/StyleGuidePage'))

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="style-guide" element={<StyleGuidePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
