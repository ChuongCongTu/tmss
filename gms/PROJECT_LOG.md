# PROJECT LOG — Garage Management System (GMS)

> File này là "bộ nhớ" của dự án. Bất kỳ AI agent hay member nào mở dự án lên đọc file này
> là tiếp tục được công việc, không phụ thuộc vào tab chat nào.
>
> **Cách làm việc (mentor mode):** Lead giao ticket + acceptance criteria (nói CẦN ĐẠT GÌ, không
> nói CODE THẾ NÀO). Member tự code, lead review kiểu PR (không viết code hộ), quiz câu hỏi
> phỏng vấn ẩn sau mỗi ticket. Chỉ chuyển ticket khi member thực sự hiểu. Khi member bí → gợi mở
> kiểu Socratic, tăng dần độ rõ của hint, không quăng đáp án; tối đa snippet 2-3 dòng minh hoạ.

---

## Thông tin member (để giao ticket đúng tầm)
- 5 năm Angular 9 + Java Spring Boot, chủ yếu ĐỌC code & FIX bug — chưa build hệ thống từ đầu.
- Yếu nhất: (1) Core Java & concurrency, (2) JPA/Hibernate & DB, (3) Security & testing, (4) Kiến trúc & thiết kế.
- Mục tiêu: build end-to-end, đủ tự tin phỏng vấn BE 3-5 năm.
- Biết: tạo project qua Spring Initializr, tự viết @Entity/@Repository (mức "sơ qua").
- Chưa rành: Docker compose (chỉ copy), thiết kế DB (cơ bản), Flyway (lần đầu dùng ở dự án này).

## Stack & quyết định kiến trúc đã chốt
- Spring Boot 3.5.x, Java 17, Maven, PostgreSQL 16, Docker.
- **Monolith, layered architecture.** KHÔNG microservices. Tránh over-engineer.
- **Package by feature** (mỗi nghiệp vụ gom controller/service/repository/entity cùng chỗ),
  tạo package DẦN khi cần — không dựng sẵn 6 feature khi chưa code.
- **Flyway là single source of truth cho schema.** Hibernate `ddl-auto: validate` (chỉ kiểm tra,
  không sửa schema). Migration đã chạy = BẤT BIẾN, đổi gì thì tạo file V mới (không sửa file cũ —
  Flyway lưu checksum, sửa file cũ → checksum mismatch → app fail-fast; cứu bằng `flyway repair`).
- Khóa chính: **UUID** (gen_random_uuid). Chấp nhận đánh đổi: insert/index chậm hơn BIGSERIAL
  (UUIDv4 random gây phân mảnh B-tree), bù lại không lộ thông tin & merge-DB an toàn.
- **Tách bạch `customer` (dữ liệu nghiệp vụ, KHÔNG đăng nhập) vs `user/account` (người thao tác
  hệ thống — nhân viên gara, có password/role).** Customer không có password.

## Cấu trúc hiện tại
```
d:/tmss/gms/
├── docker-compose.yml          # postgres:16-alpine (named volume postgres_data) + adminer:8081
├── pom.xml                     # web, data-jpa, actuator, flyway-core + flyway-database-postgresql, postgresql
├── src/main/resources/
│   ├── application.yaml         # datasource gms_db, ddl-auto=validate, flyway enabled, port 8080
│   └── db/migration/V1__init.sql  # CREATE TABLE customers(id uuid pk, full_name, address, phone, created_at tstz)
└── src/main/java/gms/example/gms/GmsApplication.java
```
Kết nối DB: `jdbc:postgresql://localhost:5432/gms_db`, user `postgres` / pass `postgres123`.
Chạy: `docker-compose up -d` rồi `./mvnw spring-boot:run`. Health: localhost:8080/actuator/health.
Xem DB: Adminer localhost:8081.

---

## TIẾN ĐỘ TICKET

### ✅ Ticket #1 — Project Skeleton & DB Connectivity — DONE (2026-06-09)
Dựng khung dự án + kết nối Postgres qua Docker + Flyway migration đầu tiên.
- Verified: actuator/health = UP; bảng `customers` + `flyway_schema_history` tồn tại; V1 success.
- Bug member tự sửa trong quá trình: `TIMESTAMPZ`→`TIMESTAMPTZ`, dấu phẩy thừa cuối CREATE TABLE.
- Bài học member đã nắm (quiz pass): Flyway checksum & immutability, ddl-auto=validate vs update/none
  (single source of truth), UUID vs BIGSERIAL trade-offs (kể cả phân mảnh B-tree của UUIDv4).
- **Nhắc:** member 2 lần liên tiếp có lỗi syntax SQL → cần rèn thói quen đọc lại SQL trước khi chạy.

