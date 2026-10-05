import { configureStore } from '@reduxjs/toolkit'

import authReducer from './authSlice'
import cartReducer from './cartSlice'
import notificationsReducer from './notificationsSlice'

/**
 * ONE store for the whole app. Each slice owns one part of the state:
 *   auth (who is logged in), cart (the last cart view from the server), notifications (toasts).
 * Later: checkout (Phase 4), seller (Phase 5), admin (Phase 6) ...
 */
export const store = configureStore({
  reducer: {
    auth: authReducer,
    cart: cartReducer,
    notifications: notificationsReducer,
  },
})
