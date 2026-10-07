import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { SharedHouseService } from '../../../../core/shared-house/shared-house.service';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { HouseIllustration } from '../../../../shared/components/house-illustration/house-illustration';

/** Page affichée quand on est connecté mais sans coloc : créer ou rejoindre. */
@Component({
  selector: 'app-onboarding-page',
  imports: [ReactiveFormsModule, HouseIllustration],
  templateUrl: './onboarding-page.html',
  styleUrl: './onboarding-page.css',
})
export class OnboardingPage {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly houses = inject(SharedHouseService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly createForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    address: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.required],
  });
  protected readonly joinForm = this.fb.group({
    invitationCode: ['', Validators.required],
  });

  protected readonly createError = signal<string | null>(null);
  protected readonly joinError = signal<string | null>(null);
  protected readonly busy = signal(false);

  protected create(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.createError.set(null);
    this.houses.create(this.createForm.getRawValue()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.router.navigateByUrl('/home'),
      error: (err) => {
        this.createError.set(apiErrorMessage(err));
        this.busy.set(false);
      },
    });
  }

  protected join(): void {
    if (this.joinForm.invalid) {
      this.joinForm.markAllAsTouched();
      return;
    }
    const code = this.joinForm.getRawValue().invitationCode.trim().toUpperCase();
    this.busy.set(true);
    this.joinError.set(null);
    this.houses.join(code).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.router.navigateByUrl('/home'),
      error: (err) => {
        this.joinError.set(apiErrorMessage(err));
        this.busy.set(false);
      },
    });
  }
}
