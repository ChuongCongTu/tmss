# GMS Frontend Auth Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dựng project Angular mới nhất tại `d:\tmss\gms-fe` và làm luồng đăng nhập đầu-cuối: login → JWT → localStorage → trang được bảo vệ, với HTTP interceptor gắn Bearer + auto-logout 401, và route guard.

**Architecture:** Standalone Angular (no NgModule). Functional `HttpInterceptorFn` + `CanActivateFn`. `AuthService` quản token trong localStorage. Login component gọi `POST /api/auth/login`, lưu `data.token`, điều hướng `/customers`. Customers component (được guard bảo vệ) gọi `GET /api/customers` qua interceptor.

**Tech Stack:** Angular CLI mới nhất (v20+), TypeScript, RxJS, CSS thuần. Node 24 / npm 11.

## Global Constraints

- Project tại `d:\tmss\gms-fe` (cùng cấp `d:\tmss\gms`). Tên: `gms-fe`.
- Standalone (mặc định CLI mới), routing BẬT, SSR **No**, styling CSS.
- BE base URL: `http://localhost:8080/api`. BE CORS đã cho phép origin `http://localhost:4200`.
- Login request body: `{ username, password }`. Login response: `ApiResponse` → token ở **`data.token`**.
- Login fail → **401** message mơ hồ. FE hiện `'Sai tên đăng nhập hoặc mật khẩu'`.
- localStorage key: `'gms_token'`.
- Interceptor: gắn `Authorization: Bearer <token>` cho mọi request TRỪ URL chứa `/api/auth/`. Bắt 401 từ request KHÔNG-phải-`/api/auth/` → logout + redirect `/login`.
- Seed login để test: `chuongtdq` / `123456` (role MANAGER).
- "Done" = `ng build` pass + verify trên app đang chạy (BE đã chạy + restart nếu cần) + đọc từng field response. KHÔNG báo done khi chưa chạy.

---

### Task 1: Scaffold project Angular

**Files:**
- Create: toàn bộ cây `d:\tmss\gms-fe/` (do `ng new` sinh).

**Interfaces:**
- Produces: project Angular chạy được tại `:4200`, `app.config.ts`, `app.routes.ts`, `app.ts`.

- [ ] **Step 1: Tạo project** (chạy từ `d:\tmss`, KHÔNG cd vào gms)

```powershell
npx -y @angular/cli@latest new gms-fe --routing --style=css --ssr=false --skip-git --defaults
```
Lệnh chạy trong `d:\tmss` → sinh `d:\tmss\gms-fe`. `--defaults` né prompt analytics/zoneless.

- [ ] **Step 2: Verify build**

Run: `npm --prefix d:\tmss\gms-fe run build`
Expected: build SUCCESS, sinh `dist/`. Không lỗi.

- [ ] **Step 3: Xác nhận standalone + routing**

Đọc `gms-fe/src/app/app.config.ts` (có `provideRouter`), `app.routes.ts` (mảng `routes` rỗng/mặc định). Nếu CLI sinh tên file khác (`app.component.ts` vs `app.ts`) → ghi nhận tên thực tế, các task sau dùng tên đó.

- [ ] **Step 4: Commit** (gms-fe có git riêng do `ng new`, đã `--skip-git` nên init thủ công nếu muốn — bỏ qua nếu không cần version control FE riêng)

```powershell
# Optional: nếu muốn track riêng
# git -C d:\tmss\gms-fe init; git -C d:\tmss\gms-fe add -A; git -C d:\tmss\gms-fe commit -m "chore: scaffold gms-fe"
```

---

### Task 2: AuthService

**Files:**
- Create: `gms-fe/src/app/core/auth.service.ts`

**Interfaces:**
- Consumes: `HttpClient` (provide ở Task 5; tạm thời inject sẽ lỗi runtime cho tới khi `provideHttpClient` thêm — build TS vẫn pass).
- Produces:
  - `LoginResponseData` interface `{ id: string; username: string; token: string; role: string; enabled: boolean; fullName: string; createdAt: string; updatedAt: string }`
  - `ApiResponse<T>` interface `{ status: number; message: string; data: T }`
  - `AuthService.login(username: string, password: string): Observable<ApiResponse<LoginResponseData>>`
  - `AuthService.getToken(): string | null`
  - `AuthService.isLoggedIn(): boolean`
  - `AuthService.logout(): void`