### ✅ Ticket #2 — Customer & Vehicle — DONE (2026-06-09)
Quan hệ 1 customer - N vehicle (UNIDIRECTIONAL: chỉ Vehicle giữ @ManyToOne→Customer,
Customer KHÔNG có List<Vehicle>; lấy xe qua VehicleRepository.findByCustomerId).
API mục tiêu: POST/GET /api/customers, GET /api/customers/{id}, POST+GET /api/customers/{id}/vehicles.

**Đã xong & verified (phần Entity + Schema):**
- Entity `Customer` (bảng customers), `Vehicle` (bảng vehicles). Class PascalCase.
- V2 tạo vehicles: FK customer_id→customers (NOT NULL), plate_no VARCHAR(20) UNIQUE NOT NULL,
  brand/model/color VARCHAR(255), year INTEGER, timestamps TIMESTAMPTZ, + INDEX idx_vehicles_customer_id.
- V3: ALTER customers ADD updated_at (file mới, không sửa V1).
- Vehicle.customer = @ManyToOne(fetch=LAZY) + @JoinColumn(customer_id, nullable=false).
- Timestamp dùng `Instant` (KHÔNG LocalDateTime) để khớp TIMESTAMPTZ — tránh lỗi mất timezone âm thầm.
- Verified: Flyway chạy đủ V1,V2,V3 success; vehicles tồn tại đủ index/constraint; app validate PASS, health UP.

**Quyết định thiết kế lớp trên (đã chốt, chưa code):**
- DTO tách input/output (CreateXxxRequest vs XxxResponse). KHÔNG trả Entity ra API.
- VehicleResponse chỉ chứa `customerId` (KHÔNG nhét nguyên Customer → tránh Lazy/vòng lặp Jackson).
- Not-found → throw exception + map sang HTTP 404 (qua @ResponseStatus hoặc @RestControllerAdvice;
  throw KHÔNG tự thành 404, mặc định là 500).

**Đã code & verified (5 API, end-to-end PASS):**
- common/: ApiResponse<T> (status/message/data + success()/created()/error()),
  exception/{ResourceNotFoundException→404, BusinessException→400, GlobalExceptionHandler @RestControllerAdvice
  + handle MethodArgumentNotValidException→400}.
- DTO: CreateCustomerRequest(@NotBlank fullName), CustomerResponse(có id!), CreateVehicleRequest, VehicleResponse(có id+plateNo+customerId).
- Repository: CustomerRepository (trống, dùng sẵn JpaRepository), VehicleRepository
  (existsByPlateNo, findAllByCustomer). DI bằng constructor (@RequiredArgsConstructor + final).
- Service @Transactional, orElseThrow(ResourceNotFoundException) cho not-found, BusinessException khi trùng biển.
- Controller: POST→created()/201, GET→success()/200 (đã sửa lỗi đảo nhau).
- Verified curl: tạo customer→lấy id→thêm xe→list đúng→trùng biển 400→id bậy 404. plateNo/customerId/id đều có giá trị.

**Bài học member nắm qua quiz #2:** N+1 query (LAZY GÂY ra N+1 khi getCustomer() trong vòng lặp, KHÔNG
chữa; fix bằng JOIN FETCH/@EntityGraph — chưa code, để dành ticket có query phức tạp); @Transactional(readOnly=true)
tắt dirty checking; trùng lặp HTTP status ở header+body = vi phạm single source of truth (cân nhắc bỏ field status khỏi body);
spring-boot-devtools để auto-restart khi sửa code (NÊN THÊM vào pom — member chưa thêm).

**⚠️ BÀI HỌC HÀNH VI — QUAN TRỌNG NHẤT, NHẮC LẠI MỖI TICKET:**
Member ~8 lần liên tiếp báo "done/xong/thành công/test đầy đủ" khi thực tế CHƯA đạt:
sửa nhầm file V1 đã-chạy (checksum mismatch); thêm field entity quên cột SQL; chưa reset DB đã báo xong;
mapper thiếu .id()/.plateNo() → response null mà vẫn báo xong; SỬA CODE NHƯNG KHÔNG RESTART APP →
test thấy hành vi cũ (cả lead lẫn member đều dính bẫy này → LUÔN verify trên app ĐÃ restart, check health trước).
Gốc rễ: member CHẠY LỆNH MÀ KHÔNG ĐỌC KỸ KẾT QUẢ từng field so với acceptance.
→ Code member khá (kiến trúc/exception/layering tốt), nhưng KỶ LUẬT VERIFY là điểm yếu lớn nhất, cần rèn liên tục.
→ Lead PHẢI tự verify bằng DB/curl/log thật trên app đã restart, KHÔNG tin báo cáo suông. Khi giao việc
nhắc member: "done = đã chạy trên app restart + tự đọc từng field kết quả khớp acceptance".
Cũng hay lặp lỗi syntax SQL (dấu phẩy thừa 3 lần) → rèn đọc SQL như compiler.

