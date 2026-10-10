import { Component, computed, DestroyRef, inject, input, OnInit, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { Modal } from '../../../../shared/components/modal/modal';
import { METHOD_OPTIONS, PaymentMethod, Pot } from '../../models/expense.models';
import { PotService } from '../../services/pot.service';
import { EurPipe } from '../../utils/eur.pipe';
import { maxTwoDecimals, todayIso, toCents } from '../../utils/money';

@Component({
  selector: 'app-pot-payment-dialog',
  imports: [ReactiveFormsModule, Modal, EurPipe],
  templateUrl: './pot-payment-dialog.html',
})
export class PotPaymentDialog implements OnInit {
  private readonly potService = inject(PotService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly modal = viewChild.required(Modal);

  readonly pot = input.required<Pot>();
  readonly saved = output<void>();
  readonly closed = output<void>();

  readonly methods = METHOD_OPTIONS;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.formBuilder.group({
    amount: this.formBuilder.control<number | null>(null),
    method: this.formBuilder.control<PaymentMethod>('VIREMENT'),
    paymentDate: [todayIso(), Validators.required],
  });

  private readonly amountValue = toSignal(this.form.controls.amount.valueChanges, { initialValue: null });

  readonly myShare = computed(() => this.pot().myShare ?? 0);
  readonly myRemaining = computed(() => Math.max(0, this.myShare() - this.pot().myPaid));
  readonly myPercent = computed(() =>
    this.myShare() > 0 ? Math.min(100, (this.pot().myPaid / this.myShare()) * 100) : 0,
  );
  readonly collectedAfter = computed(() => this.pot().collected + (this.amountValue() ?? 0));
  readonly myCompleteAfter = computed(() => this.pot().myPaid + (this.amountValue() ?? 0) >= this.myShare());

  ngOnInit(): void {
    const pot = this.pot();
    const suggested = Math.min(this.myRemaining(), pot.remaining);
    const max = pot.remaining;
    this.form.controls.amount.setValidators([
      Validators.required,
      Validators.min(0.01),
      Validators.max(max),
      maxTwoDecimals,
    ]);
    this.form.controls.amount.setValue(suggested > 0 ? toCents(suggested) / 100 : null);
  }

  cancel(): void {
    this.modal().close();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitting.set(true);
    this.errorMessage.set(null);

    this.potService
      .addPayment(this.pot().id, {
        amount: Math.round((value.amount ?? 0) * 100) / 100,
        method: value.method,
        paymentDate: value.paymentDate,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saved.emit();
          this.modal().close();
        },
        error: (error: unknown) => {
          this.errorMessage.set(apiErrorMessage(error, "Le versement n'a pas pu être enregistré."));
          this.submitting.set(false);
        },
      });
  }
}
