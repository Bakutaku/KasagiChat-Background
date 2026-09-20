package com.kasagichat.api.credential.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.credential.LlmProperties;
import com.kasagichat.api.credential.controller.dto.request.CredentialUpdateRequest;
import com.kasagichat.api.credential.controller.dto.response.CredentialOptionsResponse;
import com.kasagichat.api.credential.controller.dto.response.CredentialResponse;
import com.kasagichat.api.credential.controller.dto.response.DemoUsageResponse;
import com.kasagichat.api.credential.controller.dto.response.ProviderOptionResponse;
import com.kasagichat.api.credential.crypto.ApiKeyCipher;
import com.kasagichat.api.credential.exception.InvalidApiCredentialException;
import com.kasagichat.api.credential.exception.InvalidCredentialRequestException;
import com.kasagichat.api.credential.exception.InvalidPassphraseException;
import com.kasagichat.api.credential.exception.LlmConfigurationException;
import com.kasagichat.api.credential.model.ApiCredential;
import com.kasagichat.api.credential.model.enums.LlmProvider;
import com.kasagichat.api.credential.provider.base.LlmProviderRegistry;
import com.kasagichat.api.credential.repository.ApiCredentialRepository;
import com.kasagichat.api.credential.repository.DemoUsageRepository;
import com.kasagichat.api.master.model.DemoPassphrase;
import com.kasagichat.api.master.repository.DemoPassphraseRepository;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * 認証済みユーザー本人のAPIキー・デモ利用設定を管理する。
 */
@Service
@RequiredArgsConstructor
public class CredentialService {

    private final ApiCredentialRepository apiCredentialRepository;
    private final DemoUsageRepository demoUsageRepository;
    private final DemoPassphraseRepository demoPassphraseRepository;
    private final UserService userService;
    private final LlmProviderRegistry providerRegistry;
    private final ApiKeyCipher apiKeyCipher;
    private final LlmProperties properties;

    /**
     * 現在の設定を取得する。生のAPIキーと合言葉は返さない。
     *
     * @param userId 認証済みユーザーの内部ID
     * @return 現在の設定
     */
    @Transactional(readOnly = true)
    public CredentialResponse getCurrent(Long userId) {
        return apiCredentialRepository.findByUserId(userId)
            .map(credential -> toResponse(credential, userId))
            .orElseGet(CredentialResponse::unconfigured);
    }

    /**
     * 設定画面で選択可能なプロバイダーとモデルを返す。
     *
     * @return プロバイダー選択肢
     */
    public CredentialOptionsResponse getOptions() {
        ProviderOptionResponse openai = byokOption(LlmProvider.OPENAI);
        ProviderOptionResponse anthropic = byokOption(LlmProvider.ANTHROPIC);
        List<String> demoModels = properties.demo().model().isBlank()
            ? List.of()
            : List.of(properties.demo().model());
        ProviderOptionResponse demo = new ProviderOptionResponse(
            LlmProvider.DEMO,
            demoModels,
            false,
            false,
            true,
            properties.isDemoConfigured()
        );
        return new CredentialOptionsResponse(List.of(openai, anthropic, demo));
    }

    /**
     * APIキーまたはデモ利用設定を登録・変更する。
     *
     * @param userId 認証済みユーザーの内部ID
     * @param request 登録内容
     * @return 保存後のマスク済み設定
     */
    public CredentialResponse update(Long userId, CredentialUpdateRequest request) {
        Users user = userService.getCurrentUser(userId);
        return switch (request.provider()) {
            case OPENAI, ANTHROPIC -> updateByok(user, request);
            case DEMO -> updateDemo(user, request);
        };
    }

    /**
     * 現在の設定を削除する。デモ利用回数はアカウント単位のため保持する。
     *
     * @param userId 認証済みユーザーの内部ID
     */
    @Transactional
    public void delete(Long userId) {
        apiCredentialRepository.findByUserId(userId).ifPresent(apiCredentialRepository::delete);
    }

