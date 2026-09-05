package com.kasagichat.api.common.model;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * 監査情報を持つエンティティのベースクラス
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity extends BaseTimeEntity {
    
    /**
     * 作成者
     */
    @CreatedBy
    @Column(updatable = false)
    private Long createdBy;

    /**
     * 更新者
     */
    @LastModifiedBy
    @Column
    private Long updatedBy;

}
