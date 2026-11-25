package org.sy.baemin.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 통합 테스트 공통 부모 클래스.
 * <p>
 * 실제 MySQL 컨테이너를 띄워서 테스트한다. H2 같은 인메모리 DB는 락, 인덱스, SQL 문법이 MySQL과 달라
 * 동시성·쿼리 테스트 결과를 믿을 수 없기 때문이다.
 * <p>
 * 컨테이너는 static 블록에서 한 번만 띄우고 모든 테스트 클래스가 공유한다(싱글톤 컨테이너 패턴).
 * 테스트 클래스마다 새로 띄우면 MySQL 기동에 매번 수십 초가 걸린다.
 * <p>
 * {@code @ServiceConnection} 덕분에 컨테이너의 접속 주소·계정이 자동으로 datasource 설정에 들어간다.
 */
public abstract class IntegrationTestSupport {

    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    static {
        MYSQL.start();
    }
}
