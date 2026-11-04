import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Link, useLocation } from 'react-router-dom'

import { askQuestion, fetchQuestions } from '../../api/questions'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatWhen } from '../../lib/dates'
import { selectUser } from '../../store/authSlice'
import { notify } from '../../store/notificationsSlice'
import Button from '../ui/Button'
import { TextAreaField } from '../ui/FormField'
import Pagination from '../ui/Pagination'

const MIN = 10
const MAX = 300

/** "Questions & answers" on the product page (after launch): ask the seller, read earlier answers. */
export default function ProductQuestions({ slug, shopName }) {
  const dispatch = useDispatch()
  const user = useSelector(selectUser)
  const location = useLocation()
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [text, setText] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const list = useAsync(() => fetchQuestions(slug, page), [slug, page, reload])

  const submit = async (e) => {
    e.preventDefault()
    const question = text.trim()
    if (question.length < MIN) {
      setError(`Please write at least ${MIN} characters.`)
      return
    }
    setBusy(true)
    setError(null)
    try {
      await askQuestion(slug, question)
      setText('')
      setPage(0)
      setReload((n) => n + 1)
      dispatch(notify(`Sent to ${shopName} - you will get a bell when they answer.`))
    } catch (err) {
      setError(describeError(err).message) // e.g. 3 questions already waiting, or a shared demo account
    } finally {
      setBusy(false)
    }
  }

  const questions = list.data
  return (
    <section id="questions" className="max-w-3xl scroll-mt-20 space-y-4">
      <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Questions &amp; answers</h2>

      {user ? (
        <form onSubmit={submit} noValidate className="space-y-2">
          <TextAreaField id="ask-question" label={`Ask ${shopName} about this product`} rows={2} maxLength={MAX} value={text}
            onChange={(e) => setText(e.target.value)} error={error}
            hint={`${text.trim().length}/${MAX} - for example: does it fit, what is in the box, how long is the warranty?`} />
          <Button type="submit" size="sm" disabled={busy}>{busy ? 'Sending…' : 'Ask the seller'}</Button>
        </form>
      ) : (
        <p className="text-sm text-slate-600 dark:text-slate-400">
          <Link to={`/login?next=${encodeURIComponent(location.pathname)}`} className="font-semibold text-brand-700 hover:underline dark:text-brand-300">Log in</Link> to ask the seller a question.
        </p>
      )}

      {list.error && <p className="text-sm text-red-600 dark:text-red-400">{describeError(list.error).message}</p>}
      {questions && questions.content.length === 0 && (
        <p className="text-sm text-slate-600 dark:text-slate-400">No questions yet - be the first to ask.</p>
      )}
      <ul className="divide-y divide-slate-200 dark:divide-slate-800">
        {(questions?.content ?? []).map((q) => (
          <li key={q.id} className="space-y-1 py-3">
            <p className="text-sm font-medium text-slate-900 dark:text-white"><span aria-hidden="true">Q: </span>{q.question}</p>
            {q.answer ? (
              <p className="whitespace-pre-line text-sm text-slate-700 dark:text-slate-300">
                <span className="font-semibold text-brand-700 dark:text-brand-300">{shopName}: </span>{q.answer}
              </p>
            ) : (
              <p className="text-sm italic text-slate-500 dark:text-slate-400">Waiting for the seller's answer</p>
            )}
            <p className="text-xs text-slate-500 dark:text-slate-400">Asked by {q.askedBy} · {formatWhen(q.createdAt)}</p>
          </li>
        ))}
      </ul>
      {questions && <Pagination page={questions.page} totalPages={questions.totalPages} onChange={setPage} />}
    </section>
  )
}
