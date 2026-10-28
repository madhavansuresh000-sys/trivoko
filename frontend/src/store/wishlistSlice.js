import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'

import { addToWishlist, fetchWishlistIds, removeFromWishlist } from '../api/wishlist'
import { describeError } from '../hooks/useAsync'
import { logout } from './authSlice'

/**
 * Which products are on my wishlist - only the ids, so every heart on every card knows if it is filled.
 * Loaded after login (App.jsx); a guest has none.
 */

export const loadWishlistIds = createAsyncThunk('wishlist/load', () => fetchWishlistIds())

/** Fill or empty one heart. The heart changes at once; if the server says no, it changes back. */
export const toggleWishlist = createAsyncThunk('wishlist/toggle', async ({ productId, saved }, { rejectWithValue }) => {
  try {
    await (saved ? removeFromWishlist(productId) : addToWishlist(productId))
    return { productId, saved: !saved }
  } catch (e) {
    return rejectWithValue(describeError(e))
  }
})

const wishlistSlice = createSlice({
  name: 'wishlist',
  initialState: { ids: [] },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(loadWishlistIds.fulfilled, (state, action) => {
        state.ids = action.payload
      })
      .addCase(toggleWishlist.pending, (state, action) => {
        const { productId, saved } = action.meta.arg
        state.ids = saved ? state.ids.filter((id) => id !== productId) : [...state.ids, productId]
      })
      .addCase(toggleWishlist.rejected, (state, action) => {
        const { productId, saved } = action.meta.arg // put the heart back as it was
        state.ids = saved ? [...state.ids, productId] : state.ids.filter((id) => id !== productId)
      })
      .addCase(logout.fulfilled, (state) => {
        state.ids = []
      })
  },
})

export default wishlistSlice.reducer

export const selectIsSaved = (productId) => (state) => state.wishlist.ids.includes(productId)
