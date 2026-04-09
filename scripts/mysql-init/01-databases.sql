CREATE DATABASE IF NOT EXISTS menu_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS delivery_payment_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'foodorder'@'%' IDENTIFIED BY 'foodorder';
GRANT ALL PRIVILEGES ON menu_service_db.* TO 'foodorder'@'%';
GRANT ALL PRIVILEGES ON order_service_db.* TO 'foodorder'@'%';
GRANT ALL PRIVILEGES ON delivery_payment_service_db.* TO 'foodorder'@'%';
FLUSH PRIVILEGES;
