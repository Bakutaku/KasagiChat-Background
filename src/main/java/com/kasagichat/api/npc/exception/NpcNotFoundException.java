package com.kasagichat.api.npc.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * ユーザーのNPCがまだ作成されていない場合に発生する例外。
 */
public final class NpcNotFoundException extends BaseException {

    /**
     * 存在しないNPCを表す例外を生成する。
     */
    public NpcNotFoundException() {
        super(
                "NPC_NOT_FOUND",
                HttpStatus.NOT_FOUND,
                "NPCが見つかりませんでした。"
        );
    }
}
