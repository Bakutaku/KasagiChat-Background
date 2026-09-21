package com.kasagichat.api.conversation.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 会話へのユーザー発言。
 *
 * @param text 発言本文
 * @param expectedTurn クライアントが最後に確認した往復数
 */
public record SendMessageRequest(
    @NotBlank @Size(max = 2000) String text,
    @NotNull @Min(0) Integer expectedTurn
) {
}
