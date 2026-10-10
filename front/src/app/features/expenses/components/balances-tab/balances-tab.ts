import { DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { RoommateSummary } from '../../../../shared/models/roommate-summary.model';
import { Balances, methodLabel, Reimbursement, TransferView } from '../../models/expense.models';
import { EurPipe } from '../../utils/eur.pipe';
import { parseIsoDate } from '../../utils/money';

@Component({
  selector: 'app-balances-tab',
  imports: [DatePipe, RoommateAvatar, EurPipe],
  templateUrl: './balances-tab.html',
})
export class BalancesTab {
  readonly balances = input<Balances | null>(null);
  readonly reimbursements = input.required<Reimbursement[]>();
  readonly meId = input.required<number>();

  readonly reimburse = output<TransferView>();

  readonly people = computed(() => {
    const map = new Map<number, RoommateSummary>();
    for (const item of this.balances()?.balances ?? []) {
      map.set(item.roommateId, { id: item.roommateId, name: item.name, surname: item.surname });
    }
    return map;
  });

  readonly transfers = computed<TransferView[]>(() => {
    const result: TransferView[] = [];
    for (const transfer of this.balances()?.suggestedTransfers ?? []) {
      const from = this.people().get(transfer.fromId);
      const to = this.people().get(transfer.toId);
      if (from && to) {
        result.push({ from, to, amount: transfer.amount });
      }
    }
    return result;
  });

  private readonly maxAbs = computed(() =>
    Math.max(0.01, ...(this.balances()?.balances ?? []).map((item) => Math.abs(item.amount))),
  );

  readonly rows = computed(() =>
    (this.balances()?.balances ?? []).map((item) => ({
      item,
      summary: this.people().get(item.roommateId) as RoommateSummary,
      width: (Math.abs(item.amount) / this.maxAbs()) * 50,
      cents: Math.round(item.amount * 100),
    })),
  );

  involvesMe(transfer: TransferView): boolean {
    return transfer.from.id === this.meId() || transfer.to.id === this.meId();
  }

  toDate(iso: string): Date {
    return parseIsoDate(iso);
  }

  method(reimbursement: Reimbursement): string {
    return methodLabel(reimbursement.method);
  }
}
