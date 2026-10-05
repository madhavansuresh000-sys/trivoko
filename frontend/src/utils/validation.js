/**
 * Small validation rules. Each returns an error message, or '' when the value is fine.
 * The backend checks the same things again (never trust the browser alone).
 */

export const required = (label) => (v) => (String(v ?? '').trim() ? '' : `${label} is required`)

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/
export const email = (v) => (!v || EMAIL.test(v.trim()) ? '' : 'Enter a valid email, e.g. name@gmail.com')

export const minLength = (n, label) => (v) => (!v || v.length >= n ? '' : `${label} must be at least ${n} characters`)

export const maxLength = (n, label) => (v) => (!v || v.length <= n ? '' : `${label} must be at most ${n} characters`)

/** Whole number between min and max, e.g. seats. Empty is left to required(). */
export const wholeNumberBetween = (min, max, label) => (v) => {
  if (v === '' || v === null || v === undefined) return ''
  const n = Number(v)
  if (!Number.isInteger(n)) return `${label} must be a whole number`
  if (n < min || n > max) return `${label} must be between ${min} and ${max}`
  return ''
}

export const notNegative = (label) => (v) => (v === '' || Number(v) >= 0 ? '' : `${label} cannot be negative`)

export const strongPassword = (v) => {
  if (!v) return ''
  if (v.length < 8) return 'Use at least 8 characters'
  if (v.length > 72) return 'Use at most 72 characters' // BCrypt only reads the first 72
  if (!/[a-zA-Z]/.test(v) || !/\d/.test(v)) return 'Use both letters and numbers'
  return ''
}

/** Compares with another field, e.g. "confirm password" must equal "password". */
export const matches = (otherField, message) => (v, values) => (v === values[otherField] ? '' : message)

export const mustBeTrue = (message) => (v) => (v ? '' : message)

/** Runs the rules of every field; returns { field: firstError } for fields with a problem. */
export function validate(values, rules) {
  const errors = {}
  for (const [field, fieldRules] of Object.entries(rules)) {
    for (const rule of fieldRules) {
      const message = rule(values[field], values)
      if (message) {
        errors[field] = message
        break
      }
    }
  }
  return errors
}

/** 0-4 score for the password strength meter. */
export function passwordScore(v = '') {
  let score = 0
  if (v.length >= 8) score++
  if (v.length >= 12) score++
  if (/[a-z]/.test(v) && /[A-Z]/.test(v)) score++
  if (/\d/.test(v) && /[^a-zA-Z0-9]/.test(v)) score++
  return score
}
