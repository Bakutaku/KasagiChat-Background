package com.kasagichat.api.conversation.model;

import java.time.Instant;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.security.model.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 今日のひとことでNPCがユーザーに聞く質問を保持するEntity。振り返り時に生成する。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "daily_questions",
    indexes = @Index(name = "idx_daily_questions_user_consumed_at", columnList = "user_id, consumed_at")
)
public class DailyQuestion extends BaseTimeEntity {

    /**
     * 質問の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 質問の対象ユーザー。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * 質問文。
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    /**
     * 質問を生成した振り返りの会話。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_conversation_id")
    private Conversation sourceConversation;

    /**
     * 今日のひとことで使用した日時。未使用の場合はnull。
     */
    @Column
    private Instant consumedAt;
}
