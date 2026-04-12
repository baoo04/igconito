# 🎬 Cinema Ticket Booking — Spring Boot 3.3 Microservices Coding Prompt

---

## 🎯 TỔNG QUAN NHIỆM VỤ

Bạn là một senior Java developer chuyên Spring Boot. Nhiệm vụ của bạn là **implement toàn bộ backend microservices** cho hệ thống đặt vé xem phim trực tuyến theo đúng kiến trúc và nghiệp vụ được mô tả dưới đây.

**Tech stack bắt buộc:**
- Java 21 (LTS) với Virtual Threads
- Spring Boot 3.3.x
- Spring Web MVC (REST API)
- Spring Data JPA + Hibernate
- MySQL 8 (cho tất cả service)
- Maven multi-module hoặc mỗi service là một Maven project độc lập
- Lombok (giảm boilerplate)
- MapStruct (mapping entity ↔ DTO)
- OpenAPI 3 / Springdoc (swagger-ui)
- Docker + Dockerfile cho từng service

---

## 📁 CẤU TRÚC THƯ MỤC DỰ ÁN

Workspace gốc: `/mid-project-024060432`

Tất cả service được đặt trong thư mục `services/`. **Xóa** `service-a` và `service-b` mẫu, tạo các thư mục sau:

```
services/
├── movie-service/          # Port nội bộ 5000, external 5001
├── seat-availability-service/  # Port nội bộ 5000, external 5002
├── customer-service/       # Port nội bộ 5000, external 5003
├── payment-service/        # Port nội bộ 5000, external 5004
├── ticket-booking-service/ # Port nội bộ 5000, external 5005
└── notification-service/   # Port nội bộ 5000, external 5006
```

Gateway đặt ở `gateway/` (dùng Spring Cloud Gateway hoặc Nginx — xem phần Gateway).

---

## 🏗️ KIẾN TRÚC TỔNG THỂ

```
[Browser] → [Frontend :3000] → [API Gateway :8080]
                                      ↓ route theo prefix
                    /movies/**  → movie-service:5000
                    /cinemas/** → movie-service:5000
                    /showtimes/** → movie-service:5000
                    /seats/**   → seat-availability-service:5000
                    /bookings/** → ticket-booking-service:5000
                    /customers/** → customer-service:5000
                    /payments/** → payment-service:5000
                    /notifications/** → notification-service:5000

ticket-booking-service gọi nội bộ (Docker Compose DNS):
  → http://seat-availability-service:5000
  → http://customer-service:5000
  → http://payment-service:5000
  → http://notification-service:5000
```

**Quan trọng:** Mỗi service có schema database riêng (Database per Service pattern). KHÔNG dùng shared database.

---

## 📋 LUỒNG NGHIỆP VỤ CHÍNH

### Luồng 1 — Chọn ghế và tạo hold
1. FE gọi `GET /movies/{movieId}/showtimes` → Gateway → Movie Service trả danh sách suất chiếu
2. FE gọi `GET /seats/showtimes/{showtimeId}` → Gateway → Seat Availability Service trả sơ đồ ghế
3. User chọn ghế, FE gọi `POST /seats/holds` → Gateway → Seat Availability Service tạo hold tạm thời (TTL 5 phút)
   - Nếu ghế đã được giữ/đặt → trả `409 Conflict`
   - Nếu thành công → trả `201` với `holdId` và `expiresAt`

### Luồng 2 — Checkout (Ticket Booking Service điều phối)
FE gọi `POST /bookings/checkout` với body: `{ holdId, customerInfo, paymentInfo }`

Ticket Booking Service thực hiện tuần tự:
1. **Validate hold**: Gọi `GET http://seat-availability-service:5000/holds/{holdId}` — nếu hết hạn/không tồn tại → `409`
2. **Validate customer**: Gọi `POST http://customer-service:5000/customers/validate` — nếu không hợp lệ → `400`
3. **Create payment**: Gọi `POST http://payment-service:5000/payments` — nếu thất bại → Release hold + trả `402`
4. **Confirm seats**: Gọi `POST http://seat-availability-service:5000/holds/{holdId}/confirm` → đổi trạng thái ghế sang `BOOKED`
5. **Persist booking**: Lưu Booking + Ticket(s) vào `booking_db`
6. **Send notification**: Gọi `POST http://notification-service:5000/notifications/booking-confirmations` (fire-and-forget, lỗi không rollback)
7. Trả về `201 Created` với booking detail

