package com.kasagichat.api.master.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.master.model.Item;

/**
 * 解禁アイテムのマスタを操作するRepository。
 */
public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * 識別コードからアイテムを取得する。
     *
     * @param code 識別コード
     * @return 該当するアイテム。存在しない場合は空
     */
    Optional<Item> findByCode(String code);
}
