package com.kasagichat.api.master.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.kasagichat.api.conversation.model.enums.ConversationScene;
import com.kasagichat.api.conversation.model.enums.ConversationType;
import com.kasagichat.api.master.model.AchievementDef;
import com.kasagichat.api.master.model.ConversationOpening;
import com.kasagichat.api.master.model.CounterDef;
import com.kasagichat.api.master.model.DemoPassphrase;
import com.kasagichat.api.master.model.ExpRule;
import com.kasagichat.api.master.model.Item;
import com.kasagichat.api.master.model.LevelCurve;
import com.kasagichat.api.master.model.TopicCategory;
import com.kasagichat.api.master.model.enums.ItemType;
import com.kasagichat.api.master.model.enums.RewardType;
import com.kasagichat.api.master.repository.AchievementDefRepository;
import com.kasagichat.api.master.repository.ConversationOpeningRepository;
import com.kasagichat.api.master.repository.CounterDefRepository;
import com.kasagichat.api.master.repository.DemoPassphraseRepository;
import com.kasagichat.api.master.repository.ExpRuleRepository;
import com.kasagichat.api.master.repository.ItemRepository;
import com.kasagichat.api.master.repository.LevelCurveRepository;
import com.kasagichat.api.master.repository.TopicCategoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 開発環境でマスタが空の場合に、動作確認用の仮の値を作成する初期化処理。
 *
 * <p>値はゲームバランス調整前の仮のもの。マスタごとに空かどうかを判定し、空のものだけ作成する。</p>
 */
