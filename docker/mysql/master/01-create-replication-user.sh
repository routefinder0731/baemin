#!/bin/bash
# master 최초 기동 시 1회 실행: replica가 접속할 복제 전용 계정 생성
set -e
mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
SET SESSION sql_log_bin = 0;  -- 이 계정 생성은 replica로 복제하지 않음
CREATE USER 'repl'@'%' IDENTIFIED BY '${REPL_PASSWORD}';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
SQL
