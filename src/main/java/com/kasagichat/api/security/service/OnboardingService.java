package com.kasagichat.api.security.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.credential.repository.ApiCredentialRepository;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.security.controller.dto.response.OnboardingStatusResponse;

import lombok.RequiredArgsConstructor;

/**
 * 初回オンボーディングの進行状態を集約する。
 */
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final ApiCredentialRepository apiCredentialRepository;
    private final NpcRepository npcRepository;

    /**
     * 認証済みユーザーのオンボーディング状態を取得する。
     *
     * @param userId ユーザーの内部ID
     * @return オンボーディング状態
     */
    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus(Long userId) {
        return new OnboardingStatusResponse(
            apiCredentialRepository.existsByUserId(userId),
            npcRepository.existsByUserId(userId),
            npcRepository.existsByUserIdAndBornAtIsNotNull(userId)
        );
    }
}
