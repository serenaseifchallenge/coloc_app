import { DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { BalanceItem, Balances, Expense, ExpenseSummary, Pot } from '../../models/expense.models';
import { EurPipe } from '../../utils/eur.pipe';
import { parseIsoDate } from '../../utils/money';

@Component({
  selector: 'app-expenses-tab',
  imports: [DatePipe, RoommateAvatar, EurPipe],
  templateUrl: './expenses-tab.html',
})
export class ExpensesTab {
  readonly month = input.required<string>();
  readonly summary = input<ExpenseSummary | null>(null);
  readonly expenses = input.required<Expense[]>();
  readonly balances = input<Balances | null>(null);
  readonly pots = input.required<Pot[]>();
  readonly loading = input(false);

  readonly monthChange = output<number>();
  readonly editExpense = output<Expense>();
  readonly goToBalances = output<void>();
  readonly createPot = output<void>();
  readonly openPot = output<Pot>();

  readonly monthDate = computed(() => parseIsoDate(`${this.month()}-01`));
  readonly previewPots = computed(() => this.pots().slice(0, 2));
  readonly balanceItems = computed<BalanceItem[]>(() => this.balances()?.balances ?? []);

  toDate(iso: string): Date {
    return parseIsoDate(iso);
  }

  balanceClass(amount: number): string {
    return Math.round(amount * 100) > 0 ? 'exp-positive' : Math.round(amount * 100) < 0 ? 'exp-negative' : '';
  }
}
