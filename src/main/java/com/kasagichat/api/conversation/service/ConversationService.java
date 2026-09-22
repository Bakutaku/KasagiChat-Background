package com.kasagichat.api.conversation.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.conversation.controller.dto.request.SendMessageRequest;
import com.kasagichat.api.conversation.controller.dto.request.StartConversationRequest;
import com.kasagichat.api.conversation.controller.dto.response.ConversationMessageResponse;
import com.kasagichat.api.conversation.controller.dto.response.ConversationResponse;
import com.kasagichat.api.conversation.controller.dto.response.ConversationSummaryResponse;
import com.kasagichat.api.conversation.controller.dto.response.NewTopicResponse;
import com.kasagichat.api.conversation.controller.dto.response.ReviewConversationResponse;
import com.kasagichat.api.conversation.controller.dto.response.SendMessageResponse;
import com.kasagichat.api.conversation.exception.ConversationConfigurationException;
import com.kasagichat.api.conversation.exception.ConversationFinishedException;
import com.kasagichat.api.conversation.exception.ConversationNotFoundException;
import com.kasagichat.api.conversation.exception.ConversationTooShortException;
import com.kasagichat.api.conversation.exception.DailyQuestionNotAvailableException;
import com.kasagichat.api.conversation.exception.TurnMismatchException;
import com.kasagichat.api.conversation.model.Conversation;
import com.kasagichat.api.conversation.model.DailyQuestion;
import com.kasagichat.api.conversation.model.Message;
import com.kasagichat.api.conversation.model.enums.ConversationScene;
import com.kasagichat.api.conversation.model.enums.ConversationStatus;
import com.kasagichat.api.conversation.model.enums.ConversationType;
import com.kasagichat.api.conversation.model.enums.MessageRole;
import com.kasagichat.api.conversation.repository.ConversationRepository;
import com.kasagichat.api.conversation.repository.DailyQuestionRepository;
import com.kasagichat.api.conversation.repository.MessageRepository;
import com.kasagichat.api.credential.service.LlmChatService;
import com.kasagichat.api.master.model.ConversationOpening;
import com.kasagichat.api.master.model.LevelCurve;
import com.kasagichat.api.master.model.TopicCategory;
import com.kasagichat.api.master.repository.ConversationOpeningRepository;
import com.kasagichat.api.master.repository.ExpRuleRepository;
import com.kasagichat.api.master.repository.LevelCurveRepository;
import com.kasagichat.api.master.repository.TopicCategoryRepository;
import com.kasagichat.api.npc.exception.NpcNotFoundException;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.Topic;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.TopicRepository;

import lombok.RequiredArgsConstructor;

