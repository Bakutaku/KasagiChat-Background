package com.kasagichat.api.npc.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 1ユーザー1体のNPCを重複作成しようとした場合の例外。
 */
public final class NpcAlreadyExistsException extends BaseException {

    public NpcAlreadyExistsException() {
        super("NPC_ALREADY_EXISTS", HttpStatus.CONFLICT, "NPCはすでに作成されています。");
    }
}
