package com.kasagichat.api.master.model;

import com.kasagichat.api.common.model.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 話題カテゴリと、家に置く思い出の品の対応を管理するマスタEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "topic_categories",
    uniqueConstraints = @UniqueConstraint(name = "uk_topic_categories_code", columnNames = "code")
)
public class TopicCategory extends BaseTimeEntity {

    /**
     * 話題カテゴリの内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * プログラムから参照する識別コード。
     */
    @Column(nullable = false, length = 50)
    private String code;

    /**
     * 振り返り時にLLMが分類に使うカテゴリ名。
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 家に置く思い出の品の表示名。
     */
    @Column(nullable = false, length = 100)
    private String displayName;

    /**
     * 思い出の品の画像パス。
     */
    @Column(nullable = false, length = 255)
    private String itemImagePath;

    /**
     * 図鑑などでの表示順。
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer sortOrder = 0;

    /**
     * カテゴリが有効かどうか。
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;
}
