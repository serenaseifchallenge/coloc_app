import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { AuthWaves } from '../../components/auth-waves/auth-waves';
import { HouseIllustration } from '../../../../shared/components/house-illustration/house-illustration';

type RegisterField = 'email' | 'name' | 'surname' | 'birthday' | 'password';

/** Vérifie que les deux mots de passe sont identiques. */
function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirm')?.value;
  return password && confirm && password !== confirm ? { passwordsMismatch: true } : null;
}

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, AuthWaves, HouseIllustration],
  templateUrl: './register-page.html',
  styleUrl: './register-page.css',
})
export class RegisterPage {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly form = this.fb.group(
    {
      email: ['', [Validators.required, Validators.email]],
      name: ['', [Validators.required, Validators.maxLength(100)]],
      surname: ['', [Validators.required, Validators.maxLength(100)]],
      birthday: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirm: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  protected invalid(name: RegisterField): boolean {
    const control = this.form.controls[name];
    return control.touched && control.invalid;
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { confirm: _confirm, ...body } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.auth.register(body).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.router.navigateByUrl('/onboarding'),
      error: (err) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
