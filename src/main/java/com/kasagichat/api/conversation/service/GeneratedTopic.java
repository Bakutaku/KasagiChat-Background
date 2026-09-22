package com.kasagichat.api.conversation.service;

/**
 * 振り返りで抽出した話題1件。
 *
 * @param name 話題名
 * @param categoryCode 話題カテゴリの識別コード。判別できなかった場合はnull
 */
public record GeneratedTopic(String name, String categoryCode) {
}
