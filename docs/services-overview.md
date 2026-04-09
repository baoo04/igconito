# Các microservice — vai trò trong hệ thống đặt món

Tài liệu ngắn gọn: mỗi service làm gì, dùng DB nào, gọi ai qua REST.

---

## 1. `gateway-service` (Spring Cloud Gateway)

**Nhiệm vụ**

- Cổng vào duy nhất cho trình duyệt / client: nhận request từ `localhost:8080` và chuyển tới đúng backend.
- Cấu hình **CORS** để frontend (port 3000) gọi API được.
- **Không** lưu dữ liệu nghiệp vụ.

**Định tuyến (qua gateway)**

| Tiền tố URL | Service đích | Ghi chú |
|-------------|----------------|---------|
| `/menu/**` | menu-service | Bỏ 1 đoạn `menu` trước khi forward |
| `/orders/**` | order-service | Giữ nguyên path |
| `/delivery/**` | delivery-payment-service | Bỏ 1 đoạn `delivery` trước khi forward |

**Ví dụ**

- `GET /menu/foods` → menu-service: `GET /foods`
- `GET /delivery/payments/orders/2/latest` → delivery-payment-service: `GET /payments/orders/2/latest`

---

## 2. `menu-service` (Spring Boot + JPA + MySQL)

**Nhiệm vụ**

- Quản lý **thực đơn**: danh mục (`categories`) và món (`food_items`).
- CRUD category, CRUD món, lọc/tìm món (`categoryId`, `q`, `availableOnly`).
- Quản lý **combo/bundle** và **review** (lite).
- Là **source of truth** về **pricing** (happy hour + size + combo).
- Database riêng: **`menu_service_db`**.

**Ai gọi**

- Frontend qua gateway (`/menu/...`).
- **order-service** gọi trực tiếp nội bộ Docker để lấy **price quote** khi tạo/cập nhật đơn:
  - `http://menu-service:5000/foods/{id}/price?size=M|L|S`
  - `http://menu-service:5000/combos/{id}/price`

---

## 3. `order-service` (Spring Boot + JPA + WebClient + MySQL)

**Nhiệm vụ**

- Quản lý **đơn hàng** và **dòng đơn** (Order ↔ OrderItem).
- Tính **tổng tiền** từ **unitPrice** lấy từ menu-service (price quote).
- Luồng trạng thái đơn (PLACED → … → DELIVERED / CANCELLED).
- Database riêng: **`order_service_db`**.

**Ai gọi**

- Frontend qua gateway (`/orders/...`).
- Không gọi delivery-payment-service (tách bạch: thanh toán/giao hàng là bounded context khác).
- Có endpoint xoá một dòng món khỏi đơn (nếu đơn chưa DELIVERED/CANCELLED):
  - `DELETE /orders/{orderId}/items/{itemId}`

---

## 4. `delivery-payment-service` (Spring Boot + JPA + MySQL)

**Nhiệm vụ**

- **Thanh toán (mock)**: ghi nhận thanh toán giả lập cho một `orderId` (không kết nối cổng thanh toán thật).
- **Giao hàng**: tạo bản ghi giao hàng, mã vận đơn, cập nhật trạng thái giao (PENDING → … → DELIVERED).
- Database riêng: **`delivery_payment_service_db`**.
- **Không** chứa chi tiết món; chỉ tham chiếu `orderId` (định danh đơn ở order-service).

**API “tra cứu tùy chọn” (200, không còn 404 khi chưa có dữ liệu)**

- `GET /payments/orders/{orderId}/latest` → body JSON: `{ "payment": { ... } hoặc null }` — chưa thanh toán thì `payment` là `null`.
- `GET /deliveries/orders/{orderId}` → body JSON: `{ "delivery": { ... } hoặc null }` — chưa tạo giao hàng thì `delivery` là `null`.

**Ai gọi**

- Frontend qua gateway (`/delivery/...`).

---

## 5. `frontend` (React + Vite, nginx trong Docker)

**Nhiệm vụ**

- Giao diện: xem menu, đặt hàng, xem đơn, thanh toán mock, tạo/cập nhật giao hàng.
- Chỉ gọi **gateway** (`VITE_API_BASE`, mặc định `http://localhost:8080`).

---

## Sơ đồ luồng tổng quát

```
[Trình duyệt] → gateway-service:8080 → menu-service | order-service | delivery-payment-service
                                              ↓              ↓
                                         menu DB        order DB
                                                              ↑
                    order-service ──WebClient──→ menu-service (lấy giá món)
                    delivery-payment-service (chỉ biết orderId, không gọi order/menu)
```

Nếu cần mở rộng (thanh toán thật, tracking GPS), giữ pattern **database per service** và thêm adapter/gọi REST có kiểm soát lỗi.
