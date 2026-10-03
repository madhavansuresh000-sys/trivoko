/** A white box with a soft border; dark grey in dark mode. */
export default function Card({ as: Tag = 'div', className = '', ...props }) {
  return (
    <Tag
      className={
        'rounded-2xl border border-slate-200 bg-white shadow-sm ' +
        'dark:border-slate-800 dark:bg-slate-900 ' +
        className
      }
      {...props}
    />
  )
}
