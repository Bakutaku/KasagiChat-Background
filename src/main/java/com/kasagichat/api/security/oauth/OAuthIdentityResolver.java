package com.kasagichat.api.security.oauth;

import java.util.Locale;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.kasagichat.api.security.model.enums.AuthProvider;

/**
 * Spring Securityが検証したOAuth認証情報を、アプリケーション共通の
 * {@link OAuthIdentity} へ変換する。
 *
 * <p>GoogleとGitHubでは表示名やアバターURLの属性名が異なるため、その違いをこのクラスで吸収する。
 * 呼び出し側はプロバイダー固有の属性構造を意識せずに、既存ユーザーの検索や仮登録を行える。</p>
 */
@Component
public class OAuthIdentityResolver {

    /**
     * OAuth認証結果から、プロバイダー、ユーザー識別子、プロフィール候補を抽出する。
     *
     * <p>{@link OAuth2User#getName()} はClientRegistrationに設定されたユーザー名属性を返す。
     * Googleでは通常 {@code sub}、GitHubでは通常 {@code id} が使われるため、
     * 表示名やメールアドレスではなくプロバイダー側の不変な識別子として利用する。</p>
     *
     * @param authentication Spring Securityによって検証済みのOAuth認証情報
     * @return プロバイダー差異を吸収したユーザー識別情報
     * @throws IllegalArgumentException ClientRegistration IDに対応する
     *         {@link AuthProvider} が定義されていない場合
     */
    public OAuthIdentity resolve(OAuth2AuthenticationToken authentication) {
        OAuth2User principal = authentication.getPrincipal();

        // application.ymlのregistration ID（google、github）をAuthProviderへ対応付ける。
        AuthProvider provider = AuthProvider.valueOf(
                authentication.getAuthorizedClientRegistrationId()
                        .toUpperCase(Locale.ROOT)
        );

        // Googleはname、GitHubはnameが未設定の場合があるためloginを予備値として使う。
        String displayName = firstNonBlank(
                principal.getAttribute("name"),
                principal.getAttribute("login")
        );

        // GoogleとGitHubでアバターURLの属性名が異なる。
        String avatarUrl = firstNonBlank(
                principal.getAttribute("picture"),
                principal.getAttribute("avatar_url")
        );

        return new OAuthIdentity(
                provider,
                principal.getName(),
                displayName,
                avatarUrl
        );
    }

    /**
     * 候補を先頭から確認し、最初に見つかった空でない値を文字列として返す。
     *
     * @param values プロバイダーごとの属性値候補
     * @return 最初の有効な値。すべて未設定または空文字の場合は {@code null}
     */
    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }
}