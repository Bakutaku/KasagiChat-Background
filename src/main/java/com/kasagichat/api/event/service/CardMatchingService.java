package com.kasagichat.api.event.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.event.model.Card;
import com.kasagichat.api.event.model.Event;
import com.kasagichat.api.event.model.EventParticipant;
import com.kasagichat.api.event.repository.CardRepository;
import com.kasagichat.api.event.repository.EventParticipantRepository;
import com.kasagichat.api.event.repository.EventRepository;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.Topic;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.TopicRepository;
import com.kasagichat.api.security.model.Users;

import lombok.RequiredArgsConstructor;

/**
 * 公開話題のタグ一致から、イベントの出会いカードを作り直す。
 *
 * <p>計算はすべてサーバー内で完結させる。LLMもembeddingも使わず、同じ入力からは必ず同じ結果になる
 * 決定的な計算にすることで、説明可能性とテストのしやすさを保つ（idea.md「マッチングロジック」）。</p>
 *
 * <p>参加のたびにイベント全体を計算し直す。数十人規模を想定しており、差分計算にする利点よりも、
 * 「現在参加中の全員が同じ規則で並んでいる」という単純さのほうが価値が大きい。</p>
 *
 * <p>すでにあるカードは組み合わせごとに更新する（upsert）。開封済みの会話報告は作り直さない。
 * 一度読んだ手紙の内容が後から書き換わらないようにするためで、更新するのは相性スコアと共通タグだけである。
 * 選ばれなくなった組み合わせのカードも消さない。届いた手紙を後から取り上げないための方針である。</p>
 */
@Service
@RequiredArgsConstructor
public class CardMatchingService {

    /** 各参加者に届けるカードの基準となる上位人数。 */
    private static final int TOP_PARTNERS = 3;

    /** カテゴリ一致の重み。互いの興味度の積に掛ける。 */
    private static final int CATEGORY_WEIGHT = 2;

    /** 同カテゴリ内で話題名も近い場合の加点。 */
    private static final int NAME_MATCH_BONUS = 5;

    /** カード表面に載せる共通タグの上限。 */
    private static final int MAX_COMMON_TAGS = 5;

    /** 部分一致とみなす話題名の最小の長さ。1文字の包含は偶然の一致が多いため除く。 */
    private static final int MIN_NAME_MATCH_LENGTH = 2;

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final NpcRepository npcRepository;
    private final TopicRepository topicRepository;
    private final CardRepository cardRepository;