---

## 🔧 YÊU CẦU KỸ THUẬT CHUNG CHO MỌI SERVICE

### Maven `pom.xml` template
```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.3.5</version>
</parent>
<java.version>21</java.version>

<!-- Dependencies bắt buộc -->
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-validation
mysql:mysql-connector-j:runtime
org.projectlombok:lombok
org.mapstruct:mapstruct + mapstruct-processor
org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0
```

### `application.yml` template (mỗi service)
```yaml
server:
  port: 5000

spring:
  application:
    name: <service-name>
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:<service>_db}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html

logging:
  level:
    root: INFO
    com.<package>: DEBUG
```

### Package structure (mỗi service)
```
com.cinema.<servicename>/
├── CinemaServiceApplication.java   (main class)
├── config/
│   ├── OpenApiConfig.java
│   └── WebConfig.java (CORS nếu cần)
├── controller/
│   └── <Entity>Controller.java
├── service/
│   └── <Entity>Service.java (interface + impl)
├── repository/
│   └── <Entity>Repository.java
├── entity/
│   └── <Entity>.java
├── dto/
│   ├── request/
│   └── response/
├── mapper/
│   └── <Entity>Mapper.java (MapStruct)
└── exception/
    ├── GlobalExceptionHandler.java
    └── <Custom>Exception.java
```

### Response format chuẩn
```json
// Success
{ "data": {...}, "message": "Success", "status": 200 }

// Error
{ "error": "SEAT_CONFLICT", "message": "Seat already booked", "status": 409 }
```

### Health endpoint (mọi service phải có)
```java
@GetMapping("/health")
public ResponseEntity<Map<String,String>> health() {
    return ResponseEntity.ok(Map.of("status", "ok", "service", "<name>"));
}
```

### Docker (Dockerfile cho mỗi service)
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 🎬 SERVICE 1: MOVIE SERVICE

**Thư mục:** `services/movie-service/`
**Main class:** `com.cinema.movie.MovieServiceApplication`
**DB:** `movie_db`

### Entities

**Movie**
```
id (Long, PK), title (String, NOT NULL), description (TEXT),
genre (String), durationMinutes (Int), rating (Double),
posterUrl (String), releaseDate (LocalDate),
status (Enum: ACTIVE/INACTIVE), createdAt, updatedAt
```

**Cinema**
```
id (Long, PK), name (String), address (String), city (String),
phone (String), createdAt, updatedAt
```

**Auditorium**
```
id (Long, PK), cinema (ManyToOne Cinema),
name (String), totalSeats (Int), createdAt
```

**Showtime**
```
id (Long, PK), movie (ManyToOne Movie), auditorium (ManyToOne Auditorium),
startTime (LocalDateTime), endTime (LocalDateTime),
basePrice (BigDecimal), status (Enum: SCHEDULED/CANCELLED/COMPLETED),
createdAt, updatedAt
```

### REST Endpoints (internal path)
| Method | Path | Description | Response |
|--------|------|-------------|----------|
| GET | `/health` | Health check | 200 |
| GET | `/movies` | Danh sách phim (filter: status, genre) | 200 |
| POST | `/movies` | Tạo phim mới | 201 |
| GET | `/movies/{movieId}` | Chi tiết phim | 200, 404 |
| PUT | `/movies/{movieId}` | Cập nhật phim | 200, 404 |
| DELETE | `/movies/{movieId}` | Xóa phim | 204, 404 |
| GET | `/movies/{movieId}/showtimes` | Danh sách suất chiếu theo phim | 200 |
| GET | `/cinemas` | Danh sách rạp | 200 |
| POST | `/cinemas` | Tạo rạp | 201 |
| GET | `/cinemas/{cinemaId}` | Chi tiết rạp | 200, 404 |
| GET | `/showtimes` | Danh sách suất chiếu (filter: date, cinemaId) | 200 |
| POST | `/showtimes` | Tạo suất chiếu | 201 |
| GET | `/showtimes/{showtimeId}` | Chi tiết suất chiếu | 200, 404 |
| PUT | `/showtimes/{showtimeId}` | Cập nhật suất chiếu | 200, 404 |
| DELETE | `/showtimes/{showtimeId}` | Xóa suất chiếu | 204, 404 |

