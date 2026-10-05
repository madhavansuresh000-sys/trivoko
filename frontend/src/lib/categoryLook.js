/**
 * How each top category looks: a small emoji and a soft colour (category tiles, chips and the
 * placeholder art drawn while the sample products have no photos yet).
 * Keys are the top-category slugs from the seed (Flyway V2).
 */
const LOOKS = {
  'mobiles-accessories': { emoji: '📱', short: 'Mobiles', tile: 'bg-sky-100 dark:bg-sky-950/60' },
  'laptops-computers': { emoji: '💻', short: 'Laptops', tile: 'bg-indigo-100 dark:bg-indigo-950/60' },
  'audio-wearables': { emoji: '🎧', short: 'Audio', tile: 'bg-violet-100 dark:bg-violet-950/60' },
  'mens-fashion': { emoji: '👔', short: 'Men', tile: 'bg-blue-100 dark:bg-blue-950/60' },
  'womens-fashion': { emoji: '👗', short: 'Women', tile: 'bg-pink-100 dark:bg-pink-950/60' },
  footwear: { emoji: '👟', short: 'Footwear', tile: 'bg-orange-100 dark:bg-orange-950/60' },
  'sports-fitness': { emoji: '🏏', short: 'Sports', tile: 'bg-lime-100 dark:bg-lime-950/60' },
  'home-kitchen': { emoji: '🍳', short: 'Home', tile: 'bg-amber-100 dark:bg-amber-950/60' },
  books: { emoji: '📚', short: 'Books', tile: 'bg-emerald-100 dark:bg-emerald-950/60' },
  'kids-toys': { emoji: '🧸', short: 'Kids', tile: 'bg-rose-100 dark:bg-rose-950/60' },
}

const FALLBACK = { emoji: '🛍️', short: 'Shop', tile: 'bg-slate-100 dark:bg-slate-800' }

/** The look of a category; a sub-category ("phones") uses its parent's look (needs the tree). */
export function lookOf(slug, tree = []) {
  if (LOOKS[slug]) return LOOKS[slug]
  const parent = tree.find((top) => top.children?.some((c) => c.slug === slug))
  return (parent && LOOKS[parent.slug]) || FALLBACK
}

/** The top category of a slug ("phones" -> the Mobiles & Accessories node), or null. */
export function topCategoryOf(slug, tree = []) {
  return tree.find((top) => top.slug === slug || top.children?.some((c) => c.slug === slug)) ?? null
}
