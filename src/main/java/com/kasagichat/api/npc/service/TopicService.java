package com.kasagichat.api.npc.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.npc.controller.dto.npc.response.TopicResponse;
import com.kasagichat.api.npc.exception.NpcNotFoundException;
import com.kasagichat.api.npc.exception.TopicNotFoundException;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.Topic;
import com.kasagichat.api.npc.repository.TopicRepository;

import lombok.RequiredArgsConstructor;

/**
 * NPCが覚えた話題の一覧・公開設定・削除を行うService。
 */
@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    private final NpcService npcService;

    /**
     * ユーザーのNPCが覚えた話題を、覚えた日時の新しい順に取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 話題の一覧
     * @throws NpcNotFoundException NPCが未作成の場合
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopics(Long userId) {
        Npc npc = npcService.findOwnNpc(userId);
        return topicRepository.findByNpcIdOrderByLearnedAtDesc(npc.getId()).stream()
            .map(TopicResponse::from)
            .toList();
    }

    /**
     * 話題の公開/非公開を切り替える。
     *
     * @param userId ユーザーの内部ID
     * @param topicId 話題の内部ID
     * @param isPublic イベントのマッチングとカード生成に使ってよいかどうか
     * @return 更新後の話題
     * @throws TopicNotFoundException 話題が存在しない、または他人の話題の場合
     */
    @Transactional
    public TopicResponse updateVisibility(Long userId, Long topicId, boolean isPublic) {
        Topic topic = findOwnTopic(userId, topicId);
        topic.setPublicTopic(isPublic);
        topic.setVisibilityDecidedAt(Instant.now());
        return TopicResponse.from(topic);
    }

    /**
     * 話題を削除する。プライバシーのため物理削除とし、所属する思い出も一緒に削除する。
     *
     * @param userId ユーザーの内部ID
     * @param topicId 話題の内部ID
     * @throws TopicNotFoundException 話題が存在しない、または他人の話題の場合
     */
    @Transactional
    public void deleteTopic(Long userId, Long topicId) {
        topicRepository.delete(findOwnTopic(userId, topicId));
    }

    private Topic findOwnTopic(Long userId, Long topicId) {
        return topicRepository.findByIdAndNpcUserId(topicId, userId)
            .orElseThrow(TopicNotFoundException::new);
    }
}
