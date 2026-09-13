package com.kasagichat.api.npc.controller.dto.npc.request;

import com.kasagichat.api.npc.model.enums.NpcPreset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * NPCの作成に必要な入力値。
 *
 * @param presetId 見た目のプリセットID
 * @param name NPCの名前
 */
public record CreateNpcRequest(
    @NotNull NpcPreset presetId,
    @NotBlank @Size(max = 30) String name
) {
}
