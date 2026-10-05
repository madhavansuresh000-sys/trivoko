import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'

import { completeTestPayment, fetchTestPayment } from '../api/orders'
import { Notice } from '../components/auth/AuthCard'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { Spinner } from '../components/ui/Loader'
import useAsync, { describeError } from '../hooks/useAsync'
import { formatRupees } from '../lib/money'

/**
 * The built-in TEST payment page (development and demo, when there is no Stripe key). "Pay" runs the same
 * server code as a real Stripe webhook. With Stripe keys, customers go to Stripe's own page instead.
 */
export default function TestPaymentPage() {
  const { session } = useParams()
  const navigate = useNavigate()
  const { data, loading, error } = useAsync(() => fetchTestPayment(session), [session])
  const [paying, setPaying] = useState(false)
  const [payError, setPayError] = useState(null)

  if (loading) return <Spinner label="Opening the payment page" />
  if (error) return <EmptyState title="This payment page does not exist" message={describeError(error).message} action={<Button to="/orders">My orders</Button>} />

  const pay = async () => {
    setPaying(true)
    try {
      const order = await completeTestPayment(session)
      navigate(`/payment/success?order=${order.number}`, { replace: true })
    } catch (e) {
      setPayError(describeError(e).message)
      setPaying(false)
    }
  }

  return (
    <div className="mx-auto max-w-md">
      <Card className="space-y-5 p-6 text-center">
        <Badge color="amber">TEST PAYMENT - no real money</Badge>
        <div>
          <p className="text-sm text-slate-600 dark:text-slate-400">Order {data.orderNumber}</p>
          <p className="mt-1 text-4xl font-extrabold text-slate-900 dark:text-white">{formatRupees(data.amount)}</p>
        </div>
        {data.status !== 'CREATED' ? (
          <Notice tone={data.status === 'PAID' ? 'success' : 'error'}>
            {data.status === 'PAID' ? 'This order is already paid.' : 'This payment page has closed (the 10 minutes are over).'}
          </Notice>
        ) : (
          <>
            {payError && <Notice tone="error">{payError}</Notice>}
            <Button size="lg" className="w-full" disabled={paying} onClick={pay}>{paying ? 'Paying…' : `Pay ${formatRupees(data.amount)}`}</Button>
            <Button variant="ghost" className="w-full" onClick={() => navigate(`/payment/cancelled?order=${data.orderNumber}`)}>Cancel</Button>
          </>
        )}
        <p className="text-xs text-slate-500 dark:text-slate-400">
          This page stands in for Stripe while the shop runs without payment keys.
        </p>
      </Card>
    </div>
  )
}
