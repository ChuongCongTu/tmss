import { Routes } from '@angular/router';
import { Login } from './features/login/login';
import { Customers } from './features/customers/customers';
import { MainLayout } from './layout/main-layout/main-layout';
import { authGuard } from './core/auth.guard';
import {Parts} from './features/part/parts';

export const routes: Routes = [
  { path: 'login', component: Login },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: 'customers', component: Customers },
      { path: 'parts', component: Parts },
      { path: '', pathMatch: 'full', redirectTo: 'customers' },
    ],
  },
  { path: '**', redirectTo: 'customers' },
];
