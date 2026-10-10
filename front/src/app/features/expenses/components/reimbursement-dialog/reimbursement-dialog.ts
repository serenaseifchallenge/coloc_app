import { Component, DestroyRef, inject, input, OnInit, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { Modal } from '../../../../shared/components/modal/modal';
import { RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { RoommateSummary } from '../../../../shared/models/roommate-summary.model';
import { METHOD_OPTIONS, PaymentMethod } from '../../models/expense.models';
import { ExpenseService } from '../../services/expense.service';
import { maxTwoDecimals, todayIso } from '../../utils/money';

@Component({
  selector: 'app-reimbursement-dialog',
  imports: [ReactiveFormsModule, Modal, RoommateAvatar],
  templateUrl: './reimbursement-dialog.html',
})
export class ReimbursementDialog implements OnInit {
  private readonly expenseService = inject(ExpenseService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly modal = viewChild.required(Modal);

  readonly from = input.required<RoommateSummary>();
  readonly to = input.required<RoommateSummary>();
  readonly suggestedAmount = input.required<number>();
  readonly saved = output<void>();
  readonly closed = output<void>();

  readonly methods = METHOD_OPTIONS;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.formBuilder.group({
    amount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      Validators.max(100000),
      maxTwoDecimals,
    ]),
    method: this.formBuilder.control<PaymentMethod>('VIREMENT'),
    reimbursementDate: [todayIso(), Validators.required],
  });

  ngOnInit(): void {
    this.form.patchValue({ amount: this.suggestedAmount() });
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

    this.expenseService
      .createReimbursement({
        payerId: this.from().id,
        receiverId: this.to().id,
        amount: Math.round((value.amount ?? 0) * 100) / 100,
        method: value.method,
        reimbursementDate: value.reimbursementDate,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saved.emit();
          this.modal().close();
        },
        error: (error: unknown) => {
          this.errorMessage.set(apiErrorMessage(error, "Le remboursement n'a pas pu être enregistré."));
          this.submitting.set(false);
        },
      });
  }
}
