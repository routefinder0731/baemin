package org.sy.baemin.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

/**
 * 모든 엔티티의 부모 클래스. ID, 생성 시각, 수정 시각을 공통으로 관리한다.
 *
 * <h3>ID는 저장 전에 애플리케이션이 정한다</h3>
 * DB가 ID를 정하는 AUTO_INCREMENT 대신, 엔티티를 만들 때 Snowflake ID를 생성자로 받는다.
 * 저장하기 전에 ID를 알 수 있어서, 같은 트랜잭션 안에서 "주문 123번 생성" 같은 이벤트를 만들 수 있다.
 * <pre>
 * Order order = new Order(idGenerator.nextId(), ...);
 * </pre>
 *
 * <h3>Persistable을 구현하는 이유</h3>
 * Spring Data JPA의 save()는 기본적으로 "ID가 null이면 새 데이터"로 판단한다.
 * ID를 미리 넣으면 이미 있는 데이터로 오해해 persist() 대신 merge()를 호출하고,
 * 그 결과 (1) 불필요한 SELECT가 한 번 더 나가고, (2) 넘긴 객체 대신 복사본이 관리되어 변경 감지가 동작하지 않는다.
 * isNew()를 직접 구현해 "아직 저장도 조회도 안 된 객체"만 새 데이터로 판단하게 한다.
 */
@MappedSuperclass   // 이 클래스 자체는 테이블이 아니고, 필드만 자식 엔티티의 컬럼으로 물려준다
@EntityListeners(AuditingEntityListener.class)   // @CreatedDate, @LastModifiedDate 를 자동으로 채운다
public abstract class BaseEntity implements Persistable<Long> {

    @Id
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** DB 컬럼이 아닌, 메모리에서만 쓰는 표시. 새로 만든 객체는 true로 시작한다. */
    @Transient
    private boolean isNew = true;

    /** JPA가 DB에서 조회한 데이터로 객체를 만들 때 쓰는 기본 생성자. 직접 호출하지 않는다. */
    protected BaseEntity() {
    }

    /** 새 엔티티를 만들 때 쓰는 생성자. 자식 클래스가 super(id)로 호출한다. */
    protected BaseEntity(Long id) {
        this.id = Objects.requireNonNull(id, "id는 null일 수 없습니다.");
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 저장(INSERT) 직후, 또는 DB에서 조회한 직후에 JPA가 호출한다.
     * 이때부터는 "이미 DB에 있는 데이터"이므로 save()가 merge()로 동작해야 한다.
     */
    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
