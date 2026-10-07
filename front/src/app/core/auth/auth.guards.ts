import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SharedHouseService } from '../shared-house/shared-house.service';
import { AuthService } from './auth.service';

// Important : tous les inject() se font AVANT le premier await.

/** Pages connexion / inscription : réservées aux personnes non connectées. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn() ? router.parseUrl('/home') : true;
};

/** Il faut être connecté (avec ou sans coloc). */
export const authGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return (await auth.loadUser()) ? true : router.parseUrl('/login');
};

/** Il faut être connecté ET avoir une coloc. */
export const hasHouseGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const houses = inject(SharedHouseService);
  const router = inject(Router);

  if (!(await auth.loadUser())) {
    return router.parseUrl('/login');
  }
  try {
    return (await houses.load()) ? true : router.parseUrl('/onboarding');
  } catch {
    return false;
  }
};

/** Il faut être connecté SANS coloc (page créer / rejoindre). */
export const noHouseGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const houses = inject(SharedHouseService);
  const router = inject(Router);

  if (!(await auth.loadUser())) {
    return router.parseUrl('/login');
  }
  try {
    return (await houses.load()) ? router.parseUrl('/home') : true;
  } catch {
    return false;
  }
};
