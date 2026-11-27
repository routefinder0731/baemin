package org.sy.baemin.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sy.baemin.auth.dto.SignupRequest;
import org.sy.baemin.auth.dto.SignupResponse;
import org.sy.baemin.common.BusinessException;
import org.sy.baemin.common.ErrorCode;
import org.sy.baemin.common.id.SnowflakeIdGenerator;
import org.sy.baemin.user.domain.User;
import org.sy.baemin.user.domain.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator idGenerator;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 애플리케이션으로 이메일 중복 1차 체크
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // Snowflake ID로 User를 만들어 저장
        User user = new User(
                idGenerator.nextId(),
                request.email(),
                encodedPassword,
                request.name(),
                request.role()
        );

        // save는 영속성 컨텍스트에만 보관 (트랜잭션이 끝나야 커밋 (insert 전))
        // saveAndFlush는 저장하고 즉시 DB에 반영(insert가 메서드 안에서 수행)
        // 동시 요청은 DB UNIQUE 제약으로 2차 체크
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // SignupResponse로 변환해서 반환
        return SignupResponse.from(user);
    }
}
