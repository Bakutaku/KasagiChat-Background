package com.kasagichat.api.npc.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 配置・移動先。収納はDELETEで行い、nullや省略を更新として扱わない。 */
public record HomePlacementRequest(@NotBlank @Size(max = 30) String slotId) {
}
