import { useState } from 'react'
import { useDispatch } from 'react-redux'
import { Link, useNavigate, useParams } from 'react-router-dom'

import { fetchCategories } from '../../api/catalog'
import { createProduct, fetchMyProduct, saveProductImages, submitProduct, updateProduct } from '../../api/seller'
import { Notice } from '../../components/auth/AuthCard'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import { SelectField, TextAreaField, TextField } from '../../components/ui/FormField'
import { ChevronDownIcon, PlusIcon, TrashIcon } from '../../components/ui/icons'
import { Spinner } from '../../components/ui/Loader'
import useAsync, { describeError } from '../../hooks/useAsync'
import { uploadPhoto } from '../../lib/cloudinary'
import { notify } from '../../store/notificationsSlice'

/**
 * Add or edit one of MY products (Phase 2 rules on the server):
 *   DRAFT / REJECTED -> everything editable; "Submit for approval" sends it to the admin
 *   ACTIVE (live)    -> only prices and stock (a live product cannot be secretly renamed)
 *   PENDING          -> nothing until the admin decides
 * Photos (max 8) are added once the product is saved: they upload straight to Cloudinary.
 */

const MAX_PHOTOS = 8
const emptyVariant = () => ({ key: crypto.randomUUID(), id: null, label: '', size: '', colour: '', price: '', mrp: '', stock: '' })

function toForm(p) {
  return {
    name: p.name, categoryId: String(p.categoryId), brand: p.brand, description: p.description ?? '',
    variants: p.variants.map((v) => ({ key: String(v.id), id: v.id, label: v.label, size: v.size ?? '', colour: v.colour ?? '',
      price: String(v.price), mrp: String(v.mrp), stock: String(v.stock) })),
  }
}

/** The server's shape (numbers as numbers, empty size/colour as null). */
function toRequest(f) {
  return {
    name: f.name.trim(), categoryId: Number(f.categoryId), brand: f.brand.trim(), description: f.description.trim(),
    variants: f.variants.map((v) => ({ id: v.id, label: v.label.trim(), size: v.size.trim() || null, colour: v.colour.trim() || null,
      price: Number(v.price), mrp: Number(v.mrp), stock: Number(v.stock) })),
  }
}

function check(f) {
  if (f.name.trim().length < 3) return 'Name must be at least 3 characters.'
  if (!f.categoryId) return 'Choose a sub-category.'
  if (!f.brand.trim()) return 'Brand is required.'
  if (!f.description.trim()) return 'Description is required.'
  if (f.variants.length === 0) return 'A product needs at least one variant.'
  for (const v of f.variants) {
    if (!v.label.trim()) return 'Every variant needs a label (e.g. "Blue / UK 8").'
    if (!(Number(v.price) > 0)) return `Price of "${v.label || 'a variant'}" must be more than 0.`
    if (!(Number(v.mrp) >= Number(v.price))) return `MRP of "${v.label}" must not be lower than its price.`
    if (!Number.isInteger(Number(v.stock)) || Number(v.stock) < 0 || v.stock === '') return `Stock of "${v.label}" must be 0 or more.`
  }
  return null
}

