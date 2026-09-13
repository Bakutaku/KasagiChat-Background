package com.kasagichat.api.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.conversation.model.enums.ConversationScene;
import com.kasagichat.api.conversation.model.enums.ConversationType;
import com.kasagichat.api.master.model.ConversationOpening;

/**
 * 会話の冒頭マスタを操作するRepository。
 */
public interface ConversationOpeningRepository extends JpaRepository<ConversationOpening, Long> {

    /**
     * 会話種別とシーンに対応する、抽選対象の冒頭を取得する。
     *
     * @param conversationType 会話種別
     * @param scene 練習シーン。BIRTHの場合はnull
     * @return 抽選対象の冒頭の一覧
     */
    List<ConversationOpening> findByConversationTypeAndSceneAndEnabledTrue(
        ConversationType conversationType,
        ConversationScene scene
    );
}