### DTOs quan trọng
```java
// Request
CreateMovieRequest { title, description, genre, durationMinutes, rating, posterUrl, releaseDate }
CreateShowtimeRequest { movieId, auditoriumId, startTime, endTime, basePrice }

// Response
MovieResponse { id, title, description, genre, durationMinutes, rating, posterUrl, releaseDate, status }
ShowtimeResponse { id, movieId, movieTitle, cinemaId, cinemaName, auditoriumId, auditoriumName, startTime, endTime, basePrice, status }
```

### Seed data (insert qua Flyway hoặc data.sql)
- 3 rạp phim (Hà Nội, HCM, Đà Nẵng)
- 5 phim mẫu
- 10 suất chiếu mẫu

---

## 💺 SERVICE 2: SEAT AVAILABILITY SERVICE

**Thư mục:** `services/seat-availability-service/`
**Main class:** `com.cinema.seat.SeatAvailabilityServiceApplication`
**DB:** `seat_db`

### Entities

**Seat**
```
id (Long, PK), showtimeId (Long, NOT NULL), rowLabel (String), seatNumber (Int),
seatCode (String e.g. "A1"), type (Enum: STANDARD/VIP/COUPLE),
price (BigDecimal),
status (Enum: FREE/HELD/BOOKED),
UNIQUE constraint: (showtimeId, seatCode)
```

**SeatHold**
```
id (UUID, PK), showtimeId (Long), seatIds (OneToMany hoặc JSON list),
expiresAt (LocalDateTime, TTL = now + 5 phút),
status (Enum: ACTIVE/EXPIRED/CONFIRMED/RELEASED),
createdAt
```

**SeatHoldItem**
```
id (Long, PK), seatHold (ManyToOne SeatHold), seat (ManyToOne Seat)
```

### REST Endpoints
| Method | Path | Description | Response |
|--------|------|-------------|----------|
| GET | `/health` | Health check | 200 |
| GET | `/showtimes/{showtimeId}/seats` | Lấy sơ đồ ghế (tất cả ghế + trạng thái) | 200, 404 |
| POST | `/showtimes/{showtimeId}/availability` | Kiểm tra ghế còn trống không | 200, 409 |
| POST | `/holds` | Tạo hold tạm thời cho ghế đã chọn | 201, 400, 409 |
| GET | `/holds/{holdId}` | Lấy thông tin hold | 200, 404 |
| POST | `/holds/{holdId}/release` | Giải phóng hold (hoàn ghế về FREE) | 200, 404, 409 |
| POST | `/holds/{holdId}/confirm` | Xác nhận hold → đổi ghế sang BOOKED | 200, 404, 409 |

### Logic nghiệp vụ quan trọng

**POST /holds — Tạo hold:**
```
1. Nhận: { showtimeId, seatIds: [1,2,3] }
2. Trong @Transactional:
   a. SELECT ... FOR UPDATE các seat rows theo seatIds
   b. Kiểm tra tất cả đều có status = FREE
   c. Nếu có ghế nào không FREE → throw 409 SeatConflictException
   d. Cập nhật status = HELD cho tất cả ghế
   e. Tạo SeatHold với expiresAt = now() + 5 phút
   f. Tạo các SeatHoldItem
3. Return 201 { holdId, expiresAt, seats: [...] }
```

