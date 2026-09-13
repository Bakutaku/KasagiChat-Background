package com.kasagichat.api.conversation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.conversation.model.DailyQuestion;

/**
 * 今日のひとことの質問を永続化するRepository。
 */
public interface DailyQuestionRepository extends JpaRepository<DailyQuestion, Long> {

    /**
     * ユーザーの未使用の質問のうち、最も古いものを取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 未使用の質問。存在しない場合は空
     */
    Optional<DailyQuestion> findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtAsc(Long userId);
}
