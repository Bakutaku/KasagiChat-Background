package com.kasagichat.api.npc.controller.dto.request;

import jakarta.validation.constraints.Size;

/**
 * プロフィール帳からのNPC設定の部分更新。
 *
 * <p>3項目とも省略可能で、nullは「変更しない」を意味する。空文字列は「内容を空にする」という
 * 正当な更新として扱い、nullとは区別する。{@link Size}はnullを素通りさせるため、
 * 送られた場合だけ文字数上限を検証する。</p>
 *
 * @param profile 人格文書。上限は振り返りで生成する長さ（4000文字）に揃える
 * @param speechStyle 口調の説明文。上限は振り返りで生成する長さ（1000文字）に揃える
 * @param speechStyleEnabled 口調を会話へ反映するかどうか
 */
public record UpdateNpcSettingsRequest(
    @Size(max = 4000) String profile,
    @Size(max = 1000) String speechStyle,
    Boolean speechStyleEnabled
) {
}
