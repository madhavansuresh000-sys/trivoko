import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'

import AuthCard from '../components/auth/AuthCard'
import Button from '../components/ui/Button'
import { PasswordField, TextField } from '../components/ui/FormField'
import useForm from '../hooks/useForm'
import { guestCart } from '../lib/guestCart'
import { safeNext } from '../lib/safeNext'
import { firstName, login, selectUser } from '../store/authSlice'
import { mergeGuestCart } from '../store/cartSlice'
import { notify } from '../store/notificationsSlice'
import { email, required } from '../utils/validation'

const rules = {
  email: [required('Email'), email],
  password: [required('Password')],
}

export default function LoginPage() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const user = useSelector(selectUser)
  const [serverError, setServerError] = useState(null)
  const next = safeNext(params.get('next'))
  const guestItems = guestCart.read().length

  const { field, handleSubmit, submitting } = useForm({ email: '', password: '' }, rules, async (v) => {
    setServerError(null)
    try {
      // POST /api/auth/login: the server checks the BCrypt hash and sets the httpOnly JWT cookie
      const me = await dispatch(login({ email: v.email.trim(), password: v.password })).unwrap()
      await dispatch(mergeGuestCart()) // the guest cart joins the saved cart (Phase 3 "done when")
      dispatch(notify(`Welcome back, ${firstName(me)}!`))
      navigate(next ?? '/', { replace: true })
    } catch (e) {
      setServerError(e.message) // 401 wrong email or password, 429 locked after 5 tries
    }
  })

  // already logged in (e.g. pressed Back after logging in): no need for this page
  if (user && !submitting) return <Navigate to={next ?? '/'} replace />

  return (
    <AuthCard
      title="Welcome back"
      subtitle={guestItems > 0
        ? `Log in to keep the ${guestItems} ${guestItems === 1 ? 'item' : 'items'} in your cart.`
        : next ? 'Please log in to continue.' : 'Log in to see your cart, addresses and orders.'}
      footer={<>New to TriVoKo? <Link to={`/register${next ? `?next=${encodeURIComponent(next)}` : ''}`} className="font-semibold text-brand-700 hover:underline dark:text-brand-400">Create an account</Link></>}
    >
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        {serverError && (
          <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800 dark:bg-red-950/40 dark:text-red-300">
            {serverError}
          </p>
        )}
        <TextField label="Email" type="email" autoComplete="email" placeholder="you@gmail.com" {...field('email')} />
        <PasswordField label="Password" autoComplete="current-password" {...field('password')} />
        <Button type="submit" size="lg" className="w-full" disabled={submitting}>
          {submitting ? 'Checking…' : 'Log in'}
        </Button>
      </form>
    </AuthCard>
  )
}
