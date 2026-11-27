package org.sy.baemin.user.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.sy.baemin.auth.dto.SignupRequest;
import org.sy.baemin.auth.dto.SignupResponse;
import org.sy.baemin.common.BusinessException;
import org.sy.baemin.common.ErrorCode;
import org.sy.baemin.common.id.SnowflakeIdGenerator;
import org.sy.baemin.support.IntegrationTestSupport;
import org.sy.baemin.user.domain.User;
import org.sy.baemin.user.domain.UserRepository;
import org.sy.baemin.user.domain.UserRole;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 MySQL(Testcontainers)에 저장해 보는 통합 테스트.
 * 테스트마다 저장한 회원은 @AfterEach에서 지운다.
 * (동시성 테스트는 여러 스레드가 각자 커밋해야 해서 @Transactional 롤백 방식을 쓰지 않는다)
 */
@SpringBootTest
class UserServiceTest extends IntegrationTestSupport {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    SnowflakeIdGenerator idGenerator;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAllInBatch();
    }

    private SignupRequest request(String email) {
        return new SignupRequest(email, "pass1234!", "김배민", UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("회원가입하면 회원이 저장되고, 응답에 ID가 문자열로 담긴다")
    void signup() {
        SignupResponse response = userService.signup(request("kim@baemin.com"));

        User saved = userRepository.findByEmail("kim@baemin.com").orElseThrow();
        assertThat(response.id()).isEqualTo(String.valueOf(saved.getId()));
        assertThat(saved.getName()).isEqualTo("김배민");
        assertThat(saved.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(saved.getCreatedAt()).isNotNull();   // JPA Auditing 동작 확인
    }

    @Test
    @DisplayName("비밀번호는 평문이 아니라 BCrypt로 암호화되어 저장된다")
    void passwordIsEncoded() {
        userService.signup(request("kim@baemin.com"));

        User saved = userRepository.findByEmail("kim@baemin.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("pass1234!");
        assertThat(passwordEncoder.matches("pass1234!", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 DUPLICATE_EMAIL 예외가 발생한다")
    void duplicateEmail() {
        userService.signup(request("kim@baemin.com"));

        assertThatThrownBy(() -> userService.signup(request("kim@baemin.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("같은 이메일로 동시에 10번 가입해도 1명만 성공하고 나머지는 DUPLICATE_EMAIL이다")
    void concurrentSignupWithSameEmail() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();   // 모든 스레드가 준비되면 동시에 출발
                    userService.signup(request("same@baemin.com"));
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.DUPLICATE_EMAIL) {
                        duplicate.incrementAndGet();
                    } else {
                        other.incrementAndGet();
                    }
                } catch (Exception e) {
                    other.incrementAndGet();   // 500으로 새어 나갈 예외가 있으면 여기로 온다
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(success.get()).isEqualTo(1);
        assertThat(duplicate.get()).isEqualTo(threadCount - 1);
        assertThat(other.get()).isZero();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("새 엔티티를 save()하면 merge가 아니라 persist로 처리된다 (BaseEntity.isNew)")
    void saveUsesPersistNotMerge() {
        User user = new User(idGenerator.nextId(), "new@baemin.com", "encoded", "김배민", UserRole.CUSTOMER);
        assertThat(user.isNew()).isTrue();

        User returned = userRepository.save(user);

        // persist는 넘긴 객체를 그대로 관리하고, merge는 복사본을 만들어 돌려준다.
        assertThat(returned).isSameAs(user);
        assertThat(user.isNew()).isFalse();   // @PostPersist 가 호출됨
    }
}