**POST /holds/{holdId}/confirm — Xác nhận booking:**
```
1. Tìm SeatHold theo holdId
2. Kiểm tra status = ACTIVE và expiresAt > now()
3. Trong @Transactional:
   a. Cập nhật tất cả Seat liên quan sang BOOKED
   b. Cập nhật SeatHold.status = CONFIRMED
4. Return 200
```

**POST /holds/{holdId}/release — Giải phóng:**
```
1. Tìm SeatHold theo holdId
2. Trong @Transactional:
   a. Cập nhật tất cả Seat liên quan sang FREE
   b. Cập nhật SeatHold.status = RELEASED
3. Return 200
```

### Scheduled Task — Tự động hết hạn hold
```java
@Scheduled(fixedRate = 60000) // mỗi 1 phút
public void expireStaleHolds() {
    // Lấy tất cả SeatHold có status=ACTIVE và expiresAt < now()
    // Với mỗi hold: đổi ghế về FREE, đổi hold sang EXPIRED
}
```

### DTOs
```java
// Request
CreateHoldRequest { showtimeId: Long, seatIds: List<Long> }

// Response
HoldResponse {
  holdId: UUID, showtimeId: Long, status: String,
  expiresAt: LocalDateTime,
  seats: List<SeatInfo { seatId, seatCode, rowLabel, seatNumber, type, price }>
}
SeatMapResponse { showtimeId, seats: List<SeatStatus { seatId, seatCode, status, type, price }> }
```

---

## 👤 SERVICE 3: CUSTOMER SERVICE

**Thư mục:** `services/customer-service/`
**Main class:** `com.cinema.customer.CustomerServiceApplication`
**DB:** `customer_db`

### Entity

**Customer**
```
id (Long, PK), fullName (String, NOT NULL), email (String, NOT NULL),
phone (String, NOT NULL), createdAt, updatedAt
UNIQUE constraint: email
```

### REST Endpoints
| Method | Path | Description | Response |
|--------|------|-------------|----------|
| GET | `/health` | Health check | 200 |
| POST | `/customers` | Tạo hoặc cập nhật customer (upsert theo email) | 200/201 |
| GET | `/customers/lookup` | Tìm customer theo email hoặc phone (?email=...&phone=...) | 200, 404 |
| POST | `/customers/validate` | Validate thông tin customer cho booking | 200, 400 |

### Logic Validate
```
POST /customers/validate body: { fullName, email, phone }
Rules:
- fullName: không null, ít nhất 2 từ, không chứa ký tự đặc biệt
- email: hợp lệ theo RFC format
- phone: 10-11 chữ số, bắt đầu bằng 0 (Việt Nam)
Return 200 { valid: true, customerId: ... } hoặc 400 { valid: false, errors: [...] }
```

### DTOs
```java
CustomerRequest { fullName, email, phone }
CustomerValidateRequest { fullName, email, phone }
CustomerResponse { id, fullName, email, phone, createdAt }
CustomerValidateResponse { valid: boolean, customerId: Long (nullable), errors: List<String> }
```

---

## 💳 SERVICE 4: PAYMENT SERVICE

**Thư mục:** `services/payment-service/`
**Main class:** `com.cinema.payment.PaymentServiceApplication`
**DB:** `payment_db`

### Entity

**PaymentTransaction**
```
id (UUID, PK), bookingReference (String), amount (BigDecimal),
currency (String, default "VND"), status (Enum: PENDING/SUCCESS/FAILED/REFUNDED),
paymentMethod (Enum: CREDIT_CARD/DEBIT_CARD/BANK_TRANSFER/MOCK),
gatewayTransactionId (String, nullable),
gatewayResponse (TEXT, nullable, JSON string from gateway),
createdAt, updatedAt
```

### REST Endpoints
| Method | Path | Description | Response |
|--------|------|-------------|----------|
| GET | `/health` | Health check | 200 |
| POST | `/payments` | Tạo và xử lý payment transaction | 201 (success) / 402 (failed) |
| GET | `/payments/{paymentId}` | Lấy chi tiết transaction | 200, 404 |
| POST | `/payments/{paymentId}/confirm` | Manual confirm (optional flow) | 200, 400, 409 |

