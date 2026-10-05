import { lazy } from 'react'
import { Route, Routes } from 'react-router-dom'

import Layout from './components/layout/Layout'
import HomePage from './pages/HomePage'
import ComingSoonPage from './pages/ComingSoonPage'
import NotFoundPage from './pages/NotFoundPage'

// loaded only when someone opens it (React.lazy = code splitting)
const StyleGuidePage = lazy(() => import('./pages/StyleGuidePage'))

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="style-guide" element={<StyleGuidePage />} />
        {/* until Phase 3 builds the listing; the 404 page's search and category links land here */}
        <Route
          path="products"
          element={<ComingSoonPage title="All products" description="The product list with filters arrives in Phase 3." />}
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
