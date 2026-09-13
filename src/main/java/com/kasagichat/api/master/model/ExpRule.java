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
 * 行動ごとに加算するEXPを管理するマスタEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "exp_rules",
    uniqueConstraints = @UniqueConstraint(name = "uk_exp_rules_code", columnNames = "code")
)
public class ExpRule extends BaseTimeEntity {

    /**
     * EXPルールの内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 行動を表すコード。例: USER_MESSAGE、PRACTICE_COMPLETE。
     */
    @Column(nullable = false, length = 50)
    private String code;

    /**
     * 行動1回あたりに加算するEXP。
     */
    @Column(nullable = false)
    private Integer exp;

    /**
     * ルールの説明。
     */
    @Column(length = 500)
    private String description;
}
