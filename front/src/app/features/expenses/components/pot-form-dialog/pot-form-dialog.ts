import { Component, computed, DestroyRef, inject, input, OnInit, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { Modal } from '../../../../shared/components/modal/modal';
import { RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { RoommateSummary } from '../../../../shared/models/roommate-summary.model';
import { Pot, POT_TYPE_OPTIONS, PotType } from '../../models/expense.models';
import { PotService } from '../../services/pot.service';
import { EurPipe } from '../../utils/eur.pipe';
import { maxTwoDecimals, splitEqually } from '../../utils/money';

@Component({
  selector: 'app-pot-form-dialog',
  imports: [ReactiveFormsModule, Modal, RoommateAvatar, EurPipe],
  templateUrl: './pot-form-dialog.html',
})
export class PotFormDialog implements OnInit {
  private readonly potService = inject(PotService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly modal = viewChild.required(Modal);

  readonly pot = input<Pot | null>(null);
  readonly roommates = input.required<RoommateSummary[]>();
  readonly saved = output<void>();
  readonly deleted = output<void>();
  readonly closed = output<void>();

  readonly types = POT_TYPE_OPTIONS;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly selected = signal<ReadonlySet<number>>(new Set());

  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(150), Validators.pattern(/\S/)]],
    type: this.formBuilder.control<PotType>('AUTRE'),
    targetAmount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      Validators.max(100000),
      maxTwoDecimals,
    ]),
    deadline: [''],
  });

  private readonly targetValue = toSignal(this.form.controls.targetAmount.valueChanges, {
    initialValue: this.form.controls.targetAmount.value,
  });

  readonly count = computed(() => this.selected().size);
  readonly perPerson = computed(() => {
    const values = [...splitEqually(this.targetValue() ?? 0, [...this.selected()]).values()];
    return values.length ? Math.max(...values) : 0;
  });

  ngOnInit(): void {
    const pot = this.pot();
    if (pot) {
      this.form.patchValue({
        name: pot.name,
        type: pot.type,
        targetAmount: pot.targetAmount,
        deadline: pot.deadline ?? '',
      });
      this.selected.set(new Set(pot.participantIds));
    } else {
      this.selected.set(new Set(this.roommates().map((roommate) => roommate.id)));
    }
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

  hasError(controlName: 'name' | 'targetAmount'): boolean {
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
      type: value.type,
      targetAmount: Math.round((value.targetAmount ?? 0) * 100) / 100,
      deadline: value.deadline || null,
      participantIds: [...this.selected()],
    };

    this.submitting.set(true);
    this.errorMessage.set(null);

    const pot = this.pot();
    const call = pot ? this.potService.updatePot(pot.id, request) : this.potService.createPot(request);

    call.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saved.emit();
        this.modal().close();
      },
      error: (error: unknown) => {
        this.errorMessage.set(apiErrorMessage(error, "La cagnotte n'a pas pu être enregistrée."));
        this.submitting.set(false);
      },
    });
  }

  remove(): void {
    const pot = this.pot();
    if (!pot || !confirm(`Supprimer la cagnotte « ${pot.name} » et tous ses versements ?`)) {
      return;
    }
    this.submitting.set(true);
    this.potService
      .deletePot(pot.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deleted.emit();
          this.modal().close();
        },
        error: (error: unknown) => {
          this.errorMessage.set(apiErrorMessage(error, 'Suppression impossible.'));
          this.submitting.set(false);
        },
      });
  }
}
