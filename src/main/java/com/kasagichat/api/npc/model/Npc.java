package com.kasagichat.api.npc.model;

import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.security.model.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ユーザーの分身であるNPCの成長状態と人格を保持するEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "npcs",
    uniqueConstraints = @UniqueConstraint(name = "uk_npcs_user_id", columnNames = "user_id")
)
public class Npc extends BaseTimeEntity {

    /**
     * NPCの内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * NPCの持ち主。1ユーザーにつき1体。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * NPCの名前。
     */
    @Column(nullable = false, length = 30)
    private String name;

    /**
     * 見た目のプリセットID。フロントエンドとサーバーで同じIDを定義する。
     */
    @Column(nullable = false, length = 50)
    private String presetId;

    /**
     * 現在のレベル。
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer level = 1;

    /**
     * これまでに獲得した累計EXP。
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer exp = 0;

    /**
     * 着せ替えのレイヤー構成。キーはレイヤー名、値はアイテムコード。
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> appearance;

    /**
     * 人格文書。性格・価値観・コミュニケーションの癖を日本語の文章で持つ。
     */
    @Column(columnDefinition = "TEXT")
    private String profile;

    /**
     * 口調の特徴を説明する文章。振り返り時にLLMが更新する。
     */
    @Column(columnDefinition = "TEXT")
    private String speechStyle;

    /**
     * 口調を会話へ反映するかどうか。
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean speechStyleEnabled = true;

    /**
     * 誕生した日時。誕生会話の振り返りが成功するまではnull。
     */
    @Column
    private Instant bornAt;
}
