import BackendStatus from './BackendStatus'

export default function Footer() {
  return (
    <footer className="mt-16 border-t print:hidden border-slate-200 dark:border-slate-800">
      <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-8 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between dark:text-slate-400">
        <p>© 2026 TriVoKo · A portfolio project by Madhavan Suresh</p>
        <div className="flex flex-wrap items-center gap-4">
          <BackendStatus />
        </div>
      </div>
    </footer>
  )
}
