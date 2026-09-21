# 家API（実装済み）

`KasagiChat/requirements.md`（2026-09-19時点、3-5・3-6・4章）を正とする。
思い出の品は既存の `topics` と `topic_categories` から導出する。新しい家・所持品テーブルは作らない。
同じカテゴリの話題でも1話題につき1品とし、品物の識別子は `topicId`、入手日時は `learnedAt`。
非公開の話題も本人の家には表示する。話題の削除で配置情報も消える。

`unlocked_items` は現在 `CLOTHES` / `ACCESSORY` のみ。取得APIで返すが、部屋の配置対象ではない。
着せ替えや新しい家具カテゴリ・所持品取得処理は今回追加していない。

## 認証とAPI

既存のセッションCookieと `ROLE_USER` を使用する。変更には既存のCSRFヘッダーが必要。
所有者は `LoginUserPrincipal.userId()` のみから取得し、ユーザーIDやNPC IDを入力として受け付けない。

| Method | Path | 成功レスポンス |
| --- | --- | --- |
| GET | `/api/home` | 200 `HomeResponse` |
| PUT | `/api/home/items/{topicId}/placement` | 200 更新後の `HomeItem` |
| DELETE | `/api/home/items/{topicId}/placement` | 204（収納） |

PUTのJSONは `{ "slotId": "DISPLAY_1" }`。配置・移動共通。`null`・省略・空白は不可。
同じスロットへの再送、収納済みの品物へのDELETEは成功する。
占有済みスロットへの移動は409で拒否し、元の配置を維持する。交換・押し出しは行わない。
品物は初期状態では収納（`slotId: null`）。GETによる自動配置・書き込みはない。

## フロントへ渡す型

```ts
type HomeItemKind = "SOUVENIR" | "BOOK";
type HomeSlotId =
  | "DISPLAY_1" | "DISPLAY_2" | "DISPLAY_3"
  | "DISPLAY_4" | "DISPLAY_5" | "DISPLAY_6"
  | "BOOKSHELF_1" | "BOOKSHELF_2" | "BOOKSHELF_3"
  | "BOOKSHELF_4" | "BOOKSHELF_5" | "BOOKSHELF_6";

interface HomeItem {
  topicId: number;
  topicName: string;
  kind: HomeItemKind;
  category: { code: string; name: string } | null;
  displayName: string;
  imagePath: string | null;
  acquiredAt: string; // UTC ISO 8601
  sourceConversationId: string | null; // 本人の会話の公開UUID
  publicTopic: boolean;
  slotId: HomeSlotId | null; // null = 収納
}

interface HomeResponse {
  npc: {
    name: string;
    presetId: string;
    appearance: Record<string, string> | null;
    level: number;
    exp: number; // 累計EXP
    bornAt: string | null;
    nextLevel: {
      level: number;
      requiredTotalExp: number; // 次レベル到達に必要な累計EXP
      remainingExp: number; // requiredTotalExp - exp
    } | null;
  };
  slots: Array<{ slotId: HomeSlotId; acceptedKind: HomeItemKind }>;
  items: HomeItem[];
  unlockedItems: Array<{
    unlockedItemId: number; // topicIdとは別。配置APIに渡さない
    code: string;
    itemType: "CLOTHES" | "ACCESSORY";
    name: string;
    imagePath: string;
    acquiredAt: string;
  }>;
}
```

- `items` は獲得日時降順、同日時はtopicId降順。`unlockedItems` も解禁日時・ID降順。
- カテゴリあり: `SOUVENIR`。表示名・画像は既存カテゴリマスタから取得する。
- カテゴリなし: `BOOK`。表示名は話題名、画像は `null`。フロントで汎用の本画像を用意する。存在しない画像URLは生成しない。
- カテゴリの `code` は話題の分類、`kind` は配置互換性の分類。クライアントから分類は送らない。
- 品物の起点の会話がない場合、またはデータ不整合で他人の会話を参照する場合、`sourceConversationId` は `null`。会話本文や要約は今回返さない。
- 取得済みの品物はマスタが無効化されても一覧から除外しない。
- 次レベルは現在レベルと直後のレベルのマスタがあり、`0 <= 現在レベル必要EXP <= 現在EXP < 次レベル必要EXP` のときのみ返す。欠落や不整合時は `nextLevel: null`。これは最大レベルであることを保証しない。
- NPC未作成は404。誕生前のNPCも取得可能で `bornAt: null`。

## 配置の正規データと排他制御

初期配置枠は飾り棚6枠（`DISPLAY_1`～`DISPLAY_6`）と本棚6枠（`BOOKSHELF_1`～`BOOKSHELF_6`）。
スロットIDと互換性の正はサーバーの `HomeSlot`。フロントは `slots` を参照し、表示座標だけを管理する。
この12枠は今回の初期API契約であり、requirementsに元から指定されている数ではない。
`DISPLAY_*` はカテゴリ付き話題、`BOOKSHELF_*` はカテゴリなし話題だけを受け付ける。
未知のID、カテゴリ不一致はサーバーで拒否し、SQL CHECKでも制限する。

