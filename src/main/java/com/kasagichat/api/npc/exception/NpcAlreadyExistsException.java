package com.kasagichat.api.npc.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * NPCを作成済みのユーザーが、もう1体作成しようとした場合に発生する例外。
 */
public final class NpcAlreadyExistsException extends BaseException {

    /**
     * 作成済みのNPCを表す例外を生成する。
     */
    public NpcAlreadyExistsException() {
        super(
                "NPC_ALREADY_EXISTS",
                HttpStatus.CONFLICT,
                "NPCはすでに作成されています。"
        );
    }
}
