import { Routes } from '@angular/router';
import { Login } from './features/login/login';
import { Customers } from './features/customers/customers';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'customers', component: Customers, canActivate: [authGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'customers' },
  { path: '**', redirectTo: 'customers' },
];