/**
 * 会話の開始・継続・振り返りを扱う。
 *
 * <p>NPC誕生・練習・今日のひとことで手続きは共通で、種別ごとに変わる規則は
 * {@link ConversationPolicy}が持つ。手続きを種別ごとに分けると、振り返りの冪等性や
 * LLM失敗時のロールバックといった不変条件が実装間でずれるため、1本にまとめている。</p>
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final short DEFAULT_TOPIC_INTEREST = 3;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final DailyQuestionRepository dailyQuestionRepository;
    private final ConversationOpeningRepository conversationOpeningRepository;
    private final NpcRepository npcRepository;
    private final TopicRepository topicRepository;
    private final ExpRuleRepository expRuleRepository;
    private final LevelCurveRepository levelCurveRepository;
    private final TopicCategoryRepository topicCategoryRepository;
    private final LlmChatService llmChatService;
    private final ConversationPromptFactory promptFactory;
    private final ConversationReviewParser reviewParser;

    /**
     * 会話を開始する。同じ種別・シーンに未振り返りの会話があれば新規作成せず再開する。
     *
     * <p>開始時にLLMは呼ばない。冒頭の台詞はマスタからの抽選か、今日のひとことの
     * 未消化の質問を、そのまま1件目のメッセージとして保存する。</p>
     */
    @Transactional
    public ConversationStartResult start(Long userId, StartConversationRequest request) {
        ConversationPolicy policy = ConversationPolicy.of(request.type());
        policy.verifyScene(request.scene());

        // NPCの行ロックは前提状態の検証だけでなく、同じ種別・シーンの未振り返り会話を
        // 1件に保つための直列化点も兼ねる。同時に開始要求が来ても二重作成にならない。
        Npc npc = npcRepository.findByUserIdForUpdate(userId)
            .orElseThrow(NpcNotFoundException::new);
        policy.verifyNpc(npc);

        var existing = conversationRepository
            .findFirstByUserIdAndTypeAndSceneAndStatusNotOrderByCreatedAtDesc(
                userId,
                request.type(),
                request.scene(),
                ConversationStatus.REVIEWED
            );
        if (existing.isPresent()) {
            return new ConversationStartResult(toResponse(existing.get()), false);
        }

        Conversation.ConversationBuilder builder = Conversation.builder()
            .user(npc.getUser())
            .type(request.type())
            .scene(request.scene());
        String openingLine;
        if (policy == ConversationPolicy.DAILY) {
            DailyQuestion question = dailyQuestionRepository
                .findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtAsc(userId)
                .orElseThrow(DailyQuestionNotAvailableException::new);
            // 再開はこの分岐へ到達しないため、同じ質問が二重に消化されることはない。
            question.setConsumedAt(Instant.now());
            builder.dailyQuestion(question);
            openingLine = question.getQuestion();
        } else {
            ConversationOpening opening = selectOpening(userId, request.type(), request.scene());
            builder.opening(opening);
            openingLine = opening.getLine();
        }

        Conversation conversation = builder.build();
        conversationRepository.saveAndFlush(conversation);
        messageRepository.save(Message.builder()
            .conversation(conversation)
            .seq(1)
            .role(MessageRole.ASSISTANT)
            .text(openingLine)
            .build());
        return new ConversationStartResult(toResponse(conversation), true);
    }

    /**
     * 本人の会話を保存済みログとともに取得する。
     */
    @Transactional(readOnly = true)
    public ConversationResponse get(Long userId, UUID conversationId) {
        Conversation conversation = conversationRepository
            .findByPublicIdAndUserId(conversationId, userId)
            .orElseThrow(ConversationNotFoundException::new);
        return toResponse(conversation);
    }

    /**
     * 未振り返りの会話を新しい順に取得する。家の一覧から再開するために使う。
     *
     * @param userId ユーザーの内部ID
     * @return 進行中と終了済みを合わせた会話の要約
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> findUnreviewed(Long userId) {
        return conversationRepository
            .findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, ConversationStatus.REVIEWED)
            .stream()
            .map(ConversationSummaryResponse::from)
            .toList();
    }

    /**
     * 今日のひとことに使える未消化の質問を取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 未消化の質問。存在しない場合は空
     */
    @Transactional(readOnly = true)
    public Optional<String> findDailyQuestion(Long userId) {
        return dailyQuestionRepository
            .findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtAsc(userId)
            .map(DailyQuestion::getQuestion);
    }

    /**
     * 発言を送り、設定済みLLMからNPCの応答を1件得る。
     *
     * <p>LLM呼び出しを含む処理全体を同じトランザクションに置き、失敗時は
     * ユーザー発言もデモ利用回数も保存しない。</p>
     */
    @Transactional
    public SendMessageResponse send(Long userId, UUID conversationId, SendMessageRequest request) {
        Conversation conversation = findForUpdate(userId, conversationId);
        ConversationPolicy policy = ConversationPolicy.of(conversation.getType());
        if (conversation.getStatus() != ConversationStatus.IN_PROGRESS) {
            throw new ConversationFinishedException();
        }
        if (!conversation.getTurn().equals(request.expectedTurn())) {
            throw new TurnMismatchException();
        }

        Npc npc = npcRepository.findByUserId(userId).orElseThrow(NpcNotFoundException::new);
        policy.verifyNpc(npc);

        List<Message> history = messageRepository.findByConversationIdOrderBySeqAsc(conversation.getId());
        int nextTurn = conversation.getTurn() + 1;
        String replyText = llmChatService.call(
            userId,
            promptFactory.conversationPrompt(
                npc,
                conversation,
                policy,
                history,
                request.text().strip(),
                nextTurn
            )
        );

        int userSequence = history.size() + 1;
        messageRepository.save(Message.builder()
            .conversation(conversation)
            .seq(userSequence)
            .role(MessageRole.USER)
            .text(request.text().strip())
            .build());
        Message reply = messageRepository.save(Message.builder()
            .conversation(conversation)
            .seq(userSequence + 1)
            .role(MessageRole.ASSISTANT)
            .text(replyText)
            .build());

        conversation.setTurn(nextTurn);
        boolean finished = policy.isLastTurn(nextTurn);
        if (finished) {
            conversation.setStatus(ConversationStatus.FINISHED);
        }
        return new SendMessageResponse(
            nextTurn,
            ConversationMessageResponse.from(reply),
            policy.canFinish(nextTurn, conversation.getStatus()),
            finished
        );
    }

    /**
     * 会話を振り返り、人格・口調・話題・EXPを一括反映する。NPC誕生では誕生日時も確定する。
     */
    @Transactional
    public ReviewConversationResponse review(Long userId, UUID conversationId) {
        Conversation conversation = findForUpdate(userId, conversationId);
        ConversationPolicy policy = ConversationPolicy.of(conversation.getType());
        // 振り返り済みの再実行では必ずここで保存済み結果を返す。NPCの状態検証より後ろに置くと、
        // 誕生確定後の再実行がNPC_STATE_INVALIDになり、リトライ導線と冪等性が壊れる。
        if (conversation.getStatus() == ConversationStatus.REVIEWED) {
            return savedReview(conversation);
        }
        if (conversation.getTurn() < policy.getMinTurns()) {
            throw new ConversationTooShortException(policy.getMinTurns());
        }

        Npc npc = npcRepository.findByUserIdForUpdate(userId).orElseThrow(NpcNotFoundException::new);
        policy.verifyNpc(npc);

        List<Message> history = messageRepository.findByConversationIdOrderBySeqAsc(conversation.getId());
        String generated = llmChatService.call(userId, promptFactory.reviewPrompt(npc, policy, history, categoryCatalog()));
        ConversationReview review = reviewParser.parse(generated);
        Instant now = Instant.now();

        npc.setProfile(review.profile());
        npc.setSpeechStyle(review.speechStyle());
        if (policy.isConfirmsBirth()) {
            npc.setBornAt(now);
        }

        int gainedExp = calculateExp(policy, conversation.getTurn());
        int previousLevel = npc.getLevel();
        int totalExp = npc.getExp() + gainedExp;
        int reachedLevel = levelCurveRepository.findAllByOrderByLevelAsc().stream()
            .filter(curve -> curve.getRequiredExp() <= totalExp)
            .mapToInt(LevelCurve::getLevel)
            .max()
            .orElse(previousLevel);
        int level = Math.max(previousLevel, reachedLevel);
        npc.setExp(totalExp);
        npc.setLevel(level);

        List<Topic> newTopics = saveTopics(npc, conversation, review.topics(), now);
        saveDailyQuestion(npc, conversation, review.dailyQuestion());

        conversation.setStatus(ConversationStatus.REVIEWED);
        conversation.setReviewFeedback(review.feedback());
        conversation.setReviewExpGained(gainedExp);
        conversation.setReviewLevel(level);
        conversation.setReviewLeveledUp(level > previousLevel);
        conversation.setReviewedAt(now);

        return new ReviewConversationResponse(
            review.feedback(),
            gainedExp,
            level,
            level > previousLevel,
            newTopics.stream().map(NewTopicResponse::from).toList()
        );
    }

    private Conversation findForUpdate(Long userId, UUID conversationId) {
        return conversationRepository
            .findByPublicIdAndUserIdForUpdate(conversationId, userId)
            .orElseThrow(ConversationNotFoundException::new);
    }

    private ConversationOpening selectOpening(
        Long userId,
        ConversationType type,
        ConversationScene scene
    ) {
        List<ConversationOpening> openings = conversationOpeningRepository
            .findByConversationTypeAndSceneAndEnabledTrue(type, scene);
        if (openings.isEmpty()) {
            throw new ConversationConfigurationException("この会話の開始設定がありません。");
        }

        Long previousId = conversationRepository.findFirstByUserIdAndOpeningIsNotNullOrderByCreatedAtDesc(userId)
            .map(Conversation::getOpening)
            .map(ConversationOpening::getId)
            .orElse(null);
        List<ConversationOpening> candidates = openings.stream()
            .filter(opening -> openings.size() == 1 || !opening.getId().equals(previousId))
            .toList();
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return ConversationResponse.from(
            conversation,
            messageRepository.findByConversationIdOrderBySeqAsc(conversation.getId())
        );
    }

    private int calculateExp(ConversationPolicy policy, int turns) {
        int perMessage = expRuleRepository.findByCode("USER_MESSAGE")
            .map(rule -> rule.getExp())
            .orElse(0);
        int completion = expRuleRepository.findByCode(policy.getExpRuleCode())
            .map(rule -> rule.getExp())
            .orElse(0);
        return Math.max(0, perMessage) * turns + Math.max(0, completion);
    }

    /**
     * 振り返りのプロンプトへ渡す話題カテゴリの候補を組み立てる。
     *
     * <p>カタログをプロンプトへ入れないと、LLMは自由なカテゴリ名を返してしまい
     * マスタと結び付かない。結果として思い出の品が本棚にしか並ばなくなる。</p>
     */
    private String categoryCatalog() {
        return topicCategoryRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
            .map(category -> category.getCode() + "=" + category.getName())
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
    }

    /**
     * カテゴリコードをマスタへ解決する。未知のコードは分類なしとして扱う。
     */
    private TopicCategory resolveCategory(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return topicCategoryRepository.findByCode(code).orElse(null);
    }

    /**
     * 次回の今日のひとことの質問を保存する。
     *
     * <p>未消化の質問が残っている場合は追加しない。練習の回数だけ質問が積み上がると
     * 在庫を抱えた状態が続き、家の「今日のひとこと」が毎日の区切りとして機能しなくなる。</p>
     */
    private void saveDailyQuestion(Npc npc, Conversation conversation, String question) {
        if (question == null) {
            return;
        }
        boolean pendingExists = dailyQuestionRepository
            .findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtAsc(npc.getUser().getId())
            .isPresent();
        if (pendingExists) {
            return;
        }
        dailyQuestionRepository.save(DailyQuestion.builder()
            .user(npc.getUser())
            .question(question)
            .sourceConversation(conversation)
            .build());
    }

    private List<Topic> saveTopics(
        Npc npc,
        Conversation conversation,
        List<GeneratedTopic> generated,
        Instant learnedAt
    ) {
        if (generated.isEmpty()) {
            return List.of();
        }
        // 既存の話題名は大文字小文字を無視して突き合わせる。SQLのINは区別するため、
        // 名前を取り出してから比較しないと一意制約違反で振り返り全体がロールバックし、
        // 同じ理由で再実行も失敗し続ける会話が生まれる。
        Set<String> seen = new HashSet<>();
        topicRepository.findByNpcIdOrderByLearnedAtDescIdDesc(npc.getId()).forEach(topic ->
            seen.add(topic.getName().toLowerCase(Locale.ROOT))
        );
        List<Topic> topics = generated.stream()
            .filter(topic -> seen.add(topic.name().toLowerCase(Locale.ROOT)))
            .map(topic -> Topic.builder()
                .npc(npc)
                .name(topic.name())
                .category(resolveCategory(topic.categoryCode()))
                .interest(DEFAULT_TOPIC_INTEREST)
                .publicTopic(false)
                .learnedAt(learnedAt)
                .sourceConversation(conversation)
                .build())
            .toList();
        return topicRepository.saveAll(topics);
    }

    private ReviewConversationResponse savedReview(Conversation conversation) {
        List<NewTopicResponse> topics = topicRepository
            .findBySourceConversationIdOrderByIdAsc(conversation.getId())
            .stream()
            .map(NewTopicResponse::from)
            .toList();
        return new ReviewConversationResponse(
            conversation.getReviewFeedback(),
            conversation.getReviewExpGained(),
            conversation.getReviewLevel(),
            conversation.getReviewLeveledUp(),
            topics
        );
    }
}
