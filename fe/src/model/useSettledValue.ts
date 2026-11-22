import { useEffect, useState } from 'react'

/** The value once it has stayed the same for `delayMillis`, so work on it is not redone on every keystroke. */
export function useSettledValue<T>(value: T, delayMillis: number): T {
  const [settled, updateSettled] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => updateSettled(value), delayMillis)
    return () => clearTimeout(timer)
  }, [value, delayMillis])
  return settled
}
