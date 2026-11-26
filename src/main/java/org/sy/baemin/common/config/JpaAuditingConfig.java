package org.sy.baemin.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseEntity의 생성 시각(@CreatedDate)과 수정 시각(@LastModifiedDate)을 자동으로 채우는 기능을 켠다.
 * <p>
 * BaeminApplication(메인 클래스)에 붙이지 않고 별도 클래스로 둔 이유:
 * {@code @WebMvcTest}처럼 웹 계층만 띄우는 테스트에서도 메인 클래스는 항상 읽히는데,
 * 거기에 JPA 설정이 있으면 JPA 없이 띄우는 테스트가 실패한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