### Mock Payment Gateway
Vì chưa tích hợp gateway thật, implement mock:
```java
@Service
public class MockPaymentGatewayService {
    // Simulate: nếu amount > 10,000,000 VND → FAILED
    // Ngẫu nhiên 90% SUCCESS, 10% FAILED (có thể dùng random seed)
    // Hoặc check cardNumber: nếu kết thúc bằng "0000" → FAILED
    public PaymentGatewayResult charge(PaymentRequest request) { ... }
}
```

### DTOs
```java
// Request
CreatePaymentRequest {
  bookingReference: String,
  amount: BigDecimal,
  currency: String,
  paymentMethod: String, // "CREDIT_CARD", "MOCK"
  cardNumber: String (optional, masked before saving),
  cardHolderName: String
}

// Response
PaymentResponse {
  paymentId: UUID, bookingReference: String,
  amount: BigDecimal, status: String,
  gatewayTransactionId: String, createdAt: LocalDateTime
}
```

**Quan trọng:** KHÔNG lưu đầy đủ cardNumber vào DB. Chỉ lưu 4 số cuối (masked).

---

## 🎟️ SERVICE 5: TICKET BOOKING SERVICE (Orchestrator)

**Thư mục:** `services/ticket-booking-service/`
**Main class:** `com.cinema.booking.TicketBookingServiceApplication`
**DB:** `booking_db`

### Entities

**Booking**
```
id (UUID, PK), bookingCode (String, unique, format: BK-YYYYMMDD-RANDOM6),
status (Enum: PENDING/CONFIRMED/CANCELLED/FAILED),
showtimeId (Long), customerEmail (String), customerName (String),
customerPhone (String), totalAmount (BigDecimal),
paymentId (UUID), holdId (UUID),
createdAt, updatedAt
```

**Ticket**
```
id (UUID, PK), booking (ManyToOne Booking),
seatId (Long), seatCode (String), seatType (String),
price (BigDecimal), ticketCode (String, unique, random UUID short),
showtimeId (Long), movieTitle (String snapshot), startTime (LocalDateTime snapshot),
cinemaName (String snapshot), auditoriumName (String snapshot),
createdAt
```

### REST Endpoints
| Method | Path | Description | Response |
|--------|------|-------------|----------|
| GET | `/health` | Health check | 200 |
| POST | `/bookings/checkout` | Orchestrate booking flow | 201, 400, 402, 409, 502 |
| GET | `/bookings/{bookingId}` | Chi tiết booking | 200, 404 |
| GET | `/bookings/{bookingId}/tickets` | Danh sách vé của booking | 200, 404 |
| POST | `/bookings/{bookingId}/cancel` | Huỷ booking | 200, 404, 409 |

### Checkout Request/Response
```java
// POST /bookings/checkout Request
CheckoutRequest {
  holdId: UUID,
  showtimeId: Long,
  customer: {
    fullName: String,
    email: String,
    phone: String
  },
  payment: {
    method: String,      // "CREDIT_CARD", "MOCK"
    cardNumber: String,
    cardHolderName: String,
    totalAmount: BigDecimal
  }
}

// 201 Response
BookingConfirmedResponse {
  bookingId: UUID,
  bookingCode: String,
  status: String,
  customer: { fullName, email, phone },
  showtime: { showtimeId, movieTitle, startTime, cinemaName, auditoriumName },
  tickets: [ { ticketCode, seatCode, seatType, price } ],
  totalAmount: BigDecimal,
  paymentId: UUID,
  createdAt: LocalDateTime
}
```

