import { useDispatch, useSelector } from 'react-redux'
import { useLocation, useNavigate } from 'react-router-dom'

import HeartIcon from '../ui/HeartIcon'
import { selectUser } from '../../store/authSlice'
import { notify } from '../../store/notificationsSlice'
import { selectIsSaved, toggleWishlist } from '../../store/wishlistSlice'

/**
 * The heart. A guest is sent to log in (and comes back to this page); a customer fills / empties it.
 * Price-drop alerts watch the saved products.
 */
export default function WishlistButton({ product, className = '' }) {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const user = useSelector(selectUser)
  const saved = useSelector(selectIsSaved(product.id))

  const press = async (e) => {
    e.preventDefault() // the heart sits on top of a card link: do not open the product
    if (!user) {
      navigate(`/login?next=${encodeURIComponent(location.pathname + location.search)}`)
      return
    }
    try {
      await dispatch(toggleWishlist({ productId: product.id, saved })).unwrap()
      dispatch(notify(saved ? 'Removed from your wishlist' : "Saved - we'll tell you when the price drops"))
    } catch (err) {
      dispatch(notify(err.message ?? 'Could not update your wishlist.', 'error'))
    }
  }

  return (
    <button type="button" onClick={press} aria-pressed={saved}
      aria-label={saved ? `Remove ${product.name} from wishlist` : `Save ${product.name} to wishlist`}
      className={`rounded-full bg-white/90 p-2 shadow-sm transition hover:scale-110 dark:bg-slate-900/90 ${
        saved ? 'text-rose-600' : 'text-slate-500 hover:text-rose-600 dark:text-slate-400'} ${className}`}>
      <HeartIcon filled={saved} />
    </button>
  )
}
