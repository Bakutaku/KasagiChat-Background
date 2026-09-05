package com.kasagichat.api.security.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/** 最新の必須規約への同意が満たされていない場合の例外。 */
public final class TermsAgreementRequiredException extends BaseException {

    public TermsAgreementRequiredException() {
        super(
                "TERMS_AGREEMENT_REQUIRED",
                HttpStatus.BAD_REQUEST,
                "最新の必須規約への同意が必要です"
        );
    }
}
