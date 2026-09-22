package com.kasagichat.api.event.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/** 既存のProblem Detailsハンドラーで返す、イベントAPIの業務エラー。 */
public class EventException extends BaseException {
    private EventException(String code, HttpStatus status, String message) {
        super(code, status, message);
    }

    /** 参加者以外からの参照も、存在を秘匿するためこれを返す。 */
    public static EventException eventNotFound() {
        return new EventException("EVENT_NOT_FOUND", HttpStatus.NOT_FOUND, "イベントが見つかりません");
    }

    public static EventException npcNotBorn() {
        return new EventException("NPC_NOT_BORN", HttpStatus.CONFLICT, "分身が誕生していないため、イベントに参加できません");
    }

    public static EventException eventEnded() {
        return new EventException("EVENT_ENDED", HttpStatus.CONFLICT, "このイベントは終了しています");
    }

    public static EventException eventHasParticipants() {
        return new EventException("EVENT_HAS_PARTICIPANTS", HttpStatus.CONFLICT, "参加者がいるイベントは削除できません");
    }

    public static EventException invalidEventPeriod() {
        return new EventException("INVALID_EVENT_PERIOD", HttpStatus.BAD_REQUEST, "終了日時は開始日時より後にしてください");
    }

    /** 他人宛てのカードも、存在を秘匿するためこれを返す。 */
    public static EventException cardNotFound() {
        return new EventException("CARD_NOT_FOUND", HttpStatus.NOT_FOUND, "カードが見つかりません");
    }

    public static EventException eventNotStarted() {
        return new EventException("EVENT_NOT_STARTED", HttpStatus.CONFLICT, "このイベントはまだ始まっていません");
    }

    public static EventException inviteCodeGenerationFailed() {
        return new EventException(
            "INVITE_CODE_GENERATION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "招待コードを発行できませんでした");
    }
}
