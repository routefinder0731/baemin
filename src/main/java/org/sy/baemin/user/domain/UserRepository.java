package org.sy.baemin.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 가입된 회원이 있는지 (회원가입 중복 확인용)
    boolean existsByEmail(String email);

    // 이메일로 회원 찾기 (다음 브랜치 로그인용)
    Optional<User> findByEmail(String email);
}
