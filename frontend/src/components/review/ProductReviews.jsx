import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'

import { deleteMyReview, fetchMyReview, fetchReviews, saveMyReview } from '../../api/reviews'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatWhen } from '../../lib/dates'
import { selectUser } from '../../store/authSlice'
import { notify } from '../../store/notificationsSlice'
import Badge from '../ui/Badge'
import Button from '../ui/Button'
import Card from '../ui/Card'
import Pagination from '../ui/Pagination'
import { Stars } from '../ui/StarRating'
import RatingSummary from './RatingSummary'
import ReviewForm from './ReviewForm'

/** The "Reviews" part of the product page: summary, my review (if I may write one), then everyone's reviews. */
export default function ProductReviews({ slug }) {
  const dispatch = useDispatch()
  const user = useSelector(selectUser)
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [editing, setEditing] = useState(false)
  const list = useAsync(() => fetchReviews(slug, page), [slug, page, reload])
  const mine = useAsync(() => (user ? fetchMyReview(slug) : Promise.resolve(null)), [slug, user?.id, reload])

  const save = async (body) => {
    try {
      await saveMyReview(slug, body)
    } catch (e) {
      throw describeError(e) // ReviewForm shows the message
    }
    setEditing(false)
    setReload((n) => n + 1)
    dispatch(notify('Thank you - your review is on the page.'))
  }

  const remove = async () => {
    try {
      await deleteMyReview(slug)
      setReload((n) => n + 1)
      dispatch(notify('Your review was deleted.'))
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    }
  }

  const my = mine.data
  const reviews = list.data?.reviews
  return (
    <section id="reviews" className="max-w-3xl scroll-mt-20 space-y-5">
      <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Ratings &amp; reviews</h2>
      {list.data && <RatingSummary summary={list.data.summary} />}

      {my?.canReview && (
        <Card className="p-4">
          {my.review && !editing ? (
            <div className="space-y-2">
              <p className="text-sm font-medium text-slate-900 dark:text-white">Your review</p>
              <Stars value={my.review.rating} />
              <div className="flex gap-2">
                <Button size="sm" variant="secondary" onClick={() => setEditing(true)}>Edit</Button>
                <Button size="sm" variant="ghost" onClick={remove}>Delete</Button>
              </div>
            </div>
          ) : (
            <>
              <p className="mb-3 text-sm font-medium text-slate-900 dark:text-white">
                {my.review ? 'Edit your review' : 'You received this - how was it?'}
              </p>
              <ReviewForm initial={my.review} onSave={save} onCancel={my.review ? () => setEditing(false) : null} />
            </>
          )}
        </Card>
      )}

      {list.error && <p className="text-sm text-red-600 dark:text-red-400">{describeError(list.error).message}</p>}
      <ul className="divide-y divide-slate-200 dark:divide-slate-800">
        {(reviews?.content ?? []).map((r) => (
          <li key={r.id} className="space-y-1 py-4">
            <div className="flex flex-wrap items-center gap-2">
              <Stars value={r.rating} />
              {r.title && <span className="font-semibold text-slate-900 dark:text-white">{r.title}</span>}
            </div>
            <p className="whitespace-pre-line text-sm text-slate-700 dark:text-slate-300">{r.body}</p>
            <p className="flex flex-wrap items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
              {r.author} · {formatWhen(r.createdAt)}
              {r.verifiedBuyer && <Badge color="green">Verified buyer</Badge>}
            </p>
          </li>
        ))}
      </ul>
      {reviews && <Pagination page={reviews.page} totalPages={reviews.totalPages} onChange={setPage} />}
    </section>
  )
}
