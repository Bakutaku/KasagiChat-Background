package com.kasagichat.api.security.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.security.exception.TermsAgreementRequiredException;
import com.kasagichat.api.security.model.Terms;
import com.kasagichat.api.security.model.UserTermsAgreement;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.repository.TermsRepository;
import com.kasagichat.api.security.repository.UserTermsAgreementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 最新規約の取得、同意内容の検証および同意履歴の保存を行うService。
 */
@Service
@RequiredArgsConstructor
@Slf4j
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
     * ユーザーが現在有効なすべての最新規約へ同意していることを検証する。
     *
     * @param agreedTermsIds ユーザーが同意した規約IDの集合
     * @return 同意履歴へ保存する最新規約の一覧
     * @throws TermsAgreementRequiredException 有効な規約が存在しないか、同意が不足している場合
     */
    @Transactional(readOnly = true)
    public List<Terms> validateAndGetLatestTerms(Set<Long> agreedTermsIds) {
        List<Terms> latestTerms = getLatestTerms();

        Set<Long> latestTermsIds = latestTerms.stream()
            .map(terms -> terms.getId())
            .collect(Collectors.toUnmodifiableSet());
        
        if (latestTermsIds.isEmpty()) {
            log.warn("有効な最新規約が存在しないため、ユーザー登録を拒否しました");
            throw new TermsAgreementRequiredException();
        }
        if (agreedTermsIds == null || !agreedTermsIds.containsAll(latestTermsIds)) {
            throw new TermsAgreementRequiredException();
        }
        return latestTerms;
    }


    /**
     * 指定されたユーザーの規約同意履歴を現在日時で保存する。
     *
     * @param consentTerms 同意履歴へ保存する規約の一覧
     * @param user 規約へ同意したユーザー
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
