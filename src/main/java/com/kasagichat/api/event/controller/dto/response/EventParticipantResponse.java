package com.kasagichat.api.event.controller.dto.response;

/**
 * 会場の賑わい表示に使う参加者。
 *
 * <p>連番IDを公開しないため識別子は返さない。表示の並びはサーバーが決めた順序が正となる。</p>
 *
 * @param name 参加者の分身の名前
 * @param presetId 参加者の分身の見た目プリセットID
 */
public record EventParticipantResponse(String name, String presetId) {
}
