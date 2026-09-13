package com.kasagichat.api.event.config;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.kasagichat.api.event.model.Event;
import com.kasagichat.api.event.model.EventParticipant;
import com.kasagichat.api.event.model.enums.VenueTemplate;
import com.kasagichat.api.event.repository.EventParticipantRepository;
import com.kasagichat.api.event.repository.EventRepository;
import com.kasagichat.api.master.repository.TopicCategoryRepository;
import com.kasagichat.api.npc.model.Memory;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.Topic;
import com.kasagichat.api.npc.repository.MemoryRepository;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.TopicRepository;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 開発環境で、サンプルNPCが参加する常設デモイベントを作成する初期化処理。
 *
 * <p>サンプルユーザーはOAuthアカウントを持たず、ログインできない。
 * 話題カテゴリを参照するため、マスタの初期化処理の後に実行する。</p>
 */
@Component
@Profile("debug")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DevelopmentDemoEventInitializer implements ApplicationRunner {

    /**
     * 常設デモイベントの招待コード。
     */
    public static final String DEMO_INVITE_CODE = "DEMO2026";

    private static final Instant DEMO_STARTS_AT = Instant.parse("2026-01-01T00:00:00Z");

    // 常設イベントとして扱うため、終了日時を十分先にする。
    private static final Instant DEMO_ENDS_AT = Instant.parse("2099-12-31T23:59:59Z");

    private static final List<SampleNpc> SAMPLE_NPCS = List.of(
        new SampleNpc(
            "サンプル ユウキ", "ユウ", "SAMPLE_A",
            "好奇心旺盛で、新しいものを見つけると誰かに話したくなるタイプ。初対面でも相手の好きなものを聞くのが得意。",
            "語尾が柔らかく「〜だよね」「〜かも」をよく使う。短い文でテンポよく話す。",
            List.of(
                new SampleTopic("ELDEN RING", "GAME", 5, true, "最近ELDEN RINGを100時間遊んで、ようやく全ボスを倒したらしい。"),
                new SampleTopic("スペシャルティコーヒー", "CAFE", 4, true, "休日は浅煎りのコーヒー豆を買いに喫茶店を巡っている。"),
                new SampleTopic("Kotlin", "TECH", 3, true, "仕事でAndroidアプリをKotlinで作っている。")
            )
        ),
        new SampleNpc(
            "サンプル ハルカ", "ハル", "SAMPLE_B",
            "落ち着いていて聞き上手。計画を立てるのが好きで、準備をしっかりしてから動く。",
            "丁寧語が基本で「〜ですね」とやさしく相づちを打つ。",
            List.of(
                new SampleTopic("北海道旅行", "TRAVEL", 5, true, "夏に北海道を一周して、富良野のラベンダー畑が一番よかったと話していた。"),
                new SampleTopic("ジブリ映画", "MOVIE", 4, true, "『千と千尋の神隠し』を毎年一度は見返している。"),
                new SampleTopic("ハンドドリップ", "CAFE", 3, true, "朝は必ずハンドドリップでコーヒーを淹れる。")
            )
        ),
        new SampleNpc(
            "サンプル ソラ", "ソラ", "SAMPLE_C",
            "明るくて行動派。チームで何かを作るのが好きで、ハッカソンによく参加している。",
            "元気な口調で「！」が多い。擬音をよく使う。",
            List.of(
                new SampleTopic("ハッカソン", "TECH", 5, true, "先月のハッカソンでチームリーダーを務め、審査員賞をもらった。"),
                new SampleTopic("スプラトゥーン", "GAME", 4, true, "週末は友達とスプラトゥーンのナワバリバトルをしている。"),
                new SampleTopic("ソロキャンプ", "OUTDOOR", 3, true, "春に初めてソロキャンプに挑戦して、焚き火で料理をした。"),
                // 非公開の話題がイベントで使われないことを確認するためのサンプル。
                new SampleTopic("祖母の介護", null, 3, false, "週末は実家で祖母の介護を手伝っている。")
            )
        )
    );

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final UsersRepository usersRepository;
    private final NpcRepository npcRepository;
    private final TopicRepository topicRepository;
    private final MemoryRepository memoryRepository;
    private final TopicCategoryRepository topicCategoryRepository;

    /**
     * 常設デモイベントが未作成の場合だけ、サンプルユーザー・NPC・話題・思い出とイベントを作成する。
     *
     * @param args 起動引数
     */
    @Override
    public void run(ApplicationArguments args) {
        if (eventRepository.findByInviteCode(DEMO_INVITE_CODE).isPresent()) {
            return;
        }

        Instant now = Instant.now();
        List<Users> users = new ArrayList<>();
        List<Npc> npcs = new ArrayList<>();
        List<Topic> topics = new ArrayList<>();
        List<Memory> memories = new ArrayList<>();

        for (int i = 0; i < SAMPLE_NPCS.size(); i++) {
            SampleNpc sample = SAMPLE_NPCS.get(i);
            Users user = Users.builder().displayName(sample.userName()).build();
            Npc npc = Npc.builder()
                .user(user)
                .name(sample.npcName())
                .presetId(sample.presetId())
                .level(3)
                .exp(60)
                .appearance(Map.of("body", "body_01", "hair", "hair_0" + (i + 1), "clothes", "casual_01"))
                .profile(sample.profile())
                .speechStyle(sample.speechStyle())
                .bornAt(now)
                .build();
            users.add(user);
            npcs.add(npc);

            for (SampleTopic sampleTopic : sample.topics()) {
                Topic topic = Topic.builder()
                    .npc(npc)
                    .name(sampleTopic.name())
                    .category(sampleTopic.categoryCode() == null
                        ? null
                        : topicCategoryRepository.findByCode(sampleTopic.categoryCode()).orElse(null))
                    .interest((short) sampleTopic.interest())
                    .publicTopic(sampleTopic.publicTopic())
                    .visibilityDecidedAt(now)
                    .learnedAt(now)
                    .build();
                topics.add(topic);
                memories.add(Memory.builder().topic(topic).content(sampleTopic.memory()).occurredAt(now).build());
            }
        }

        usersRepository.saveAll(users);
        npcRepository.saveAll(npcs);
        topicRepository.saveAll(topics);
        memoryRepository.saveAll(memories);

        Event event = Event.builder()
            .creator(users.getFirst())
            .title("KasagiChat 常設デモイベント")
            .description("審査・動作確認用の常設イベントです。サンプルNPCが参加しています。")
            .startsAt(DEMO_STARTS_AT)
            .endsAt(DEMO_ENDS_AT)
            .venueTemplate(VenueTemplate.HALL)
            .inviteCode(DEMO_INVITE_CODE)
            .build();
        eventRepository.save(event);

        eventParticipantRepository.saveAll(users.stream()
            .map(user -> EventParticipant.builder().event(event).user(user).joinedAt(now).build())
            .toList());

        log.info("常設デモイベントが未作成のため、サンプルNPC{}体とイベントを作成しました", SAMPLE_NPCS.size());
    }

    /**
     * サンプルNPCの定義。
     *
     * @param userName サンプルユーザーの表示名
     * @param npcName NPCの名前
     * @param presetId 見た目のプリセットID（仮）
     * @param profile 人格文書
     * @param speechStyle 口調
     * @param topics 話題の一覧
     */
    private record SampleNpc(
        String userName,
        String npcName,
        String presetId,
        String profile,
        String speechStyle,
        List<SampleTopic> topics
    ) {
    }

    /**
     * サンプルの話題と思い出の定義。
     *
     * @param name 話題名
     * @param categoryCode 話題カテゴリのコード。nullの場合は本棚
     * @param interest 興味度
     * @param publicTopic 公開するかどうか
     * @param memory 話題に付ける思い出
     */
    private record SampleTopic(String name, String categoryCode, int interest, boolean publicTopic, String memory) {
    }
}
