import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
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

  protected readonly hasHouse = computed(() => this.houses.house() !== null);

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
}
