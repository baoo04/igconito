#!/usr/bin/env bash
# Dev only: allow root from any host so Workbench via Docker Desktop (client seen as e.g. 172.18.0.1) works.
# Uses MYSQL_ROOT_PASSWORD from the MySQL container environment.
set -eo pipefail

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
ALTER USER 'root'@'%' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
SQL
