import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { Pot, PotPayment } from '../../models/expense.models';
import { PotService } from '../../services/pot.service';
import { EurPipe } from '../../utils/eur.pipe';
import { parseIsoDate } from '../../utils/money';

@Component({
  selector: 'app-pot-detail',
  imports: [DatePipe, RoommateAvatar, EurPipe],
  templateUrl: './pot-detail.html',
})
export class PotDetail {
  private readonly potService = inject(PotService);

  readonly pot = input.required<Pot>();

  readonly back = output<void>();
  readonly edit = output<void>();
  readonly pay = output<void>();
  /** La cagnotte a changé côté serveur (versement retiré, achat coché) : la page doit se recharger. */
  readonly changed = output<void>();

  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly canPay = computed(
    () => this.pot().participant && this.pot().remaining > 0 && !this.pot().purchased,
  );

  withdraw(payment: PotPayment): void {
    if (this.busy() || !confirm(`Retirer ton versement de ${payment.amount.toFixed(2).replace('.', ',')} € ?`)) {
      return;
    }
    this.run(this.potService.withdrawPayment(this.pot().id, payment.id), 'Impossible de retirer ce versement.');
  }

  setPurchased(purchased: boolean): void {
    if (this.busy()) {
      return;
    }
    this.run(this.potService.setPurchased(this.pot().id, purchased), "Impossible de mettre à jour l'achat.");
  }

  private run(request: Observable<Pot>, fallback: string, onError?: () => void): void {
    this.busy.set(true);
    this.errorMessage.set(null);
    request.subscribe({
      next: () => {
        this.busy.set(false);
        this.changed.emit();
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.errorMessage.set(apiErrorMessage(error, fallback));
        onError?.();
      },
    });
  }

  toDate(iso: string): Date {
    return parseIsoDate(iso);
  }

  percentOf(paid: number, share: number): number {
    return share > 0 ? Math.min(100, (paid / share) * 100) : 0;
  }
}
