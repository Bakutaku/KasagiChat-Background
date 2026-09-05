package com.kasagichat.api.security.service;

import com.kasagichat.api.security.SecurityProperties;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.security.controller.dto.request.UserRegistrationRequest;
import com.kasagichat.api.security.exception.PendingRegistrationExpiredException;
import com.kasagichat.api.security.exception.PendingRegistrationNotFoundException;
import com.kasagichat.api.security.exception.TermsAgreementRequiredException;
import com.kasagichat.api.security.exception.UserAlreadyRegisteredException;
import com.kasagichat.api.security.model.PendingUsers;
import com.kasagichat.api.security.model.Terms;
import com.kasagichat.api.security.model.UserAuth;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.model.enums.AuthProvider;
import com.kasagichat.api.security.repository.PendingUsersRepository;
import com.kasagichat.api.security.repository.UserAuthRepository;
import com.kasagichat.api.security.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

/**
 * OAuth認証後の仮登録情報を管理し、ユーザーの本登録を行うService。
 */
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final SecurityProperties securityProperties;

    private final PendingUsersRepository pendingUsersRepository;
    private final UserAuthRepository userAuthRepository;
    private final UsersRepository usersRepository;
    private final TermsService termsService;

    /**
     * 仮登録情報と規約同意を検証し、ユーザーを本登録する。
     *
     * @param pendingUserId 本登録する仮登録ユーザーの内部ID
     * @param request 表示名と同意した規約IDを含む登録リクエスト
     * @return 登録されたユーザー
     * @throws PendingRegistrationNotFoundException 仮登録情報が存在しない場合
     * @throws PendingRegistrationExpiredException 仮登録情報が期限切れの場合
     * @throws UserAlreadyRegisteredException OAuthアカウントが登録済みの場合
     * @throws TermsAgreementRequiredException 最新規約への同意が不足している場合
     */
    @Transactional
    public Users registration(Long pendingUserId, UserRegistrationRequest request) {
        // 仮登録ユーザー取得
        PendingUsers pendingUser = pendingUsersRepository.findById(pendingUserId)
            .orElseThrow(PendingRegistrationNotFoundException::new);

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
     * @param pendingUserId サーバーセッション内のPrincipalから取得した仮登録ユーザーID
     * @return 有効な仮登録情報
     * @throws PendingRegistrationNotFoundException 仮登録情報が存在しない場合
     * @throws PendingRegistrationExpiredException 仮登録情報が期限切れの場合
     */
    public PendingUsers getPendingUsers(Long pendingUserId) {
        PendingUsers pendingUser = pendingUsersRepository.findById(pendingUserId)
            .orElseThrow(PendingRegistrationNotFoundException::new);
        if (!pendingUser.getExpiresAt().isAfter(Instant.now())) {
            throw new PendingRegistrationExpiredException();
        }
        return pendingUser;
    }

    /**
     * OAuth識別情報に対応する仮登録情報を作成または更新する。
     *
     * <p>同じOAuthアカウントで再度ログインした場合はレコードを増やさず、
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
     * 仮登録情報、重複登録および規約同意を検証する。
     *
     * @param pendingUser 登録対象の仮登録ユーザー
     * @param agreedTermsIds ユーザーが同意した規約IDの集合
     * @return 同意履歴へ保存する最新規約の一覧
     * @throws PendingRegistrationExpiredException 仮登録情報が期限切れの場合
     * @throws UserAlreadyRegisteredException OAuthアカウントが登録済みの場合
     * @throws TermsAgreementRequiredException 最新規約への同意が不足している場合
     */
    private List<Terms> userRegistrationCheck(PendingUsers pendingUser,Set<Long> agreedTermsIds) {
        // 仮登録の有効期限確認
        if (!pendingUser.getExpiresAt().isAfter(Instant.now())) {
            throw new PendingRegistrationExpiredException();
        }

        // 同じOAuthアカウントの二重登録を防止する。
        if(userAuthRepository.findByProviderAndSubject(
            pendingUser.getProvider(),
            pendingUser.getSubject()
        ).isPresent()) {
            throw new UserAlreadyRegisteredException();
        }

        // 同意チェック & 同意対象返却
        return termsService.validateAndGetLatestTerms(agreedTermsIds);
    }


    /**
     * OAuth側の表示名をデータベースの必須・最大長制約に収まる値へ整形する。
     *
     * @param displayName OAuthプロバイダーから取得した表示名
     * @return 必須・最大長制約を満たす表示名
     */
    private String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "New User";
        }
        String trimmed = displayName.trim();
        return trimmed.length() <= 50 ? trimmed : trimmed.substring(0, 50);
    }
}
