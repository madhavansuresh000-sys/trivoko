import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'

import VariantPicker from './VariantPicker'

const v = (id, colour, size, inStock = true) =>
  ({ id, label: [colour, size && `UK ${size}`].filter(Boolean).join(' / '), colour, size, price: 2499, mrp: 3999, inStock })

const shoes = [v(1, 'Blue', '7'), v(2, 'Blue', '8'), v(3, 'Blue', '9', false), v(4, 'Black', '8'), v(5, 'Black', '9')]

function Picker({ variants, start }) {
  const [selected, setSelected] = useState(start)
  return <><VariantPicker variants={variants} selectedId={selected} onSelect={setSelected} /><p>chosen {selected}</p></>
}

describe('VariantPicker', () => {
  it('a sold-out size of the chosen colour is disabled and crossed out', async () => {
    render(<Picker variants={shoes} start={1} />)
    const nine = screen.getByRole('button', { name: /9/ })
    expect(nine).toBeDisabled()
    expect(nine.className).toContain('line-through')
    expect(screen.getByRole('button', { name: /8/ })).toBeEnabled()
  })

  it('a size that does not exist in the colour is disabled too', () => {
    render(<Picker variants={shoes} start={4} />) // Black has no size 7
    expect(screen.getByRole('button', { name: /7/ })).toBeDisabled()
  })

  it('changing the colour keeps the size when it is available', async () => {
    render(<Picker variants={shoes} start={2} />) // Blue 8
    await userEvent.click(screen.getByRole('button', { name: 'Black' }))
    expect(screen.getByText('chosen 4')).toBeInTheDocument() // Black 8
  })

  it('variants without size or colour show their labels', async () => {
    const phones = [{ id: 7, label: 'Black / 128 GB', price: 1, mrp: 1, inStock: true },
      { id: 8, label: 'Black / 256 GB', price: 1, mrp: 1, inStock: false }]
    render(<Picker variants={phones} start={7} />)
    expect(screen.getByRole('button', { name: /256 GB/ })).toBeDisabled()
    expect(screen.getByRole('button', { name: /128 GB/ })).toHaveAttribute('aria-pressed', 'true')
  })
})
