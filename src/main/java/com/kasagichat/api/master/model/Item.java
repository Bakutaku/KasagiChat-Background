package com.kasagichat.api.master.model;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.master.model.enums.ItemType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 服やアクセサリーなど、解禁できるアイテムを管理するマスタEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "items",
    uniqueConstraints = @UniqueConstraint(name = "uk_items_code", columnNames = "code")
)
public class Item extends BaseTimeEntity {

    /**
     * アイテムの内部ID。
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
     * アイテムの種類。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemType itemType;

    /**
     * アイテムの表示名。
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * アイテムの画像パス。
     */
    @Column(nullable = false, length = 255)
    private String imagePath;

    /**
     * アイテムが有効かどうか。
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;
}
