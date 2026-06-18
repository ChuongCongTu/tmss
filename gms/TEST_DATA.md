# GMS – Bộ test data cho API

Base URL: `http://localhost:8080`
Content-Type: `application/json` cho mọi request có body.

> **Thứ tự gọi quan trọng**: các API phụ thuộc ID lẫn nhau. Gọi theo đúng thứ tự
> dưới đây và copy `id` từ response trước dán vào request sau.
>
> Luồng phụ thuộc:
> `Customer` → `Vehicle (theo customerId)` → `Part` → `RepairOrder (cần vehicleId + partId)` → `Invoice (cần orderId)`

---

## 1. Customer

### 1.1. Tạo customer
- **POST** `/api/customers`
```json
{
  "fullName": "Nguyễn Văn An",
  "address": "12 Lê Lợi, Quận 1, TP.HCM",
  "phone": "0901234567"
}
```
> `fullName` bắt buộc (`@NotBlank`). Lưu lại `data.id` → gọi là `{customerId}`.

Thêm vài customer khác:
```json
{ "fullName": "Trần Thị Bình", "address": "45 Nguyễn Huệ, Đà Nẵng", "phone": "0912345678" }
```
```json
{ "fullName": "Lê Hoàng Cường", "address": "9 Trần Phú, Hà Nội", "phone": "0987654321" }
```

### 1.2. Lấy customer theo id
- **GET** `/api/customers/{customerId}`

### 1.3. Lấy tất cả customer
- **GET** `/api/customers`

---

## 2. Vehicle (gắn với customer)

### 2.1. Thêm xe cho customer
- **POST** `/api/customers/{customerId}/vehicles`
```json
{
  "plateNo": "51F-123.45",
  "brand": "Toyota",
  "color": "Trắng",
  "model": "Vios",
  "year": 2020
}
```
> `plateNo` bắt buộc (`@NotBlank`). Lưu lại `data.id` → `{vehicleId}`.

Xe thứ 2:
```json
{ "plateNo": "30A-678.90", "brand": "Honda", "color": "Đen", "model": "City", "year": 2022 }
```

### 2.2. Lấy tất cả xe của customer
- **GET** `/api/customers/{customerId}/vehicles`

---

## 3. Part (phụ tùng)

### 3.1. Tạo part
- **POST** `/api/parts`
```json
{
  "partNo": "OIL-FILTER-001",
  "partName": "Lọc dầu động cơ",
  "price": 150000,
  "quantity": 100
}
```
> Lưu lại `data.id` → `{partId}`.

Part thứ 2 và 3:
```json
{ "partNo": "BRAKE-PAD-002", "partName": "Má phanh trước", "price": 450000, "quantity": 50 }
```
```json
{ "partNo": "SPARK-PLUG-003", "partName": "Bugi đánh lửa", "price": 80000, "quantity": 200 }
```

### 3.2. Lấy part theo id
- **GET** `/api/parts/{partId}`

### 3.3. Lấy tất cả part
- **GET** `/api/parts`

### 3.4. Điều chỉnh tồn kho
- **POST** `/api/parts/{partId}/stock-adjustments`
```json
{
  "reason": "Nhập thêm hàng từ NCC",
  "delta": 30
}
```
Giảm kho (delta âm):
```json
{ "reason": "Kiểm kê phát hiện hư hỏng", "delta": -5 }
```

---

## 4. Repair Order (đơn sửa chữa)

### 4.1. Tạo repair order
- **POST** `/api/repair-orders`
> Cần `vehicleId` (từ mục 2) và `partId` (từ mục 3). `items` sẽ trừ tồn kho part.
```json
{
  "vehicleId": "DÁN_VEHICLE_ID_VÀO_ĐÂY",
  "status": "PENDING",
  "laborCost": 200000,
  "items": [
    { "partId": "DÁN_PART_ID_1", "quantity": 2 },
    { "partId": "DÁN_PART_ID_2", "quantity": 1 }
  ]
}
```
> Lưu lại `data.id` → `{orderId}`.
> `totalAmount` được hệ thống tự tính = Σ(unitPrice × quantity) + laborCost.
> `status` là chuỗi tự do (gợi ý: `PENDING`, `IN_PROGRESS`, `COMPLETED`).

Đơn không có phụ tùng (chỉ tiền công):
```json
{
  "vehicleId": "DÁN_VEHICLE_ID_VÀO_ĐÂY",
  "status": "IN_PROGRESS",
  "laborCost": 350000,
  "items": []
}
```

#### Case lỗi (test validation):
Không đủ tồn kho → trả `BusinessException` "Lỗi không đủ tồn kho.":
```json
{
  "vehicleId": "DÁN_VEHICLE_ID_VÀO_ĐÂY",
  "status": "PENDING",
  "laborCost": 100000,
  "items": [
    { "partId": "DÁN_PART_ID_1", "quantity": 999999 }
  ]
}
```

### 4.2. Lấy repair order theo id
- **GET** `/api/repair-orders/{orderId}`

### 4.3. Lấy repair order theo xe
- **GET** `/api/{vehicleId}/repair-orders`

---

## 5. Invoice (hóa đơn)

### 5.1. Tạo invoice cho repair order
- **POST** `/api/repair-orders/{orderId}/invoice`
```json
{
  "taxRate": 0.1,
  "totalAmount": 750000
}
```
> Lưu lại `data.id` → `{invoiceId}`.

### 5.2. Lấy invoice theo id
- **GET** `/api/invoices/{invoiceId}`

### 5.3. Thanh toán / đổi trạng thái invoice
- **POST** `/api/invoices/{invoiceId}/payment`
> Không cần body.

---

## Lưu ý các bug tiềm ẩn phát hiện khi đọc code
Những endpoint sau có thể trả lỗi do tên biến `@PathVariable` không khớp với tên trong URL template (Spring sẽ không bind được nếu không có `-parameters` khi compile):

- `POST /api/repair-orders/{orderId}/invoice` → code khai báo `@PathVariable UUID orderid` (URL là `orderId`).
- `GET /api/repair-orders/{id}` → code khai báo `@PathVariable UUID uuid` (URL là `id`).

Nếu các API này báo lỗi 400/500 về missing path variable, cần sửa cho khớp tên, ví dụ `@PathVariable("orderId") UUID orderId`.
