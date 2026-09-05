package com.kasagichat.api.security.service;

import com.kasagichat.api.security.SecurityProperties;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.kasagichat.api.security.controller.dto.request.UserRegistrationRequest;
import com.kasagichat.api.security.exception.TempException;
import com.kasagichat.api.security.model.PendingUsers;
import com.kasagichat.api.security.model.Terms;
import com.kasagichat.api.security.model.UserAuth;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.model.enums.AuthProvider;
import com.kasagichat.api.security.repository.PendingUsersRepository;
import com.kasagichat.api.security.repository.UserAuthRepository;
import com.kasagichat.api.security.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final SecurityProperties securityProperties;

    private final PendingUsersRepository pendingUsersRepository;
    private final UserAuthRepository userAuthRepository;
    private final UsersRepository usersRepository;

    private final TermsService termsService;

    @Transactional
    public Users registration(Long pendingUserId, UserRegistrationRequest request) {
        // 仮登録ユーザー取得
        PendingUsers pendingUser = pendingUsersRepository.findById(pendingUserId)
            .orElseThrow(() -> new TempException());    // TODO 仮登録情報がない場合

        // 作成可否
        List<Terms> consentTerms = userRegistrationCheck(pendingUser, request.agreedTermsIds());

        // ユーザー情報作成
        Users user = usersRepository.save(
            Users.builder()
                .displayName(request.displayName())
                .avatarUrl(pendingUser.getAvatarUrl())
                .build()
        );
        userAuthRepository.save(
            UserAuth.builder()
                .user(user)
                .provider(pendingUser.getProvider())
                .subject(pendingUser.getSubject())
                .build()
        );

        // 規約同意履歴作成
        termsService.consent(consentTerms, user);

        // 仮登録ユーザー削除
        pendingUsersRepository.delete(pendingUser);
        return user;
    }
    
    /**
     * アカウント作成画面に表示する仮登録情報の存在と有効期限を検証する。
     *
     * @param pendingUserId サーバーセッション内のPrincipalから取得した仮登録ID
     * @return 有効な仮登録情報
     * @throws ResponseStatusException 仮登録情報が存在しない、または期限切れの場合
     */
    public PendingUsers getPendingUsers(Long pendingUserId) {
        PendingUsers pendingUser = pendingUsersRepository.findById(pendingUserId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "仮登録情報が存在しません"
            ));
        if (!pendingUser.getExpiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "仮登録の有効期限が切れています"
            );
        }
        return pendingUser;
    }

    /**
     * OAuth識別情報に対応する仮登録レコードを作成または更新する。
     *
     * <p>同じOAuthアカウントでログインをやり直した場合はレコードを増やさず、
     * プロフィール候補と有効期限を更新する。</p>
     *
     * @param provider 認証に使用したOAuthプロバイダー
     * @param subject プロバイダー内で一意なユーザー識別子
     * @param displayName OAuthから取得した表示名候補
     * @param avatarUrl OAuthから取得したアバター画像URL
     * @return 保存された仮登録情報
     */
    @Transactional
    public PendingUsers createPendingUser(AuthProvider provider,String subject, String displayName, String avatarUrl) {
        PendingUsers pendingUser = pendingUsersRepository
            .findByProviderAndSubject(provider, subject)
            .orElseGet(PendingUsers::new);

        pendingUser.setProvider(provider);
        pendingUser.setSubject(subject);
        pendingUser.setDisplayName(normalizeDisplayName(displayName));
        pendingUser.setAvatarUrl(avatarUrl);
        pendingUser.setExpiresAt(
            Instant.now().plus(securityProperties.pendingRegistrationTtl())
        );

        return pendingUsersRepository.save(pendingUser);
    }


    /**
     * ユーザー作成可能か判定
     * @return 作成可否
     */
    private List<Terms> userRegistrationCheck(PendingUsers pendingUser,Set<Long> agreedTermsIds) {
        // 仮登録の有効期限確認
        if (!pendingUser.getExpiresAt().isAfter(Instant.now())) {
            // TODO 有効期限が切れている場合
            throw new TempException();
        }

        // 二重登録防止のため既存登録がないか確認する
        if(userAuthRepository.findByProviderAndSubject(
            pendingUser.getProvider(),
            pendingUser.getSubject()
        ).isPresent()) {
            // TODO すでに登録済みユーザーの場合
            throw new TempException();
        }

        // 同意チェック & 同意対象返却
        return termsService.validateAndGetLatestTerms(agreedTermsIds);
    }


    /**
     * OAuth側の表示名をDBの必須・最大長制約に収まる値へ整形する。
     */
    private String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "New User";
        }
        String trimmed = displayName.trim();
        return trimmed.length() <= 50 ? trimmed : trimmed.substring(0, 50);
    }
}
