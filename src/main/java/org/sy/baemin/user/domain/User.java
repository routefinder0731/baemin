package org.sy.baemin.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sy.baemin.common.entity.BaseEntity;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String email;

    // password, name 도 같은 방식으로 (길이는 SQL과 같게) / 완
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    public User(Long id, String email, String password, String name, UserRole role) {
        // 1. 부모(BaseEntity)에게 id 넘기기 / 완
        super(id);
        // 2. 나머지 필드 저장 / 완
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }
}
