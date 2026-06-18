import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  // login/register là public và chưa có token → KHÔNG gắn Bearer, KHÔNG auto-logout khi 401
  const isAuthEndpoint = req.url.includes('/api/auth/');
  const token = auth.getToken();

  const finalReq =
    !isAuthEndpoint && token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(finalReq).pipe(
    catchError((err: HttpErrorResponse) => {
      // Token hết hạn/xấu trên API đã bảo vệ → BE trả 403 (filter bỏ qua token xấu,
      // rồi authorization layer chặn) hoặc 401 → auto-logout + về login.
      // Chỉ xử lý khi có token (đã đăng nhập) và KHÔNG phải endpoint /api/auth/
      // (sai pass lúc login để component login tự hiện message, tránh redirect-loop).
      if ((err.status === 401 || err.status === 403) && !isAuthEndpoint && token) {
        auth.logout();
        router.navigateByUrl('/login');
      }
      return throwError(() => err);
    })
  );
};