### ✅ Ticket #3 — Parts & Inventory — DONE (2026-06-10)
Module kho — trọng tâm CONCURRENCY (mảng member yếu nhất).
- V4: bảng `parts` (id, part_no UNIQUE NOT NULL, part_name, price NUMERIC(15,2), quantity INTEGER NOT NULL, timestamps).
- V5: bảng `stock_adjustments` (LEDGER bất biến: id, part_id FK, delta INTEGER, reason, created_at — KHÔNG updated_at).
- V6: drop check cũ, add CHECK (quantity >= 0). (V4 từng lỡ ra `> 0` — sai vì tồn=0 hợp lệ.)
- Package `part/` riêng: entity Part/StockAdjustment, repo, dto, service, controller.
- API: POST /api/parts, GET /api/parts/{id}, GET /api/parts, POST /api/parts/{id}/stock-adjustments (body: delta +/-, reason).
- **CONCURRENCY (cốt lõi):** trừ/cộng kho bằng ATOMIC UPDATE qua @Modifying @Query, KHÔNG đọc-sửa-ghi ở Java.
  - decreaseStock: `UPDATE Part SET quantity = quantity - :qty WHERE id=:id AND quantity >= :qty`, nhận qty DƯƠNG, trả int rows affected.
  - increaseStock: `quantity + :qty` (atomic, tránh lost update kể cả khi cộng).
  - Service @Transactional: delta>0→increase, delta<0→decrease(abs). rows==0 → BusinessException 400, KHÔNG insert ledger.
    rows==1 → insert stock_adjustments (cùng transaction → 2 nguồn quantity & ledger luôn đồng bộ).
  - Response quantity: đã sửa bug lệch-một-nhịp (object cũ trong RAM không phản ánh @Modifying do L1 cache).
- Verified curl + DB: ton 10 → xuat 3 = 7 → xuat 10 = 400 từ chối → nhap 5 = 12; response khớp DB; ledger chỉ 2 dòng (-3,+5).

**Quyết định thiết kế đã chốt (member giải thích được):** 3 hướng chống race khi trừ kho — pessimistic lock (FOR UPDATE),
optimistic lock (version, TỆ khi tranh chấp nhiều), atomic UPDATE (CHỌN: thao tác đủ đơn giản gói 1 câu nguyên tử,
ít round-trip, không giữ lock lâu). quantity = stored-state (cột) thay vì derived (SUM ledger): đọc nhanh + áp được
CHECK>=0 & atomic check. price=BigDecimal/NUMERIC (KHÔNG double). quantity=INTEGER (đếm cái, không lẻ).

