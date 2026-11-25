package org.sy.baemin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sy.baemin.support.IntegrationTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BaeminApplicationTests extends IntegrationTestSupport {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("애플리케이션이 MySQL에 연결되어 정상 기동된다")
    void contextLoads() {
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);

        assertThat(version).startsWith("8.4");
    }

    @Test
    @DisplayName("기동 시 Flyway 마이그레이션이 모두 성공한다")
    void flywayMigrationsApplied() {
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Integer.class);
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);

        assertThat(failed).isZero();
        assertThat(applied).isGreaterThanOrEqualTo(1);
    }
}