### Orchestration Logic — CheckoutService
```java
@Service
@Transactional
public class CheckoutService {

    // RestTemplate hoặc WebClient để gọi service khác
    // URL config từ application.yml: seat-availability-service.url, customer-service.url, ...

    public BookingConfirmedResponse checkout(CheckoutRequest req) {
        // 1. Validate hold
        HoldResponse hold = seatClient.getHold(req.holdId());
        if (hold == null || hold.isExpired()) throw new SeatHoldExpiredException(409);

        // 2. Validate customer
        CustomerValidateResponse cv = customerClient.validate(req.customer());
        if (!cv.valid()) throw new CustomerValidationException(400, cv.errors());

        // 3. Create payment
        PaymentResponse payment = paymentClient.createPayment(
            new CreatePaymentRequest(generateBookingRef(), req.payment())
        );
        if (!"SUCCESS".equals(payment.status())) {
            seatClient.releaseHold(req.holdId()); // compensating action
            throw new PaymentFailedException(402);
        }

        // 4. Confirm seats
        seatClient.confirmHold(req.holdId());

        // 5. Persist booking + tickets
        Booking booking = createAndSaveBooking(req, hold, payment);

        // 6. Notify (fire-and-forget, do NOT throw on failure)
        try {
            notificationClient.sendConfirmation(booking);
        } catch (Exception e) {
            log.warn("Notification failed for booking {}: {}", booking.getId(), e.getMessage());
        }

        // 7. Return response
        return mapToConfirmedResponse(booking);
    }
}
```

### HTTP Clients (RestTemplate với @Bean config)
```java
// ServiceClientConfig.java
@Bean
public RestTemplate restTemplate() {
    // Timeout: connect 3s, read 10s
    return new RestTemplateBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(10))
        .build();
}
```

---

## 📧 SERVICE 6: NOTIFICATION SERVICE

**Thư mục:** `services/notification-service/`
**Main class:** `com.cinema.notification.NotificationServiceApplication`
**DB:** `notification_db`

### Entity

**NotificationLog**
```
id (UUID, PK), bookingId (UUID), recipientEmail (String),
subject (String), status (Enum: PENDING/SENT/FAILED),
attemptCount (Int, default 0), lastAttemptAt (LocalDateTime),
errorMessage (TEXT, nullable), sentAt (LocalDateTime, nullable),
createdAt
```

### REST Endpoints
| Method | Path | Description | Response |
|--------|------|-------------|----------|
| GET | `/health` | Health check | 200 |
| POST | `/notifications/booking-confirmations` | Gửi email xác nhận booking | 202, 400 |
| GET | `/notifications/{notificationId}` | Lấy trạng thái notification | 200, 404 |

### Email Logic
```java
// Nếu cần tích hợp SMTP thật: dùng spring-boot-starter-mail + JavaMailSender
// Cho assignment: implement mock sender
@Service
public class MockEmailSender {
    // Log email ra console thay vì gửi thật
    // Lưu NotificationLog với status SENT
    public void send(String to, String subject, String body) {
        log.info("📧 MOCK EMAIL SENT TO: {} | SUBJECT: {}", to, subject);
        // Save as SENT
    }
}
```

### Request/Response
```java
// POST /notifications/booking-confirmations Request
BookingConfirmationRequest {
  bookingId: UUID,
  bookingCode: String,
  recipientEmail: String,
  customerName: String,
  movieTitle: String,
  cinemaName: String,
  auditoriumName: String,
  startTime: LocalDateTime,
  tickets: List<{ ticketCode, seatCode, seatType, price }>,
  totalAmount: BigDecimal
}

// 202 Response
NotificationResponse {
  notificationId: UUID,
  status: String,
  message: String
}
```

---

## 🌐 GATEWAY

**Thư mục:** `gateway/`

**Option A (Ưu tiên):** Dùng **Spring Cloud Gateway** (Spring Boot app)

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

```yaml
# gateway/src/main/resources/application.yml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        - id: movie-service
          uri: http://${MOVIE_SERVICE_HOST:movie-service}:5000
          predicates:
            - Path=/movies/**, /cinemas/**, /showtimes/**
        - id: seat-service
          uri: http://${SEAT_SERVICE_HOST:seat-availability-service}:5000
          predicates:
            - Path=/seats/**
        - id: customer-service
          uri: http://${CUSTOMER_SERVICE_HOST:customer-service}:5000
          predicates:
            - Path=/customers/**
        - id: payment-service
          uri: http://${PAYMENT_SERVICE_HOST:payment-service}:5000
          predicates:
            - Path=/payments/**
        - id: booking-service
          uri: http://${BOOKING_SERVICE_HOST:ticket-booking-service}:5000
          predicates:
            - Path=/bookings/**
        - id: notification-service
          uri: http://${NOTIFICATION_SERVICE_HOST:notification-service}:5000
          predicates:
            - Path=/notifications/**
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
```

