import { Routes } from '@angular/router';

export const routes: Routes = [{
    path: 'tasks',
    loadComponent: () => import('./features/tasks/pages/tasks-page/tasks-page').then((m) => m.TasksPage),
},
{ path: '', pathMatch: 'full', redirectTo: 'tasks' },];
