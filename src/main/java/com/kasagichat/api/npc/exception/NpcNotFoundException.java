package com.kasagichat.api.npc.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 本人のNPCが未作成の場合の例外。
 */
public final class NpcNotFoundException extends BaseException {

    public NpcNotFoundException() {
        super("NPC_NOT_FOUND", HttpStatus.NOT_FOUND, "NPCが見つかりませんでした。");
    }
}
