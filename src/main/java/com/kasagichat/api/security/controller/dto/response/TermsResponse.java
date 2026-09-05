package com.kasagichat.api.security.controller.dto.response;

import java.time.Instant;

import com.kasagichat.api.security.model.Terms;
import com.kasagichat.api.security.model.enums.TermsType;

/**
 * 規約情報のレスポンス
 * 
 * @param id          規約ID
 * @param type        規約種別
 * @param version     バージョン
 * @param title       タイトル
 * @param content     本文
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
     * DBエンティティを変換
     * @param terms 規約エンティティ
     * @return レスポンス用の規約情報
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
