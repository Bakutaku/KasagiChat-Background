package com.kasagichat.api.security.config;

import java.time.Instant;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.kasagichat.api.security.model.Terms;
import com.kasagichat.api.security.model.enums.TermsType;
import com.kasagichat.api.security.repository.TermsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 開発環境で規約マスタが空の場合に、動作確認用の規約を作成する初期化処理。
 */
@Component
@Profile("debug")
@RequiredArgsConstructor
@Slf4j
public class DevelopmentTermsInitializer implements ApplicationRunner {

    private static final String INITIAL_VERSION = "1.0.0";

    private static final String TERMS_OF_SERVICE_CONTENT = """
        KasagiChat 利用規約

        第1条（適用）
        本規約は、KasagiChat（以下「本サービス」といいます）の開発版の利用条件を定めるものです。
        利用者は、本規約に同意した上で本サービスを利用するものとします。

        第2条（開発版について）
        1. 本サービスは現在開発中であり、機能、仕様、表示内容および提供方法は予告なく変更される場合があります。
        2. 本サービスは動作確認および検証を目的として提供され、継続的または安定的な提供を保証するものではありません。
        3. 開発中の不具合、通信障害その他の事情により、登録情報、投稿内容その他のデータが消失または破損する場合があります。重要な情報は本サービスに保存しないでください。

        第3条（利用登録）
        1. 利用者は、外部認証サービスを利用し、正確な情報を提供して利用登録を行うものとします。
        2. 利用者は、自己のアカウントおよび認証情報を適切に管理し、第三者に利用させてはなりません。
        3. アカウントの不正利用によって生じた損害について、運営者は、運営者に故意または重大な過失がある場合を除き、責任を負いません。

        第4条（禁止事項）
        利用者は、本サービスの利用にあたり、次の行為をしてはなりません。
        1. 法令または公序良俗に違反する行為
        2. 他者になりすます行為、または第三者のアカウントを不正に利用する行為
        3. 他者の権利、プライバシーまたは知的財産権を侵害する行為
        4. 本サービスまたは関連するシステムに過度な負荷を与え、もしくは不正にアクセスする行為
        5. 本サービスの運営または開発を妨害する行為
        6. その他、運営者が不適切と判断する行為

        第5条（利用者が送信する情報）
        1. 利用者は、本サービスへ送信する情報について必要な権利を有し、第三者の権利を侵害しないことを保証するものとします。
        2. 運営者は、開発、保守、不具合調査および本サービスの改善に必要な範囲で、利用者が送信した情報を取り扱うことがあります。

        第6条（提供の変更、中断および終了）
        運営者は、保守、障害対応、仕様変更その他の必要がある場合、利用者への事前通知なく、本サービスの全部または一部を変更、中断または終了できます。

        第7条（保証の否認および免責）
        1. 運営者は、本サービスの正確性、完全性、有用性、安全性、特定目的への適合性、継続性および不具合がないことを保証しません。
        2. 利用者は自己の責任で本サービスを利用するものとし、本サービスの利用または利用不能、データの消失、第三者との紛争その他本サービスに関連して生じた損害について、運営者は一切の責任を負いません。
        3. 前項にかかわらず、適用される法令により運営者の責任を免除できない場合、本条の免責はその範囲では適用されません。

        第8条（規約の変更）
        運営者は、必要に応じて本規約を変更できます。変更後の規約は、本サービス上への掲載その他適切な方法で通知した時点から適用されます。

        第9条（準拠法および管轄）
        本規約は日本法に準拠します。本サービスに関して紛争が生じた場合は、法令に別段の定めがある場合を除き、運営者の所在地を管轄する裁判所を第一審の専属的合意管轄裁判所とします。

        制定日：2026年9月7日
        """;

    private final TermsRepository termsRepository;

    /**
     * 規約が未登録の場合だけ、テスト用の利用規約とプライバシーポリシーを作成する。
     *
     * @param args 起動引数
     */
    @Override
    public void run(ApplicationArguments args) {
        if (termsRepository.count() > 0) {
            return;
        }

        termsRepository.saveAll(List.of(
            testTerms(
                TermsType.TERMS_OF_SERVICE,
                "KasagiChat 利用規約（開発版）",
                TERMS_OF_SERVICE_CONTENT
            ),
            testTerms(
                TermsType.PRIVACY_POLICY,
                "テスト用プライバシーポリシー",
                "これは開発環境での動作確認にのみ使用するテスト用プライバシーポリシーです。"
            )
        ));

        log.info("規約マスタが未登録のため、開発用テスト規約を作成しました");
    }

    private Terms testTerms(TermsType type, String title, String content) {
        Terms terms = new Terms();
        terms.setType(type);
        terms.setVersion(INITIAL_VERSION);
        terms.setTitle(title);
        terms.setContent(content);
        terms.setEffectiveAt(Instant.EPOCH);
        return terms;
    }
}
