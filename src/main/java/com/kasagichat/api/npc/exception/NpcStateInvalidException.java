package com.kasagichat.api.npc.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * NPCの作成・誕生状態と要求された操作が一致しない場合の例外。
 */
public final class NpcStateInvalidException extends BaseException {

    public NpcStateInvalidException(String message) {
        super("NPC_STATE_INVALID", HttpStatus.CONFLICT, message);
    }
}
