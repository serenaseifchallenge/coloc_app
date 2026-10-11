import { Component, computed, DestroyRef, inject, input, OnInit, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { Modal } from '../../../../shared/components/modal/modal';
import { RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { RoommateSummary } from '../../../../shared/models/roommate-summary.model';
import { CATEGORY_OPTIONS, Expense, ExpenseCategory } from '../../models/expense.models';
import { ExpenseService } from '../../services/expense.service';
import { EurPipe } from '../../utils/eur.pipe';
import { maxTwoDecimals, splitEqually, todayIso } from '../../utils/money';

@Component({
  selector: 'app-expense-form-dialog',
  imports: [ReactiveFormsModule, Modal, RoommateAvatar, EurPipe],
  templateUrl: './expense-form-dialog.html',
})
export class ExpenseFormDialog implements OnInit {
  private readonly expenseService = inject(ExpenseService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly modal = viewChild.required(Modal);

  readonly expense = input<Expense | null>(null);
  readonly roommates = input.required<RoommateSummary[]>();
  readonly meId = input.required<number>();
  readonly saved = output<void>();
  readonly closed = output<void>();

  readonly categories = CATEGORY_OPTIONS;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly selected = signal<ReadonlySet<number>>(new Set());

  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(150), Validators.pattern(/\S/)]],
    amount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      Validators.max(100000),
      maxTwoDecimals,
    ]),
    expenseDate: [todayIso(), Validators.required],
    payerId: this.formBuilder.control<number | null>(null, Validators.required),
    category: this.formBuilder.control<ExpenseCategory>('COURANTE'),
  });

  private readonly amountValue = toSignal(this.form.controls.amount.valueChanges, {
    initialValue: this.form.controls.amount.value,
  });

  readonly shares = computed(() => splitEqually(this.amountValue() ?? 0, [...this.selected()]));
  readonly count = computed(() => this.selected().size);
  readonly perPerson = computed(() => {
    const values = [...this.shares().values()];
    return values.length ? Math.max(...values) : 0;
  });
  readonly allSelected = computed(() => this.selected().size === this.roommates().length);

  ngOnInit(): void {
    const expense = this.expense();
    if (expense) {
      this.form.patchValue({
        name: expense.name,
        amount: expense.amount,
        expenseDate: expense.expenseDate,
        payerId: expense.payer.id,
        category: expense.category,
      });
      this.selected.set(new Set(expense.participantIds));
    } else {
      this.form.patchValue({ payerId: this.meId() });
      this.selected.set(new Set(this.roommates().map((roommate) => roommate.id)));
    }
  }

  isMe(roommate: RoommateSummary): boolean {
    return roommate.id === this.meId();
  }

  toggle(roommateId: number): void {
    this.selected.update((current) => {
      const next = new Set(current);
      if (next.has(roommateId)) {
        next.delete(roommateId);
      } else {
        next.add(roommateId);
      }
      return next;
    });
  }

  selectAll(): void {
    this.selected.set(new Set(this.roommates().map((roommate) => roommate.id)));
  }

  hasError(controlName: 'name' | 'amount'): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && control.touched;
  }

  cancel(): void {
    this.modal().close();
  }

  submit(): void {
    if (this.form.invalid || this.selected().size === 0) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const request = {
      name: value.name.trim(),
      amount: Math.round((value.amount ?? 0) * 100) / 100,
      expenseDate: value.expenseDate,
      payerId: value.payerId as number,
      category: value.category,
      participantIds: [...this.selected()],
    };

    this.submitting.set(true);
    this.errorMessage.set(null);

    const expense = this.expense();
    const call = expense
      ? this.expenseService.updateExpense(expense.id, request)
      : this.expenseService.createExpense(request);

    call.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saved.emit();
        this.modal().close();
      },
      error: (error: unknown) => {
        this.errorMessage.set(apiErrorMessage(error, "La dépense n'a pas pu être enregistrée."));
        this.submitting.set(false);
      },
    });
  }

  remove(): void {
    const expense = this.expense();
    if (!expense || !confirm(`Supprimer la dépense « ${expense.name} » ?`)) {
      return;
    }
    this.submitting.set(true);
    this.expenseService
      .deleteExpense(expense.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saved.emit();
          this.modal().close();
        },
        error: (error: unknown) => {
          this.errorMessage.set(apiErrorMessage(error, 'Suppression impossible.'));
          this.submitting.set(false);
        },
      });
  }
}
