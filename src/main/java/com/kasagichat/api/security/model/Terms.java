package com.kasagichat.api.security.model;

import java.time.Instant;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.security.model.enums.TermsType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * 規約マスタ
 * 利用規約等の規約管理用のマスタ
 */
@Getter
@Setter
@Entity
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_terms_type_version",
        columnNames = {"type", "version"} // typeとversionの組み合わせが一意であることを保証
    )
)
public class Terms extends BaseTimeEntity {

    /**
     * 規約ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 規約種類
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private TermsType type;

    /**
     * バージョン
     */
    @Column(nullable = false, length = 30, updatable = false)
    private String version;

    /**
     * タイトル
     */
    @Column(nullable = false, length = 200, updatable = false)
    private String title;

    /**
     * 規約内容
     */
    @Lob
    @Column(nullable = false, updatable = false)
    private String content;

    /**
     * 効力発生日
     */
    @Column(nullable = false)
    private Instant effectiveAt;
}
