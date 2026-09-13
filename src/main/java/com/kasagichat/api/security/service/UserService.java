package com.kasagichat.api.security.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.kasagichat.api.credential.repository.ApiCredentialRepository;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.security.controller.dto.response.OnboardingResponse;
import com.kasagichat.api.security.exception.UserNotFoundException;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@RequiredArgsConstructor 
@Slf4j 
public class UserService {
    
    private final UsersRepository usersRepository;

    private final ApiCredentialRepository apiCredentialRepository;

    private final NpcRepository npcRepository;

    /**
     * 公開ユーザーIDからユーザーを取得する。
     * @param publicId 公開ユーザーID
     * @return 該当するユーザー
     */
    public Users getCurrentUser(UUID publicId) {
        return usersRepository.findByPublicId(publicId)
            .orElseThrow(() -> {
                log.warn("ユーザーが見つかりませんでした。publicId: {}", publicId);
                return new UserNotFoundException();
            });
    }

        /**
     * 公開ユーザーIDからユーザーを取得する。
     * @param id ユーザーID
     * @return 該当するユーザー
     */
    public Users getCurrentUser(Long id) {
        return usersRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("ユーザーが見つかりませんでした。id: {}", id);
                return new UserNotFoundException();
            });
    }

    /**
     * 初回フロー（APIキー設定 → NPC誕生）の進み具合を取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 初回フローの進み具合
     */
    public OnboardingResponse getOnboarding(Long userId) {
        Optional<Npc> npc = npcRepository.findByUserId(userId);
        return new OnboardingResponse(
            apiCredentialRepository.existsByUserId(userId),
            npc.isPresent(),
            npc.map(found -> found.getBornAt() != null).orElse(false)
        );
    }
}
