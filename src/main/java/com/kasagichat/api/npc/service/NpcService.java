package com.kasagichat.api.npc.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.credential.exception.CredentialNotConfiguredException;
import com.kasagichat.api.credential.repository.ApiCredentialRepository;
import com.kasagichat.api.npc.controller.dto.request.CreateNpcRequest;
import com.kasagichat.api.npc.controller.dto.response.NpcResponse;
import com.kasagichat.api.npc.exception.NpcAlreadyExistsException;
import com.kasagichat.api.npc.exception.NpcNotFoundException;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * 誕生会話前のNPC作成と基本状態の取得を行う。
 */
@Service
@RequiredArgsConstructor
public class NpcService {

    private final NpcRepository npcRepository;
    private final ApiCredentialRepository apiCredentialRepository;
    private final UserService userService;

    /**
     * 未誕生状態のNPCを1体だけ作成する。
     *
     * @param userId 認証済みユーザーの内部ID
     * @param request 名前とプリセット
     * @return 作成したNPC
     */
    @Transactional
    public NpcResponse create(Long userId, CreateNpcRequest request) {
        if (!apiCredentialRepository.existsByUserId(userId)) {
            throw new CredentialNotConfiguredException();
        }
        if (npcRepository.existsByUserId(userId)) {
            throw new NpcAlreadyExistsException();
        }

        Users user = userService.getCurrentUser(userId);
        Npc npc = Npc.builder()
            .user(user)
            .name(request.name().strip())
            .presetId(request.presetId().strip())
            .build();
        try {
            npcRepository.saveAndFlush(npc);
        } catch (DataIntegrityViolationException exception) {
            throw new NpcAlreadyExistsException();
        }
        return NpcResponse.from(npc);
    }

    /**
     * 本人のNPCを取得する。
     *
     * @param userId 認証済みユーザーの内部ID
     * @return NPCの基本状態
     */
    @Transactional(readOnly = true)
    public NpcResponse get(Long userId) {
        return NpcResponse.from(npcRepository.findByUserId(userId)
            .orElseThrow(NpcNotFoundException::new));
    }
}
