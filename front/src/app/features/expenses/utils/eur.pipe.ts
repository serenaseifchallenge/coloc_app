import { Pipe, PipeTransform } from '@angular/core';

const FORMAT = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });
const FORMAT_ROUND = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 2, minimumFractionDigits: 0 });

/** {{ 54.2 | eur }} -> « 54,20 € » ; {{ 10 | eur:'signed' }} -> « +10,00 € » ; {{ 340 | eur:'short' }} -> « 340 € ». */
@Pipe({ name: 'eur' })
export class EurPipe implements PipeTransform {
  transform(value: number | null | undefined, mode: '' | 'signed' | 'short' = ''): string {
    const amount = value ?? 0;
    if (mode === 'short') {
      return FORMAT_ROUND.format(amount);
    }
    if (mode === 'signed') {
      const formatted = FORMAT.format(Math.abs(amount));
      if (Math.round(amount * 100) === 0) {
        return formatted;
      }
      return `${amount > 0 ? '+' : '−'}${formatted}`;
    }
    return FORMAT.format(amount);
  }
}
