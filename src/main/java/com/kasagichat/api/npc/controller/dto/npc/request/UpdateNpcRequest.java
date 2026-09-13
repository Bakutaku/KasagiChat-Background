package com.kasagichat.api.npc.controller.dto.npc.request;

import jakarta.validation.constraints.Size;

/**
 * NPCのプロフィール帳を更新する入力値。nullの項目は変更しない。
 *
 * @param profile 人格文書
 * @param speechStyle 口調の特徴を説明する文章
 * @param speechStyleEnabled 口調を会話へ反映するかどうか
 */
public record UpdateNpcRequest(
    @Size(max = 2000) String profile,
    @Size(max = 500) String speechStyle,
    Boolean speechStyleEnabled
) {
}
