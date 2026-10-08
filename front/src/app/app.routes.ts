import { Routes } from '@angular/router';
import { authGuard, guestGuard, hasHouseGuard, noHouseGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  // Pas connecté
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/pages/login-page/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/pages/register-page/register-page').then((m) => m.RegisterPage),
  },

  // Connecté, sans coloc
  {
    path: 'onboarding',
    canActivate: [noHouseGuard],
    loadComponent: () =>
      import('./features/shared-house/pages/onboarding-page/onboarding-page').then((m) => m.OnboardingPage),
  },

  // Connecté, avec ou sans coloc
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () => import('./features/settings/pages/settings-page/settings-page').then((m) => m.SettingsPage),
  },

  // Connecté ET dans une coloc : chacun ajoute ses pages ici
  {
    path: '',
    canActivate: [hasHouseGuard],
    children: [
      {
        path: 'home',
        loadComponent: () => import('./features/home/pages/home-page/home-page').then((m) => m.HomePage),
      },
      {
        path: 'tasks',
        loadComponent: () => import('./features/tasks/pages/tasks-page/tasks-page').then((m) => m.TasksPage),
      },
      
      { 
        path: 'shopping',
        loadComponent: () => import('./features/shopping/pages/shopping-page/shopping-page').then((m) => m.ShoppingPage),
      },


      // { path: 'expenses', loadComponent: ... },
      // { path: 'calendar', loadComponent: ... },
      // { path: 'notes', loadComponent: ... },
      {
        path: 'points',
        loadComponent: () => import('./features/points/pages/points-page/points-page').then((m) => m.PointsPage),
      },
      
      { path: '', pathMatch: 'full', redirectTo: 'home' },
    ],
  },

  { path: '**', redirectTo: '' },
];
