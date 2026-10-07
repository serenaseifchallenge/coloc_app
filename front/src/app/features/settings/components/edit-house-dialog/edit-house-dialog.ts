import { Component, DestroyRef, OnInit, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { SharedHouseService } from '../../../../core/shared-house/shared-house.service';
import { Modal } from '../../../../shared/components/modal/modal';
import { SharedHouse } from '../../../../shared/models/shared-house.model';

/** Pop-up « Editer colocation » : nom, adresse et description. */
@Component({
  selector: 'app-edit-house-dialog',
  imports: [ReactiveFormsModule, Modal],
  templateUrl: './edit-house-dialog.html',
})
export class EditHouseDialog implements OnInit {
  private readonly houses = inject(SharedHouseService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly house = input.required<SharedHouse>();
  readonly closed = output<void>();

  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    address: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.required],
  });
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const { name, address, description } = this.house();
    this.form.reset({ name, address, description });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.houses
      .update(this.form.getRawValue())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.closed.emit(),
        error: (err) => {
          this.error.set(apiErrorMessage(err));
          this.saving.set(false);
        },
      });
  }
}
