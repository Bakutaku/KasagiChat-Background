package com.kasagichat.api.conversation.service;

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
 * 振り返りのLLM出力からタグ付きの各項目を取り出す。
 *
 * <p>必須タグが欠けている場合は生成失敗として扱い、会話を未振り返りのまま残す。
 * ユーザーは同じエンドポイントで再実行できる。</p>
 */
@Component
public class ConversationReviewParser {

    private static final int MAX_TOPICS = 5;
    private static final int MAX_TOPIC_LENGTH = 100;

    /**
     * 振り返りの生成結果を解析する。
     *
     * @param generated LLMの出力
     * @return 解析結果
     * @throws LlmCallFailedException 必須タグが欠けている場合
     */
    public ConversationReview parse(String generated) {
        String feedback = extractRequired(generated, "feedback", 500);
        String profile = extractRequired(generated, "profile", 4000);
        String speechStyle = extractRequired(generated, "speech_style", 1000);
        String topicsBlock = extractOptional(generated, "topics", 1000);
        String dailyQuestion = extractOptional(generated, "daily_question", 500);

        return new ConversationReview(
            feedback,
            profile,
            speechStyle,
            parseTopics(topicsBlock),
            dailyQuestion
        );
    }

    /**
     * 「話題名|カテゴリコード」形式の行を解析する。
     *
     * <p>カテゴリの指定は任意で、欠けていてもnullとして受け入れる。話題名は
     * 大文字小文字を無視して重複を除く。保存側の一意制約と同じ基準で先に絞ることで、
     * 振り返り全体が一意制約違反で巻き戻るのを防ぐ。</p>
     */
    private List<GeneratedTopic> parseTopics(String topicsBlock) {
        if (topicsBlock == null) {
            return List.of();
        }

        List<GeneratedTopic> topics = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String rawLine : topicsBlock.lines().toList()) {
            if (topics.size() >= MAX_TOPICS) {
                break;
            }
            String line = rawLine.strip().replaceFirst("^[-*・]\\s*", "");
            if (line.isBlank()) {
                continue;
            }

            String name = line;
            String categoryCode = null;
            int separator = line.indexOf('|');
            if (separator >= 0) {
                name = line.substring(0, separator).strip();
                String code = line.substring(separator + 1).strip().toUpperCase(Locale.ROOT);
                categoryCode = code.isBlank() ? null : code;
            }
            if (name.isBlank()) {
                continue;
            }
            if (name.length() > MAX_TOPIC_LENGTH) {
                name = name.substring(0, MAX_TOPIC_LENGTH);
            }
            if (!seen.add(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            topics.add(new GeneratedTopic(name, categoryCode));
        }
        return List.copyOf(topics);
    }

    private String extractRequired(String source, String tag, int maxLength) {
        String value = extractOptional(source, tag, maxLength);
        if (value == null) {
            throw new LlmCallFailedException();
        }
        return value;
    }

    private String extractOptional(String source, String tag, int maxLength) {
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
