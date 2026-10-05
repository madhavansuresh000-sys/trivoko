/**
 * Colour and size buttons (sketch 3). Stock lives on the VARIANT (Blue / UK 8), so a size can be sold
 * out in one colour and not in another: sold-out or missing combinations are disabled and crossed out.
 * Variants with neither size nor colour (e.g. "Black / 128 GB") are shown as one row of label buttons.
 */

const unique = (values) => [...new Set(values.filter(Boolean))]

function Option({ label, pressed, disabled, onClick, hint }) {
  return (
    <button type="button" aria-pressed={pressed} disabled={disabled} onClick={onClick}
      title={disabled ? hint : undefined}
      className={
        'min-w-11 rounded-full border px-3.5 py-1.5 text-sm font-medium transition-colors ' +
        (pressed
          ? 'border-brand-700 bg-brand-700 text-white'
          : disabled
            ? 'cursor-not-allowed border-slate-200 text-slate-400 line-through dark:border-slate-800 dark:text-slate-600'
            : 'border-slate-300 text-slate-700 hover:border-brand-600 dark:border-slate-600 dark:text-slate-200')
      }>
      {disabled && <span className="sr-only">Sold out: </span>}
      {label}
    </button>
  )
}

function Row({ title, value, children }) {
  return (
    <fieldset>
      <legend className="text-sm font-semibold text-slate-900 dark:text-white">
        {title}{value && <span className="font-normal text-slate-500 dark:text-slate-400">: {value}</span>}
      </legend>
      <div className="mt-2 flex flex-wrap gap-2">{children}</div>
    </fieldset>
  )
}

export default function VariantPicker({ variants, selectedId, onSelect }) {
  const selected = variants.find((v) => v.id === selectedId) ?? variants[0]
  const colours = unique(variants.map((v) => v.colour))
  const sizes = unique(variants.map((v) => v.size))

  if (colours.length === 0 && sizes.length === 0) {
    if (variants.length < 2) return null
    return (
      <Row title="Choose">
        {variants.map((v) => (
          <Option key={v.id} label={v.label} pressed={v.id === selected.id} disabled={!v.inStock}
            hint="Sold out" onClick={() => onSelect(v.id)} />
        ))}
      </Row>
    )
  }

  const find = (colour, size) =>
    variants.find((v) => (colours.length === 0 || v.colour === colour) && (sizes.length === 0 || v.size === size))

  /** New colour: keep the size if that colour has it in stock, else its first in-stock variant. */
  const pickColour = (colour) => {
    const same = find(colour, selected.size)
    const next = same?.inStock ? same : variants.find((v) => v.colour === colour && v.inStock) ?? variants.find((v) => v.colour === colour)
    onSelect(next.id)
  }

  return (
    <div className="space-y-4">
      {colours.length > 0 && (
        <Row title="Colour" value={selected.colour}>
          {colours.map((c) => (
            <Option key={c} label={c} pressed={c === selected.colour}
              disabled={!variants.some((v) => v.colour === c && v.inStock)} hint="Sold out in every size"
              onClick={() => pickColour(c)} />
          ))}
        </Row>
      )}
      {sizes.length > 0 && (
        <Row title="Size" value={selected.size}>
          {sizes.map((s) => {
            const v = find(selected.colour, s)
            return (
              <Option key={s} label={s} pressed={v?.id === selected.id} disabled={!v?.inStock}
                hint={v ? 'Sold out' : 'Not made in this colour'} onClick={() => onSelect(v.id)} />
            )
          })}
        </Row>
      )}
    </div>
  )
}
