package com.kasagichat.api.npc.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.npc.controller.dto.npc.request.CreateNpcRequest;
import com.kasagichat.api.npc.controller.dto.npc.request.UpdateNpcRequest;
import com.kasagichat.api.npc.controller.dto.npc.response.CounterResponse;
import com.kasagichat.api.npc.controller.dto.npc.response.NpcResponse;
import com.kasagichat.api.npc.controller.dto.npc.response.NpcStatsResponse;
import com.kasagichat.api.npc.exception.NpcAlreadyExistsException;
import com.kasagichat.api.npc.exception.NpcNotFoundException;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.repository.AchievementCounterRepository;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.TopicRepository;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NPCの作成とプロフィール帳の取得・更新を行うService。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NpcService {

    private final NpcRepository npcRepository;

    private final TopicRepository topicRepository;

    private final AchievementCounterRepository achievementCounterRepository;

    private final UserService userService;

    private final LevelService levelService;

    /**
     * 未誕生状態のNPCを作成する。誕生会話の振り返りが成功すると誕生済みになる。
     *
     * @param userId ユーザーの内部ID
     * @param request プリセットIDと名前
     * @return 作成したNPC
     * @throws NpcAlreadyExistsException NPCを作成済みの場合
     */
    @Transactional
    public NpcResponse createNpc(Long userId, CreateNpcRequest request) {
        if (npcRepository.findByUserId(userId).isPresent()) {
            throw new NpcAlreadyExistsException();
        }

        Users user = userService.getCurrentUser(userId);
        Npc npc = Npc.builder()
            .user(user)
            .name(request.name().strip())
            .presetId(request.presetId().name())
            .build();
        try {
            // 同時に作成された場合はユニーク制約違反になるため、ここでフラッシュして検出する。
            npcRepository.saveAndFlush(npc);
        } catch (DataIntegrityViolationException exception) {
            log.warn("NPCの同時作成を検出しました。userId: {}", userId);
            throw new NpcAlreadyExistsException();
        }
        return toResponse(npc);
    }

    /**
     * ユーザーのNPCのプロフィール帳を取得する。
     *
     * @param userId ユーザーの内部ID
     * @return NPCのプロフィール帳
     * @throws NpcNotFoundException NPCが未作成の場合
     */
    @Transactional(readOnly = true)
    public NpcResponse getNpc(Long userId) {
        return toResponse(findOwnNpc(userId));
    }

    /**
     * NPCの人格文書・口調・口調反映の設定を更新する。nullの項目は変更しない。
     *
     * @param userId ユーザーの内部ID
     * @param request 更新する項目
     * @return 更新後のNPCのプロフィール帳
     * @throws NpcNotFoundException NPCが未作成の場合
     */
    @Transactional
    public NpcResponse updateNpc(Long userId, UpdateNpcRequest request) {
        Npc npc = npcRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> {
                log.debug("NPCが見つかりませんでした。userId: {}", userId);
                return new NpcNotFoundException();
            });
        if (request.profile() != null) {
            npc.setProfile(request.profile());
        }
        if (request.speechStyle() != null) {
            npc.setSpeechStyle(request.speechStyle());
        }
        if (request.speechStyleEnabled() != null) {
            npc.setSpeechStyleEnabled(request.speechStyleEnabled());
        }
        return toResponse(npc);
    }

    /**
     * ユーザーのNPCを取得する。
     *
     * @param userId ユーザーの内部ID
     * @return ユーザーのNPC
     * @throws NpcNotFoundException NPCが未作成の場合
     */
    public Npc findOwnNpc(Long userId) {
        return npcRepository.findByUserId(userId)
            .orElseThrow(() -> {
                log.debug("NPCが見つかりませんでした。userId: {}", userId);
                return new NpcNotFoundException();
            });
    }

    private NpcResponse toResponse(Npc npc) {
        NpcStatsResponse stats = new NpcStatsResponse(
            topicRepository.countByNpcId(npc.getId()),
            achievementCounterRepository.findByUserId(npc.getUser().getId()).stream()
                .map(CounterResponse::from)
                .toList()
        );
        return NpcResponse.from(npc, levelService.findNextLevelExp(npc.getLevel()), stats);
    }
}
