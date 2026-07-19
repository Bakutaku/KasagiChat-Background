package com.kasagichat.api.user;

import java.util.Optional;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserService {

    private final AppUserRepository repository;

    public AppUserService(AppUserRepository repository) {
        this.repository = repository;
    }

    /**
     * OAuthログイン成功時にユーザーを登録/更新する。
     * subject には principal.getName() を使う(GitHub=数値ID, Google=subクレーム。
     * どちらもプロバイダ内で不変の識別子。表示名やメールは変わり得るため識別子にしない)。
     */
    @Transactional
    public AppUser upsertFromLogin(OAuth2AuthenticationToken token) {
        String provider = token.getAuthorizedClientRegistrationId();
        OAuth2User principal = token.getPrincipal();
        String subject = principal.getName();
        String displayName = resolveDisplayName(provider, principal);
        String avatarUrl = resolveAvatarUrl(provider, principal);

        return repository.findByProviderAndSubject(provider, subject)
                .map(user -> {
                    user.setDisplayName(displayName);
                    user.setAvatarUrl(avatarUrl);
                    return user;
                })
                .orElseGet(() -> repository.save(new AppUser(provider, subject, displayName, avatarUrl)));
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findByLogin(OAuth2AuthenticationToken token) {
        return repository.findByProviderAndSubject(
                token.getAuthorizedClientRegistrationId(),
                token.getPrincipal().getName());
    }

    private String resolveDisplayName(String provider, OAuth2User principal) {
        String name = principal.getAttribute("name");
        if (name == null && "github".equals(provider)) {
            name = principal.getAttribute("login");
        }
        return name != null ? name : "名無しさん";
    }

    private String resolveAvatarUrl(String provider, OAuth2User principal) {
        return switch (provider) {
            case "github" -> principal.getAttribute("avatar_url");
            case "google" -> principal.getAttribute("picture");
            default -> null;
        };
    }
}
