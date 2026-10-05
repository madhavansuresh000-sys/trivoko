import { useSelector } from 'react-redux'
import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { selectAuthStatus, selectUser } from '../../store/authSlice'
import Button from '../ui/Button'
import EmptyState from '../ui/EmptyState'
import { Spinner } from '../ui/Loader'

/**
 * Guards a group of routes:
 *   <Route element={<RequireAuth allow={isAdmin} />}> ...admin pages... </Route>
 * - still asking the server who you are -> spinner
 * - not logged in -> login page, then back here (?next=)
 * - logged in but not allowed -> a friendly "no permission" page
 * This only hides pages. The real protection is the backend, which checks every request again.
 */
export default function RequireAuth({ allow = () => true, what = 'this page' }) {
  const user = useSelector(selectUser)
  const status = useSelector(selectAuthStatus)
  const location = useLocation()

  if (status === 'checking') {
    return <div className="flex justify-center py-24"><Spinner label="Checking your login" /></div>
  }
  if (!user) {
    const next = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?next=${next}`} replace />
  }
  if (!allow(user)) {
    return (
      <EmptyState
        title="You cannot open this page"
        message={`Your account does not have permission for ${what}. Ask the admin if you think this is a mistake.`}
        action={<Button to="/" variant="secondary">Go to Home</Button>}
      />
    )
  }
  return <Outlet />
}