function Photos({ product, onSaved }) {
  const dispatch = useDispatch()
  const [photos, setPhotos] = useState(product.images.map((i) => ({ url: i.url, altText: i.altText })))
  const [busy, setBusy] = useState(false)

  const save = async (next) => {
    setBusy(true)
    try {
      const saved = await saveProductImages(product.id, next)
      setPhotos(saved)
      onSaved(saved)
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    } finally {
      setBusy(false)
    }
  }

  const add = async (files) => {
    const room = MAX_PHOTOS - photos.length
    setBusy(true)
    try {
      const urls = []
      for (const file of [...files].slice(0, room)) urls.push(await uploadPhoto(file))
      await save([...photos, ...urls.map((url) => ({ url, altText: product.name }))])
      if (files.length > room) dispatch(notify(`Only ${MAX_PHOTOS} photos per product - the rest were skipped.`, 'error'))
    } catch (e) {
      dispatch(notify(e.message ?? describeError(e).message, 'error'))
      setBusy(false)
    }
  }

  const move = (i, by) => {
    const next = [...photos]
    ;[next[i], next[i + by]] = [next[i + by], next[i]]
    save(next)
  }

  return (
    <Card className="space-y-3 p-5">
      <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Photos <span className="text-sm font-normal text-slate-500">({photos.length} of {MAX_PHOTOS} - the first is the main photo)</span></h2>
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {photos.map((p, i) => (
          <li key={p.url} className="space-y-1">
            <img src={p.url} alt={p.altText} className="aspect-square w-full rounded-lg object-cover" />
            <div className="flex justify-between">
              <button type="button" disabled={busy || i === 0} onClick={() => move(i, -1)} aria-label={`Move photo ${i + 1} left`}
                className="rounded px-2 text-sm disabled:opacity-30"><ChevronDownIcon className="h-4 w-4 rotate-90" /></button>
              <button type="button" disabled={busy} onClick={() => save(photos.filter((_, j) => j !== i))} aria-label={`Remove photo ${i + 1}`}
                className="rounded px-2 text-red-700 dark:text-red-400"><TrashIcon className="h-4 w-4" /></button>
              <button type="button" disabled={busy || i === photos.length - 1} onClick={() => move(i, 1)} aria-label={`Move photo ${i + 1} right`}
                className="rounded px-2 text-sm disabled:opacity-30"><ChevronDownIcon className="h-4 w-4 -rotate-90" /></button>
            </div>
          </li>
        ))}
      </ul>
      {photos.length < MAX_PHOTOS && (
        <label className="inline-flex cursor-pointer items-center gap-2 rounded-lg border border-dashed border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:border-brand-600 dark:border-slate-600 dark:text-slate-200">
          <PlusIcon className="h-4 w-4" /> {busy ? 'Uploading…' : 'Add photos (JPG, PNG or WebP, max 5 MB)'}
          <input type="file" accept="image/jpeg,image/png,image/webp" multiple className="sr-only" disabled={busy}
            onChange={(e) => { add(e.target.files); e.target.value = '' }} />
        </label>
      )}
    </Card>
  )
}

