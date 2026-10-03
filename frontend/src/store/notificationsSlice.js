import { createSlice, nanoid } from '@reduxjs/toolkit'

/**
 * Small pop-up messages (toasts) at the bottom of the screen, e.g. "Ticket cancelled".
 * Any page can show one: dispatch(notify('Ticket cancelled')). <Toaster /> draws them.
 */
const notificationsSlice = createSlice({
  name: 'notifications',
  initialState: [],
  reducers: {
    notify: {
      reducer: (state, action) => {
        state.push(action.payload)
      },
      /** notify('Saved') or notify('Could not save', 'error') */
      prepare: (text, tone = 'success') => ({ payload: { id: nanoid(), text, tone } }),
    },
    dismissed: (state, action) => state.filter((n) => n.id !== action.payload),
  },
})

export const { notify, dismissed } = notificationsSlice.actions
export default notificationsSlice.reducer

export const selectNotifications = (state) => state.notifications
