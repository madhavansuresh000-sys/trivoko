/** Why a customer returns an item - the server's ReturnReason values with friendly labels. */
export const REASONS = [
  { value: 'DAMAGED', label: 'Arrived damaged' },
  { value: 'WRONG_ITEM', label: 'Wrong item sent' },
  { value: 'NOT_AS_DESCRIBED', label: 'Not as described' },
  { value: 'SIZE_FIT', label: 'Size or fit is wrong' },
  { value: 'CHANGED_MIND', label: 'Changed my mind' },
  { value: 'OTHER', label: 'Something else' },
]

export const reasonLabel = (value) => REASONS.find((r) => r.value === value)?.label ?? value
