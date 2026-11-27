CREATE TABLE users
(
    id          BIGINT          NOT NULL,   -- Snowflake ID
    email       VARCHAR(255)    NOT NULL,
    password    VARCHAR(100)    NOT NULL,   -- BCrypt 암호화 값
    name        VARCHAR(50)     NOT NULL,
    role        VARCHAR(20)     NOT NULL,   -- CUSTOMER, OWNER, RIDER
    created_at  DATETIME(6)     NOT NULL,
    updated_at  DATETIME(6)     NOT NULL,

    PRIMARY KEY (id),

    -- 회원 가입 시 이메일 중복 체크
    -- 로그인 시 이메일로 회원 조회에 사용
    -- 동시에 같은 이메일로 가입 요청이 와도 DB 레벨에서 중복을 최종 차단
    CONSTRAINT uk_users_email UNIQUE (email)
)   ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;