@Component
@Profile("debug")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DevelopmentMasterDataInitializer implements ApplicationRunner {

    /**
     * 開発環境で使うデモ用の合言葉。
     */
    public static final String DEMO_PASSPHRASE = "HOSHI26";

    private static final int DEMO_CALL_LIMIT = 100;

    private final LevelCurveRepository levelCurveRepository;
    private final ExpRuleRepository expRuleRepository;
    private final TopicCategoryRepository topicCategoryRepository;
    private final ConversationOpeningRepository conversationOpeningRepository;
    private final DemoPassphraseRepository demoPassphraseRepository;
    private final CounterDefRepository counterDefRepository;
    private final ItemRepository itemRepository;
    private final AchievementDefRepository achievementDefRepository;

    /**
     * 空のマスタだけ、開発用の仮の値を作成する。
     *
     * @param args 起動引数
     */
    @Override
    public void run(ApplicationArguments args) {
        seedLevelCurves();
        seedExpRules();
        seedTopicCategories();
        seedConversationOpenings();
        seedDemoPassphrases();
        seedAchievements();
    }

    private void seedLevelCurves() {
        if (levelCurveRepository.count() > 0) {
            return;
        }
        // 審査員が数回触るだけで2〜3回レベルアップするよう、序盤を軽くする。
        int[] requiredExps = {0, 20, 50, 100, 170, 260, 370, 500, 650, 820};
        List<LevelCurve> curves = new ArrayList<>();
        for (int i = 0; i < requiredExps.length; i++) {
            curves.add(LevelCurve.builder().level(i + 1).requiredExp(requiredExps[i]).build());
        }
        levelCurveRepository.saveAll(curves);
        log.info("レベル曲線マスタが未登録のため、開発用の値を作成しました");
    }

    private void seedExpRules() {
        if (expRuleRepository.count() > 0) {
            return;
        }
        expRuleRepository.saveAll(List.of(
            expRule("USER_MESSAGE", 1, "自分の発言1回"),
            expRule("BIRTH_COMPLETE", 10, "NPC誕生の振り返り完了"),
            expRule("PRACTICE_COMPLETE", 10, "練習シーンの振り返り完了"),
            expRule("DAILY_COMPLETE", 5, "今日のひとことの振り返り完了")
        ));
        log.info("EXPルールマスタが未登録のため、開発用の値を作成しました");
    }

    private void seedTopicCategories() {
        if (topicCategoryRepository.count() > 0) {
            return;
        }
        String[][] categories = {
            {"GAME", "ゲーム", "ゲーム機"},
            {"MUSIC", "音楽", "ヘッドホン"},
            {"COOKING", "料理", "フライパン"},
            {"SPORTS", "スポーツ", "サッカーボール"},
            {"TRAVEL", "旅行", "スーツケース"},
            {"TECH", "技術・プログラミング", "ノートパソコン"},
            {"MOVIE", "映画・ドラマ", "ポップコーン"},
            {"ANIME", "アニメ・漫画", "フィギュア"},
            {"CAFE", "カフェ・飲み物", "コーヒーミル"},
            {"ANIMAL", "動物", "ぬいぐるみ"},
            {"OUTDOOR", "アウトドア", "テント"},
            {"ART", "アート・ものづくり", "イーゼル"}
        };
        List<TopicCategory> topicCategories = new ArrayList<>();
        for (int i = 0; i < categories.length; i++) {
            String code = categories[i][0];
            topicCategories.add(TopicCategory.builder()
                .code(code)
                .name(categories[i][1])
                .displayName(categories[i][2])
                .itemImagePath("/images/mementos/" + code.toLowerCase() + ".png")
                .sortOrder(i + 1)
                .build());
        }
        topicCategoryRepository.saveAll(topicCategories);
        log.info("話題カテゴリマスタが未登録のため、開発用の値を作成しました");
    }

    private void seedConversationOpenings() {
        if (conversationOpeningRepository.count() > 0) {
            return;
        }
        conversationOpeningRepository.saveAll(List.of(
            opening(ConversationType.BIRTH, null,
                "はじめまして！ぼく、生まれたばかりのあなたの分身です。まずは、あなたのことを教えてくれる？休みの日は何をしてることが多い？",
                "休日の過ごし方から趣味を聞き出す"),
            opening(ConversationType.BIRTH, null,
                "やっと会えたね！ねえ、最近ちょっと嬉しかったことってある？",
                "最近の嬉しい出来事から価値観を聞き出す"),
            opening(ConversationType.BIRTH, null,
                "こんにちは、今日からよろしくね。あなたが夢中になれるものって何？",
                "夢中になれるものから関心を聞き出す"),
            opening(ConversationType.BIRTH, null,
                "わあ、あなたがご主人なんだね！友達からはどんな人って言われる？",
                "周りからの印象から性格を聞き出す"),
            opening(ConversationType.BIRTH, null,
                "はじめまして。好きな食べ物から聞いてもいい？",
                "好きな食べ物から好みや思い出を聞き出す"),

            opening(ConversationType.PRACTICE, ConversationScene.CAFE,
                "いらっしゃいませ。今日は雨ですね。雨の日はどう過ごしますか？",
                "天気の話題から日常の過ごし方へ広げる雑談"),
            opening(ConversationType.PRACTICE, ConversationScene.CAFE,
                "いらっしゃいませ！最近この近くに新しいお店ができたんですよ。新しいお店にはよく行かれます？",
                "新しいお店の話題から好きな場所についての雑談"),
            opening(ConversationType.PRACTICE, ConversationScene.CAFE,
                "こんにちは。今日のおすすめは季節のブレンドです。コーヒーはよく飲まれますか？",
                "飲み物の好みから休憩の過ごし方についての雑談"),
            opening(ConversationType.PRACTICE, ConversationScene.CAFE,
                "いらっしゃいませ。お疲れさまです、今日はお仕事帰りですか？",
                "一日の出来事をねぎらいながら聞く雑談"),
            opening(ConversationType.PRACTICE, ConversationScene.CAFE,
                "こんにちは！週末は何か予定があるんですか？",
                "週末の予定から趣味についての雑談"),

            opening(ConversationType.PRACTICE, ConversationScene.LOBBY,
                "はじめまして。このイベント、初めて来られたんですか？",
                "イベント参加のきっかけから自己紹介を引き出す"),
            opening(ConversationType.PRACTICE, ConversationScene.LOBBY,
                "こんにちは、隣いいですか？どちらから来られたんですか？",
                "出身地や住んでいる場所から話を広げる"),
            opening(ConversationType.PRACTICE, ConversationScene.LOBBY,
                "はじめまして。お仕事は何をされているんですか？",
                "仕事や学業の話から共通点を探す"),
            opening(ConversationType.PRACTICE, ConversationScene.LOBBY,
                "こんにちは。今日のセッションで気になるものはありました？",
                "興味のあるテーマから相手の関心を知る"),
            opening(ConversationType.PRACTICE, ConversationScene.LOBBY,
                "はじめまして！人が多くて緊張しますね。こういう場所にはよく来られます？",
                "緊張を共有しながら打ち解ける"),

            opening(ConversationType.PRACTICE, ConversationScene.OFFICE,
                "本日はお越しいただきありがとうございます。まずは簡単に自己紹介をお願いできますか？",
                "自己紹介から経歴と強みを深掘りする面接"),
            opening(ConversationType.PRACTICE, ConversationScene.OFFICE,
                "よろしくお願いします。当社に興味を持ったきっかけを教えてください。",
                "志望動機を具体的に聞く面接"),
            opening(ConversationType.PRACTICE, ConversationScene.OFFICE,
                "こんにちは。これまでで一番力を入れて取り組んだことは何ですか？",
                "力を入れて取り組んだことを深掘りする面接"),
            opening(ConversationType.PRACTICE, ConversationScene.OFFICE,
                "よろしくお願いします。周りの人からはどんな人だと言われることが多いですか？",
                "自己分析と周囲からの評価を聞く面接"),
            opening(ConversationType.PRACTICE, ConversationScene.OFFICE,
                "本日はありがとうございます。最近気になったニュースや出来事はありますか？",
                "関心のある出来事から考え方を聞く面接")
        ));
        log.info("会話の冒頭マスタが未登録のため、開発用の値を作成しました");
    }

    private void seedDemoPassphrases() {
        if (demoPassphraseRepository.count() > 0) {
            return;
        }
        demoPassphraseRepository.save(DemoPassphrase.builder()
            .passphrase(DEMO_PASSPHRASE)
            .callLimit(DEMO_CALL_LIMIT)
            .build());
        log.info("デモ用合言葉マスタが未登録のため、開発用の合言葉を作成しました");
    }

    private void seedAchievements() {
        // 実績定義はカウンター種別とアイテムを参照するため、3つとも空の場合だけまとめて作成する。
        if (counterDefRepository.count() > 0
                || itemRepository.count() > 0
                || achievementDefRepository.count() > 0) {
            return;
        }

        List<CounterDef> counters = List.of(
            counter("CONVERSATION_TOTAL", "総会話数"),
            counter("PRACTICE_CAFE", "カフェでの練習回数"),
            counter("PRACTICE_LOBBY", "ロビーでの練習回数"),
            counter("PRACTICE_OFFICE", "オフィスでの練習回数"),
            counter("DAILY_TOTAL", "今日のひとことの回数"),
            counter("EVENT_JOINED", "イベント参加回数")
        );
        counterDefRepository.saveAll(counters);

        List<Item> items = List.of(
            item("CASUAL_SHIRT", ItemType.CLOTHES, "カジュアルシャツ"),
            item("SUIT", ItemType.CLOTHES, "スーツ"),
            item("GAMER_HOODIE", ItemType.CLOTHES, "ゲーマーパーカー"),
            item("BERET", ItemType.ACCESSORY, "ベレー帽")
        );
        itemRepository.saveAll(items);

        Map<String, CounterDef> counterByCode = counters.stream()
            .collect(Collectors.toMap(CounterDef::getCode, Function.identity()));
        Map<String, Item> itemByCode = items.stream()
            .collect(Collectors.toMap(Item::getCode, Function.identity()));

        achievementDefRepository.saveAll(List.of(
            AchievementDef.builder()
                .code("FIRST_CAFE").name("はじめてのカフェ").description("カフェで1回練習する")
                .counterDef(counterByCode.get("PRACTICE_CAFE")).threshold(1L)
                .rewardType(RewardType.ITEM).rewardItem(itemByCode.get("CASUAL_SHIRT"))
                .build(),
            AchievementDef.builder()
                .code("OFFICE_3").name("面接に慣れてきた").description("オフィスで3回練習する")
                .counterDef(counterByCode.get("PRACTICE_OFFICE")).threshold(3L)
                .rewardType(RewardType.ITEM).rewardItem(itemByCode.get("SUIT"))
                .build(),
            AchievementDef.builder()
                .code("CONVERSATION_10").name("おしゃべり好き").description("合計10回会話する")
                .counterDef(counterByCode.get("CONVERSATION_TOTAL")).threshold(10L)
                .rewardType(RewardType.EXP).rewardExp(20)
                .build(),
            AchievementDef.builder()
                .code("FIRST_EVENT").name("はじめてのイベント").description("イベントに1回参加する")
                .counterDef(counterByCode.get("EVENT_JOINED")).threshold(1L)
                .rewardType(RewardType.NONE)
                .build()
        ));
        log.info("実績関連のマスタが未登録のため、開発用の値を作成しました");
    }

    private ExpRule expRule(String code, int exp, String description) {
        return ExpRule.builder().code(code).exp(exp).description(description).build();
    }

    private ConversationOpening opening(ConversationType type, ConversationScene scene, String line, String theme) {
        return ConversationOpening.builder()
            .conversationType(type)
            .scene(scene)
            .line(line)
            .theme(theme)
            .build();
    }

    private CounterDef counter(String code, String name) {
        return CounterDef.builder().code(code).name(name).build();
    }

    private Item item(String code, ItemType itemType, String name) {
        return Item.builder()
            .code(code)
            .itemType(itemType)
            .name(name)
            .imagePath("/images/items/" + code.toLowerCase() + ".png")
            .build();
    }
}
