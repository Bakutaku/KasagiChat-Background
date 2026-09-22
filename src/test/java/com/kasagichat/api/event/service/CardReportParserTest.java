package com.kasagichat.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.kasagichat.api.credential.exception.LlmCallFailedException;

/** カード開封のLLM出力を解析するパーサーの単体テスト。 */
class CardReportParserTest {

    private final CardReportParser parser = new CardReportParser();

    @Test
    void extractsReportAndTopicsFromTags() {
        CardReport report = parser.parse("""
            前置き
            <report>ゲームの話で盛り上がってきたよ。</report>
            <recommended_topics>
            - 最近遊んだゲーム
            2) おすすめのコーヒー豆
            ・旅行の予定
            </recommended_topics>
            後書き
            """);

        assertThat(report.report()).isEqualTo("ゲームの話で盛り上がってきたよ。");
        assertThat(report.recommendedTopics())
            .containsExactly("最近遊んだゲーム", "おすすめのコーヒー豆", "旅行の予定");
    }

    @Test
    void keepsAtMostThreeTopicsAndDropsDuplicates() {
        CardReport report = parser.parse("""
            <report>報告</report>
            <recommended_topics>
            ゲーム
            ゲーム
            コーヒー
            旅行
            読書
            </recommended_topics>
            """);

        assertThat(report.recommendedTopics()).containsExactly("ゲーム", "コーヒー", "旅行");
    }

    @Test
    void acceptsAReportWithoutRecommendedTopics() {
        CardReport report = parser.parse("<report>報告だけ</report>");

        assertThat(report.recommendedTopics()).isEmpty();
    }

    @Test
    void failsWhenTheReportTagIsMissingOrEmpty() {
        assertThatThrownBy(() -> parser.parse("<recommended_topics>ゲーム</recommended_topics>"))
            .isInstanceOf(LlmCallFailedException.class);
        assertThatThrownBy(() -> parser.parse("<report>   </report>"))
            .isInstanceOf(LlmCallFailedException.class);
    }
}
