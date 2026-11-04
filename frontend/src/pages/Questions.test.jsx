import { configureStore } from '@reduxjs/toolkit'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { answerQuestion, askQuestion, fetchQuestions, fetchSellerQuestions } from '../api/questions'
import ProductQuestions from '../components/question/ProductQuestions'
import authReducer from '../store/authSlice'
import notificationsReducer from '../store/notificationsSlice'
import SellerQuestionsPage from './seller/SellerQuestionsPage'

vi.mock('../api/questions')

/** After launch: shoppers ask on the product page, the seller answers on /seller/questions. */

const ravi = { id: 2, email: 'ravi@trivoko.test', fullName: 'Ravi Kumar', roles: ['CUSTOMER'], seller: null }
const page = (content) => ({ content, page: 0, totalPages: 1, totalElements: content.length })
const waiting = { id: 7, question: 'Does it come with a charger?', askedBy: 'Ravi K.', createdAt: '2026-11-04T10:00:00', answer: null, answeredAt: null }

function renderWith(ui, user = null) {
  const store = configureStore({
    reducer: { auth: authReducer, notifications: notificationsReducer },
    preloadedState: { auth: { user, status: 'ready' } },
  })
  render(<Provider store={store}><MemoryRouter initialEntries={['/products/volta-v12-5g']}>{ui}</MemoryRouter></Provider>)
  return userEvent.setup()
}

describe('Product questions', () => {
  beforeEach(() => vi.clearAllMocks())

  it('a guest reads the questions and is asked to log in to ask', async () => {
    vi.mocked(fetchQuestions).mockResolvedValue(page([waiting]))
    renderWith(<ProductQuestions slug="volta-v12-5g" shopName="Chennai Mobiles" />)
    expect(await screen.findByText('Does it come with a charger?')).toBeInTheDocument()
    expect(screen.getByText(/Waiting for the seller/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Log in' })).toHaveAttribute('href', '/login?next=%2Fproducts%2Fvolta-v12-5g')
  })

  it('a logged-in shopper asks; too short is stopped before the server', async () => {
    vi.mocked(fetchQuestions).mockResolvedValue(page([]))
    vi.mocked(askQuestion).mockResolvedValue(waiting)
    const user = renderWith(<ProductQuestions slug="volta-v12-5g" shopName="Chennai Mobiles" />, ravi)
    const box = await screen.findByLabelText('Ask Chennai Mobiles about this product')

    await user.type(box, 'Why?')
    await user.click(screen.getByRole('button', { name: 'Ask the seller' }))
    expect(screen.getByText(/at least 10 characters/)).toBeInTheDocument()
    expect(askQuestion).not.toHaveBeenCalled()

    await user.type(box, ' Does it come with a charger?')
    await user.click(screen.getByRole('button', { name: 'Ask the seller' }))
    await waitFor(() => expect(askQuestion).toHaveBeenCalledWith('volta-v12-5g', 'Why? Does it come with a charger?'))
    await waitFor(() => expect(fetchQuestions).toHaveBeenCalledTimes(2)) // the list is loaded again
  })

  it('the seller answers a waiting question', async () => {
    vi.mocked(fetchSellerQuestions).mockResolvedValue(page([{ ...waiting, productName: 'Volta V12 5G', productSlug: 'volta-v12-5g' }]))
    vi.mocked(answerQuestion).mockResolvedValue({ ...waiting, answer: 'Yes, a 33 W charger.' })
    const user = renderWith(<SellerQuestionsPage />)

    await user.type(await screen.findByLabelText('Your answer'), 'Yes, a 33 W charger.')
    await user.click(screen.getByRole('button', { name: 'Answer' }))
    await waitFor(() => expect(answerQuestion).toHaveBeenCalledWith(7, 'Yes, a 33 W charger.'))
    expect(fetchSellerQuestions).toHaveBeenCalledWith(false, 0)
  })
})
