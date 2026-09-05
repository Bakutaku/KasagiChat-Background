package com.kasagichat.api.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.security.model.UserTermsAgreement;

/**
 * 規約同意履歴リポジトリ
 */
public interface UserTermsAgreementRepository
        extends JpaRepository<UserTermsAgreement, Long> {
}
