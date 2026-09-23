package com.kasagichat.api.credential;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.kasagichat.api.credential.model.enums.LlmProvider;

/**
 * LLM接続とAPIキー暗号化に使用する運営設定。
 *
 * <p>秘密値は{@code application.yml}に直書きせず、環境変数からバインドする。</p>
 *
 * @param credentialEncryptionKey Base64形式の32バイト暗号化鍵
 * @param openai OpenAIのBYOK許可モデル
 * @param anthropic AnthropicのBYOK許可モデル
 * @param demo 運営キーを使用するデモ設定
 */
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
    String credentialEncryptionKey,
    ProviderProperties openai,
    ProviderProperties anthropic,
    DemoProperties demo
) {

    public LlmProperties {
        credentialEncryptionKey = credentialEncryptionKey == null ? "" : credentialEncryptionKey;
        openai = openai == null ? new ProviderProperties(List.of()) : openai;
        anthropic = anthropic == null ? new ProviderProperties(List.of()) : anthropic;
        demo = demo == null ? new DemoProperties(null, null, "", "", false) : demo;
    }

    /**
     * BYOKで選択を許可するモデル。
     *
     * @param models モデルID一覧
     */
    public record ProviderProperties(List<String> models) {
        public ProviderProperties {
            models = models == null
                ? List.of()
                : models.stream().map(String::strip).filter(model -> !model.isEmpty()).distinct().toList();
        }
    }

    /**
     * デモ利用時にサーバーが固定して使用する接続情報。
     *
     * @param provider 実際に接続するプロバイダー
     * @param baseUrl 固定接続先URL
     * @param model 固定モデルID
     * @param apiKey 運営APIキー
     * @param iamAuth 本番BedrockのIAMロール認証を使う場合にtrue
     */
    public record DemoProperties(LlmProvider provider, String baseUrl, String model, String apiKey, boolean iamAuth) {
        public DemoProperties {
            model = model == null ? "" : model.strip();
            apiKey = apiKey == null ? "" : apiKey.strip();
        }
    }

    /**
     * 指定プロバイダーのBYOK許可モデルを返す。
     *
     * @param provider LLMプロバイダー
     * @return 許可モデル一覧
     */
    public List<String> modelsFor(LlmProvider provider) {
        return switch (provider) {
            case OPENAI -> openai.models();
            case ANTHROPIC -> anthropic.models();
            case DEMO -> List.of();
        };
    }

    /**
     * デモ接続に必要な運営設定が揃っているか確認する。
     *
     * @return 設定済みならtrue
     */
    public boolean isDemoConfigured() {
        return !demo.model().isBlank()
            && (demo.iamAuth() || (demo.provider() != null && !demo.apiKey().isBlank()));
    }
}
