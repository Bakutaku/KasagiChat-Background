package com.kasagichat.api.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.master.model.AchievementDef;

/**
 * 実績定義のマスタを操作するRepository。
 */
public interface AchievementDefRepository extends JpaRepository<AchievementDef, Long> {

    /**
     * 指定したカウンター種別を達成条件に持つ、有効な実績定義を取得する。
     *
     * @param counterCode カウンター種別のコード
     * @return 該当する実績定義の一覧
     */
    List<AchievementDef> findByCounterDefCodeAndEnabledTrue(String counterCode);
}
