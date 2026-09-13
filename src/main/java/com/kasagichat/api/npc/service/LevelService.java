package com.kasagichat.api.npc.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.master.model.LevelCurve;
import com.kasagichat.api.master.repository.LevelCurveRepository;
import com.kasagichat.api.npc.model.GrowthEvent;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.enums.GrowthEventType;
import com.kasagichat.api.npc.repository.GrowthEventRepository;

import lombok.RequiredArgsConstructor;

/**
 * NPCのEXPとレベルを扱うService。
 */
@Service
@RequiredArgsConstructor
public class LevelService {

    private final LevelCurveRepository levelCurveRepository;

    private final GrowthEventRepository growthEventRepository;

    /**
     * 次のレベルに到達するために必要な累計EXPを取得する。
     *
     * @param level 現在のレベル
     * @return 次のレベルに必要な累計EXP。最大レベルの場合はnull
     */
    public Integer findNextLevelExp(int level) {
        return levelCurveRepository.findById(level + 1)
            .map(LevelCurve::getRequiredExp)
            .orElse(null);
    }

    /**
     * NPCにEXPを加算し、レベル曲線からレベルを再計算する。レベルアップした場合は通知を保存する。
     *
     * <p>NPCは呼び出し側のトランザクションで排他ロックされ、管理されている前提とし、
     * 変更は変更検知で保存する。</p>
     *
     * @param npc EXPを加算するNPC
     * @param gainedExp 加算するEXP
     * @return 加算後のレベルと、レベルアップしたかどうか
     */
    @Transactional
    public LevelUpResult applyExp(Npc npc, int gainedExp) {
        int beforeLevel = npc.getLevel();
        int totalExp = npc.getExp() + gainedExp;
        int reachedLevel = levelCurveRepository.findAllByOrderByLevelAsc().stream()
            .filter(curve -> curve.getRequiredExp() <= totalExp)
            .mapToInt(LevelCurve::getLevel)
            .max()
            .orElse(beforeLevel);
        // マスタを調整した場合でも、レベルは下げない。
        int level = Math.max(beforeLevel, reachedLevel);

        npc.setExp(totalExp);
        npc.setLevel(level);

        boolean leveledUp = level > beforeLevel;
        if (leveledUp) {
            growthEventRepository.save(GrowthEvent.builder()
                .user(npc.getUser())
                .type(GrowthEventType.LEVEL_UP)
                .message(npc.getName() + "がレベル" + level + "になりました！")
                .build());
        }
        return new LevelUpResult(level, leveledUp);
    }
}
