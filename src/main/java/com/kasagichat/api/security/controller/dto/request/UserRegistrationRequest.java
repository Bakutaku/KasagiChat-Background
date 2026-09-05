package com.kasagichat.api.security.controller.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/**
 * ユーザーの本登録に必要な入力値。
 *
 * @param displayName 登録する表示名
 * @param agreedTermsIds ユーザーが同意した規約IDの集合
 */
public record UserRegistrationRequest(
    @NotNull @NotBlank @Size(max = 50) String displayName,
    @NotNull Set<Long> agreedTermsIds
) {
}