**Bài học member nắm (quiz #3):** @Transactional KHÔNG tự chống lost update — cần atomic update/lock;
@Modifying bypass L1 cache → entity load trước bị "lỗi thời"; rows affected ≠ quantity (=0 nghĩa "không dòng nào
thỏa WHERE" = không đủ tồn, KHÔNG phải tồn=0); ledger chỉ ghi sự kiện ĐÃ xảy ra (audit).

**Tiến bộ hành vi:** cuối ticket #3 member ĐÃ tự sửa & test lại đúng (response khớp DB) — nhưng GIỮA ticket vẫn
nhiều lần báo "test đầy đủ" khi response trả quantity sai rõ ràng (lệch một nhịp) → vẫn chạy lệnh không đọc kết quả.
Còn lặp lỗi chính tả schema (detal/ajust). Tiếp tục rèn kỷ luật verify + đọc kết quả.

### ✅ Ticket #4 — Repair Order — DONE (2026-06-12)
Phiếu sửa chữa — quan hệ nhiều bảng + transaction nhiều bước + snapshot pattern.
- V6: `repair_orders` (id, vehicle_id FK NOT NULL, status, labor_cost/total_amount NUMERIC(12,2), timestamps, INDEX vehicle_id)
  + `repair_order_items` (id, repair_order_id FK, part_id FK, unit_price, quantity INTEGER CHECK>0, INDEX repair_order_id).
- Package: vẫn để trong `part/` (LƯU Ý: nên tách package `repairorder/` riêng cho sạch — nợ kỹ thuật nhỏ).
- Quan hệ: RepairOrder @ManyToOne→Vehicle (LAZY); RepairOrder @OneToMany→items HAI CHIỀU
  (mappedBy, cascade=ALL, orphanRemoval=true, helper addItem() đồng bộ 2 đầu); RepairOrderItem @ManyToOne→Part (LAZY).
- API: POST /api/repair-orders, GET /api/repair-orders/{id}, GET /api/vehicles/{vehicleId}/repair-orders.
  Request DTO {vehicleId, laborCost, status, items:[{partId, quantity}]} — DTO con, KHÔNG nhận Entity.
- **Logic tạo phiếu (1 @Transactional):** check vehicle (404) → vòng lặp item: check part (404),
  decreaseStock (tái dùng ticket #3) → rows==0 throw BusinessException → ROLLBACK TẤT CẢ (all-or-nothing),
  SNAPSHOT unit_price = part.price lúc lập (KHÔNG join live), cộng dồn total → total_amount = total + labor → LƯU cột.
- Verified DB+curl: A×3 B×4 → kho 7/6, unitPrice snapshot đúng, total 700k đúng; phiếu vượt tồn → 400,
  kho KHÔNG đổi + phiếu KHÔNG lưu (rollback đúng cả kho lẫn phiếu); đổi field camelCase total vẫn 210k đúng.

**Quyết định thiết kế (member tự bảo vệ được):** total_amount = stored (tính 1 lần lúc lập rồi LƯU) vì là dữ liệu
LỊCH SỬ BẤT BIẾN, phải chính xác kể cả khi part gốc đổi/xóa — KHÁC quantity ticket#3 (cũng stored nhưng vì lý do
biến động liên tục + áp CHECK). Snapshot giá = cùng họ ledger: ghi sự thật tại thời điểm, bất biến với tương lai.

**Bài học member nắm (quiz #4):** @Transactional chỉ rollback với RuntimeException/Error (KHÔNG checked exception)
→ BusinessException extends RuntimeException nên rollback chạy; N+1 ở toResponse (getItems LAZY + mỗi item getPart()
LAZY trong vòng lặp = 1+1+N query; getRepairOrderByVehicle còn N+1 lồng nhau) → fix bằng JOIN FETCH/@EntityGraph
(CHƯA fix — chấp nhận vì data nhỏ, "đo trước khi tối ưu", nhưng PHẢI biết để fix khi cần).
Bài học điều tra sự cố: count=2 phiếu hóa ra do DB BẨN từ test trước, KHÔNG phải bug → nhìn dữ liệu trước khi đổ lỗi code.

**Tiến bộ hành vi QUAN TRỌNG:** Đầu ticket #4 vẫn báo "test đầy đủ" khi code có `extends RuntimeException`(!) +
bug addItem nhầm object làm mất snapshot giá → KHÔNG thể đã test. NHƯNG sau khi sửa, lần báo done CUỐI member
test THẬT và code chạy ĐÚNG ngay — lần đầu trong 4 ticket. Đà tốt, tiếp tục siết "done = đã chạy app restart + đọc từng số".
Còn lỗi chính tả schema lặp lại (orsers) + naming snake_case trong Java (đã sửa) → vẫn cần đọc lại code trước khi nộp.

### ✅ Ticket #5 — Invoice — DONE (2026-06-15)
Hóa đơn từ phiếu — idempotency/chống trùng + snapshot + sinh số an toàn concurrency.
- V7: bảng `invoices` (id, repair_order_id FK **UNIQUE** NOT NULL ← chống trùng hóa đơn, invoice_no UNIQUE,
  subtotal/tax_rate/tax_amount/total_amount NUMERIC, status UNPAID/PAID, issued_at, timestamps)
  + bảng `invoice_counters` (year PK, last_no BIGINT, updated_at) cho sinh số reset-năm.
- Package `part/` (NỢ KT: nên tách `invoice/`).
- API: POST /api/repair-orders/{orderid}/invoice, GET /api/invoices/{id}, POST /api/invoices/{id}/payment.
- **Chống xuất hóa đơn trùng — 2 lớp:** (1) check existsByRepairOrderId (UX) + (2) UNIQUE constraint DB (correctness)
  + (3) bắt DataIntegrityViolationException → 409. Verified: xuất lần 2 cùng phiếu → 409.
- **Sinh invoice_no GAPLESS reset-năm (member tự thiết kế, GIỎI):** counter-table + 1 câu native upsert
  `INSERT INTO invoice_counters (year, last_no) VALUES (:y, 1) ON CONFLICT (year) DO UPDATE SET last_no = last_no + 1 RETURNING last_no`
  — nguyên tử, né check-then-act (MAX/COUNT rồi +1 = check-then-act = KHÔNG an toàn concurrency). Format hiện tại "SC-{year}-{6 chữ số}".
- **Snapshot:** subtotal lấy từ order.totalAmount, tax = subtotal*rate setScale(2, HALF_UP), total lưu cứng.
- **Transition validation:** payment chỉ cho khi status != PAID. Verified: payment lần 2 (đã PAID) → 400.
- Verified app-đã-restart: B1 201 total 770k, B2 409, B3 PAID, B4 400, GET PAID, DB 1 dòng, counter year=2026 last_no=2.

**Bài học member nắm:** UNIQUE constraint > check-then-act cho idempotency; DataIntegrityViolationException (Spring,
độc lập DB) bắt vi phạm UNIQUE/FK/NOT NULL → dịch 409; BigDecimal.setScale(RoundingMode.HALF_UP) cho tiền (mặc định
chia vô hạn → ArithmeticException); SEQUENCE (unique, có gap) vs counter-table-in-txn (gapless, nhưng tuần tự hóa →
điểm nghẽn) — trade-off gapless vs throughput; invoice_no là dữ liệu HỆ THỐNG sinh, KHÔNG nhận từ request DTO.
Bài học điều tra (CẢ LEAD lẫn member): "đã từng chạy" (bản ghi cũ trong DB) ≠ "đang chạy" (app hiện tại) — chỉ app
đã-restart mới tính; query DB phải RỘNG + đúng tên bảng (lead 2 lần kết luận sai do query hẹp/gõ nhầm invoice_counter
vs invoice_counters) → `\dt` xem tên thật trước khi query, đừng kết luận "không có" vội.

**⚠️ HÀNH VI — vẫn là rào cản LỚN NHẤT:** Đầu/giữa ticket #5 member nhiều lần báo "test xong/test thật bằng Postman"
khi API xuất hóa đơn đang trả 500 (lỗi @PathVariable orderid + chưa restart app) và DB chưa có bản ghi từ code mới.
Có lúc phản bác lead ("không phải như bạn nói") trong khi server thật trả 500. ĐÃ chấn chỉnh gắt: khi DỮ LIỆU mâu thuẫn
lời nói → tin dữ liệu, tự chạy lại, KHÔNG bảo vệ lời nói. Gốc rễ chưa dứt: (a) sửa code không restart app → test bản cũ;
(b) không đọc status code/field response. CAM KẾT quy trình: sửa→restart→health UP→gọi từng API đọc status→mới báo done.
GHI NHẬN: member đã có lúc đẩy lại lead bằng ảnh DB đúng cách (bằng chứng, không cãi suông) — đó là phản xạ ĐÚNG cần giữ.
KHUYẾN NGHỊ: thêm spring-boot-devtools vào pom (đã nhắc 3 ticket, member CHƯA làm) để hết bẫy "chạy bản cũ".

### ✅ Ticket #6 — Staff & Security — DONE (2026-06-17)
Spring Security + JWT + phân quyền. Mảng member yếu nhất + interview nặng nhất. Chẻ 6A (auth nền tảng) / 6B (bảo vệ API + phân quyền role).

#### ✅ 6A — User & Đăng nhập — DONE (2026-06-17)
- pom: thêm spring-boot-starter-security + jjwt (jjwt-api/impl/jackson). Lưu ý: thêm security → MẶC ĐỊNH khóa MỌI
  endpoint (kể cả /actuator/health đang trả 403, /api/auth) → 6B phải cấu hình SecurityFilterChain permitAll /api/auth/**.
- V8: bảng `staffs` (id, username UNIQUE NOT NULL, password_hash VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL,
  enabled bool default true, full_name, timestamps) + CHECK chk_staff_role IN (RECEPTIONIST/TECHNICIAN/MANAGER).
- BCryptPasswordEncoder bean; register: encode(password) lưu password_hash; login: findByUsername → matches → JWT.
- JWT (HS384, secret từ config KHÔNG hardcode): payload {sub=id, username, role, iat, exp}. role NẰM TRONG token (6B đọc role từ token).
- API: POST /api/auth/register (409 nếu trùng username), POST /api/auth/login.
- Verified app-restart + DB: register tạo nhiều staff, password_hash = BCrypt $2a$10$; login đúng→200+token(role MANAGER);
  sai pass→401 "Login không thành công"; user không tồn tại→401 GIỐNG HỆT (chống user enumeration); CHECK chặn role lạ.

**Bài học member nắm (6A):** hash (1 chiều, lộ DB không ra pass) vs encrypt (giải ngược được); BCrypt = slow hash
(cost factor, chậm có chủ đích chống brute-force GPU) + auto salt (chống rainbow table) — KHÔNG dùng MD5/SHA (nhanh=tệ
cho pass); JWT self-contained → stateless scale tốt nhưng KHÓ thu hồi (trade-off); login fail phải 401 + message MƠ HỒ
chung cho cả sai-user lẫn sai-pass (chống enumeration); JWT secret lộ = kẻ tấn công TỰ KÝ token bất kỳ role → secret
phải từ env/không commit/đủ dài; role kiểm soát 2 lớp (enum app + CHECK DB).

**Bài học điều tra (CẢ LEAD):** message lỗi gộp quá mức ("đã tồn tại HOẶC vi phạm ràng buộc") CHE nguyên nhân thật
→ nên log chi tiết server, trả client câu chung. Lead 2 lần kết luận sai (query nhầm staff vs staffs; kết luận
"register luôn hỏng" trong khi DB có 4 staff) → member đẩy lại đúng bằng ảnh DB. ĐẢO VAI: lần này member làm ĐÚNG
phản xạ (bị nói sai → đưa dữ liệu, không cãi suông) — đây CHÍNH là điều cần rèn, áp dụng cho cả leader. Luôn nhìn
data thật + đúng tên bảng trước khi kết luận.

#### ✅ 6B — Bảo vệ API + phân quyền role — DONE (2026-06-17)
- JwtAuthenticationFilter extends OncePerRequestFilter: đọc header Authorization: Bearer, verify, set Authentication
  (username + ROLE_<role>) vào SecurityContext; token xấu → bỏ qua đi tiếp (KHÔNG tự chặn, để AuthorizationFilter lo).
- SecurityConfig (common/config): csrf disable, cors (allow localhost:4200), STATELESS, permitAll /api/auth/**,
  GET dữ liệu vận hành (customers/parts/repair-orders/invoices) → hasAnyRole(3 role), POST /api/parts/** → MANAGER,
  POST customers/repair-orders/invoices → RECEPTIONIST|MANAGER, anyRequest authenticated;
  addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).
- StaffRole enum (RECEPTIONIST/TECHNICIAN/MANAGER). UnauthorizedException → 401.
- **Ma trận phân quyền (verified app-restart):** MANAGER POST parts→201; RECEPTIONIST POST parts→403; RECEPTIONIST
  POST customers→201; RECEPTIONIST GET parts→200; không token→403. Register 3 role đều OK, enabled=true.

**Bug đã fix (bài học lớn):** register set enabled từ request → request không gửi → enabled=NULL → vi phạm NOT NULL →
DataIntegrityViolationException → handler dịch 409 "đã tồn tại HOẶC vi phạm ràng buộc" → MESSAGE GỘP CHE BUG, cả lead
lẫn member đi nhầm hướng (tưởng trùng username). Bài học: (1) field hệ thống (enabled, id, invoice_no, taxRate) KHÔNG
nhận từ request — để default; (2) message lỗi gộp quá mức che nguyên nhân → LOG chi tiết server, trả client câu chung;
(3) điều tra DB/security phải đọc STACK TRACE gốc, không đoán từ message client.

**Bài học member nắm (quiz 6A/6B):** 401 (chưa xác thực - anh là ai) vs 403 (đã xác thực, thiếu quyền); JWT filter đặt
TRƯỚC UsernamePasswordAuthenticationFilter (soát vé trước khi gác cửa); "JWT hợp lệ ≠ login hợp lệ" (secret lộ / token
bị trộm / account đã khóa mà token chưa hết hạn / alg=none) → điều tra auth phải nghĩ nhiều đường; HS384 (1 secret, monolith)
vs RS256 (private ký/public verify, bắt buộc khi nhiều service cùng verify); access token ngắn + refresh token; Spring
hasRole("X") ngầm tìm authority ROLE_X (phải set ROLE_ prefix).

**KNOWN LIMITATIONS (chưa làm, ghi để sau):** (a) /actuator/health đang 403 — production nên permitAll cho probe;
(b) /api/auth/register vẫn permitAll → ai cũng tạo staff/tự phong MANAGER → nên siết chỉ MANAGER + DataInitializer seed
MANAGER đầu tiên (idempotent); (c) chưa có refresh token; (d) JWT secret cần đảm bảo đọc từ env (không commit).

**Tiến bộ hành vi:** member viết security (JWT util/filter/config/login) gần như chuẩn ngay — phần khó nhất dự án.
Vẫn còn thói "báo done khi app chưa nạp code mới" (devtools cần IDE build mới restart) → nhắc build/restart trước khi test.
ĐIỂM SÁNG: member nhiều lần đẩy lại lead bằng ảnh DB thật khi lead kết luận sai → phản xạ "đưa dữ liệu, không cãi suông"
đã hình thành (áp cho cả 2 phía). Đây là kỹ năng quan trọng nhất cần giữ.


---

## PHẦN FRONTEND (Angular mới nhất — standalone, signals, control flow mới)

> Member thạo Angular 9, NÂNG CẤP lên Angular hiện đại (v20). Lead đóng vai FE lead/chuyên gia Angular.
> Mode: member tự đọc docs, lead REVIEW SÂU (kiến trúc/RxJS/signals/change detection) + đào quiz phỏng vấn.
> FE ở d:/tmss/gms-fe (tách khỏi BE d:/tmss/gms). Token lưu localStorage (trade-off: sợ XSS, chấp nhận vì đơn giản
> + BE trả token trong body; phải phòng XSS bằng CSP + Angular auto-sanitize. Cookie HttpOnly an toàn hơn nhưng lo CSRF).

### Lưu ý chuyển đổi Angular 9 -> hiện đại (member CẦN nhớ):
- NgModule -> standalone components; *ngIf/*ngFor -> @if/@for; BehaviorSubject -> signals; @Input/@Output -> input()/output();
  constructor inject -> inject(); HttpClientModule -> provideHttpClient(); class interceptor/guard -> functional (HttpInterceptorFn/CanActivateFn).

### ✅ Ticket FE #1 — Khởi tạo Angular + Login flow — DONE (2026-06-18)
- Project d:/tmss/gms-fe (Angular v20 standalone, routing). core/ (auth.service/interceptor/guard) + features/ (login/customers).
- AuthService: login POST /api/auth/login, lưu token localStorage (key gms_token), getToken/isLoggedIn/logout.
  isLoggedIn() decode JWT exp (base64url->base64->atob, exp*1000 so Date.now(), malformed->logout) — client check chỉ UX.
- authInterceptor (functional, provideHttpClient(withInterceptors)): gắn Bearer trừ /api/auth/; catch 401|403 (khi có token, ko phải auth endpoint) -> logout + về /login (tránh redirect loop).
- authGuard (functional CanActivateFn): isLoggedIn ? true : router.parseUrl(/login).
- Login component: signal(loading/errorMessage), @if control flow, FormsModule ngModel, 401->message mơ hồ. Tailwind.
- Verified browser (member tự bấm): chưa login vào /customers->đá /login; login đúng->thấy danh sách; login sai->message.

**Bài học member nắm (quiz FE#1):** signal() gọi có NGOẶC = vừa đọc giá trị vừa ĐĂNG KÝ dependency tracking (bỏ ngoặc mất
tracking); computed() = memoization + lazy (cache, chỉ tính lại khi dep đổi) KHÁC method (chạy lại mỗi CD); biến thường
render được nhờ ZONE.JS trigger change detection sau async, KHÔNG phải reactive thật -> zoneless sẽ chết -> dùng signal;
template đọc mọi public property (signal hay thường); CLIENT check (exp/guard) chỉ UX, KHÔNG phải bảo mật — BE verify
chữ ký mới là gác cổng thật (sửa exp/role -> chữ ký sai -> BE 401); memory LEAK (ko phải crash) khi ko unsubscribe
observable dài hạn -> takeUntilDestroyed/async pipe/toSignal; 401 vs 403; interceptor xử 401/403 cần vì token bị thu hồi/account khóa khi token chưa hết hạn (chỉ BE biết).

### ⏭️ Ticket FE #2 — Customer Management (NEXT)
List (phân trang + tìm kiếm) + form tạo/sửa (Reactive Forms, khác ngModel) + chi tiết + danh sách xe. Signals cho state list.
**BE PHẢI PHÁT TRIỂN THÊM:** GET /api/customers hiện CHƯA phân trang -> thêm Pageable (đúng kịch bản FE lộ ra việc BE cần làm).

### ✅ Ticket FE #2 — Customer Management (list+CRUD+phân trang) — DONE (2026-06-18)
- **BE thêm:** GET /api/customers phân trang (Pageable + Page->PageResponse DTO, KHÔNG trả Page thô); search
  fullName/phone optional (member chọn Specification; bug đã fix: ko truyền filter -> phải trả TẤT CẢ, ko phải LIKE %null% rỗng).
  PATCH /api/customers/{id} (partial update, set field != null, dirty checking KHÔNG save thủ công); DELETE /{id}
  (CẤM xóa khi còn xe -> BusinessException 400 "Khách hàng còn xe, không thể xóa", tránh nổ 409 FK khó hiểu / mất dữ liệu lịch sử).
  Verified curl: phân trang qua nhiều trang (OFFSET đúng), search, blank->trả tất cả, PATCH 200, DELETE no-car 200, DELETE has-car 400.
- **FE (Angular hiện đại):** features/customers (component + service). List signals (customers/page/totalPages/totalElements/loading).
  Search: ngModelChange -> Subject + debounceTime(300) + distinctUntilChanged. MỘT đường load duy nhất: reload$ Subject +
  switchMap (chống race khi đổi trang nhanh + hủy request cũ) — đã gộp, bỏ loadCustomers() trùng. Đổi trang/search/create đều reload$.next().
  Form: Reactive Forms (fb.nonNullable.group, formControlName, Validators, markAllAsTouched) KHÁC ngModel ở login.
  Modal create. takeUntilDestroyed(destroyRef) chống leak. Mọi HTTP gọi qua CustomersService (component ko đụng HttpClient).
- **Validation FE phải KHỚP BE** (đã sửa: bỏ Validators.required cho address vì BE nullable — đừng chặt hơn BE vô cớ).

**Bài học member nắm:** PUT (full replace) vs PATCH (partial) — code set-field-!=-null là PATCH semantics nên đặt @PatchMapping;
DELETE dữ liệu có tham chiếu FK phải có chủ đích (cấm/soft-delete/cascade) — KHÔNG để nổ 409 ngẫu nhiên; Reactive Forms vs
template-driven; debounce + switchMap chống spam request + race; mọi gọi API dồn vào service (component ko biết URL);
state component nên signal đồng bộ (sẵn sàng zoneless).

**Bài học điều tra (LEAD tự rút):** lead test PUT trong khi member đã đổi sang PATCH -> kết luận sai "bug 403" (thực ra
PUT ko còn handler). Phải KIỂM CODE/trạng thái thật (grep @*Mapping) TRƯỚC khi test — đúng bài học "nhìn data thật trước khi kết luận", áp cho cả leader.

### ⏭️ TIẾP THEO (chưa chốt): nút Sửa/Xóa trên FE customer (gọi PATCH/DELETE + xác nhận xóa), hoặc xem chi tiết + danh sách xe,
hoặc nhảy module mới (Phụ tùng/Kho FE — có concurrency; Repair Order FE). Member chọn đi sâu Customer hay mở rộng.

### ✅ Ticket FE #3 — Parts & Inventory (FE) — DONE (2026-06-21)
- **BE:** GET /api/parts thêm phân trang (Pageable -> PageResponse, tái dùng pattern customer) + search partNo/partName optional.
  PATCH /api/parts/{id} (update). KHÔNG có DELETE part (đã thêm rồi BỎ — xem dưới). stock-adjustments giữ nguyên (ticket #3).
- **FE features/part/:** list signals + reload$/switchMap + debounce search (copy pattern customer — chấp nhận, rule-of-three chưa tới).
  Reactive Forms tạo/sửa (editingId). **Điều chỉnh kho** (đặc thù module Kho): stockForm delta+reason -> POST stock-adjustments,
  bắt lỗi BE hiện message thật (err.error.message) khi xuất quá tồn (400).
- **Phân quyền FE (UX) + BE (thật) — verified:** AuthService.role signal (lưu localStorage gms_role, set lúc login, clear logout) +
  getRole/hasRole. Template @if (role()===MANAGER) ẩn nút tạo/sửa/điều-chỉnh. BE: RECEPTIONIST POST parts -> 403, POST
  stock-adjustments -> 403, GET parts -> 200 (verified curl). => FE ẩn nút chỉ UX, BE mới chặn thật (gọi thẳng API vẫn 403).
- **Quyết định nghiệp vụ: part KHÔNG hard-delete** (nằm trong repair_order_items lịch sử -> xóa nổ 409 FK / mồ côi phiếu,
  GIỐNG bug delete-customer-có-xe). Đã bỏ TRỌN nút+method+API ở cả FE (html/component/service) lẫn BE (controller/service).
  Bài học: bỏ tính năng = bỏ HẾT mọi tầng (chỉ ẩn nút FE mà giữ API = còn cửa hậu, gọi thẳng vẫn xóa được).

**Bài học member nắm/lặp:** pattern "xóa thực thể có dữ liệu tham chiếu FK" xuất hiện LẦN 2 (customer-có-xe, part-trong-phiếu)
-> cùng cách xử (cấm + message tử tế, HOẶC không cho xóa); message gộp DataIntegrityViolationException ("đã tồn tại HOẶC
vi phạm ràng buộc") đã đánh lừa điều tra 2 lần -> NÊN log chi tiết server (member chưa làm, khuyến nghị).

**Hành vi:** member 3 lần báo "đã sửa/bỏ/done" khi code CHƯA đụng hoặc còn sót dead code (onDelete/delete service) ->
vẫn cần tự grep/đọc lại TRƯỚC khi báo done. Tiến bộ: phần BE (đóng cửa hậu DELETE) + điều chỉnh kho + phân quyền làm đúng & đủ.

## TRANG THAI TONG: BE 6 module (customer/vehicle, part/inventory, repair-order, invoice, staff/security) + FE Angular
hiện đại (login, customer CRUD, part/inventory CRUD+adjust+phân quyền). 2 module fullstack hoàn chỉnh (customer, part).
KNOWN LIMITATIONS (chưa làm): register vẫn permitAll; /actuator/health 403; JWT secret nên ra env; refresh token;
N+1 ở repair-order toResponse; chưa có test tự động (member tự nhận yếu testing — hướng đáng làm tiếp).
