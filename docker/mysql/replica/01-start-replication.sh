#!/bin/bash
# replica 최초 기동 시 1회 실행: master에 연결해 복제를 시작하고 읽기 전용으로 전환
set -e
mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST = 'mysql-master',
  SOURCE_PORT = 3306,
  SOURCE_USER = 'repl',
  SOURCE_PASSWORD = '${REPL_PASSWORD}',
  SOURCE_AUTO_POSITION = 1,      -- GTID로 어디까지 받았는지 자동 추적
  GET_SOURCE_PUBLIC_KEY = 1;     -- MySQL 8 기본 인증(caching_sha2_password) 접속에 필요
START REPLICA;
-- 애플리케이션이 실수로 replica에 쓰지 못하게 막음 (복제 스레드는 계속 쓸 수 있음)
SET PERSIST read_only = ON;
SET PERSIST super_read_only = ON;
SQL
