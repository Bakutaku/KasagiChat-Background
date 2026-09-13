package com.kasagichat.api.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.master.model.LevelCurve;

/**
 * レベル曲線のマスタを操作するRepository。
 */
public interface LevelCurveRepository extends JpaRepository<LevelCurve, Integer> {

    /**
     * レベル曲線をレベルの昇順で取得する。
     *
     * @return レベル曲線の一覧
     */
    List<LevelCurve> findAllByOrderByLevelAsc();
}
