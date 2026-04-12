# Frontend

Static HTML/CSS/JS served by nginx. API calls target the **API Gateway** (never individual service ports from the browser when avoidable).

## API base URL

| Cách chạy | Mặc định |
|-----------|----------|
| **Docker Compose** (`http://localhost:3000`) | Cùng origin: `http://localhost:3000/api` — nginx reverse proxy tới container `gateway:8080` (không CORS). |
| Mở file `index.html` trực tiếp (`file://`) | Mặc định `http://localhost:8080` (xem `getApiBase()` trong `js/api.js`). |

Có thể ghi đè base URL qua `localStorage` key `cinema_api_base` nếu cần (không còn ô cấu hình trên UI). Giá trị cũ `http://localhost:8080` lưu riêng cho Docker được tự xóa một lần khi chuyển sang proxy `/api`.

## Build

```bash
docker compose build frontend
```

## Layout

```
frontend/
├── Dockerfile
├── nginx.conf      # proxy /api/ → http://gateway:8080/
├── index.html
├── css/styles.css
├── js/api.js
├── js/app.js
└── readme.md
```
