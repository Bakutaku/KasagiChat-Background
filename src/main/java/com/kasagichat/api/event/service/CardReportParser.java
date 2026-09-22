package com.kasagichat.api.event.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.kasagichat.api.credential.exception.LlmCallFailedException;

/**
 * カード開封のLLM出力からタグ付きの各項目を取り出す。
 *
 * <p>会話報告が取れない場合は生成失敗として扱い、カードを未開封のまま残す。ユーザーは同じ
 * エンドポイントで開き直せる。おすすめ話題は欠けていても開封は成立させる。</p>
 */
@Component
public class CardReportParser {

    private static final int MAX_REPORT_LENGTH = 4000;
    private static final int MAX_TOPICS_BLOCK_LENGTH = 1000;
    private static final int MAX_TOPICS = 3;
    private static final int MAX_TOPIC_LENGTH = 200;

    /**
     * 開封の生成結果を解析する。
     *
     * @param generated LLMの出力
     * @return 解析結果
     * @throws LlmCallFailedException 会話報告のタグが欠けている場合
     */
    public CardReport parse(String generated) {
        String report = extract(generated, "report", MAX_REPORT_LENGTH);
        if (report == null) {
            throw new LlmCallFailedException();
        }
        return new CardReport(
            report,
            parseTopics(extract(generated, "recommended_topics", MAX_TOPICS_BLOCK_LENGTH))
        );
    }

    /**
     * おすすめ話題を改行区切りで解析する。箇条書きの記号は表示に使わないため落とす。
     */
    private List<String> parseTopics(String topicsBlock) {
        if (topicsBlock == null) {
            return List.of();
        }
        List<String> topics = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String rawLine : topicsBlock.lines().toList()) {
            if (topics.size() >= MAX_TOPICS) {
                break;
            }
            String line = rawLine.strip().replaceFirst("^[-*・]\\s*", "").replaceFirst("^\\d+[.)]\\s*", "");
            if (line.isBlank()) {
                continue;
            }
            if (line.length() > MAX_TOPIC_LENGTH) {
                line = line.substring(0, MAX_TOPIC_LENGTH);
            }
            if (!seen.add(line.toLowerCase(Locale.ROOT))) {
                continue;
            }
            topics.add(line);
        }
        return List.copyOf(topics);
    }

    private String extract(String source, String tag, int maxLength) {
        Pattern pattern = Pattern.compile(
            "<" + Pattern.quote(tag) + ">\\s*(.*?)\\s*</" + Pattern.quote(tag) + ">",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find() || matcher.group(1).isBlank()) {
            return null;
        }
        String value = matcher.group(1).strip();
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
