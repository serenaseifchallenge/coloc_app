import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { SharedHouseService } from '../../../../core/shared-house/shared-house.service';
import { RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { EditHouseDialog } from '../../components/edit-house-dialog/edit-house-dialog';
import { EditProfileDialog } from '../../components/edit-profile-dialog/edit-profile-dialog';

@Component({
  selector: 'app-settings-page',
  imports: [DatePipe, RoommateAvatar, EditProfileDialog, EditHouseDialog],
  templateUrl: './settings-page.html',
  styleUrl: './settings-page.css',
})
export class SettingsPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly houses = inject(SharedHouseService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly user = this.auth.user;
  protected readonly house = this.houses.house;

  protected readonly editing = signal<'profile' | 'house' | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly copied = signal(false);

  ngOnInit(): void {
    // Accessible avec ou sans coloc : on recharge pour avoir les membres à jour.
    this.houses.load(true).catch((err) => this.error.set(apiErrorMessage(err)));
  }

  protected async copyCode(): Promise<void> {
    const code = this.house()?.invitationCode;
    if (!code) return;
    try {
      await navigator.clipboard.writeText(code);
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    } catch {
      this.error.set('Copie impossible : sélectionnez le code et copiez-le à la main.');
    }
  }

  protected logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }

  protected deleteAccount(): void {
    if (!confirm('Supprimer définitivement votre compte ? Cette action est irréversible.')) return;
    this.auth
      .deleteAccount()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.router.navigateByUrl('/register'),
        error: (err) => this.error.set(apiErrorMessage(err)),
      });
  }

  protected leaveHouse(): void {
    const name = this.house()?.name ?? 'la colocation';
    if (!confirm(`Quitter ${name} ? Si vous êtes le dernier membre, la coloc sera supprimée.`)) return;
    this.houses
      .leave()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.router.navigateByUrl('/onboarding'),
        error: (err) => this.error.set(apiErrorMessage(err)),
      });
  }
}
