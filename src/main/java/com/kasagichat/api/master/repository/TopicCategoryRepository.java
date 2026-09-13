package com.kasagichat.api.master.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.master.model.TopicCategory;

/**
 * 話題カテゴリのマスタを操作するRepository。
 */
public interface TopicCategoryRepository extends JpaRepository<TopicCategory, Long> {

    /**
     * 識別コードから話題カテゴリを取得する。
     *
     * @param code 識別コード
     * @return 該当する話題カテゴリ。存在しない場合は空
     */
    Optional<TopicCategory> findByCode(String code);

    /**
     * 有効な話題カテゴリを表示順に取得する。
     *
     * @return 有効な話題カテゴリの一覧
     */
    List<TopicCategory> findByEnabledTrueOrderBySortOrderAsc();
}
