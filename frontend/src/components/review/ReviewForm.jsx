import { useState } from 'react'

import { checkReview } from '../../lib/reviewRules'
import Button from '../ui/Button'
import { TextAreaField, TextField } from '../ui/FormField'
import { StarInput } from '../ui/StarRating'

/** Write or edit my review. `initial` = my earlier review (edit), or null (first time). */
export default function ReviewForm({ initial, onSave, onCancel }) {
  const [form, setForm] = useState({ rating: initial?.rating ?? 0, title: initial?.title ?? '', body: initial?.body ?? '' })
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    const found = checkReview(form)
    setErrors(found)
    if (Object.keys(found).length) return
    setSaving(true)
    try {
      await onSave({ rating: form.rating, title: form.title.trim() || null, body: form.body.trim() })
    } catch (err) {
      setErrors({ form: err.message, ...(err.fieldErrors ?? {}) })
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      <div>
        <StarInput value={form.rating} onChange={(rating) => setForm({ ...form, rating })} />
        {errors.rating && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.rating}</p>}
      </div>
      <TextField id="review-title" label="Title (optional)" value={form.title} maxLength={80} error={errors.title}
        onChange={(e) => setForm({ ...form, title: e.target.value })} />
      <TextAreaField id="review-body" label="Your review" value={form.body} rows={4} error={errors.body}
        hint="What did you like or not like? At least 10 characters."
        onChange={(e) => setForm({ ...form, body: e.target.value })} />
      {errors.form && <p className="text-sm text-red-600 dark:text-red-400" role="alert">{errors.form}</p>}
      <div className="flex gap-2">
        <Button type="submit" disabled={saving}>{saving ? 'Saving…' : initial ? 'Save changes' : 'Post review'}</Button>
        {onCancel && <Button type="button" variant="ghost" onClick={onCancel}>Cancel</Button>}
      </div>
    </form>
  )
}
