import { configureStore } from '@reduxjs/toolkit'

import notificationsReducer from './notificationsSlice'

/**
 * ONE store for the whole app. Each slice owns one part of the state.
 * Phase 0: only the toasts (state.notifications).
 * Later: auth (Phase 2), cart (Phase 3), checkout (Phase 4), seller (Phase 5), admin (Phase 6) ...
 */
export const store = configureStore({
  reducer: {
    notifications: notificationsReducer,
  },
})
