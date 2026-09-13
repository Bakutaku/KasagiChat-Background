package com.kasagichat.api.npc.model;

import java.time.Instant;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.master.model.Item;
import com.kasagichat.api.security.model.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ユーザーが解禁した服・アクセサリーを保持するEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "unlocked_items",
    uniqueConstraints = @UniqueConstraint(name = "uk_unlocked_items_user_item", columnNames = {"user_id", "item_id"})
)
public class UnlockedItem extends BaseTimeEntity {

    /**
     * 解禁記録の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * アイテムを解禁したユーザー。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * 解禁したアイテム。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /**
     * アイテムを得た実績。実績以外で得た場合はnull。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id")
    private Achievement achievement;

    /**
     * アイテムを解禁した日時。
     */
    @Column(nullable = false)
    private Instant unlockedAt;
}