---

## 🐳 DOCKER COMPOSE (Cập nhật `docker-compose.yml`)

Cập nhật file `docker-compose.yml` ở thư mục gốc:

```yaml
version: '3.8'

services:
  # ─── Databases ─────────────────────────────────
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD:-root}
      MYSQL_DATABASE: movie_db
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./scripts/init-databases.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-network

  # ─── Movie Service ─────────────────────────────
  movie-service:
    build: ./services/movie-service
    ports:
      - "${MOVIE_SERVICE_PORT:-5001}:5000"
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: movie_db
      DB_USER: root
      DB_PASSWORD: ${DB_PASSWORD:-root}
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-q", "-O-", "http://localhost:5000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - app-network

  # ─── Seat Availability Service ─────────────────
  seat-availability-service:
    build: ./services/seat-availability-service
    ports:
      - "${SEAT_SERVICE_PORT:-5002}:5000"
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: seat_db
      DB_USER: root
      DB_PASSWORD: ${DB_PASSWORD:-root}
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-q", "-O-", "http://localhost:5000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - app-network

  # ─── Customer Service ─────────────────────────
  customer-service:
    build: ./services/customer-service
    ports:
      - "${CUSTOMER_SERVICE_PORT:-5003}:5000"
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: customer_db
      DB_USER: root
      DB_PASSWORD: ${DB_PASSWORD:-root}
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-q", "-O-", "http://localhost:5000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - app-network

  # ─── Payment Service ──────────────────────────
  payment-service:
    build: ./services/payment-service
    ports:
      - "${PAYMENT_SERVICE_PORT:-5004}:5000"
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: payment_db
      DB_USER: root
      DB_PASSWORD: ${DB_PASSWORD:-root}
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-q", "-O-", "http://localhost:5000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - app-network

  # ─── Ticket Booking Service ───────────────────
  ticket-booking-service:
    build: ./services/ticket-booking-service
    ports:
      - "${BOOKING_SERVICE_PORT:-5005}:5000"
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: booking_db
      DB_USER: root
      DB_PASSWORD: ${DB_PASSWORD:-root}
      SEAT_SERVICE_HOST: seat-availability-service
      CUSTOMER_SERVICE_HOST: customer-service
      PAYMENT_SERVICE_HOST: payment-service
      NOTIFICATION_SERVICE_HOST: notification-service
    depends_on:
      seat-availability-service:
        condition: service_healthy
      customer-service:
        condition: service_healthy
      payment-service:
        condition: service_healthy
      notification-service:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-q", "-O-", "http://localhost:5000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - app-network

  # ─── Notification Service ─────────────────────
  notification-service:
    build: ./services/notification-service
    ports:
      - "${NOTIFICATION_SERVICE_PORT:-5006}:5000"
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: notification_db
      DB_USER: root
      DB_PASSWORD: ${DB_PASSWORD:-root}
      SMTP_HOST: ${SMTP_HOST:-smtp.gmail.com}
      SMTP_PORT: ${SMTP_PORT:-587}
      SMTP_USERNAME: ${SMTP_USERNAME:-mock@example.com}
      SMTP_PASSWORD: ${SMTP_PASSWORD:-mock}
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-q", "-O-", "http://localhost:5000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - app-network

  # ─── API Gateway ──────────────────────────────
  gateway:
    build: ./gateway
    ports:
      - "${GATEWAY_PORT:-8080}:8080"
    environment:
      MOVIE_SERVICE_HOST: movie-service
      SEAT_SERVICE_HOST: seat-availability-service
      CUSTOMER_SERVICE_HOST: customer-service
      PAYMENT_SERVICE_HOST: payment-service
      BOOKING_SERVICE_HOST: ticket-booking-service
      NOTIFICATION_SERVICE_HOST: notification-service
    depends_on:
      movie-service:
        condition: service_healthy
      seat-availability-service:
        condition: service_healthy
      customer-service:
        condition: service_healthy
      payment-service:
        condition: service_healthy
      ticket-booking-service:
        condition: service_healthy
      notification-service:
        condition: service_healthy
    networks:
      - app-network

networks:
  app-network:
    driver: bridge

volumes:
  mysql-data:
```

