import { lazy, useEffect } from 'react'
import { useDispatch } from 'react-redux'
import { Route, Routes } from 'react-router-dom'

import RequireAuth from './components/auth/RequireAuth'
import Layout from './components/layout/Layout'
import CartPage from './pages/CartPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import ProductPage from './pages/ProductPage'
import ProductsPage from './pages/ProductsPage'
import RegisterPage from './pages/RegisterPage'
import ShopPage from './pages/ShopPage'
import { loadSession } from './store/authSlice'
import { loadCart } from './store/cartSlice'

// loaded only when someone opens them (React.lazy = code splitting)
const StyleGuidePage = lazy(() => import('./pages/StyleGuidePage'))
const BecomeSellerPage = lazy(() => import('./pages/BecomeSellerPage'))
const AddressesPage = lazy(() => import('./pages/AddressesPage'))
const CheckoutPage = lazy(() => import('./pages/CheckoutPage'))
const TestPaymentPage = lazy(() => import('./pages/TestPaymentPage'))
const PaymentResultPage = lazy(() => import('./pages/PaymentResultPage'))
const OrdersPage = lazy(() => import('./pages/OrdersPage'))
const OrderDetailPage = lazy(() => import('./pages/OrderDetailPage'))

export default function App() {
  const dispatch = useDispatch()

  // on start: "who am I?" (the cookie decides), then the cart - saved (logged in) or from this browser (guest)
  useEffect(() => {
    dispatch(loadSession()).then(() => dispatch(loadCart()))
  }, [dispatch])

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="products" element={<ProductsPage />} />
        <Route path="products/:slug" element={<ProductPage />} />
        <Route path="shops/:slug" element={<ShopPage />} />
        <Route path="cart" element={<CartPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        {/* logged-in pages: a visitor is sent to /login?next=... and comes back here */}
        <Route element={<RequireAuth />}>
          <Route path="sell" element={<BecomeSellerPage />} />
          <Route path="account/addresses" element={<AddressesPage />} />
          <Route path="checkout" element={<CheckoutPage />} />
          <Route path="test-payment/:session" element={<TestPaymentPage />} />
          <Route path="payment/success" element={<PaymentResultPage />} />
          <Route path="payment/cancelled" element={<PaymentResultPage cancelled />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route path="orders/:number" element={<OrderDetailPage />} />
        </Route>
        <Route path="style-guide" element={<StyleGuidePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
