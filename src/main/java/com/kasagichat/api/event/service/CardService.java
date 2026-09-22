package com.kasagichat.api.event.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.credential.service.LlmChatService;
import com.kasagichat.api.event.controller.dto.response.CardResponse;
import com.kasagichat.api.event.exception.EventException;
import com.kasagichat.api.event.model.Card;
import com.kasagichat.api.event.model.Event;
import com.kasagichat.api.event.model.enums.EventPhase;
import com.kasagichat.api.event.repository.CardRepository;
import com.kasagichat.api.npc.exception.NpcNotFoundException;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.TopicRepository;

import lombok.RequiredArgsConstructor;

/**
 * 出会いカードの参照と開封を扱う。
 *
 * <p>閲覧できるのは自分宛てのカードだけで、他人宛てのカードは存在を秘匿するため
 * CARD_NOT_FOUND で返す。</p>
 *
 * <p>会話報告は開封時に1回だけ生成して保存する。カードは「届いた手紙」であり、読み返すたびに
 * 内容が変わらないようにするためで、開封済みのカードはLLMを呼ばずに保存済みの内容を返す。</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final EventService eventService;
    private final CardRepository cardRepository;
    private final NpcRepository npcRepository;
    private final TopicRepository topicRepository;
    private final LlmChatService llmChatService;
    private final CardReportPromptFactory promptFactory;
    private final CardReportParser reportParser;

    /**
     * イベントでの自分宛てのカードを相性スコアの高い順に取得する。
     *
     * @param userId ユーザーの内部ID
     * @param eventId イベントの公開ID
     * @return カードの一覧
     */
    public List<CardResponse> listByEvent(Long userId, UUID eventId) {
        Event event = eventService.visibleEvent(userId, eventId);
        return toResponses(cardRepository.findByEventIdAndRecipientIdOrderByScoreDesc(event.getId(), userId));
    }

    /**
     * 自分宛てのカードを新しい順にすべて取得する。
     *
     * @param userId ユーザーの内部ID
     * @return カードの一覧
     */
    public List<CardResponse> list(Long userId) {
        return toResponses(cardRepository.findByRecipientIdOrderByCreatedAtDesc(userId));
    }

    /**
     * 自分宛てのカードを1枚取得する。開封済みの場合は保存済みの会話報告を含む。
     *
     * @param userId ユーザーの内部ID
     * @param cardId カードの公開ID
     * @return カード
     */
    public CardResponse get(Long userId, UUID cardId) {
        return toResponse(findMyCard(userId, cardId));
    }

    /**
     * カードを開封し、自分の分身からの会話報告とおすすめ話題を生成して保存する。
     *
     * <p>開封済みの場合はLLMを呼ばず、保存済みの内容をそのまま返す。生成に失敗した場合は
     * 開封日時も保存されないため、同じエンドポイントで開き直せる。</p>
     *
     * <p>生成は開いた本人のAI接続設定で実行する。各ユーザーが自分の分を自分のキーで生成し、
     * 結果を共有しないのがBYOKでの原則である。</p>
     *
     * @param userId ユーザーの内部ID
     * @param cardId カードの公開ID
     * @return 開封後のカード
     */
    @Transactional
    public CardResponse open(Long userId, UUID cardId) {
        Card card = findMyCard(userId, cardId);
        if (card.getOpenedAt() != null) {
            return toResponse(card);
        }
        if (phaseOf(card.getEvent()) == EventPhase.UPCOMING) {
            throw EventException.eventNotStarted();
        }

        Npc myNpc = npcRepository.findByUserId(userId).orElseThrow(NpcNotFoundException::new);
        // 相手は参加時に誕生済みの分身を求められているため通常は存在する。欠けている場合は
        // カードを見せられないので、存在を秘匿する側に倒す。
        Npc partnerNpc = npcRepository.findByUserId(card.getPartner().getId())
            .orElseThrow(EventException::cardNotFound);

        String generated = llmChatService.call(userId, promptFactory.reportPrompt(
            myNpc,
            topicRepository.findByNpcIdAndPublicTopicIsTrueOrderByInterestDescIdAsc(myNpc.getId()),
            partnerNpc,
            topicRepository.findByNpcIdAndPublicTopicIsTrueOrderByInterestDescIdAsc(partnerNpc.getId()),
            List.of(card.getCommonTags() == null ? new String[0] : card.getCommonTags())
        ));
        CardReport report = reportParser.parse(generated);

        card.setReport(report.report());
        card.setRecommendedTopics(report.recommendedTopics().toArray(String[]::new));
        card.setOpenedAt(Instant.now());
        // 楽観ロックで同時開封の二重生成を弾く。ここで初めてversionが突き合わされる。
        cardRepository.saveAndFlush(card);

        return toResponse(card, partnerNpc.getName(), partnerNpc.getPresetId());
    }

    private Card findMyCard(Long userId, UUID cardId) {
        return cardRepository.findByPublicIdAndRecipientId(cardId, userId)
            .orElseThrow(EventException::cardNotFound);
    }

    private EventPhase phaseOf(Event event) {
        return EventPhase.of(event.getStartsAt(), event.getEndsAt(), event.getArchivedAt(), Instant.now());
    }

    /** 相手の分身を1件ずつ引くとN+1になるため、一覧では名前とプリセットをまとめて解決する。 */
    private List<CardResponse> toResponses(List<Card> cards) {
        if (cards.isEmpty()) {
            return List.of();
        }
        Map<Long, PartnerNpc> partners = new HashMap<>();
        List<Long> partnerIds = cards.stream().map(card -> card.getPartner().getId()).distinct().toList();
        for (Object[] row : npcRepository.findNameAndPresetsByUserIds(partnerIds)) {
            partners.put((Long) row[0], new PartnerNpc((String) row[1], (String) row[2]));
        }
        return cards.stream()
            .map(card -> {
                PartnerNpc partner = partners.get(card.getPartner().getId());
                return partner == null
                    ? CardResponse.from(card, null, null)
                    : CardResponse.from(card, partner.name(), partner.presetId());
            })
            .toList();
    }

    private CardResponse toResponse(Card card) {
        return toResponses(List.of(card)).get(0);
    }

    private CardResponse toResponse(Card card, String partnerName, String partnerPresetId) {
        return CardResponse.from(card, partnerName, partnerPresetId);
    }

    /** カードに載せる相手の分身の表示情報。連番IDは公開しない。 */
    private record PartnerNpc(String name, String presetId) {
    }
}
