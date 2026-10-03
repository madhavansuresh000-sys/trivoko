import { Link } from 'react-router-dom'

const base =
  'inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-colors ' +
  'disabled:cursor-not-allowed disabled:opacity-50'

const variants = {
  primary: 'bg-brand-700 text-white hover:bg-brand-800', // teal-700: white text needs 4.5:1 (teal-600 is only 3.7:1)
  secondary:
    'border border-brand-700 text-brand-700 hover:bg-brand-50 ' +
    'dark:border-brand-400 dark:text-brand-300 dark:hover:bg-slate-800',
  ghost: 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800',
  danger: 'bg-red-600 text-white hover:bg-red-700',
  /** for coloured backgrounds, e.g. the teal hero */
  light: 'bg-white text-brand-800 hover:bg-brand-50',
  /** deals and flash sale only (saffron); dark text keeps it readable */
  deal: 'bg-accent-500 text-slate-950 hover:bg-accent-400',
}

const sizes = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-sm',
  lg: 'px-5 py-3 text-base',
}

/**
 * One button for the whole app.
 * <Button>Save</Button>, <Button variant="secondary" size="sm">Cancel</Button>,
 * <Button to="/search">Browse</Button> renders a link that looks like a button.
 */
export default function Button({ variant = 'primary', size = 'md', to, className = '', ...props }) {
  const classes = `${base} ${variants[variant]} ${sizes[size]} ${className}`
  if (to) {
    return <Link to={to} className={classes} {...props} />
  }
  return <button type="button" className={classes} {...props} />
}
