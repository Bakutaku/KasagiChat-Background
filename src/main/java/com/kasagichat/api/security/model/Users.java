package com.kasagichat.api.security.model;

import java.util.UUID;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import com.kasagichat.api.common.model.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 本登録済みユーザーのプロフィール情報を保持するEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
@Table(indexes = {
        @jakarta.persistence.Index(name = "idx_users_public_id", columnList = "public_id"),
        @jakarta.persistence.Index(name = "idx_users_display_name", columnList = "display_name")
})
public class Users extends BaseTimeEntity {

    /**
     * ユーザーの内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * APIでユーザーを識別する公開ID。
     */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID publicId;

    /**
     * ユーザーの表示名。
     */
    @Column(nullable = false, length = 50)
    private String displayName;

    /**
     * ユーザーのアバター画像URL。
     */
    @Column(length = 2048)
    private String avatarUrl;

    /**
     * ユーザーの公開IDを生成する。
     */
    @PrePersist
    void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
