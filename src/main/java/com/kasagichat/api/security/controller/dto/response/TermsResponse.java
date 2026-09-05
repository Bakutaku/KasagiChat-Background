package com.kasagichat.api.security.controller.dto.response;

import java.time.Instant;

import com.kasagichat.api.security.model.Terms;
import com.kasagichat.api.security.model.enums.TermsType;

/**
 * ユーザーへ提示する規約情報。
 *
 * @param id 規約ID
 * @param type 規約種別
 * @param version バージョン
 * @param title タイトル
 * @param content 本文
 * @param effectiveAt 効力発生日時
 */
public record TermsResponse(
        Long id,
        TermsType type,
        String version,
        String title,
        String content,
        Instant effectiveAt) {
    
    /**
     * 規約Entityをレスポンスへ変換する。
     *
     * @param terms 変換元の規約Entity
     * @return ユーザーへ提示する規約レスポンス
     */
    public static TermsResponse from(Terms terms) {
        return new TermsResponse(
            terms.getId(),
            terms.getType(),
            terms.getVersion(),
            terms.getTitle(),
            terms.getContent(),
            terms.getEffectiveAt()
        );
    }
}
