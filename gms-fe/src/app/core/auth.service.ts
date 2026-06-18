import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ApiResponse } from './models';

export interface LoginResponseData {
  id: string;
  username: string;
  token: string;
  role: string;
  enabled: boolean;
  fullName: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly api = 'http://localhost:8080/api';
  private readonly TOKEN_KEY = 'gms_token';

  login(username: string, password: string): Observable<ApiResponse<LoginResponseData>> {
    return this.http
      .post<ApiResponse<LoginResponseData>>(`${this.api}/auth/login`, { username, password })
      .pipe(tap((res) => localStorage.setItem(this.TOKEN_KEY, res.data.token)));
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    if (this.isTokenExpired(token)) {
      this.logout();
      return false;
    }
    return true;
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  /** Reads the JWT `exp` claim (Unix seconds) and checks it against now. */
  private isTokenExpired(token: string): boolean {
    const exp = this.getTokenExpiry(token);
    if (exp === null) {
      // Malformed token: treat as expired so the stale value gets cleared.
      return true;
    }
    return Date.now() >= exp * 1000;
  }

  private getTokenExpiry(token: string): number | null {
    try {
      const payload = token.split('.')[1];
      if (!payload) {
        return null;
      }
      // base64url -> base64, then decode.
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const decoded = JSON.parse(atob(base64));
      return typeof decoded.exp === 'number' ? decoded.exp : null;
    } catch {
      return null;
    }
  }
}
