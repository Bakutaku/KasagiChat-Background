package com.kasagichat.api.conversation.service;

import com.kasagichat.api.conversation.exception.InvalidConversationRequestException;
import com.kasagichat.api.conversation.model.enums.ConversationScene;
import com.kasagichat.api.conversation.model.enums.ConversationStatus;
import com.kasagichat.api.conversation.model.enums.ConversationType;
import com.kasagichat.api.npc.exception.NpcStateInvalidException;
import com.kasagichat.api.npc.model.Npc;

import lombok.Getter;

/**
 * 会話種別ごとに変わる規則をまとめた値オブジェクト。
 *
 * <p>開始・発言・振り返りの手続き自体は{@link ConversationService}が1本で持ち、
 * 種別で分岐するのは往復数の下限・上限、完了EXPのルールコード、NPCの前提状態だけに限定する。
 * 手続きを種別ごとに複製すると、振り返りの冪等性やLLM失敗時のロールバックといった
 * 不変条件が実装間でずれるため、データだけをここへ寄せている。</p>
 */
@Getter
public enum ConversationPolicy {

    /**
     * NPC誕生の初会話。3往復で振り返り可能になり、6往復で機械的に締める。
     */
    BIRTH(ConversationType.BIRTH, 3, 6, "BIRTH_COMPLETE", true, false),

    /**
     * 練習シーンでの会話。往復数の上限はなく、「会話を終える」でのみ終了する。
     */
    PRACTICE(ConversationType.PRACTICE, 1, null, "PRACTICE_COMPLETE", false, true),

    /**
     * 今日のひとこと。練習と同じく上限はない。
     */
    DAILY(ConversationType.DAILY, 1, null, "DAILY_COMPLETE", false, false);

    /**
     * 対象の会話種別。
     */
    private final ConversationType type;

    /**
     * 振り返りを実行できる最小の往復数。
     */
    private final int minTurns;

    /**
     * 会話を機械的に締める往復数。上限を設けない種別ではnull。
     */
    private final Integer maxTurns;

    /**
     * 完了時に加算するEXPのルールコード。
     */
    private final String expRuleCode;

    /**
     * 振り返りでNPCの誕生を確定するか。
     */
    private final boolean confirmsBirth;

    /**
     * 練習シーンの指定を必要とするか。
     */
    private final boolean requiresScene;

    ConversationPolicy(
        ConversationType type,
        int minTurns,
        Integer maxTurns,
        String expRuleCode,
        boolean confirmsBirth,
        boolean requiresScene
    ) {
        this.type = type;
        this.minTurns = minTurns;
        this.maxTurns = maxTurns;
        this.expRuleCode = expRuleCode;
        this.confirmsBirth = confirmsBirth;
        this.requiresScene = requiresScene;
    }

    /**
     * 会話種別に対応する規則を取得する。
     *
     * @param type 会話種別
     * @return 対応する規則
     */
    public static ConversationPolicy of(ConversationType type) {
        for (ConversationPolicy policy : values()) {
            if (policy.type == type) {
                return policy;
            }
        }
        throw new InvalidConversationRequestException("対応していない会話種別です。");
    }

    /**
     * 振り返りを実行できる状態かどうかを判定する。
     *
     * <p>レスポンスの{@code canFinish}はこのメソッドだけを根拠にする。
     * 会話一覧と会話詳細で判定が食い違わないようにするため、他の場所で往復数を比較しない。</p>
     *
     * @param turn 現在の往復数
     * @param status 会話の状態
     * @return 振り返りを実行できる場合はtrue
     */
    public boolean canFinish(int turn, ConversationStatus status) {
        return turn >= minTurns && status != ConversationStatus.REVIEWED;
    }

    /**
     * 次の応答で会話を締めるかどうかを判定する。
     *
     * @param nextTurn 応答後の往復数
     * @return 上限に達する場合はtrue。上限を設けない種別では常にfalse
     */
    public boolean isLastTurn(int nextTurn) {
        return maxTurns != null && nextTurn >= maxTurns;
    }

    /**
     * 往復数の上限があるかどうか。
     *
     * @return 上限がある場合はtrue
     */
    public boolean hasTurnLimit() {
        return maxTurns != null;
    }

    /**
     * 種別とシーンの組み合わせを検証する。
     *
     * @param scene リクエストされた練習シーン
     * @throws InvalidConversationRequestException 組み合わせが不正な場合
     */
    public void verifyScene(ConversationScene scene) {
        if (requiresScene && scene == null) {
            throw new InvalidConversationRequestException("練習会話にはsceneの指定が必要です。");
        }
        if (!requiresScene && scene != null) {
            throw new InvalidConversationRequestException("この会話種別にsceneは指定できません。");
        }
    }

    /**
     * NPCの誕生状態がこの会話種別の前提を満たすか検証する。
     *
     * @param npc 操作対象のNPC
     * @throws NpcStateInvalidException 前提を満たさない場合
     */
    public void verifyNpc(Npc npc) {
        boolean born = npc.getBornAt() != null;
        if (confirmsBirth && born) {
            throw new NpcStateInvalidException("NPCはすでに誕生しています。");
        }
        if (!confirmsBirth && !born) {
            throw new NpcStateInvalidException("NPCがまだ誕生していません。");
        }
    }
}
