package com.kasagichat.api.npc.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.npc.controller.dto.growthevent.response.GrowthEventResponse;
import com.kasagichat.api.npc.model.GrowthEvent;
import com.kasagichat.api.npc.repository.GrowthEventRepository;

import lombok.RequiredArgsConstructor;

/**
 * 成長演出・実績達成の通知を扱うService。
 */
@Service
@RequiredArgsConstructor
public class GrowthEventService {

    private final GrowthEventRepository growthEventRepository;

    /**
     * ユーザーの通知を取得する。
     *
     * @param userId ユーザーの内部ID
     * @param unreadOnly trueの場合は未読の通知を古い順に、falseの場合は直近50件を新しい順に取得する
     * @return 通知の一覧
     */
    @Transactional(readOnly = true)
    public List<GrowthEventResponse> getGrowthEvents(Long userId, boolean unreadOnly) {
        List<GrowthEvent> growthEvents = unreadOnly
            ? growthEventRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtAsc(userId)
            : growthEventRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
        return growthEvents.stream().map(GrowthEventResponse::from).toList();
    }

    /**
     * ユーザーの未読の通知をすべて既読にする。
     *
     * @param userId ユーザーの内部ID
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        growthEventRepository.markAllAsRead(userId, Instant.now());
    }
}
