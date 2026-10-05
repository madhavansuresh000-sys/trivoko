/** The admin's coupon form (Phase 6): what the form shows <-> what the API wants. */

export const EMPTY_COUPON = { code: '', description: '', type: 'PERCENT', value: '', maxDiscount: '', minOrder: '0', validUntil: '' }

/** coupon from the API -> form fields (strings; the date part only) */
export const toForm = (c) => ({
  code: c.code, description: c.description, type: c.type, value: String(c.value),
  maxDiscount: c.maxDiscount == null ? '' : String(c.maxDiscount), minOrder: String(c.minOrder),
  validUntil: c.validUntil?.slice(0, 10) ?? '',
})

/**
 * form -> request body. Empty optional numbers become null; the last day lasts until 23:59:59.
 * editing = true leaves out code and type (they never change after creation).
 */
export function toRequest(form, editing) {
  const num = (v) => (v === '' || v === null ? null : Number(v))
  const body = {
    description: form.description.trim(), value: num(form.value), maxDiscount: form.type === 'PERCENT' ? num(form.maxDiscount) : null,
    minOrder: num(form.minOrder) ?? 0, validUntil: form.validUntil ? `${form.validUntil}T23:59:59` : null,
  }
  return editing ? body : { ...body, code: form.code.trim().toUpperCase(), type: form.type }
}
