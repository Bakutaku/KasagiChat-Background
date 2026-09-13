package com.kasagichat.api.npc.service;

/**
 * EXPを加算した結果。
 *
 * @param level 加算後のレベル
 * @param leveledUp 加算によってレベルアップしたかどうか
 */
public record LevelUpResult(
    int level,
    boolean leveledUp
) {
}
