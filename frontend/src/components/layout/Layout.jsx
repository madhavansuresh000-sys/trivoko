import { Suspense } from 'react'
import { Outlet } from 'react-router-dom'

import { Spinner } from '../ui/Loader'
import Footer from './Footer'
import Navbar from './Navbar'
import Toaster from './Toaster'

/** Every page shares the same top menu and footer; <Outlet /> is where the page appears. */
export default function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        {/* pages loaded with React.lazy (see App.jsx) show a spinner while their file downloads */}
        <Suspense fallback={<Spinner />}>
          <Outlet />
        </Suspense>
      </main>
      <Footer />
      <Toaster />
    </div>
  )
}
