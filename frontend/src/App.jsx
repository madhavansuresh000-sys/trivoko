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
import FlashSalePage from './pages/FlashSalePage'
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
const SellerLayout = lazy(() => import('./pages/seller/SellerLayout'))
const SellerDashboardPage = lazy(() => import('./pages/seller/SellerDashboardPage'))
const SellerPackagesPage = lazy(() => import('./pages/seller/SellerPackagesPage'))
const SellerProductsPage = lazy(() => import('./pages/seller/SellerProductsPage'))
const ProductEditorPage = lazy(() => import('./pages/seller/ProductEditorPage'))
const EarningsPage = lazy(() => import('./pages/seller/EarningsPage'))
const AnalyticsPage = lazy(() => import('./pages/seller/AnalyticsPage'))
const AdminLayout = lazy(() => import('./pages/admin/AdminLayout'))
const AdminOverviewPage = lazy(() => import('./pages/admin/AdminOverviewPage'))
const AdminShopsPage = lazy(() => import('./pages/admin/AdminShopsPage'))
const AdminProductsPage = lazy(() => import('./pages/admin/AdminProductsPage'))
const AdminUsersPage = lazy(() => import('./pages/admin/AdminUsersPage'))
const AdminOrdersPage = lazy(() => import('./pages/admin/AdminOrdersPage'))
const AdminCouponsPage = lazy(() => import('./pages/admin/AdminCouponsPage'))
const AdminReportsPage = lazy(() => import('./pages/admin/AdminReportsPage'))
const AdminActivityPage = lazy(() => import('./pages/admin/AdminActivityPage'))
const AdminFlashSalesPage = lazy(() => import('./pages/admin/AdminFlashSalesPage'))
const SellerFlashPage = lazy(() => import('./pages/seller/SellerFlashPage'))

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
        <Route path="flash" element={<FlashSalePage />} />
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
          {/* the seller back office (Phase 5); SellerLayout shows "become a seller" to everyone else */}
          <Route path="seller" element={<SellerLayout />}>
            <Route index element={<SellerDashboardPage />} />
            <Route path="orders" element={<SellerPackagesPage />} />
            <Route path="products" element={<SellerProductsPage />} />
            <Route path="products/new" element={<ProductEditorPage />} />
            <Route path="products/:id" element={<ProductEditorPage />} />
            <Route path="earnings" element={<EarningsPage />} />
            <Route path="analytics" element={<AnalyticsPage />} />
            <Route path="flash" element={<SellerFlashPage />} />
          </Route>
          {/* the admin panel (Phase 6); AdminLayout shows "not for you" to everyone who is not ADMIN */}
          <Route path="admin" element={<AdminLayout />}>
            <Route index element={<AdminOverviewPage />} />
            <Route path="shops" element={<AdminShopsPage />} />
            <Route path="products" element={<AdminProductsPage />} />
            <Route path="users" element={<AdminUsersPage />} />
            <Route path="orders" element={<AdminOrdersPage />} />
            <Route path="coupons" element={<AdminCouponsPage />} />
            <Route path="reports" element={<AdminReportsPage />} />
            <Route path="flash" element={<AdminFlashSalesPage />} />
            <Route path="activity" element={<AdminActivityPage />} />
          </Route>
        </Route>
        <Route path="style-guide" element={<StyleGuidePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
