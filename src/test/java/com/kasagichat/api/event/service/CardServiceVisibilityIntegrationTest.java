package com.kasagichat.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.kasagichat.api.common.exception.BaseException;

/**
 * イベント配下のカード一覧が、{@link EventService}の可視性判定をBean越しに使えることを確認する。
 *
 * <p>可視性の規則をコピーせず同じパッケージのメソッドを呼んでいるため、トランザクションのプロキシを
 * 挟んでも正しく届くことを実体のBeanで確かめる。</p>
 */
@SpringBootTest
class CardServiceVisibilityIntegrationTest {

    @Autowired
    private CardService cardService;

    @Test
    void hidesEventCardsOfAnEventThatDoesNotExist() {
        assertThatThrownBy(() -> cardService.listByEvent(-1L, UUID.randomUUID()))
            .isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("EVENT_NOT_FOUND"));
    }
}
