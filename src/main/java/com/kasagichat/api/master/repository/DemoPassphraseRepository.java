package com.kasagichat.api.master.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.master.model.DemoPassphrase;

/**
 * デモ用合言葉のマスタを操作するRepository。
 */
public interface DemoPassphraseRepository extends JpaRepository<DemoPassphrase, Long> {

    /**
     * 有効な合言葉を取得する。
     *
     * @param passphrase 入力された合言葉
     * @return 一致する有効な合言葉。存在しない場合は空
     */
    Optional<DemoPassphrase> findByPassphraseAndEnabledTrue(String passphrase);
}
