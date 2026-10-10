import { AbstractControl, ValidationErrors } from '@angular/forms';

export function toCents(euros: number): number {
  return Math.round(euros * 100);
}

/**
 * Même règle que le back (ExpenseSplitter) : on travaille en centimes, le reste va aux
 * premiers participants triés par id croissant.
 */
export function splitEqually(amount: number, participantIds: number[]): Map<number, number> {
  const shares = new Map<number, number>();
  const ids = [...participantIds].sort((a, b) => a - b);
  if (ids.length === 0 || !Number.isFinite(amount) || amount <= 0) {
    return shares;
  }
  const cents = toCents(amount);
  const base = Math.floor(cents / ids.length);
  const remainder = cents - base * ids.length;
  ids.forEach((id, index) => shares.set(id, (base + (index < remainder ? 1 : 0)) / 100));
  return shares;
}

export function pad(value: number): string {
  return String(value).padStart(2, '0');
}

/** Date du jour au format « 2026-10-08 » (fuseau local). */
export function todayIso(): string {
  const now = new Date();
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

/** Mois au format « 2026-10 » (fuseau local). */
export function monthIso(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`;
}

export function addMonths(month: string, delta: number): string {
  const [year, m] = month.split('-').map(Number);
  return monthIso(new Date(year, m - 1 + delta, 1));
}

/** « 2026-10-08 » -> Date locale (évite le décalage UTC de new Date('2026-10-08')). */
export function parseIsoDate(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

export function maxTwoDecimals(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (value === null || value === '' || typeof value !== 'number') {
    return null;
  }
  return Math.abs(value * 100 - Math.round(value * 100)) < 1e-6 ? null : { decimals: true };
}
