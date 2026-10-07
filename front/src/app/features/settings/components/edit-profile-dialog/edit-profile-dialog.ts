import { Component, DestroyRef, OnInit, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { Modal } from '../../../../shared/components/modal/modal';
import { Roommate } from '../../../../shared/models/roommate.model';

/** Pop-up « Editer profil » : changer d'email et/ou de mot de passe. */
@Component({
  selector: 'app-edit-profile-dialog',
  imports: [ReactiveFormsModule, Modal],
  templateUrl: './edit-profile-dialog.html',
})
export class EditProfileDialog implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly roommate = input.required<Roommate>();
  readonly closed = output<void>();

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    newPassword: ['', Validators.minLength(8)],
  });
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.form.reset({ email: this.roommate().email, newPassword: '' });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, newPassword } = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.auth
      .updateCredentials({ email, newPassword: newPassword || null })
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
