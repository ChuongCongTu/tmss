# GMS Frontend — Auth Flow (Ticket FE-#1)

Date: 2026-06-17
Status: Approved (design), pending implementation

## Goal

Dựng project Angular mới nhất (standalone, no NgModule) tại `d:\tmss\gms-fe` (cùng cấp với BE `d:\tmss\gms`),
làm luồng đăng nhập đầu-cuối: nhập user/pass → `POST /api/auth/login` → nhận JWT → lưu localStorage → điều
hướng vào trang trong được bảo vệ.

## Backend contract (verified từ code BE, không đoán)

- **Login request** `POST http://localhost:8080/api/auth/login`:
  ```json
  { "username": "chuongtdq", "password": "123456" }
  ```
- **Login response 200** — bọc trong `ApiResponse<T>`:
  ```json
  {
    "status": 200,
    "message": "Success",
    "data": {
      "id": "...", "username": "chuongtdq", "token": "<JWT>", "role": "MANAGER",
      "enabled": true, "fullName": "...", "createdAt": "...", "updatedAt": "..."
    }
  }
  ```
  → **Token nằm ở `data.token`.**
- **Login fail** (sai pass HOẶC user không tồn tại) → **401** với message mơ hồ chung (chống user enumeration).
- **GET /api/customers** → cần header `Authorization: Bearer <token>` (hasAnyRole 3 role). Trả `ApiResponse`
  với `data` = mảng customer `{ id, fullName, address, phone, ... }`.
- BE CORS cho phép origin `http://localhost:4200` (Angular dev mặc định) với credentials — khớp.

## Stack

- Angular CLI mới nhất (Angular 20+): standalone mặc định, `provideRouter`, `provideHttpClient`,
  functional `HttpInterceptorFn` + `CanActivateFn` (idiom hiện đại, không dùng class-based).
- SSR: **No**. Styling: CSS thuần, tối giản (không Material/Tailwind). Routing: bật.

## Cấu trúc

```
gms-fe/src/app/
├── app.config.ts          # provideRouter(routes) + provideHttpClient(withInterceptors([authInterceptor]))
├── app.routes.ts          # '' → redirect /customers; /login; /customers (canActivate authGuard)
├── app.ts                 # root component: <router-outlet>
├── core/
│   ├── auth.service.ts     # login(), isLoggedIn(), getToken(), logout()
│   ├── auth.interceptor.ts # HttpInterceptorFn
│   └── auth.guard.ts       # CanActivateFn
└── features/
    ├── login/login.ts      # form username+password
    └── customers/customers.ts
```

## Thành phần

### AuthService (`core/auth.service.ts`)
- `TOKEN_KEY = 'gms_token'`, `API = 'http://localhost:8080/api'`.
- `login(username, password): Observable<...>` → `http.post('/api/auth/login', body)`, `tap` lưu `data.token`
  vào localStorage. Trả Observable để component subscribe + xử lý lỗi.
- `getToken(): string | null` → `localStorage.getItem(TOKEN_KEY)`.
- `isLoggedIn(): boolean` → `!!getToken()`.
- `logout(): void` → `localStorage.removeItem(TOKEN_KEY)`.

### authInterceptor (`core/auth.interceptor.ts`) — TRỌNG TÂM TICKET
Functional `HttpInterceptorFn`. Hai chiều:
1. **Request:** nếu URL **không** chứa `/api/auth/` VÀ có token → `req.clone({ setHeaders: { Authorization: 'Bearer ' + token } })`.
   Login/register là public và chưa có token → KHÔNG gắn.
2. **Response:** `catchError` → nếu `err.status === 401` và request KHÔNG tới `/api/auth/` (tức token hết hạn/xấu
   trên API đã bảo vệ, không phải lỗi sai-pass lúc login) → `auth.logout()` + `router.navigateByUrl('/login')`,
   rồi `throwError`. Lỗi login 401 để component login tự xử lý (hiện message), interceptor bỏ qua.

### authGuard (`core/auth.guard.ts`)
Functional `CanActivateFn`: `inject(AuthService).isLoggedIn()` ? `true` : `inject(Router).parseUrl('/login')`.

### LoginComponent (`features/login/login.ts`)
- Template-driven hoặc reactive form: `username`, `password`, nút "Đăng nhập".
- Submit → `auth.login(...).subscribe({ next: → router.navigate(['/customers']), error: → set errorMessage })`.
- 401 → `'Sai tên đăng nhập hoặc mật khẩu'`. Lỗi khác → `'Có lỗi xảy ra, thử lại sau'`.
- Đã login sẵn (isLoggedIn) → có thể tự về /customers (nice-to-have, không bắt buộc).

### CustomersComponent (`features/customers/customers.ts`)
- Hiện text "Đã đăng nhập".
- `ngOnInit` → `http.get('/api/customers')` → render `res.data` thành list (fullName, phone, address).
- Nút "Đăng xuất" → `auth.logout()` + `router.navigate(['/login'])`.

## Luồng dữ liệu
1. Login → token → localStorage → navigate /customers.
2. Request đi qua interceptor → gắn Bearer (trừ /api/auth).
3. /customers qua authGuard → chưa login đá về /login.
4. GET /api/customers (token tự gắn) → list. 401 (token chết) → interceptor auto-logout về /login.

## Error handling
- Login 401 → message mơ hồ "Sai tên đăng nhập hoặc mật khẩu".
- API khác 401 → interceptor auto-logout.
- Lỗi mạng/500 → message chung.

## Out of scope (YAGNI)
Refresh token, SSR, Material/Tailwind, role-based UI hiding, đăng ký từ FE.

## Known trade-off (ghi để review)
localStorage đọc được bởi JS → nếu có XSS thì token lộ. Production nên httpOnly cookie. Chấp nhận cho ticket học này
vì AC yêu cầu rõ "lưu localStorage".
