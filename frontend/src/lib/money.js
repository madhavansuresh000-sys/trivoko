// Indian number grouping: 1,49,999 (lakh), not 149,999
const whole = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 })
const withPaise = new Intl.NumberFormat('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/**
 * "₹2,499" for whole rupees, "₹99.50" when there are paise.
 * The API sends money as numbers (BigDecimal in Java); only the server does real money maths.
 */
export function formatRupees(value) {
  if (value === null || value === undefined || value === '') return '–'
  const n = Number(value)
  return '₹' + (Number.isInteger(n) ? whole.format(n) : withPaise.format(n))
}
