package com.kasagichat.api.security.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kasagichat.api.security.model.Terms;

/**
 * 利用規約やプライバシーポリシーのマスタを操作するRepository。
 */
public interface TermsRepository extends JpaRepository<Terms, Long> {

    /**
     * 指定日時点で有効な最新の規約を、規約種類ごとに取得する。
     *
     * <p>同じ規約種類かつ同じ効力発生日の規約が複数ある場合は、
     * IDが最大のものを取得する。</p>
     *
     * @param now 有効な規約を判定する基準日時
     * @return 規約種類ごとの最新規約一覧
     */
    @Query("""
            SELECT terms
            FROM Terms terms
            WHERE terms.effectiveAt <= :now
                AND NOT EXISTS (
                    SELECT newerTerms.id
                    FROM Terms newerTerms
                    WHERE newerTerms.type = terms.type
                        AND newerTerms.effectiveAt <= :now
                        AND (
                            newerTerms.effectiveAt > terms.effectiveAt
                            OR (
                                newerTerms.effectiveAt = terms.effectiveAt
                                AND newerTerms.id > terms.id
                            )
                        )
                )
            ORDER BY terms.type
            """)
    List<Terms> findLatestByTypeEffectiveAtLessThanEqual(@Param("now") Instant now);
}
