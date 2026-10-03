import { useState } from 'react'

const inputClass = (error) =>
  'block w-full rounded-lg border bg-white px-3 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 ' +
  'dark:bg-slate-950 dark:text-slate-100 ' +
  (error
    ? 'border-red-500 focus:outline-red-500 dark:border-red-500'
    : 'border-slate-300 dark:border-slate-700')

/**
 * Label + input + error message, correctly linked for screen readers.
 * <TextField label="Email" type="email" {...field('email')} />
 */
export function TextField({ label, hint, error, id, className = '', ...inputProps }) {
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">{label}</label>
      <input id={id} className={inputClass(error)} aria-invalid={Boolean(error)} aria-describedby={describedBy} {...inputProps} />
      <FieldMessage id={id} error={error} hint={hint} />
    </div>
  )
}

/** Password box with a Show / Hide button. */
export function PasswordField({ label, hint, error, id, className = '', children, ...inputProps }) {
  const [visible, setVisible] = useState(false)
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">{label}</label>
      <div className="relative">
        <input id={id} type={visible ? 'text' : 'password'} className={`${inputClass(error)} pr-16`}
          aria-invalid={Boolean(error)} aria-describedby={describedBy} {...inputProps} />
        <button type="button" onClick={() => setVisible((v) => !v)}
          className="absolute inset-y-0 right-0 px-3 text-xs font-semibold text-brand-600 hover:text-brand-800 dark:text-brand-400"
          aria-label={visible ? 'Hide password' : 'Show password'}>
          {visible ? 'Hide' : 'Show'}
        </button>
      </div>
      {children}
      <FieldMessage id={id} error={error} hint={hint} />
    </div>
  )
}

export function TextAreaField({ label, hint, error, id, className = '', rows = 4, ...props }) {
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">{label}</label>
      <textarea id={id} rows={rows} className={inputClass(error)} aria-invalid={Boolean(error)} aria-describedby={describedBy} {...props} />
      <FieldMessage id={id} error={error} hint={hint} />
    </div>
  )
}

export function SelectField({ label, error, id, options, placeholder, className = '', ...selectProps }) {
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">{label}</label>
      <select id={id} className={inputClass(error)} aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined} {...selectProps}>
        <option value="">{placeholder}</option>
        {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
      <FieldMessage id={id} error={error} />
    </div>
  )
}

function FieldMessage({ id, error, hint }) {
  if (error) return <p id={`${id}-error`} className="mt-1 text-sm text-red-600 dark:text-red-400">{error}</p>
  if (hint) return <p id={`${id}-hint`} className="mt-1 text-xs text-slate-500 dark:text-slate-400">{hint}</p>
  return null
}
