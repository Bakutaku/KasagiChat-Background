package com.kasagichat.api.npc.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/** 既存のProblem Detailsハンドラーで返す、家APIの業務エラー。 */
public class HomeException extends BaseException {
    private HomeException(String code, HttpStatus status, String message) {
        super(code, status, message);
    }

    public static HomeException npcNotFound() {
        return new HomeException("NPC_NOT_FOUND", HttpStatus.NOT_FOUND, "分身が見つかりません");
    }

    public static HomeException itemNotFound() {
        return new HomeException("HOME_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND, "思い出の品が見つかりません");
    }

    public static HomeException invalidSlot() {
        return new HomeException("INVALID_HOME_SLOT", HttpStatus.BAD_REQUEST, "配置先スロットが不正です");
    }

    public static HomeException incompatibleSlot() {
        return new HomeException("HOME_SLOT_INCOMPATIBLE", HttpStatus.BAD_REQUEST, "この品物は指定されたスロットに配置できません");
    }

    public static HomeException occupiedSlot() {
        return new HomeException("HOME_SLOT_OCCUPIED", HttpStatus.CONFLICT, "指定されたスロットには別の品物が配置されています");
    }
}