- [ ] **Step 1: Viết AuthService**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
}

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
    return !!this.getToken();
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }
}
```

- [ ] **Step 2: Verify build**

Run: `npm --prefix d:\tmss\gms-fe run build`
Expected: SUCCESS (HttpClient chưa provide nhưng compile TS ok).

---

### Task 3: authInterceptor (TRỌNG TÂM)

**Files:**
- Create: `gms-fe/src/app/core/auth.interceptor.ts`

**Interfaces:**
- Consumes: `AuthService` (Task 2: `getToken()`, `logout()`), `Router`.
- Produces: `authInterceptor: HttpInterceptorFn`.

- [ ] **Step 1: Viết interceptor**

```typescript
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
      // 401 trên API đã bảo vệ = token hết hạn/xấu → logout + về login.
      // 401 từ /api/auth/ (sai pass lúc login) để component login tự hiện message.
      if (err.status === 401 && !isAuthEndpoint) {
        auth.logout();
        router.navigateByUrl('/login');
      }
      return throwError(() => err);
    })
  );
};
```

- [ ] **Step 2: Verify build**

Run: `npm --prefix d:\tmss\gms-fe run build`
Expected: SUCCESS.

---

### Task 4: authGuard

**Files:**
- Create: `gms-fe/src/app/core/auth.guard.ts`

**Interfaces:**
- Consumes: `AuthService.isLoggedIn()`, `Router`.
- Produces: `authGuard: CanActivateFn`.

- [ ] **Step 1: Viết guard**

```typescript
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn() ? true : router.parseUrl('/login');
};
```

- [ ] **Step 2: Verify build**

Run: `npm --prefix d:\tmss\gms-fe run build`
Expected: SUCCESS.

---

### Task 5: Đăng ký providers (HttpClient + interceptor)

**Files:**
- Modify: `gms-fe/src/app/app.config.ts`

**Interfaces:**
- Consumes: `authInterceptor` (Task 3).
- Produces: `provideHttpClient(withInterceptors([authInterceptor]))` trong `appConfig.providers`.

- [ ] **Step 1: Sửa app.config.ts** (giữ nguyên `provideRouter(routes)` và các provider CLI sinh sẵn, THÊM http)

```typescript
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
```
> Nếu CLI đã sinh thêm provider (vd `provideZoneChangeDetection`, `provideBrowserGlobalErrorListeners`) thì GIỮ LẠI, chỉ chèn thêm `provideHttpClient(...)`. Đừng xóa cái có sẵn.

- [ ] **Step 2: Verify build**

Run: `npm --prefix d:\tmss\gms-fe run build`
Expected: SUCCESS.

---

### Task 6: LoginComponent

**Files:**
- Create: `gms-fe/src/app/features/login/login.ts`
- Create: `gms-fe/src/app/features/login/login.html`
- Create: `gms-fe/src/app/features/login/login.css`

**Interfaces:**
- Consumes: `AuthService.login()` (Task 2), `Router`, `FormsModule`.
- Produces: standalone component class `Login`, selector `app-login`.

- [ ] **Step 1: Viết login.ts**

```typescript
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  username = '';
  password = '';
  errorMessage = '';
  loading = false;

  onSubmit(): void {
    this.errorMessage = '';
    this.loading = true;
    this.auth.login(this.username, this.password).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/customers']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage =
          err.status === 401
            ? 'Sai tên đăng nhập hoặc mật khẩu'
            : 'Có lỗi xảy ra, thử lại sau';
      },
    });
  }
}
```

- [ ] **Step 2: Viết login.html**

```html
<div class="login-wrap">
  <h2>Đăng nhập GMS</h2>
  <form (ngSubmit)="onSubmit()">
    <label>
      Tên đăng nhập
      <input name="username" [(ngModel)]="username" required autocomplete="username" />
    </label>
    <label>
      Mật khẩu
      <input type="password" name="password" [(ngModel)]="password" required autocomplete="current-password" />
    </label>
    <button type="submit" [disabled]="loading">
      {{ loading ? 'Đang đăng nhập...' : 'Đăng nhập' }}
    </button>
    @if (errorMessage) {
      <p class="error">{{ errorMessage }}</p>
    }
  </form>