    private CredentialResponse updateByok(Users user, CredentialUpdateRequest request) {
        rejectPresent(request.passphrase(), "BYOKではpassphraseを指定できません");
        String apiKey = requireText(request.apiKey(), "apiKeyは必須です");
        String model = requireText(request.model(), "modelは必須です");
        if (!properties.modelsFor(request.provider()).contains(model)) {
            throw new InvalidCredentialRequestException("選択できないモデルです");
        }
        // 暗号化設定不備なら外部APIを呼ぶ前に失敗させる。
        byte[] encryptedApiKey = apiKeyCipher.encrypt(apiKey);

        try {
            // 最大1トークン、再試行なしの1呼び出しでキーとモデルの双方を検証する。
            providerRegistry.get(request.provider()).verify(apiKey, model);
        } catch (RuntimeException exception) {
            // プロバイダーの例外本文には秘密値が含まれる可能性があるため、ログやレスポンスへ引き継がない。
            throw new InvalidApiCredentialException();
        }

        ApiCredential credential = apiCredentialRepository.findByUserId(user.getId())
            .orElseGet(() -> ApiCredential.builder().user(user).build());
        credential.setProvider(request.provider());
        credential.setModelName(model);
        credential.setEncryptedApiKey(encryptedApiKey);
        credential.setMaskedKey(mask(apiKey));
        credential.setDemoPassphrase(null);
        return toResponse(apiCredentialRepository.save(credential), user.getId());
    }

    private CredentialResponse updateDemo(Users user, CredentialUpdateRequest request) {
        rejectPresent(request.apiKey(), "DEMOではapiKeyを指定できません");
        rejectPresent(request.model(), "DEMOのモデルは運営設定で固定されています");
        if (!properties.isDemoConfigured()) {
            throw new LlmConfigurationException("デモ用LLMが設定されていません");
        }
        String passphraseValue = requireText(request.passphrase(), "passphraseは必須です");
        DemoPassphrase passphrase = demoPassphraseRepository
            .findByPassphraseAndEnabledTrue(passphraseValue)
            .orElseThrow(InvalidPassphraseException::new);

        ApiCredential credential = apiCredentialRepository.findByUserId(user.getId())
            .orElseGet(() -> ApiCredential.builder().user(user).build());
        credential.setProvider(LlmProvider.DEMO);
        credential.setModelName(null);
        credential.setEncryptedApiKey(null);
        credential.setMaskedKey(null);
        credential.setDemoPassphrase(passphrase);
        return toResponse(apiCredentialRepository.save(credential), user.getId());
    }

    private ProviderOptionResponse byokOption(LlmProvider provider) {
        List<String> models = properties.modelsFor(provider);
        return new ProviderOptionResponse(provider, models, true, true, false, !models.isEmpty());
    }

    private CredentialResponse toResponse(ApiCredential credential, Long userId) {
        if (credential.getProvider() != LlmProvider.DEMO) {
            return new CredentialResponse(
                true,
                credential.getProvider(),
                credential.getModelName(),
                credential.getMaskedKey(),
                null
            );
        }

        int used = demoUsageRepository.findByUserId(userId)
            .map(usage -> usage.getCallCount() == null ? 0 : usage.getCallCount())
            .orElse(0);
        int limit = Math.max(0, credential.getDemoPassphrase().getCallLimit());
        return new CredentialResponse(
            true,
            LlmProvider.DEMO,
            properties.demo().model(),
            null,
            new DemoUsageResponse(used, limit, Math.max(0, limit - used))
        );
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidCredentialRequestException(message);
        }
        return value.strip();
    }

    private static void rejectPresent(String value, String message) {
        if (value != null && !value.isBlank()) {
            throw new InvalidCredentialRequestException(message);
        }
    }

    private static String mask(String apiKey) {
        if (apiKey.length() <= 8) {
            return "****";
        }
        String prefix = apiKey.startsWith("sk-") ? "sk-" : apiKey.substring(0, 3);
        return prefix + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