    /**
     * イベントの現在参加中の参加者すべてでマッチングを計算し直し、カードを反映する。
     *
     * <p>呼び出し元のトランザクションに参加する。参加処理とカードの反映は同じ単位で成否を揃える。</p>
     *
     * @param eventId イベントの内部ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recalculate(Long eventId) {
        // 同時参加で同じ組み合わせのカードを二重に作らないよう、イベント単位で計算を直列化する。
        Event event = eventRepository.findByIdForUpdate(eventId).orElse(null);
        if (event == null) {
            return;
        }

        List<EventParticipant> participants =
            eventParticipantRepository.findByEventIdAndLeftAtIsNull(eventId);
        if (participants.size() < 2) {
            return;
        }

        Map<Long, Users> usersById = new LinkedHashMap<>();
        for (EventParticipant participant : participants) {
            usersById.putIfAbsent(participant.getUser().getId(), participant.getUser());
        }
        // 同点のタイブレークまで決定的にするため、以降の走査順をユーザーIDで固定する。
        List<Long> userIds = usersById.keySet().stream().sorted().toList();

        Map<Long, Profile> profiles = loadProfiles(userIds);
        if (profiles.size() < 2) {
            return;
        }

        Map<Long, Map<Long, Match>> matches = computeMatches(profiles);
        Set<Pair> pairs = selectPairs(profiles, matches);
        upsertCards(event, usersById, matches, pairs);
    }

    /**
     * 参加者ごとの公開話題をカテゴリ単位にまとめる。
     *
     * <p>分身が無い参加者と、公開話題を1件も持たない参加者も対象に残す。共通点が無くても
     * 「最低1枚保証」の相手になり得るためである。</p>
     */
    private Map<Long, Profile> loadProfiles(List<Long> userIds) {
        List<Npc> npcs = npcRepository.findByUserIdIn(userIds);
        if (npcs.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> userIdByNpcId = new HashMap<>();
        Map<Long, Long> npcIdByUserId = new HashMap<>();
        for (Npc npc : npcs) {
            userIdByNpcId.put(npc.getId(), npc.getUser().getId());
            npcIdByUserId.put(npc.getUser().getId(), npc.getId());
        }

        Map<Long, Map<Long, CategoryTopics>> byUser = new HashMap<>();
        Map<Long, Integer> interestTotals = new HashMap<>();
        for (Topic topic : topicRepository.findPublicCategorizedByNpcIds(npcIdByUserId.values())) {
            Long userId = userIdByNpcId.get(topic.getNpc().getId());
            if (userId == null) {
                continue;
            }
            int interest = topic.getInterest() == null ? 0 : topic.getInterest();
            interestTotals.merge(userId, Math.max(0, interest), Integer::sum);
            byUser.computeIfAbsent(userId, key -> new LinkedHashMap<>())
                .computeIfAbsent(
                    topic.getCategory().getId(),
                    key -> new CategoryTopics(topic.getCategory().getDisplayName()))
                .add(topic.getName(), interest);
        }

        Map<Long, Profile> profiles = new LinkedHashMap<>();
        for (Long userId : userIds) {
            if (!npcIdByUserId.containsKey(userId)) {
                continue;
            }
            profiles.put(userId, new Profile(
                byUser.getOrDefault(userId, Map.of()),
                interestTotals.getOrDefault(userId, 0)
            ));
        }
        return profiles;
    }

    /** 参加者の全ペアで相性を計算する。相性は対称なので、片側だけ計算して両側に配る。 */
    private Map<Long, Map<Long, Match>> computeMatches(Map<Long, Profile> profiles) {
        List<Long> userIds = profiles.keySet().stream().sorted().toList();
        Map<Long, Map<Long, Match>> matches = new HashMap<>();
        for (Long userId : userIds) {
            matches.put(userId, new HashMap<>());
        }
        for (int left = 0; left < userIds.size(); left++) {
            for (int right = left + 1; right < userIds.size(); right++) {
                Long a = userIds.get(left);
                Long b = userIds.get(right);
                Match match = match(profiles.get(a), profiles.get(b));
                matches.get(a).put(b, match);
                matches.get(b).put(a, match);
            }
        }
        return matches;
    }

    /**
     * 2人の相性スコアと共通タグを求める。
     *
     * <p>公開話題のカテゴリが一致するたびに、互いの興味度の積で重み付けして加点する。
     * 同じカテゴリの中で話題名も部分一致していれば、さらにボーナスを加える。</p>
     */
    private Match match(Profile a, Profile b) {
        List<TagScore> tags = new ArrayList<>();
        int score = 0;
        for (Map.Entry<Long, CategoryTopics> entry : a.categories().entrySet()) {
            CategoryTopics other = b.categories().get(entry.getKey());
            if (other == null) {
                continue;
            }
            CategoryTopics mine = entry.getValue();
            int categoryScore = CATEGORY_WEIGHT * mine.maxInterest() * other.maxInterest();
            if (hasSimilarName(mine.names(), other.names())) {
                categoryScore += NAME_MATCH_BONUS;
            }
            score += categoryScore;
            tags.add(new TagScore(mine.displayName(), categoryScore));
        }
        List<String> commonTags = tags.stream()
            .sorted(Comparator.comparingInt(TagScore::score).reversed()
                .thenComparing(TagScore::displayName))
            .limit(MAX_COMMON_TAGS)
            .map(TagScore::displayName)
            .toList();
        return new Match(score, commonTags);
    }

    /** 話題名の近さは部分一致で足りる。表記ゆれまで吸収する必要はなく、加点の根拠を説明できることを優先する。 */
    private boolean hasSimilarName(List<String> left, List<String> right) {
        for (String name : left) {
            for (String candidate : right) {
                if (name.length() < MIN_NAME_MATCH_LENGTH || candidate.length() < MIN_NAME_MATCH_LENGTH) {
                    continue;
                }
                if (name.contains(candidate) || candidate.contains(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * カードを作る組み合わせを決める。
     *
     * <p>各参加者の上位{@value #TOP_PARTNERS}人を選び、相互保証として、片側の上位に入っていれば
     * 両方にカードを作る。上位は相性が0の相手も候補に含めるため、参加者が2人以上いれば
     * 全員が最低1枚受け取る。</p>
     */
    private Set<Pair> selectPairs(Map<Long, Profile> profiles, Map<Long, Map<Long, Match>> matches) {
        List<Long> userIds = profiles.keySet().stream().sorted().toList();
        Set<Pair> pairs = new HashSet<>();
        Set<Long> covered = new HashSet<>();
        Map<Long, List<Long>> rankings = new HashMap<>();
        for (Long userId : userIds) {
            List<Long> ranking = rank(userId, userIds, matches, profiles);
            rankings.put(userId, ranking);
            for (Long partnerId : ranking.stream().limit(TOP_PARTNERS).toList()) {
                pairs.add(Pair.of(userId, partnerId));
                covered.add(userId);
                covered.add(partnerId);
            }
        }
        // 最低1枚保証。上位に相性0の相手も含めているため通常は到達しないが、規則としては明示しておく。
        for (Long userId : userIds) {
            if (covered.contains(userId)) {
                continue;
            }
            rankings.get(userId).stream().findFirst().ifPresent(best -> {
                pairs.add(Pair.of(userId, best));
                covered.add(userId);
                covered.add(best);
            });
        }
        return pairs;
    }

    /**
     * 自分以外の参加者を相性の高い順に並べる。
     *
     * <p>同点は、相手の公開話題の興味度合計、共通タグの数、ユーザーIDの順で機械的に決める。
     * 最後にIDを見ることで、同じ入力なら必ず同じ並びになる。</p>
     */
    private List<Long> rank(
        Long userId,
        List<Long> userIds,
        Map<Long, Map<Long, Match>> matches,
        Map<Long, Profile> profiles
    ) {
        Map<Long, Match> mine = matches.get(userId);
        return userIds.stream()
            .filter(candidate -> !candidate.equals(userId))
            .sorted(Comparator
                .comparingInt((Long candidate) -> mine.get(candidate).score()).reversed()
                .thenComparing(Comparator.comparingInt(
                    (Long candidate) -> profiles.get(candidate).totalInterest()).reversed())
                .thenComparing(Comparator.comparingInt(
                    (Long candidate) -> mine.get(candidate).commonTags().size()).reversed())
                .thenComparing(Comparator.naturalOrder()))
            .toList();
    }

    /**
     * 決まった組み合わせをカードへ反映する。
     *
     * <p>同じ(イベント, 受取人, 相手)のカードがあれば相性スコアと共通タグだけを更新し、
     * 開封状態と会話報告はそのまま残す。</p>
     */
    private void upsertCards(
        Event event,
        Map<Long, Users> usersById,
        Map<Long, Map<Long, Match>> matches,
        Set<Pair> pairs
    ) {
        Map<String, Card> existing = new HashMap<>();
        for (Card card : cardRepository.findByEventId(event.getId())) {
            existing.put(key(card.getRecipient().getId(), card.getPartner().getId()), card);
        }

        List<Card> saved = new ArrayList<>();
        for (Pair pair : pairs.stream().sorted().toList()) {
            Match match = matches.get(pair.low()).get(pair.high());
            saved.add(upsert(event, usersById, existing, pair.low(), pair.high(), match));
            saved.add(upsert(event, usersById, existing, pair.high(), pair.low(), match));
        }
        cardRepository.saveAll(saved);
    }

    private Card upsert(
        Event event,
        Map<Long, Users> usersById,
        Map<String, Card> existing,
        Long recipientId,
        Long partnerId,
        Match match
    ) {
        String[] commonTags = match.commonTags().toArray(String[]::new);
        Card card = existing.get(key(recipientId, partnerId));
        if (card == null) {
            return Card.builder()
                .event(event)
                .recipient(usersById.get(recipientId))
                .partner(usersById.get(partnerId))
                .score(match.score())
                .commonTags(commonTags)
                .build();
        }
        card.setScore(match.score());
        card.setCommonTags(commonTags);
        return card;
    }

    private String key(Long recipientId, Long partnerId) {
        return recipientId + ":" + partnerId;
    }

    /** 参加者1人分の、カテゴリ単位にまとめた公開話題。 */
    private record Profile(Map<Long, CategoryTopics> categories, int totalInterest) {
    }

    /** 同じカテゴリに属する公開話題の集まり。重み付けには同カテゴリ内の最大の興味度を使う。 */
    private static final class CategoryTopics {
        private final String displayName;
        private final List<String> names = new ArrayList<>();
        private int maxInterest;

        private CategoryTopics(String displayName) {
            this.displayName = displayName;
        }

        private void add(String name, int interest) {
            names.add(name == null ? "" : name.strip().toLowerCase(Locale.ROOT));
            maxInterest = Math.max(maxInterest, Math.max(0, interest));
        }

        private String displayName() {
            return displayName;
        }

        private List<String> names() {
            return names;
        }

        private int maxInterest() {
            return maxInterest;
        }
    }

    /** 2人分の相性の計算結果。相性は対称なので受取人ごとに持ち分ける必要はない。 */
    private record Match(int score, List<String> commonTags) {
    }

    /** 共通タグ1件分の内訳。並び順を決めるためだけに使う。 */
    private record TagScore(String displayName, int score) {
    }

    /** カードを作る組み合わせ。向きを持たないため、ユーザーIDの小さい順に正規化する。 */
    private record Pair(Long low, Long high) implements Comparable<Pair> {

        static Pair of(Long left, Long right) {
            return left <= right ? new Pair(left, right) : new Pair(right, left);
        }

        @Override
        public int compareTo(Pair other) {
            int byLow = low.compareTo(other.low);
            return byLow != 0 ? byLow : high.compareTo(other.high);
        }
    }
}