</div>
```

- [ ] **Step 3: Viết login.css**

```css
.login-wrap { max-width: 320px; margin: 80px auto; font-family: sans-serif; }
.login-wrap label { display: block; margin-bottom: 12px; }
.login-wrap input { display: block; width: 100%; padding: 8px; box-sizing: border-box; }
.login-wrap button { width: 100%; padding: 10px; cursor: pointer; }
.error { color: #c00; margin-top: 12px; }
```

- [ ] **Step 4: Verify build**

Run: `npm --prefix d:\tmss\gms-fe run build`
Expected: SUCCESS.

---

### Task 7: CustomersComponent (trang được bảo vệ)

**Files:**
- Create: `gms-fe/src/app/features/customers/customers.ts`
- Create: `gms-fe/src/app/features/customers/customers.html`
- Create: `gms-fe/src/app/features/customers/customers.css`

**Interfaces:**
- Consumes: `HttpClient`, `AuthService.logout()`, `Router`, `ApiResponse` (Task 2).
- Produces: standalone component class `Customers`, selector `app-customers`.

- [ ] **Step 1: Viết customers.ts**

```typescript
import { Component, OnInit, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService, ApiResponse } from '../../core/auth.service';

interface Customer {
  id: string;
  fullName: string;
  address: string;
  phone: string;
}

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [],
  templateUrl: './customers.html',
  styleUrl: './customers.css',
})
export class Customers implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  customers: Customer[] = [];
  error = '';

  ngOnInit(): void {
    this.http
      .get<ApiResponse<Customer[]>>('http://localhost:8080/api/customers')
      .subscribe({
        next: (res) => (this.customers = res.data ?? []),
        error: () => (this.error = 'Không tải được danh sách khách hàng'),
      });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
```

- [ ] **Step 2: Viết customers.html**

```html
<div class="customers-wrap">
  <header>
    <h2>Đã đăng nhập</h2>
    <button (click)="logout()">Đăng xuất</button>
  </header>

  @if (error) {
    <p class="error">{{ error }}</p>
  }

  <h3>Danh sách khách hàng ({{ customers.length }})</h3>
  <ul>
    @for (c of customers; track c.id) {
      <li>{{ c.fullName }} — {{ c.phone }} — {{ c.address }}</li>
    } @empty {
      <li>Chưa có khách hàng nào.</li>
    }
  </ul>
</div>
```

- [ ] **Step 3: Viết customers.css**

```css
.customers-wrap { max-width: 640px; margin: 40px auto; font-family: sans-serif; }
.customers-wrap header { display: flex; justify-content: space-between; align-items: center; }
.customers-wrap ul { padding-left: 18px; }
.customers-wrap li { margin-bottom: 6px; }
.error { color: #c00; }
```

- [ ] **Step 4: Verify build**

Run: `npm --prefix d:\tmss\gms-fe run build`
Expected: SUCCESS.

---

### Task 8: Routes

**Files:**
- Modify: `gms-fe/src/app/app.routes.ts`
- Modify: `gms-fe/src/app/app.html` (đảm bảo chỉ còn `<router-outlet />`, xóa template demo CLI)

**Interfaces:**
- Consumes: `Login` (Task 6), `Customers` (Task 7), `authGuard` (Task 4).
- Produces: routes `/login`, `/customers` (guarded), `'' → /customers`.

- [ ] **Step 1: Viết app.routes.ts**

```typescript
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
```

- [ ] **Step 2: Dọn app.html** (CLI sinh template demo dài → thay bằng outlet)

```html
<router-outlet />
```
> Nếu `app.ts` import/dùng symbol nào của template cũ (vd `RouterOutlet` đã có sẵn trong imports) thì GIỮ. Chỉ cần đảm bảo `RouterOutlet` nằm trong `imports` của root component (CLI mới đã sinh sẵn).

- [ ] **Step 3: Verify build**

Run: `npm --prefix d:\tmss\gms-fe run build`
Expected: SUCCESS.

---

### Task 9: Verify end-to-end (NON-NEGOTIABLE — chạy thật)

**Files:** none (chỉ chạy & quan sát).

**Interfaces:** Consumes toàn bộ.

- [ ] **Step 1: Đảm bảo BE chạy**

```powershell
docker --% compose -f d:\tmss\gms\docker-compose.yml up -d
# rồi chạy BE ở terminal riêng: trong d:\tmss\gms chạy mvnw.cmd spring-boot:run
```
Verify: `curl.exe -i -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"chuongtdq\",\"password\":\"123456\"}"` → 200 + `data.token` có giá trị. (Đây là xác nhận contract trước khi test FE.)

- [ ] **Step 2: Chạy FE dev server**

```powershell
npm --prefix d:\tmss\gms-fe start
```
Expected: serve tại `http://localhost:4200`.

- [ ] **Step 3: Verify từng AC trên trình duyệt (đọc kỹ, không chỉ click)**

1. Mở `http://localhost:4200` → tự redirect `/customers` → vì chưa login, guard đá về `/login`. ✔ Guard.
2. Nhập sai pass (`chuongtdq` / `sai`) → submit → hiện "Sai tên đăng nhập hoặc mật khẩu". ✔ Error 401. (DevTools Network: request `/api/auth/login` 401, KHÔNG có header Authorization.)
3. Nhập đúng (`chuongtdq` / `123456`) → redirect `/customers`, thấy "Đã đăng nhập" + danh sách. ✔ Login + token lưu. (DevTools Application → Local Storage: key `gms_token` có JWT.)
4. DevTools Network request `GET /api/customers` → có header `Authorization: Bearer <token>`. ✔ Interceptor gắn token.
5. Application → Local Storage: xóa `gms_token` thủ công → F5 `/customers` → guard đá về `/login`. ✔ isLoggedIn.
6. (Auto-logout 401) Sửa `gms_token` thành chuỗi rác → vào `/customers` → `GET /api/customers` trả 401 → interceptor logout + về `/login`. ✔ Response handling.
7. Đăng nhập lại → bấm "Đăng xuất" → về `/login`, localStorage hết token. ✔ Logout.

- [ ] **Step 4: Cập nhật PROJECT_LOG.md**

Thêm mục Ticket FE-#1 vào `d:\tmss\gms\PROJECT_LOG.md`: tóm tắt thiết kế, interceptor 2 chiều, guard, bài học. (Theo CLAUDE.md: PROJECT_LOG là bộ nhớ dự án, cập nhật khi xong ticket.)

---

## Self-Review

**Spec coverage:**
- Scaffold standalone+routing → Task 1 ✔
- Màn Login /login form + nút → Task 6, 8 ✔
- AuthService login/getToken/isLoggedIn/logout → Task 2 ✔
- Interceptor gắn Bearer trừ login → Task 3 ✔
- Trang bảo vệ /customers "Đã đăng nhập" + GET /api/customers → Task 7 ✔
- Route guard → Task 4, 8 ✔
- Lỗi login 401 → message mơ hồ → Task 6 ✔
- Auto-logout 401 (đã chốt với user) → Task 3 ✔
- "cùng cấp với BE" → Task 1 path `d:\tmss\gms-fe` ✔

**Placeholder scan:** Không có TBD/TODO; mọi step có code/lệnh thật.

**Type consistency:** `ApiResponse<T>`, `LoginResponseData`, `login(username,password)`, `getToken/isLoggedIn/logout`, `authInterceptor`, `authGuard` nhất quán giữa các task. Tên class component (`Login`, `Customers`) dùng đồng nhất ở Task 6/7/8.

**Lưu ý CLI naming:** Angular CLI v20 sinh file `app.ts`/`app.html`/`app.config.ts`/`app.routes.ts` (không hậu tố `.component`). Task 1 Step 3 yêu cầu xác nhận tên thực tế; nếu khác, dùng tên CLI sinh ra.
