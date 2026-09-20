package com.kasagichat.api.security.controller.dto.response;

/**
 * 初回オンボーディングの進行状態。
 *
 * @param credentialConfigured APIキーまたはデモ利用が設定済みか
 * @param npcCreated NPCが作成済みか
 * @param npcBorn NPC誕生会話の振り返りが完了済みか
 */
public record OnboardingStatusResponse(
    boolean credentialConfigured,
    boolean npcCreated,
    boolean npcBorn
) {
}
