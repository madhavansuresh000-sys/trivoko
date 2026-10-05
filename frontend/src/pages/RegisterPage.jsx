import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'

import AuthCard from '../components/auth/AuthCard'
import Button from '../components/ui/Button'
import { PasswordField, TextField } from '../components/ui/FormField'
import useForm from '../hooks/useForm'
import { safeNext } from '../lib/safeNext'
import { firstName, register, selectUser } from '../store/authSlice'
import { mergeGuestCart } from '../store/cartSlice'
import { notify } from '../store/notificationsSlice'
import { email, matches, maxLength, minLength, passwordScore, required, strongPassword } from '../utils/validation'

const phone = (v) => (!v || /^\d{10}$/.test(v.trim()) ? '' : 'Phone must be 10 digits, e.g. 9876543210')

// the same limits as RegisterRequest on the server (it checks again)
const rules = {
  fullName: [required('Full name'), minLength(3, 'Full name'), maxLength(100, 'Full name')],
  email: [required('Email'), email, maxLength(160, 'Email')],
  phone: [phone],
  password: [required('Password'), strongPassword],
  confirm: [(v) => (v ? '' : 'Please type your password again'), matches('password', 'Passwords do not match')],
}

const strength = [
  { label: 'Too weak', color: 'bg-red-500' },
  { label: 'Weak', color: 'bg-red-500' },
  { label: 'Okay', color: 'bg-amber-500' },
  { label: 'Good', color: 'bg-green-500' },
  { label: 'Strong', color: 'bg-green-600' },
]

function StrengthMeter({ password }) {
  if (!password) return null
  const score = passwordScore(password)
  return (
    <div className="mt-2" aria-live="polite">
      <div className="flex gap-1">
        {[0, 1, 2, 3].map((i) => (
          <span key={i} className={`h-1.5 flex-1 rounded-full ${i < score ? strength[score].color : 'bg-slate-200 dark:bg-slate-700'}`} />
        ))}
      </div>
      <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Strength: {strength[score].label}</p>
    </div>
  )
}

export default function RegisterPage() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const user = useSelector(selectUser)
  const [serverError, setServerError] = useState(null)
  const next = safeNext(params.get('next'))

  const { field, handleSubmit, submitting, values } = useForm(
    { fullName: '', email: '', phone: '', password: '', confirm: '' },
    rules,
    async (v, { setServerErrors }) => {
      setServerError(null)
      try {
        // POST /api/auth/register: creates a CUSTOMER account and logs it in (cookie)
        const me = await dispatch(register({
          fullName: v.fullName.trim(), email: v.email.trim(), password: v.password, phone: v.phone.trim(),
        })).unwrap()
        await dispatch(mergeGuestCart())
        dispatch(notify(`Welcome to TriVoKo, ${firstName(me)}! Your account is ready.`))
        navigate(next ?? '/', { replace: true })
      } catch (e) {
        setServerError(e.message) // e.g. 409 "An account with this email already exists"
        setServerErrors(e.fieldErrors ?? {})
      }
    },
  )

  if (user && !submitting) return <Navigate to={next ?? '/'} replace />

  return (
    <AuthCard
      title="Create your account"
      subtitle="One account to shop from every seller on TriVoKo."
      footer={<>Already have an account? <Link to={`/login${next ? `?next=${encodeURIComponent(next)}` : ''}`} className="font-semibold text-brand-700 hover:underline dark:text-brand-400">Log in</Link></>}
    >
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        {serverError && (
          <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800 dark:bg-red-950/40 dark:text-red-300">
            {serverError}
          </p>
        )}
        <TextField label="Full name" autoComplete="name" placeholder="Ravi Kumar" {...field('fullName')} />
        <TextField label="Email" type="email" autoComplete="email" placeholder="you@gmail.com" {...field('email')} />
        <TextField label="Mobile number (optional)" type="tel" inputMode="numeric" autoComplete="tel-national"
          placeholder="9876543210" hint="For delivery updates." {...field('phone')} />
        <PasswordField label="Password" autoComplete="new-password" hint="At least 8 characters, with letters and numbers."
          {...field('password')}>
          <StrengthMeter password={values.password} />
        </PasswordField>
        <PasswordField label="Confirm password" autoComplete="new-password" {...field('confirm')} />
        <Button type="submit" size="lg" className="w-full" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </Button>
      </form>
    </AuthCard>
  )
}
