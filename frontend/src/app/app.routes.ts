import { Routes } from '@angular/router';
import { authGuard } from './shared/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'boats',
    pathMatch: 'full'
  },
  {
    path: 'auth-callback',
    loadComponent: () =>
      import('./auth/callback/callback.component').then(m => m.AuthCallbackComponent)
  },
  {
    path: 'boats',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./boats/list/boat-list.component').then(m => m.BoatListComponent)
      },
      {
        path: 'new',
        loadComponent: () =>
          import('./boats/form/boat-form.component').then(m => m.BoatFormComponent)
      },
      {
        path: ':id',
        loadComponent: () =>
          import('./boats/detail/boat-detail.component').then(m => m.BoatDetailComponent)
      },
      {
        path: ':id/edit',
        loadComponent: () =>
          import('./boats/form/boat-form.component').then(m => m.BoatFormComponent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'boats'
  }
];
