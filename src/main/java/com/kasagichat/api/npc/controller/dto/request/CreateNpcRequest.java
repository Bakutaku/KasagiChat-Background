package com.kasagichat.api.npc.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * NPC誕生会話を始める前の、名前と見た目の選択。
 *
 * @param presetId フロントエンドと共有する見た目のプリセットID
 * @param name NPCの名前
 */
public record CreateNpcRequest(
    @NotBlank @Size(max = 50) String presetId,
    @NotBlank @Size(max = 30) String name
) {
}
