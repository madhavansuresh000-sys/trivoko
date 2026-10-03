import { useState } from 'react'

import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { Skeleton, Spinner } from '../components/ui/Loader'
import Modal from '../components/ui/Modal'

function Section({ title, children }) {
  return (
    <Card className="p-6">
      <h2 className="mb-4 text-lg font-semibold text-slate-900 dark:text-white">{title}</h2>
      {children}
    </Card>
  )
}

/** Every shared component on one page, to check the design in light and dark mode. */
export default function StyleGuidePage() {
  const [open, setOpen] = useState(false)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">Style guide</h1>
        <p className="mt-1 text-slate-600 dark:text-slate-400">
          All shared components. Switch dark mode with the moon button to check both themes.
        </p>
      </div>

      <Section title="Brand colours (teal)">
        <div className="flex flex-wrap gap-2">
          {/* full class names, so Tailwind can find them when it scans the code */}
          {[
            ['50', 'bg-brand-50'], ['100', 'bg-brand-100'], ['200', 'bg-brand-200'], ['300', 'bg-brand-300'],
            ['400', 'bg-brand-400'], ['500', 'bg-brand-500'], ['600', 'bg-brand-600'], ['700', 'bg-brand-700'],
            ['800', 'bg-brand-800'], ['900', 'bg-brand-900'],
          ].map(([n, bg]) => (
            <div key={n} className="text-center text-xs">
              <div className={`h-12 w-12 rounded-lg border border-slate-200 dark:border-slate-700 ${bg}`} />
              {n}
            </div>
          ))}
        </div>
        <p className="mb-2 mt-4 text-sm font-medium">Accent (saffron) - deals and flash sale only</p>
        <div className="flex flex-wrap gap-2">
          {[
            ['50', 'bg-accent-50'], ['100', 'bg-accent-100'], ['200', 'bg-accent-200'], ['300', 'bg-accent-300'],
            ['400', 'bg-accent-400'], ['500', 'bg-accent-500'], ['600', 'bg-accent-600'], ['700', 'bg-accent-700'],
            ['800', 'bg-accent-800'], ['900', 'bg-accent-900'],
          ].map(([n, bg]) => (
            <div key={n} className="text-center text-xs">
              <div className={`h-12 w-12 rounded-lg border border-slate-200 dark:border-slate-700 ${bg}`} />
              {n}
            </div>
          ))}
        </div>
      </Section>

      <Section title="Buttons">
        <div className="flex flex-wrap items-center gap-3">
          <Button>Primary</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="ghost">Ghost</Button>
          <Button variant="danger">Danger</Button>
          <Button variant="deal">Deal</Button>
          <Button size="sm">Small</Button>
          <Button size="lg">Large</Button>
          <Button disabled>Disabled</Button>
        </div>
      </Section>

      <Section title="Badges">
        <div className="flex flex-wrap gap-2">
          <Badge color="green">In stock</Badge>
          <Badge color="red">Sold out</Badge>
          <Badge color="accent">40% off</Badge>
          <Badge color="amber">PENDING</Badge>
          <Badge color="brand">DELIVERED</Badge>
          <Badge>Electronics</Badge>
        </div>
      </Section>

      <Section title="Loading">
        <Spinner />
        <div className="space-y-2">
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-4 w-2/3" />
          <Skeleton className="h-4 w-1/3" />
        </div>
      </Section>

      <Section title="Empty state">
        <EmptyState
          title="No products match your filters"
          message="Try another brand or a wider price range."
          action={<Button variant="secondary" size="sm">Clear filters</Button>}
        />
      </Section>

      <Section title="Modal">
        <Button onClick={() => setOpen(true)}>Open modal</Button>
        <Modal
          open={open}
          onClose={() => setOpen(false)}
          title="Cancel this package?"
          footer={
            <>
              <Button variant="ghost" onClick={() => setOpen(false)}>Keep it</Button>
              <Button variant="danger" onClick={() => setOpen(false)}>Yes, cancel</Button>
            </>
          }
        >
          The seller will not ship it and the money for this package is refunded. This cannot be undone.
        </Modal>
      </Section>
    </div>
  )
}
