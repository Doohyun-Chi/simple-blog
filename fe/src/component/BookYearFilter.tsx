import './BookYearFilter.css'

interface BookYearFilterProps {
  /** The years that have books, newest first. */
  years: number[]
  /** The year being shown, or null for All. */
  year: number | null
  onSelect: (year: number | null) => void
}

/** The value of the All choice, which no year can have. */
const ALL = 'all'

/** The first thing on the shelf: a combo box of All and each year that has books, the one being shown selected. */
export function BookYearFilter({ years, year, onSelect }: BookYearFilterProps) {
  const choices: { label: string; value: number | null }[] = [{ label: 'All', value: null }, ...years.map((y) => ({ label: String(y), value: y }))]
  return (
    <select
      className="book-year-filter"
      name="book-year"
      aria-label="연도"
      value={year === null ? ALL : String(year)}
      onChange={(event) => onSelect(event.target.value === ALL ? null : Number(event.target.value))}
    >
      {choices.map((choice) => (
        <option key={choice.label} value={choice.value === null ? ALL : String(choice.value)}>{choice.label}</option>
      ))}
    </select>
  )
}