function Editor({ product, tree }) {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const [form, setForm] = useState(product ? toForm(product) : { name: '', categoryId: '', brand: '', description: '', variants: [emptyVariant()] })
  const [current, setCurrent] = useState(product)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  const status = current?.status ?? 'NEW'
  const live = status === 'ACTIVE'
  const locked = status === 'PENDING' || status === 'BLOCKED'
  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }))
  const setVariant = (key, field) => (e) =>
    setForm((f) => ({ ...f, variants: f.variants.map((v) => (v.key === key ? { ...v, [field]: e.target.value } : v)) }))

  const categories = tree.flatMap((top) => top.children.map((c) => ({ value: String(c.id), label: `${top.name} › ${c.name}` })))

  const save = async (andSubmit) => {
    const problem = check(form)
    if (problem) return setError(problem)
    setError(null)
    setSaving(true)
    try {
      let saved = current ? await updateProduct(current.id, toRequest(form)) : await createProduct(toRequest(form))
      if (andSubmit) saved = await submitProduct(saved.id)
      setCurrent(saved)
      setForm(toForm(saved))
      dispatch(notify(andSubmit ? 'Sent for approval - the TriVoKo team will check it.' : 'Saved'))
      if (!current) navigate(`/seller/products/${saved.id}`, { replace: true })
    } catch (e) {
      setError(describeError(e).message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <Link to="/seller/products" className="text-sm text-brand-700 hover:underline dark:text-brand-300">‹ My products</Link>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">{current ? current.name : 'New product'}</h1>
        </div>
        {current && <Badge color={live ? 'green' : status === 'REJECTED' ? 'red' : status === 'PENDING' ? 'amber' : 'gray'}>{status}</Badge>}
      </div>

      {live && <Notice>A live product can only change its prices and stock. To change anything else, contact TriVoKo support.</Notice>}
      {status === 'PENDING' && <Notice>Waiting for the TriVoKo team to approve it - no changes until then.</Notice>}
      {status === 'REJECTED' && current?.rejectionReason && <Notice tone="error">Not approved: {current.rejectionReason}. Fix it and submit again.</Notice>}
      {error && <Notice tone="error">{error}</Notice>}

      <Card className="grid gap-4 p-5 sm:grid-cols-2">
        <TextField id="name" label="Product name" value={form.name} onChange={set('name')} disabled={live || locked} className="sm:col-span-2" />
        <SelectField id="categoryId" label="Category" placeholder="Choose…" options={categories} value={form.categoryId} onChange={set('categoryId')} disabled={live || locked} />
        <TextField id="brand" label="Brand" value={form.brand} onChange={set('brand')} disabled={live || locked} />
        <TextAreaField id="description" label="Description" rows={4} value={form.description} onChange={set('description')} disabled={live || locked} className="sm:col-span-2" />
      </Card>

      <Card className="space-y-3 p-5">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Variants <span className="text-sm font-normal text-slate-500">(sizes, colours - each has its own price and stock)</span></h2>
        {form.variants.map((v, i) => (
          <fieldset key={v.key} className="grid grid-cols-2 gap-2 rounded-xl border border-slate-200 p-3 sm:grid-cols-7 dark:border-slate-700">
            <legend className="sr-only">Variant {i + 1}</legend>
            <TextField id={`label-${v.key}`} label={`Variant label ${i + 1}`} value={v.label} onChange={setVariant(v.key, 'label')} disabled={live || locked} className="col-span-2" placeholder="Blue / UK 8" />
            <TextField id={`size-${v.key}`} label="Size" value={v.size} onChange={setVariant(v.key, 'size')} disabled={live || locked} />
            <TextField id={`colour-${v.key}`} label="Colour" value={v.colour} onChange={setVariant(v.key, 'colour')} disabled={live || locked} />
            <TextField id={`price-${v.key}`} label="Price ₹" type="number" min="1" value={v.price} onChange={setVariant(v.key, 'price')} disabled={locked} />
            <TextField id={`mrp-${v.key}`} label="MRP ₹" type="number" min="1" value={v.mrp} onChange={setVariant(v.key, 'mrp')} disabled={locked} />
            <div className="flex items-end gap-1">
              <TextField id={`stock-${v.key}`} label="Stock" type="number" min="0" value={v.stock} onChange={setVariant(v.key, 'stock')} disabled={locked} className="flex-1" />
              {!live && !locked && form.variants.length > 1 && (
                <button type="button" aria-label={`Remove variant ${i + 1}`} onClick={() => setForm((f) => ({ ...f, variants: f.variants.filter((x) => x.key !== v.key) }))}
                  className="mb-1 rounded p-2 text-red-700 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950/40"><TrashIcon className="h-4 w-4" /></button>
              )}
            </div>
          </fieldset>
        ))}
        {!live && !locked && form.variants.length < 10 && (
          <Button variant="secondary" size="sm" onClick={() => setForm((f) => ({ ...f, variants: [...f.variants, emptyVariant()] }))}>
            <PlusIcon className="h-4 w-4" /> Add a variant
          </Button>
        )}
      </Card>

      {current && <Photos product={current} onSaved={(images) => setCurrent((c) => ({ ...c, images }))} />}

      {!locked && (
        <div className="flex flex-wrap justify-end gap-2">
          <Button variant="secondary" disabled={saving} onClick={() => save(false)}>{live ? 'Save prices and stock' : 'Save draft'}</Button>
          {(status === 'NEW' || status === 'DRAFT' || status === 'REJECTED') && (
            <Button disabled={saving} onClick={() => save(true)}>Submit for approval</Button>
          )}
        </div>
      )}
    </div>
  )
}

export default function ProductEditorPage() {
  const { id } = useParams()
  const categories = useAsync(() => fetchCategories(), [])
  const product = useAsync(() => (id ? fetchMyProduct(id) : Promise.resolve(null)), [id])

  if (categories.loading || product.loading) return <Spinner label="Loading the product" />
  if (product.error) return <Notice tone="error">{describeError(product.error).message}</Notice>
  return <Editor key={id ?? 'new'} product={product.data} tree={categories.data ?? []} />
}