更新は `@Transactional` 内で本人のNPC行を `PESSIMISTIC_WRITE` ロック後、
そのNPCに属する話題だけを取得する。配置・移動・収納は同一のロック順で直列化する。
同じスロットを別の品物が使用中なら409。別ユーザー同士は同じスロットIDを使用できる。
DBの `UNIQUE (npc_id, home_slot_id)` でも重複を防ぐ。収納のNULLは複数可。

将来の話題カテゴリ更新は、配置が新しいカテゴリと非互換になるなら同じ更新で `home_slot_id` をNULLへ戻すこと。
将来の別経路からの配置更新もNPC行ロックを共有すること。
スロットを変更する場合は `HomeSlot`・JPA CHECK・SQLマイグレーション・画面の座標定義を揃える。

## エラー

業務エラーは既存の `ApiExceptionHandler` により `application/problem+json`（`code`付き）で返す。

| status | code | 意味 |
| --- | --- | --- |
| 404 | `USER_NOT_FOUND` | セッションのユーザーが削除済み等 |
| 404 | `NPC_NOT_FOUND` | 本人のNPCが未作成 |
| 404 | `HOME_ITEM_NOT_FOUND` | 話題が存在しない、または本人のNPCの話題でない |
| 400 | `INVALID_HOME_SLOT` | 未定義のslotId |
| 400 | `HOME_SLOT_INCOMPATIBLE` | スロットと品物の種別が不一致 |
| 409 | `HOME_SLOT_OCCUPIED` | 別の品物が使用中 |
| 400 | `VALIDATION_FAILED` | slotIdの省略・null・空白・30文字超過 |
| 400 | `INVALID_REQUEST_BODY` | 不正JSON |

未認証401、権限不足・CSRF違反403は既存のSpring Securityによる。
数値でないパスIDなどSpring MVC側の型変換エラーには、独自codeを追加していない。

## DB適用

このリポジトリにはFlyway等が未導入で、prodは `ddl-auto: validate`。
追加SQLは **実行していない**。既存DBに対しては更新版起動前・アプリ停止中に
`scripts/migrations/20260921_home_slots.sql` を一度適用する。

追加するもの:

- `topics.home_slot_id varchar(30) NULL`
- `uk_topics_npc_home_slot` 一意制約
- `ck_topics_home_slot` スロットIDとカテゴリ互換性のCHECK

新規のdebug DBではエンティティから同じ制約を含めて生成する。
既存debug DBではHibernate updateにCHECKの追加を任せず、更新版起動前にSQLを適用する。
SQLは再実行用ではない。Hibernateが先に列を追加した環境では、既存スキーマを確認した上で不足分のみ適用する必要がある。
既存行は収納状態になり、データ削除・所持品コピー・自動配置は行わない。

## 検証範囲と未実装

今回実行した検査は変更前後のgit status/diff、`git diff --check`、新規ファイルの末尾空白検査、
enum・JPA CHECK・SQL間の12スロットIDの一致検査。
ホストのPATH上にJava/Javacがないためコンパイル・テスト実行は未実施。
依存取得、devcontainer起動、アプリ起動、ブラウザ/E2E、実DB接続、SQL適用も未実施。

`HomeServiceTest` は取得・EXP・所有権・スロット改ざん・互換性・占有時の原状維持・移動・収納・再送をMockitoで検証するテスト。
`HomeControllerTest` はstandalone MockMvcでJSON契約・入力検証・Problem Details・セッション主体の引き渡しを検証するテスト。
どちらもDBやアプリコンテキストを起動しない。実際のSecurityFilterChain、DBロックと制約、SQL適用の動作確認は含まない。

家の今回の対象外: フロント表示、汎用本画像、プロフィール編集、今日のひとこと、未振り返り一覧、成長通知、
新しい品物獲得処理、着せ替え、自由座標・回転・拡大縮小・家の訪問。
会話の公開UUIDは返すが、このチェックアウトの会話詳細APIは実装予定のままである。

## 変更ファイル

`src/main/java/com/kasagichat/api/npc/` 配下:

- 新規: `controller/HomeController.java`、`controller/dto/request/HomePlacementRequest.java`、`controller/dto/response/HomeResponse.java`
- 新規: `service/HomeService.java`、`exception/HomeException.java`、`model/enums/HomeItemKind.java`、`model/enums/HomeSlot.java`
- 更新: `model/Topic.java`、`repository/NpcRepository.java`、`repository/TopicRepository.java`、`repository/UnlockedItemRepository.java`

その他:

- 新規: `src/test/java/com/kasagichat/api/npc/service/HomeServiceTest.java`
- 新規: `src/test/java/com/kasagichat/api/npc/controller/HomeControllerTest.java`
- 新規: `scripts/migrations/20260921_home_slots.sql`、`docs/home-api.md`（本書）
- 更新: `README.md`
