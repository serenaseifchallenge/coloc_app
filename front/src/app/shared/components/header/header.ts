import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../core/http/api-error';
import { SharedHouseService } from '../../../core/shared-house/shared-house.service';
import { Logo } from '../logo/logo';
import { RoommateAvatar } from '../roommate-avatar/roommate-avatar';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive, Logo, RoommateAvatar],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  protected readonly auth = inject(AuthService);
  private readonly houses = inject(SharedHouseService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly hasHouse = computed(() => this.houses.house() !== null);

  /** Message d'erreur affiché sous le header (ex. solde pas à 0 pour supprimer le compte). */
  protected readonly error = signal<string | null>(null);

  // Chaque membre du groupe crée la route correspondante dans app.routes.ts
  protected readonly links = [
    { path: '/home', label: 'Accueil' },
    { path: '/tasks', label: 'Tâches' },
    { path: '/expenses', label: 'Dépenses' },
    { path: '/shopping', label: 'Courses' },
    { path: '/calendar', label: 'Calendrier' },
    { path: '/notes', label: 'Notes' },
    { path: '/points', label: 'Points' },
  ];

  protected logout(): void {
    this.closeMenu();
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }

  protected deleteAccount(): void {
    this.closeMenu();
    if (!confirm('Supprimer définitivement votre compte ? Cette action est irréversible.')) return;
    this.error.set(null);
    this.auth
      .deleteAccount()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.router.navigateByUrl('/register'),
        error: (err) => this.error.set(apiErrorMessage(err)),
      });
  }

  /** Après un clic dans le menu, on retire le focus pour qu'il se referme (il reste ouvert tant qu'il a le focus). */
  protected closeMenu(): void {
    (document.activeElement as HTMLElement | null)?.blur();
  }
}
