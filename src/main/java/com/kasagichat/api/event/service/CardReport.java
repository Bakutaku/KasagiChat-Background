package com.kasagichat.api.event.service;

import java.util.List;

/**
 * 出会いカードの開封で生成した内容を解析した結果。
 *
 * @param report 自分の分身からの会話報告
 * @param recommendedTopics 本人へ話しかけるときのおすすめ話題
 */
public record CardReport(String report, List<String> recommendedTopics) {
}
