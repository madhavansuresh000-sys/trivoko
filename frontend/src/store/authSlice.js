import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'

import { fetchMe, loginRequest, logoutRequest, registerRequest } from '../api/auth'
import { describeError } from '../hooks/useAsync'

/**
 * Who is logged in (copied from EventHub). The token itself is in an httpOnly cookie that JavaScript
 * cannot read, so the app asks the server "who am I?" (GET /api/auth/me) when it starts.
 *
 * user = { id, email, fullName, roles: ['CUSTOMER'|'SELLER'|'ADMIN'],
 *          seller: { id, shopName, slug, status: 'PENDING'|'APPROVED'|'REJECTED'|'BLOCKED' } | null }
 */

export const loadSession = createAsyncThunk('auth/loadSession', async (_, { rejectWithValue }) => {
  try {
    return await fetchMe()
  } catch (e) {
    return rejectWithValue(describeError(e))
  }
})

export const login = createAsyncThunk('auth/login', async (credentials, { rejectWithValue }) => {
  try {
    return await loginRequest(credentials)
  } catch (e) {
    return rejectWithValue(describeError(e))
  }
})

export const register = createAsyncThunk('auth/register', async (form, { rejectWithValue }) => {
  try {
    return await registerRequest(form)
  } catch (e) {
    return rejectWithValue(describeError(e))
  }
})

export const logout = createAsyncThunk('auth/logout', async () => {
  await logoutRequest().catch(() => {}) // even if the server is down, forget the user here
})

const authSlice = createSlice({
  name: 'auth',
  // status: 'checking' until the first /me answer, so protected pages do not send you to login too early
  initialState: { user: null, status: 'checking' },
  reducers: {
    /** A call got 401: the login ran out (8 hours). */
    sessionExpired: (state) => {
      state.user = null
    },
    /** e.g. after "Become a seller": the shop summary on /me changed. */
    userUpdated: (state, action) => {
      state.user = action.payload
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loadSession.fulfilled, (state, action) => {
        state.user = action.payload
        state.status = 'ready'
      })
      .addCase(loadSession.rejected, (state) => {
        state.user = null // server down: behave like a visitor; public pages still show their own errors
        state.status = 'ready'
      })
      .addCase(login.fulfilled, (state, action) => {
        state.user = action.payload
      })
      .addCase(register.fulfilled, (state, action) => {
        state.user = action.payload
      })
      .addCase(logout.fulfilled, (state) => {
        state.user = null
      })
  },
})

export const { sessionExpired, userUpdated } = authSlice.actions
export default authSlice.reducer

export const selectUser = (state) => state.auth.user
export const selectAuthStatus = (state) => state.auth.status

// ---- permission helpers: the SAME questions the backend asks (it always checks again) ----

export const isAdmin = (user) => Boolean(user?.roles?.includes('ADMIN'))

/** A seller whose shop is open right now (SellerAccess on the server asks the same). */
export const isActiveSeller = (user) => Boolean(user?.roles?.includes('SELLER') && user?.seller?.status === 'APPROVED')

export const firstName = (user) => user?.fullName?.split(' ')[0] ?? ''
