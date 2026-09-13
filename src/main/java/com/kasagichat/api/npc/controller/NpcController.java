package com.kasagichat.api.npc.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.npc.controller.dto.npc.request.CreateNpcRequest;
import com.kasagichat.api.npc.controller.dto.npc.request.UpdateNpcRequest;
import com.kasagichat.api.npc.controller.dto.npc.request.UpdateTopicRequest;
import com.kasagichat.api.npc.controller.dto.npc.response.NpcResponse;
import com.kasagichat.api.npc.controller.dto.npc.response.TopicResponse;
import com.kasagichat.api.npc.exception.NpcAlreadyExistsException;
import com.kasagichat.api.npc.exception.NpcNotFoundException;
import com.kasagichat.api.npc.exception.TopicNotFoundException;
import com.kasagichat.api.npc.service.NpcService;
import com.kasagichat.api.npc.service.TopicService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * NPCとプロフィール帳（話題を含む）を扱うController。
 */
@RestController
@RequestMapping("/api/npc")
@RequiredArgsConstructor
public class NpcController {

    private final NpcService npcService;

    private final TopicService topicService;

    /**
     * 未誕生状態のNPCを作成する。
     *
     * @param principal サーバーセッションから復元されたユーザーの認証主体
     * @param request プリセットIDと名前
     * @return 作成したNPC
     * @throws NpcAlreadyExistsException NPCを作成済みの場合
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NpcResponse create(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @Valid @RequestBody CreateNpcRequest request
    ) {
        return npcService.createNpc(principal.userId(), request);
    }

    /**
     * 自分のNPCのプロフィール帳を取得する。
     *
     * @param principal サーバーセッションから復元されたユーザーの認証主体
     * @return NPCのプロフィール帳
     * @throws NpcNotFoundException NPCが未作成の場合
     */
    @GetMapping
    public NpcResponse get(@AuthenticationPrincipal LoginUserPrincipal principal) {
        return npcService.getNpc(principal.userId());
    }

    /**
     * 自分のNPCの人格文書・口調・口調反映の設定を更新する。
     *
     * @param principal サーバーセッションから復元されたユーザーの認証主体
     * @param request 更新する項目。nullの項目は変更しない
     * @return 更新後のNPCのプロフィール帳
     * @throws NpcNotFoundException NPCが未作成の場合
     */
    @PatchMapping
    public NpcResponse update(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @Valid @RequestBody UpdateNpcRequest request
    ) {
        return npcService.updateNpc(principal.userId(), request);
    }

    /**
     * 自分のNPCが覚えた話題を取得する。
     *
     * @param principal サーバーセッションから復元されたユーザーの認証主体
     * @return 話題の一覧
     * @throws NpcNotFoundException NPCが未作成の場合
     */
    @GetMapping("/topics")
    public List<TopicResponse> topics(@AuthenticationPrincipal LoginUserPrincipal principal) {
        return topicService.getTopics(principal.userId());
    }

    /**
     * 話題の公開/非公開を切り替える。
     *
     * @param principal サーバーセッションから復元されたユーザーの認証主体
     * @param id 話題の内部ID
     * @param request 公開するかどうか
     * @return 更新後の話題
     * @throws TopicNotFoundException 話題が存在しない、または他人の話題の場合
     */
    @PatchMapping("/topics/{id}")
    public TopicResponse updateTopic(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @PathVariable Long id,
        @Valid @RequestBody UpdateTopicRequest request
    ) {
        return topicService.updateVisibility(principal.userId(), id, request.isPublic());
    }

    /**
     * 話題を削除する。対応する思い出の品も表示されなくなる。
     *
     * @param principal サーバーセッションから復元されたユーザーの認証主体
     * @param id 話題の内部ID
     * @throws TopicNotFoundException 話題が存在しない、または他人の話題の場合
     */
    @DeleteMapping("/topics/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTopic(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @PathVariable Long id
    ) {
        topicService.deleteTopic(principal.userId(), id);
    }
}
