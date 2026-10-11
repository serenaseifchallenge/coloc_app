import { DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { Pot } from '../../models/expense.models';
import { EurPipe } from '../../utils/eur.pipe';
import { parseIsoDate } from '../../utils/money';

@Component({
  selector: 'app-pot-detail',
  imports: [DatePipe, RoommateAvatar, EurPipe],
  templateUrl: './pot-detail.html',
})
export class PotDetail {
  readonly pot = input.required<Pot>();

  readonly back = output<void>();
  readonly edit = output<void>();
  readonly pay = output<void>();

  readonly canPay = computed(() => this.pot().participant && this.pot().remaining > 0);

  toDate(iso: string): Date {
    return parseIsoDate(iso);
  }

  percentOf(paid: number, share: number): number {
    return share > 0 ? Math.min(100, (paid / share) * 100) : 0;
  }
}
