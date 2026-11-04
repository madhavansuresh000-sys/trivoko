import { useState } from 'react'
import { useDispatch } from 'react-redux'
import { Link } from 'react-router-dom'

import { answerQuestion, fetchSellerQuestions } from '../../api/questions'
import { StatusTabs } from '../../components/admin/AdminBits'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import EmptyState from '../../components/ui/EmptyState'
import { TextAreaField } from '../../components/ui/FormField'
import { Skeleton } from '../../components/ui/Loader'
import Pagination from '../../components/ui/Pagination'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatWhen } from '../../lib/dates'
import { notify } from '../../store/notificationsSlice'

const TABS = [
  { value: 'waiting', label: 'To answer' },
  { value: 'answered', label: 'Answered' },
]

/** One question with its answer box. Answering again corrects the answer (the shopper is told only once). */
function QuestionCard({ q, onSaved }) {
  const dispatch = useDispatch()
  const [text, setText] = useState(q.answer ?? '')
  const [editing, setEditing] = useState(!q.answer)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const save = async (e) => {
    e.preventDefault()
    if (text.trim().length < 2) {
      setError('Please write an answer.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      await answerQuestion(q.id, text.trim())
      dispatch(notify(q.answer ? 'Answer updated.' : 'Answered - the shopper gets a bell and an email.'))
      onSaved()
    } catch (err) {
      setError(describeError(err).message)
      setBusy(false)
    }
  }

  return (
    <Card className="space-y-3 p-4">
      <div className="space-y-1">
        <Link to={`/products/${q.productSlug}#questions`} className="text-xs font-semibold text-brand-700 hover:underline dark:text-brand-300">{q.productName}</Link>
        <p className="font-medium text-slate-900 dark:text-white">{q.question}</p>
        <p className="text-xs text-slate-500 dark:text-slate-400">{q.askedBy} · {formatWhen(q.createdAt)}</p>
      </div>
      {editing ? (
        <form onSubmit={save} noValidate className="space-y-2">
          <TextAreaField id={`answer-${q.id}`} label="Your answer" rows={3} maxLength={1000} value={text} error={error}
            onChange={(e) => setText(e.target.value)} />
          <div className="flex gap-2">
            <Button type="submit" size="sm" disabled={busy}>{busy ? 'Saving…' : q.answer ? 'Save answer' : 'Answer'}</Button>
            {q.answer && <Button size="sm" variant="ghost" onClick={() => setEditing(false)}>Cancel</Button>}
          </div>
        </form>
      ) : (
        <div className="space-y-2">
          <p className="whitespace-pre-line text-sm text-slate-700 dark:text-slate-300">{q.answer}</p>
          <Button size="sm" variant="secondary" onClick={() => setEditing(true)}>Edit answer</Button>
        </div>
      )}
    </Card>
  )
}

/** The seller's questions (after launch): shoppers' questions about my products, oldest unanswered first. */
export default function SellerQuestionsPage() {
  const [tab, setTab] = useState('waiting')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const { data, loading, error } = useAsync(() => fetchSellerQuestions(tab === 'answered', page), [tab, page, reload])

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Questions</h1>
        <p className="text-sm text-slate-600 dark:text-slate-400">Shoppers ask before they buy - a quick answer often means a sale.</p>
      </div>
      <StatusTabs tabs={TABS} value={tab} onChange={(v) => { setTab(v); setPage(0) }} label="Questions" />
      {error && <p role="alert" className="text-sm text-red-600 dark:text-red-400">{describeError(error).message}</p>}
      {loading && !data && <Skeleton className="h-32 w-full" />}
      {data && data.content.length === 0 && (
        <EmptyState title={tab === 'waiting' ? 'No questions waiting' : 'No answered questions yet'}
          message={tab === 'waiting' ? 'When a shopper asks about one of your products, it appears here.' : null} />
      )}
      <div className="space-y-3">
        {(data?.content ?? []).map((q) => (
          <QuestionCard key={`${q.id}-${q.answeredAt ?? ''}`} q={q} onSaved={() => setReload((n) => n + 1)} />
        ))}
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  )
}
