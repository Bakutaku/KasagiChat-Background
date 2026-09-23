package com.kasagichat.api.npc.service;

import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.npc.controller.dto.response.HomeResponse.ItemResponse;
import com.kasagichat.api.npc.exception.TopicNotFoundException;
import com.kasagichat.api.npc.model.Topic;
import com.kasagichat.api.npc.repository.TopicRepository;
import com.kasagichat.api.security.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * プロフィール帳からの話題の公開範囲の管理と削除を行う。
 *
 * <p>一覧表示は家の取得（{@code GET /api/home}）が全話題を返すため、ここでは提供しない。
 * 家の配置を担う{@link HomeService}とは関心が異なるため、更新系だけを持つ専用のサービスとする。</p>
 */
@Service
@RequiredArgsConstructor
public class TopicService {

    private final UserService userService;
    private final TopicRepository topicRepository;

    /**
     * 話題の公開/非公開を切り替える。
     *
     * <p>初めて公開を判断した場合だけ判断日時を記録し、以後の切り替えでは書き換えない。
     * 判断日時は「ユーザーが一度は確認した」ことを表すため、値が変わらない再送では何も保存しない。</p>
     *
     * @param userId 認証済みユーザーの内部ID
     * @param topicId 話題の内部ID
     * @param publicTopic 公開するならtrue
     * @return 更新後の話題
     */
    @Transactional
    public ItemResponse updateVisibility(Long userId, Long topicId, Boolean publicTopic) {
        Topic topic = ownedTopic(userId, topicId);
        boolean firstDecision = topic.getVisibilityDecidedAt() == null;
        if (firstDecision || !Objects.equals(topic.getPublicTopic(), publicTopic)) {
            topic.setPublicTopic(publicTopic);
            if (firstDecision) {
                topic.setVisibilityDecidedAt(Instant.now());
            }
            topicRepository.saveAndFlush(topic);
        }
        return ItemResponse.from(topic, userId);
    }

    /**
     * 話題を物理削除する。
     *
     * <p>プライバシーのため論理削除にはせず、所属する思い出も一緒に消す（Entityのカスケード）。
     * 家に配置していた場合は配置情報ごと消え、品物も表示されなくなる。</p>
     *
     * @param userId 認証済みユーザーの内部ID
     * @param topicId 話題の内部ID
     */
    @Transactional
    public void delete(Long userId, Long topicId) {
        topicRepository.delete(ownedTopic(userId, topicId));
        topicRepository.flush();
    }

    /** 他人の話題は「存在しない」として扱い、対象の存在を知られないようにする。 */
    private Topic ownedTopic(Long userId, Long topicId) {
        userService.getCurrentUser(userId);
        return topicRepository.findByIdAndNpcUserId(topicId, userId)
            .orElseThrow(TopicNotFoundException::new);
    }
}
