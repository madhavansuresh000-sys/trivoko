import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import { BoltIcon } from '../components/ui/icons'

const coming = [
  ['Phase 1', 'Catalogue', '120 products from 8 sellers'],
  ['Phase 3', 'Shop + cart', 'one cart, many sellers'],
  ['Phase 4', 'Checkout', 'pay once, split per seller'],
  ['Phase 7', 'Flash sale', 'never oversells'],
]

/** Phase 0 placeholder: shows the TriVoKo look. The real home page (deals, categories) comes in Phase 3. */
export default function HomePage() {
  return (
    <div className="space-y-10">
      <section className="overflow-hidden rounded-3xl bg-brand-800 px-6 py-12 text-white sm:px-12">
        <Badge color="accent">Coming soon</Badge>
        <h1 className="mt-4 max-w-2xl text-4xl font-extrabold tracking-tight sm:text-5xl">
          One cart. Many sellers. <span className="text-accent-300">One payment.</span>
        </h1>
        <p className="mt-4 max-w-xl text-brand-100">
          TriVoKo is a marketplace where every shop packs its own box, and you pay only once.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Button variant="deal" size="lg"><BoltIcon className="h-5 w-5" /> Flash sale at 12:00</Button>
          <Button variant="light" size="lg" to="/style-guide">See the style guide</Button>
        </div>
      </section>

      <section>
        <h2 className="text-xl font-bold text-slate-900 dark:text-white">Being built</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {coming.map(([phase, title, text]) => (
            <Card key={phase} className="p-5">
              <p className="text-xs font-semibold uppercase tracking-wide text-brand-700 dark:text-brand-400">{phase}</p>
              <p className="mt-1 font-semibold text-slate-900 dark:text-white">{title}</p>
              <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{text}</p>
            </Card>
          ))}
        </div>
      </section>
    </div>
  )
}
