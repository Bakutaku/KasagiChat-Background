package com.kasagichat.api.master.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.master.model.CounterDef;

/**
 * カウンター種別のマスタを操作するRepository。
 */
public interface CounterDefRepository extends JpaRepository<CounterDef, Long> {

    /**
     * コードからカウンター種別を取得する。
     *
     * @param code カウンター種別のコード
     * @return 該当するカウンター種別。存在しない場合は空
     */
    Optional<CounterDef> findByCode(String code);
}
