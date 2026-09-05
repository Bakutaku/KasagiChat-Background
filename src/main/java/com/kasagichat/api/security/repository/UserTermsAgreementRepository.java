package com.kasagichat.api.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.security.model.UserTermsAgreement;

/**
 * ユーザーの規約同意履歴を永続化するRepository。
 */
public interface UserTermsAgreementRepository
        extends JpaRepository<UserTermsAgreement, Long> {
}
