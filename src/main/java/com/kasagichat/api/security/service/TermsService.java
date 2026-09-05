package com.kasagichat.api.security.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.security.exception.TempException;
import com.kasagichat.api.security.model.Terms;
import com.kasagichat.api.security.model.UserTermsAgreement;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.repository.TermsRepository;
import com.kasagichat.api.security.repository.UserTermsAgreementRepository;

import lombok.RequiredArgsConstructor;

/**
 * 規約同意
 */
@Service
@RequiredArgsConstructor
public class TermsService {

    private final TermsRepository termsRepository;
    private final UserTermsAgreementRepository userTermsAgreementRepository;

    /**
     * 現在有効な最新の規約を、規約種類ごとに取得する。
     *
     * @return 規約種類ごとの最新規約一覧
     */
    @Transactional(readOnly = true)
    public List<Terms> getLatestTerms() {
        return termsRepository.findLatestByTypeEffectiveAtLessThanEqual(
                Instant.now()
        );
    }

    /**
     * 規約確認
     * @param agreedTermsIds 同意した規約
     * @return 対象の規約
     */
    @Transactional(readOnly = true)
    public List<Terms> validateAndGetLatestTerms(Set<Long> agreedTermsIds) {
        List<Terms> latestTerms = getLatestTerms();

        Set<Long> latestTermsIds = latestTerms.stream()
            .map(terms -> terms.getId())
            .collect(Collectors.toUnmodifiableSet());
        
        if (latestTermsIds.isEmpty() || agreedTermsIds == null || !agreedTermsIds.containsAll(latestTermsIds)) {
            // TODO 規約同意失敗
            throw new TempException();
        }
        return latestTerms;
    }


    /**
     * 規約同意履歴の作成
     * @param consentTerms 同意対象
     * @param user 対象ユーザー
     * @param now 同意日時
     */
    public void consent(List<Terms> consentTerms,Users user) {
        userTermsAgreementRepository.saveAll(
            consentTerms.stream()
                .map(terms -> UserTermsAgreement.builder()
                    .user(user)
                    .terms(terms)
                    .agreedAt(Instant.now())
                    .build()
            ).toList()
        );
    }
}