---

## 🗃️ DATABASE INIT SCRIPT

Tạo file `scripts/init-databases.sql`:
```sql
CREATE DATABASE IF NOT EXISTS movie_db;
CREATE DATABASE IF NOT EXISTS seat_db;
CREATE DATABASE IF NOT EXISTS customer_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS booking_db;
CREATE DATABASE IF NOT EXISTS notification_db;
```

---

## ⚠️ YÊU CẦU CHẤT LƯỢNG CODE

1. **Exception Handling:** Mỗi service phải có `GlobalExceptionHandler` xử lý tất cả exception và trả về response JSON thống nhất.

2. **Validation:** Dùng `@Valid` + Bean Validation (`@NotNull`, `@Email`, `@Pattern`, v.v.) cho mọi request body.

3. **Logging:** Dùng SLF4J + Logback. Log ở INFO level cho business events chính. Log WARNING cho lỗi expected (seat conflict, payment failure). KHÔNG log sensitive data.

4. **Concurrency (Seat Service):** Dùng `@Transactional` với `@Lock(LockModeType.PESSIMISTIC_WRITE)` khi query ghế để tránh race condition.

5. **Idempotency key:** Checkout request nhận `X-Idempotency-Key` header. Check duplicate request trong vòng 5 phút.

6. **API Documentation:** Mỗi service phải có Swagger UI hoạt động ở `http://localhost:{port}/swagger-ui.html`.

7. **Health check:** Mỗi service phải có `GET /health` trả về `{"status":"ok","service":"<name>"}`.

8. **KHÔNG hardcode** URL, password, secret. Tất cả từ environment variables.

---

## 📁 CHECKLIST OUTPUT CẦN TẠO

Sau khi hoàn thành, các file sau phải tồn tại:

```
services/
├── movie-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/cinema/movie/...
│   └── src/main/resources/application.yml
├── seat-availability-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/cinema/seat/...
│   └── src/main/resources/application.yml
├── customer-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/cinema/customer/...
│   └── src/main/resources/application.yml
├── payment-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/cinema/payment/...
│   └── src/main/resources/application.yml
├── ticket-booking-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/cinema/booking/...
│   └── src/main/resources/application.yml
└── notification-service/
    ├── pom.xml
    ├── Dockerfile
    └── src/main/java/com/cinema/notification/...
    └── src/main/resources/application.yml

gateway/
├── pom.xml (Spring Cloud Gateway)
├── Dockerfile
└── src/main/resources/application.yml

scripts/
└── init-databases.sql

docker-compose.yml (updated)
.env.example (updated)
```

---

## 🚀 LỆNH CHẠY SAU KHI CODE XONG

```bash
# Từ thư mục gốc d:\PTIT\KI4\SOA\mid-project-024060432
docker compose up --build

# Hoặc build từng service để test
cd services/movie-service && mvn clean package -DskipTests
java -jar target/*.jar
```

---

## 📌 GHI CHÚ CUỐI

- Code phải **biên dịch được** với `mvn clean package`
- Mỗi service phải **start độc lập** được (chỉ cần MySQL up)
- Ưu tiên code đúng flow hơn code đẹp — nhưng tối thiểu phải có cấu trúc package chuẩn
- Dùng **Java Records** cho DTO nếu muốn (Java 21 hỗ trợ)
- Khi gọi inter-service qua RestTemplate, xử lý `HttpClientErrorException` và `HttpServerErrorException` riêng biệt
- Tất cả file code đặt vào đúng thư mục trong workspace `d:\PTIT\KI4\SOA\mid-project-024060432`
