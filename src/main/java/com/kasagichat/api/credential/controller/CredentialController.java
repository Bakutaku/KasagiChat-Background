package com.kasagichat.api.credential.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.credential.controller.dto.request.CredentialUpdateRequest;
import com.kasagichat.api.credential.controller.dto.response.CredentialOptionsResponse;
import com.kasagichat.api.credential.controller.dto.response.CredentialResponse;
import com.kasagichat.api.credential.service.CredentialService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 認証済みユーザー本人のLLM利用設定API。
 */
@RestController
@RequestMapping("/api/credentials")
@RequiredArgsConstructor
public class CredentialController {

    private final CredentialService credentialService;

    /**
     * 現在のマスク済み設定を取得する。
     */
    @GetMapping
    public CredentialResponse get(@AuthenticationPrincipal LoginUserPrincipal principal) {
        return credentialService.getCurrent(principal.userId());
    }

    /**
     * 設定画面で利用可能なプロバイダーとモデルを取得する。
     */
    @GetMapping("/options")
    public CredentialOptionsResponse options() {
        return credentialService.getOptions();
    }

    /**
     * APIキーまたはデモ利用設定を登録・変更する。
     */
    @PutMapping
    public CredentialResponse update(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Valid @RequestBody CredentialUpdateRequest request
    ) {
        return credentialService.update(principal.userId(), request);
    }

    /**
     * 現在の設定を削除する。
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal LoginUserPrincipal principal) {
        credentialService.delete(principal.userId());
    }
}
