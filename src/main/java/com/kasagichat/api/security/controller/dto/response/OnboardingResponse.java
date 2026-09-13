package com.kasagichat.api.security.controller.dto.response;

/**
 * 初回フロー（APIキー設定 → NPC誕生）の進み具合を返却する。
 *
 * @param credentialConfigured APIキーまたはデモの設定が済んでいるかどうか
 * @param npcCreated NPCを作成済みかどうか
 * @param npcBorn NPCが誕生済みかどうか
 */
public record OnboardingResponse(
    boolean credentialConfigured,
    boolean npcCreated,
    boolean npcBorn
) {
}
