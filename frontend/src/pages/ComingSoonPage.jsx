import Button from '../components/ui/Button'
import Card from '../components/ui/Card'

/** Placeholder for a page that is not built yet. */
export default function ComingSoonPage({ title, description }) {
  return (
    <Card className="mx-auto max-w-xl p-8 text-center">
      <p className="text-sm font-semibold uppercase tracking-wide text-brand-700 dark:text-brand-400">
        Coming later
      </p>
      <h1 className="mt-2 text-2xl font-bold text-slate-900 dark:text-white">{title}</h1>
      <p className="mt-2 text-slate-600 dark:text-slate-400">{description}</p>
      <Button to="/" variant="secondary" className="mt-6">Back to home</Button>
    </Card>
  )
}
