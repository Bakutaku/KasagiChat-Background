package com.kasagichat.api.master.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.master.model.ExpRule;

/**
 * EXPルールのマスタを操作するRepository。
 */
public interface ExpRuleRepository extends JpaRepository<ExpRule, Long> {

    /**
     * 行動コードからEXPルールを取得する。
     *
     * @param code 行動コード
     * @return 該当するEXPルール。存在しない場合は空
     */
    Optional<ExpRule> findByCode(String code);
}
