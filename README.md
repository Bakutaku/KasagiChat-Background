# KasagiChat API

KasagiChat のバックエンドAPIです。Google OAuth 2.0によるログイン、未登録ユーザーの仮登録、利用規約への同意を伴う本登録を提供します。

## 技術構成

- Java 25
- Spring Boot 4.1.0
- Spring MVC / Spring Security / OAuth 2.0 Client
- Spring Data JPA
- Spring Session JDBC
- PostgreSQL
- Gradle

## 起動方法

### 必要なもの

- Java 25
- PostgreSQL
- Google OAuth 2.0のクライアントIDとクライアントシークレット

Dev Containerを使う場合は、同梱のCompose設定によってJava開発環境とPostgreSQLが起動します。

### 環境変数

| 変数 | 必須 | デフォルト値 | 説明 |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | いいえ | `prod` | 実行環境。ローカル開発時は `debug` を明示 |
| `GOOGLE_CLIENT_ID` | はい | なし | Google OAuthクライアントID |
| `GOOGLE_CLIENT_SECRET` | はい | なし | Google OAuthクライアントシークレット |
| `POSTGRESQL_HOSTNAME` | `prod`では必須 | `localhost` | PostgreSQLのホスト名 |
| `POSTGRESQL_PORT` | `prod`では必須 | `5432` | PostgreSQLのポート |
| `POSTGRES_DB` | `prod`では必須 | `postgres` | データベース名 |
| `POSTGRES_USER` | `prod`では必須 | `postgres` | データベースユーザー |
| `POSTGRES_PASSWORD` | `prod`では必須 | `postgres` | データベースパスワード |
| `SERVER_PORT` | いいえ | `8080` | APIサーバーのポート |
| `APP_SECURITY_PUBLIC_URL` | `prod`では必須 | `http://localhost:3000` | OAuth完了後のフロントエンド遷移先 |
| `PENDING_REGISTRATION_TTL` | いいえ | `30m` | 仮登録の有効期間（SpringのDuration形式） |
| `USAGE_ESTIMATED_COST_PER_CALL_USD` | いいえ | `0.002` | 利用状況の表示に使う、LLM呼び出し1回あたりの概算コスト（USD） |

### 実行環境の切り替え

Spring Bootのプロファイルを `SPRING_PROFILES_ACTIVE` で切り替えます。指定漏れで開発用設定が本番適用されないよう、未指定の場合は `prod` が適用されます。ローカル開発時は `debug` を明示してください。

| 設定 | `debug` | `prod` |
| --- | --- | --- |
| Hibernate DDL | `update` | `validate` |
| SQL表示 | 有効 | 無効 |
| Spring Sessionスキーマ初期化 | `always` | `never` |
| セッションCookieのSecure属性 | 無効 | 有効 |
| ヘルスチェック詳細 | 表示 | 非表示 |
| DB接続値・フロントエンドURL | ローカル向けデフォルトあり | 環境変数が必須 |

デバッグ用:

```bash
SPRING_PROFILES_ACTIVE=debug ./gradlew bootRun
```

本番用:

```bash
export SPRING_PROFILES_ACTIVE=prod
export POSTGRESQL_HOSTNAME="db.example.com"
export POSTGRES_PORT="5432"
export POSTGRES_DB="kasagichat"
export POSTGRES_USER="kasagichat"
export POSTGRES_PASSWORD="change-me"
export APP_SECURITY_PUBLIC_URL="https://example.com"
export GOOGLE_CLIENT_ID="your-client-id"
export GOOGLE_CLIENT_SECRET="your-client-secret"
./gradlew bootRun
```

`prod` では起動前に、JPAエンティティに対応するテーブルと `SPRING_SESSION` テーブルを構築しておく必要があります。マイグレーションツール（Flyway）の導入は今後の課題です。

Google OAuth側には、次のリダイレクトURIを登録してください。

```text
http://localhost:8080/login/oauth2/code/google
```

環境変数を設定して起動します。

```bash
export GOOGLE_CLIENT_ID="your-client-id"
export GOOGLE_CLIENT_SECRET="your-client-secret"
./gradlew bootRun
```

テストは次のコマンドで実行できます。

```bash
./gradlew test
```

`debug` では起動時にHibernateがテーブルを作成・更新し、Spring Sessionのテーブルも初期化します。また、次のデータが空の場合は開発用の値を自動で作成します。

- 規約（利用規約・プライバシーポリシー）
- マスタ（レベル曲線、EXPルール、話題カテゴリ、会話の冒頭、カウンター種別、アイテム、実績定義）の仮の値
- デモ用の合言葉 `HOSHI26`
- サンプルNPC3体が参加する常設デモイベント（招待コード `DEMO2026`）

本登録には、現在有効な規約データがデータベースに1件以上必要です。規約データが存在しない場合、本登録は `TERMS_AGREEMENT_REQUIRED` で拒否されます。

## API共通仕様

- ベースURL（ローカル）: `http://localhost:8080`
- 認証方式: サーバーサイドセッション
- セッションCookie: `HttpOnly`、`SameSite=Lax`（`Secure` は `prod` のみ有効）
- JSONを送信する場合のContent-Type: `application/json`
- 日時: ISO 8601形式のUTC日時（例: `2026-09-05T12:34:56Z`）

ブラウザーはセッションCookieを送信する必要があります。変更系リクエストでは、先にCSRFトークンを取得し、レスポンスの `headerName` で示されたヘッダーへ `token` の値を設定してください。

現在、明示的なCORS設定はありません。別オリジンのフロントエンドから直接呼び出す場合は、同一オリジンになるリバースプロキシなどを利用するか、別途CORS設定が必要です。`prod` のセッションCookieは `Secure` のため、HTTPSを使用してください。

## 認証状態

| 状態 | Authority | 利用できるAPI |
| --- | --- | --- |
| 未ログイン | なし | CSRF取得、必須規約取得、OAuthログイン、ヘルスチェック |
| 仮登録 | `ROLE_PENDING_REGISTRATION` | `/api/registrations/**` |
| 登録済み | `ROLE_USER` | `/api/**` |

認証が必要なAPIへ未ログインでアクセスすると `401 Unauthorized`、権限が異なる場合は `403 Forbidden` を返します。

## エンドポイント一覧

| Method | Path | 認証 | 概要 |
| --- | --- | --- | --- |
| `GET` | `/oauth2/authorization/google` | 不要 | Google OAuthログインを開始 |
| `GET` | `/login/oauth2/code/google` | 不要 | Google OAuthコールバック（Spring Securityが処理） |
| `GET` | `/api/auth/csrf` | 不要 | CSRF Cookieを初期化 |
| `GET` | `/api/terms/required` | 不要 | 現在有効な最新規約を取得 |
| `GET` | `/api/registrations/me` | 仮登録 | 現在の仮登録ユーザー情報を取得 |
| `GET` | `/api/user/me` | 登録済み | 現在ログイン中のユーザー情報と初回フローの進み具合を取得 |
| `POST` | `/api/npc` | 登録済み | 未誕生状態のNPCを作成 |
| `GET` | `/api/npc` | 登録済み | NPCのプロフィール帳を取得 |
| `PATCH` | `/api/npc` | 登録済み | 人格文書・口調・口調反映の設定を更新 |
| `GET` | `/api/npc/topics` | 登録済み | NPCが覚えた話題の一覧を取得 |
| `PATCH` | `/api/npc/topics/{id}` | 登録済み | 話題の公開/非公開を切り替え |
| `DELETE` | `/api/npc/topics/{id}` | 登録済み | 話題を削除 |
| `GET` | `/api/growth-events` | 登録済み | 成長演出・実績達成の通知を取得 |
| `POST` | `/api/growth-events/read` | 登録済み | 未読の通知を一括既読 |
| `GET` | `/api/achievements` | 登録済み | 実績の一覧を取得 |
| `POST` | `/api/achievements/{id}/claim` | 登録済み | 実績の報酬を受け取る |
| `GET` | `/api/usage/summary` | 登録済み | LLMの利用状況とコストの概算を取得 |
| `POST` | `/api/registrations/complete` | 仮登録 | 規約へ同意して本登録を完了 |
| `POST` | `/api/logout` | セッション | ログアウト |
| `GET` | `/actuator/health` | 不要 | ヘルスチェック |

`AuthProvider` はGoogleとGitHubを定義していますが、現在の `application.yml` でOAuthクライアントとして設定されているのはGoogleのみです。

## OAuthログインフロー

1. ブラウザーで `GET /oauth2/authorization/google` を開きます。
2. Googleでの認証後、Googleから `/login/oauth2/code/google` へ戻ります。
3. OAuthアカウントが登録済みの場合、`ROLE_USER` のセッションを作成し、`${APP_SECURITY_PUBLIC_URL}/` へリダイレクトします。
4. 未登録の場合、30分（デフォルト）有効な仮登録情報と `ROLE_PENDING_REGISTRATION` のセッションを作成し、`${APP_SECURITY_PUBLIC_URL}/signup` へリダイレクトします。
5. OAuth認証に失敗した場合、`${APP_SECURITY_PUBLIC_URL}/auth/error` へリダイレクトします。

同じ未登録OAuthアカウントで再ログインした場合、仮登録レコードを増やさず、表示名候補、アバターURL、有効期限を更新します。

## エンドポイント詳細

### CSRF Cookieの初期化

```http
GET /api/auth/csrf
```

認証は不要です。`204 No Content` とともに、JavaScriptから読み取り可能な `XSRF-TOKEN` Cookieを設定します。

`POST /api/registrations/complete` や `POST /api/logout` では、同じセッションCookieを送信した上で、`XSRF-TOKEN` Cookieの値を `X-XSRF-TOKEN` ヘッダーへ設定します。

```http
X-XSRF-TOKEN: <XSRF-TOKEN Cookieの値>
```

### 必須規約取得

```http
GET /api/terms/required
```

認証は不要です。現在時刻以前に発効した規約のうち、規約種別ごとに最新のものを返します。同じ発効日時の場合は、IDが大きい規約が優先されます。

レスポンス例:

```json
[
  {
    "id": 1,
    "type": "TERMS_OF_SERVICE",
    "version": "1.0",
    "title": "利用規約",
    "content": "規約本文",
    "effectiveAt": "2026-09-01T00:00:00Z"
  },
  {
    "id": 2,
    "type": "PRIVACY_POLICY",
    "version": "1.0",
    "title": "プライバシーポリシー",
    "content": "ポリシー本文",
    "effectiveAt": "2026-09-01T00:00:00Z"
  }
]
```

規約種別:

- `TERMS_OF_SERVICE`: 利用規約
- `PRIVACY_POLICY`: プライバシーポリシー

### 仮登録ユーザー取得

```http
GET /api/registrations/me
```

`ROLE_PENDING_REGISTRATION` が必要です。

レスポンス例:

```json
{
  "displayName": "Kasagi User",
  "avatarUrl": "https://example.com/avatar.png"
}
```

OAuthから表示名を取得できない場合は `New User`、51文字以上の場合は先頭50文字が仮登録時の候補になります。`avatarUrl` は `null` の場合があります。

### 現在のユーザー取得

```http
GET /api/user/me
```

`ROLE_USER` が必要です。サーバーセッションに保存された認証主体からユーザーを特定するため、リクエストパラメーターは不要です。

レスポンス例:

```json
{
  "publicId": "d2719db8-5c4d-42e7-ae24-9a94d8d06b12",
  "displayName": "Kasagi User",
  "avatarUrl": "https://example.com/avatar.png",
  "onboarding": {
    "credentialConfigured": false,
    "npcCreated": true,
    "npcBorn": false
  }
}
```

`onboarding` は、フロントエンドが初回フロー（APIキー設定 → NPC誕生）の遷移先を決めるために使います。

- `credentialConfigured`: APIキーまたはデモの設定が済んでいるか（設定APIは未実装のため、現在は常に `false`）
- `npcCreated`: NPCを作成済みか
- `npcBorn`: NPCが誕生済みか（誕生会話の振り返りが成功したか）

セッションが参照するユーザーが削除済みまたは存在しない場合は、`USER_NOT_FOUND`（`404 Not Found`）を返します。

### 本登録完了

```http
POST /api/registrations/complete
Content-Type: application/json
X-XSRF-TOKEN: <XSRF-TOKEN Cookieの値>
```

`ROLE_PENDING_REGISTRATION` とCSRFトークンが必要です。

リクエスト例:

```json
{
  "displayName": "Kasagi User",
  "agreedTermsIds": [1, 2]
}
```

入力制約:

| フィールド | 型 | 必須 | 制約 |
| --- | --- | --- | --- |
| `displayName` | string | はい | 空文字・空白のみは不可、最大50文字 |
| `agreedTermsIds` | number[] | はい | 現在有効な各規約種別の最新IDをすべて含むこと |

レスポンス例:

```json
{
  "publicId": "d2719db8-5c4d-42e7-ae24-9a94d8d06b12",
  "displayName": "Kasagi User",
  "avatarUrl": "https://example.com/avatar.png"
}
```

登録成功時はセッション固定攻撃を防ぐためセッションIDを変更し、同じセッションを `ROLE_USER` のログイン状態へ更新します。また、最新規約への同意履歴を保存し、仮登録情報を削除します。

### ログアウト

```http
POST /api/logout
X-XSRF-TOKEN: <XSRF-TOKEN Cookieの値>
```

成功時は `200 OK` を返し、HTTPセッションを無効化して `SESSION` と `JSESSIONID` Cookieを削除します。

### NPCの作成

```http
POST /api/npc
Content-Type: application/json
X-XSRF-TOKEN: <XSRF-TOKEN Cookieの値>
```

`ROLE_USER` とCSRFトークンが必要です。未誕生状態（`bornAt` が `null`）のNPCを作成し、`201 Created` を返します。誕生会話の振り返りが成功すると誕生済みになります。NPCは1ユーザーにつき1体で、作成済みの場合は `NPC_ALREADY_EXISTS` を返します。

リクエスト例:

```json
{ "presetId": "PRESET_01", "name": "ユウ" }
```

| フィールド | 型 | 必須 | 制約 |
| --- | --- | --- | --- |
| `presetId` | string | はい | `PRESET_01`〜`PRESET_04`（仮の値。フロントエンドのプリセットIDに合わせて差し替える） |
| `name` | string | はい | 空文字・空白のみは不可、最大30文字。前後の空白は除去 |

NPCの見た目は固定プリセットです。プリセット一覧APIは設けず、フロントエンドとサーバーで同じプリセットIDを定義します（サーバーはenumで検証）。

レスポンスは「NPCの取得」と同じ形式です。

### NPCの取得

```http
GET /api/npc
```

レスポンス例:

```json
{
  "name": "ユウ",
  "presetId": "PRESET_01",
  "level": 2,
  "exp": 35,
  "nextLevelExp": 50,
  "expToNextLevel": 15,
  "profile": "好奇心旺盛で、新しいものを見つけると誰かに話したくなるタイプ。",
  "speechStyle": "語尾が柔らかく「〜だよね」をよく使う。",
  "speechStyleEnabled": true,
  "bornAt": "2026-09-13T12:00:00Z",
  "stats": {
    "topicCount": 3,
    "counters": [
      { "code": "PRACTICE_CAFE", "name": "カフェでの練習回数", "value": 2 }
    ]
  }
}
```

- `nextLevelExp` は次のレベルに必要な累計EXP、`expToNextLevel` は残りのEXPです。最大レベルの場合はどちらも `null` です。
- `stats.counters` は実績判定用の集計カウンターです。まだ集計していない種別は含まれません。
- NPCが未作成の場合は `NPC_NOT_FOUND` を返します（`PATCH /api/npc` と `GET /api/npc/topics` も同様）。

### NPCの更新

```http
PATCH /api/npc
Content-Type: application/json
X-XSRF-TOKEN: <XSRF-TOKEN Cookieの値>
```

リクエスト例:

```json
{ "speechStyle": "丁寧語が基本で、やさしく相づちを打つ。", "speechStyleEnabled": false }
```

| フィールド | 型 | 必須 | 制約 |
| --- | --- | --- | --- |
| `profile` | string | いいえ | 最大2000文字 |
| `speechStyle` | string | いいえ | 最大500文字 |
| `speechStyleEnabled` | boolean | いいえ | なし |

指定しない（`null` の）項目は変更しません。検証するのは文字数の上限だけです。口調は発言の引用ではなく、振り返り時にLLMが更新する文章（`speechStyle`）として持ちます。

レスポンスは「NPCの取得」と同じ形式です。

### 話題一覧

```http
GET /api/npc/topics
```

覚えた日時の新しい順に返します。カテゴリ付きの話題から、家に置く思い出の品を導出します。

レスポンス例:

```json
[
  {
    "id": 12,
    "name": "ELDEN RING",
    "category": {
      "code": "GAME",
      "name": "ゲーム",
      "displayName": "ゲーム機",
      "itemImagePath": "/images/mementos/game.png"
    },
    "interest": 5,
    "public": false,
    "visibilityDecidedAt": null,
    "learnedAt": "2026-09-13T12:00:00Z"
  }
]
```

- `category` は、どのカテゴリにも入らない場合 `null` です（本棚に置く）。
- `visibilityDecidedAt` が `null` の話題は未確認で、非公開として扱います。

### 話題の公開/非公開の切り替え

```http
PATCH /api/npc/topics/{id}
Content-Type: application/json
X-XSRF-TOKEN: <XSRF-TOKEN Cookieの値>
```

リクエスト例:

```json
{ "public": true }
```

`public`（boolean）は必須です。公開した話題だけが、イベントのマッチングとカード生成に使われます。更新後の話題を「話題一覧」の要素と同じ形式で返します。存在しない話題や他人の話題の場合は `TOPIC_NOT_FOUND` を返します。

### 話題の削除

```http
DELETE /api/npc/topics/{id}
X-XSRF-TOKEN: <XSRF-TOKEN Cookieの値>
```

成功時は `204 No Content` を返します。プライバシーのため物理削除とし、話題に属する思い出も一緒に削除します。対応する思い出の品も表示されなくなります。存在しない話題や他人の話題の場合は `TOPIC_NOT_FOUND` を返します。

### 成長通知の取得

```http
GET /api/growth-events?unread=true
```

| パラメーター | 説明 |
| --- | --- |
| `unread=true` | 未読の通知を古い順にすべて返す |
| `unread=false` または未指定 | 既読・未読を問わず、直近50件を新しい順に返す |

レスポンス例:

```json
[
  {
    "id": 7,
    "type": "LEVEL_UP",
    "message": "ユウがレベル2になりました！",
    "createdAt": "2026-09-13T12:00:00Z",
    "readAt": null
  }
]
```

`type` は `LEVEL_UP`（レベルアップ）、`TOPIC_LEARNED`（新しい話題を覚えた）、`ACHIEVEMENT`（実績を達成した）のいずれかです。

### 成長通知の一括既読

```http
POST /api/growth-events/read
X-XSRF-TOKEN: <XSRF-TOKEN Cookieの値>
```

自分の未読の通知をすべて既読にし、`204 No Content` を返します。

### 実績の取得

```http
GET /api/achievements?claimed=false
```

| パラメーター | 説明 |
| --- | --- |
| `claimed=false` | 報酬未受取の実績（カササギが届ける未開封の箱） |
| `claimed=true` | 報酬受取済みの実績 |
| 未指定 | すべての実績 |

達成日時の新しい順に返します。

レスポンス例:

```json
[
  {
    "id": 3,
    "code": "FIRST_CAFE",
    "name": "はじめてのカフェ",
    "description": "カフェで1回練習する",
    "achievedAt": "2026-09-13T12:00:00Z",
    "claimedAt": null,
    "reward": {
      "type": "ITEM",
      "exp": null,
      "item": {
        "code": "CASUAL_SHIRT",
        "name": "カジュアルシャツ",
        "itemType": "CLOTHES",
        "imagePath": "/images/items/casual_shirt.png"
      }
    }
  }
]
```

`reward.type` は `ITEM`（アイテムを解禁）、`EXP`（EXPを加算）、`NONE`（報酬なし）のいずれかです。

### 実績の報酬受取

```http
POST /api/achievements/{id}/claim
X-XSRF-TOKEN: <XSRF-TOKEN Cookieの値>
```

レスポンス例:

```json
{
  "achievement": { "id": 3, "code": "CONVERSATION_10", "claimedAt": "2026-09-13T12:05:00Z", "...": "実績の取得と同じ形式" },
  "level": 3,
  "leveledUp": true
}
```

- `ITEM` は `unlocked_items` へ追加します（解禁済みの場合は何もしません）。`EXP` はNPCに加算し、レベルアップした場合は `LEVEL_UP` の通知を作成します。`NONE` は受取日時だけを記録します。
- 受取済みの場合は報酬を反映せず、現在の状態を `200 OK` で返します。同時に呼ばれても報酬は1回だけ反映されます。
- `level` は受取後のNPCのレベルです。NPCが未作成の場合は `null` です。
- 存在しない実績や他人の実績の場合は `ACHIEVEMENT_NOT_FOUND`、EXP報酬の受取時にNPCが未作成の場合は `NPC_NOT_FOUND` を返します。

### 利用状況の概算

```http
GET /api/usage/summary
```

レスポンス例:

```json
{
  "provider": "OPENAI",
  "estimatedLlmCalls": 42,
  "estimatedCostUsd": 0.084,
  "demo": null
}
```

- 利用ログは保存していないため、LLMの呼び出し回数は、自分の発言数・振り返り済みの会話数・開封済みのカード数の合計から概算します。
- `estimatedCostUsd` は、呼び出し回数 × `USAGE_ESTIMATED_COST_PER_CALL_USD`（デフォルト `0.002`）です。デモ利用の場合は運営が負担するため `0` です。
- `provider` は、APIキー未設定の場合 `null` です。`DEMO` の場合は `demo` に `{ "callCount": 30, "callLimit": 100, "remaining": 70 }` の形で利用回数を返します。

### ヘルスチェック

```http
GET /actuator/health
```

認証は不要です。Spring Boot Actuatorのヘルス情報を返します。未認証時には詳細情報を表示しません。

## エラーレスポンス

アプリケーション固有エラーはRFC 9457のProblem Details形式（`application/problem+json`）で返します。

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "最新の必須規約への同意が必要です",
  "instance": "/api/registrations/complete",
  "code": "TERMS_AGREEMENT_REQUIRED"
}
```

| HTTP status | code | 発生条件 |
| --- | --- | --- |
| `400 Bad Request` | `TERMS_AGREEMENT_REQUIRED` | 有効な規約がない、または最新規約への同意が不足している |
| `401 Unauthorized` | `PENDING_REGISTRATION_NOT_FOUND` | セッションが参照する仮登録情報が存在しない |
| `401 Unauthorized` | `PENDING_REGISTRATION_EXPIRED` | 仮登録の有効期限が切れている |
| `409 Conflict` | `USER_ALREADY_REGISTERED` | 同じOAuthアカウントがすでに登録済み |
| `404 Not Found` | `USER_NOT_FOUND` | セッションが参照する登録済みユーザーが存在しない |
| `404 Not Found` | `NPC_NOT_FOUND` | NPCがまだ作成されていない |
| `404 Not Found` | `TOPIC_NOT_FOUND` | 話題が存在しない、または他人の話題 |
| `404 Not Found` | `ACHIEVEMENT_NOT_FOUND` | 実績が存在しない、または他人の実績 |
| `409 Conflict` | `NPC_ALREADY_EXISTS` | NPCを作成済みのユーザーが、もう1体作成しようとした |

入力バリデーション違反、CSRFエラー、未認証、権限不足など、Spring SecurityまたはSpring MVCが直接返すエラーは上記のアプリケーション固有形式とは異なる場合があります。

## 実装上の補足

- `/api/terms/required` を除く `/api/**` は、原則として登録済みユーザー権限が必要です。仮登録ユーザーには `/api/registrations/**` だけが許可されます。
- 規約は `effectiveAt` が現在時刻以前のものだけが有効です。本登録時には規約種別ごとの最新規約すべてへの同意が必要です。
- `debug` ではDDL更新とSQLログを有効にし、`prod` ではスキーマ検証のみを行います。本番スキーマはマイグレーションなどで管理してください。

## 実装予定のAPI（設計）

KasagiChat本体リポジトリの `requirements.md`（2026-09-13改訂）に基づく設計です。未実装のため、実装時に詳細を確定し、上記の「エンドポイント一覧」「エンドポイント詳細」へ移します。

### 共通方針

- 特記がないものは `ROLE_USER` が必要です。変更系リクエストにはCSRFヘッダーが必要です。
- 認可: 自分のデータのみ操作できます。イベントは参加者のみ閲覧でき、カードは受取人のみ開封できます。
- ID: 会話・イベント・カードはUUIDをパスに使用し、連番を公開しません。
- エラー: 既存と同じProblem Details形式に `code` を付けて返します。
- LLMを呼び出すのは、メッセージ送信・振り返り・カード開封・APIキー検証の4つのみです。いずれも明示的なユーザー操作で呼ばれ、応答は一括で返します（SSEなし）。タイムアウトは60秒で、自動リトライは行いません（振り返りのJSONパース失敗のみ1回だけ再試行）。
- 優先度: M=Must、S=Should、C=Could

### 設定（APIキー・デモキー）

| 優先度 | Method | Path | 概要 |
| --- | --- | --- | --- |
| M | `GET` | `/api/credentials` | 現在の設定。プロバイダ、マスク済みキー、DEMOの場合はデモ残り回数 |
| M | `PUT` | `/api/credentials` | 登録・変更。`provider` によって入力を切り替える |
| M | `DELETE` | `/api/credentials` | 削除 |

`PUT /api/credentials` のリクエスト例:

```json
{ "provider": "OPENAI", "apiKey": "sk-..." }
```

```json
{ "provider": "DEMO", "passphrase": "HOSHI26" }
```

- `OPENAI` / `ANTHROPIC`: 保存前にモデル一覧取得などで有効性を1回検証します。無効な場合は `INVALID_API_KEY` を返します。キーは暗号化して保存します。
- `DEMO`: 合言葉を `demo_passphrases` と照合します。一致しない場合は `INVALID_PASSPHRASE` を返します。呼び出し回数はアカウント単位で数えます。
- OpenAI互換エンドポイント（base URL指定）は運営設定のみとし、APIからは指定できません（SSRF対策）。

### 会話（NPC誕生・練習・今日のひとこと）

会話は次の順に呼び出します。

```text
1. POST /api/conversations                会話を開始（LLMは呼ばない）
2. POST /api/conversations/{id}/messages  発言するたびに呼ぶ（LLMを1回呼ぶ）
3. POST /api/conversations/{id}/review    「会話を終える」で呼ぶ（振り返り。LLMを1回呼ぶ）
```

| 優先度 | Method | Path | 概要 |
| --- | --- | --- | --- |
| M | `GET` | `/api/daily-question` | 今日のひとことの質問。ない場合は `204 No Content` |
| M | `POST` | `/api/conversations` | 会話を開始、または途中の会話を再開 |
| M | `GET` | `/api/conversations?status=UNREVIEWED` | 未振り返りの会話一覧（家に表示） |
| M | `GET` | `/api/conversations/{id}` | メッセージ、状態、往復数、`canFinish` |
| M | `POST` | `/api/conversations/{id}/messages` | 発言を送り、NPCの応答を受け取る |
| M | `POST` | `/api/conversations/{id}/review` | 会話を終えて振り返りを実行（リトライも同じ） |
| C | `GET` | `/api/conversations` | 会話履歴 |

#### 会話の開始

リクエスト例:

```json
{ "type": "PRACTICE", "scene": "CAFE" }
```

- `type`: `BIRTH`（NPC誕生）/ `PRACTICE`（練習）/ `DAILY`（今日のひとこと）
- `scene`: `PRACTICE` のときのみ指定（`CAFE` / `LOBBY` / `OFFICE`）

レスポンス例:

```json
{
  "id": "0b8f6c1e-2d4a-4f7b-9c3e-5a1d2e3f4a5b",
  "type": "PRACTICE",
  "scene": "CAFE",
  "status": "IN_PROGRESS",
  "turn": 0,
  "canFinish": false,
  "messages": [
    { "role": "ASSISTANT", "text": "今日は雨ですね。雨の日はどう過ごしますか？" }
  ]
}
```

- 同じ種別・シーンに未振り返りの会話がある場合は、その会話を `200 OK` で返します（再開）。ない場合は新規作成して `201 Created` を返します。
- 冒頭の台詞はLLMを使わずに用意し、1件目の `ASSISTANT` メッセージとして保存します。
  - `BIRTH` / `PRACTICE`: マスタ `conversation_openings` から抽選します。同じユーザーの直前の会話と同じ行は避けます。行の `theme` はシステムプロンプトへ差し込みます。
  - `DAILY`: `daily_questions` の未消化の質問を使います。
- NPCが誕生していない状態で `PRACTICE` / `DAILY` を開始した場合、または誕生済みで `BIRTH` を開始した場合は `NPC_STATE_INVALID` を返します。

#### メッセージ送信

リクエスト例:

```json
{ "text": "今日は本を読みながら、ゆっくり過ごしました。", "expectedTurn": 1 }
```

レスポンス例:

```json
{
  "turn": 2,
  "reply": { "role": "ASSISTANT", "text": "いいですね。どんな本を読んでいたんですか？" },
  "canFinish": true,
  "finished": false
}
```

- `text` は1〜2000文字です。
- `expectedTurn` が現在の往復数と一致しない場合は `TURN_MISMATCH` を返します（二重送信対策）。
- LLM呼び出しに失敗した場合は `LLM_CALL_FAILED` を返し、ユーザーの発言も保存しません。フロントエンドは同じ `text` と `expectedTurn` で再送します。
- `BIRTH` は6往復目の応答に締めの指示を注入し、`finished: true` を返します。以降の送信は `CONVERSATION_FINISHED` を返します。

#### 振り返り

レスポンス例:

```json
{
  "feedback": "自分の言葉で、気持ちを伝えられたね。",
  "expGained": 40,
  "level": 2,
  "leveledUp": true,
  "newTopics": [{ "id": 12, "name": "読書", "public": false }]
}
```

- `BIRTH` は3往復以上、それ以外は1往復以上で実行できます。不足している場合は `CONVERSATION_TOO_SHORT` を返します。
- 振り返り済みの場合は保存済みの結果を返し、EXPなどを二重に反映しません。
- LLM呼び出しに失敗した場合は `LLM_CALL_FAILED` を返し、会話は未振り返りのまま残ります。同じエンドポイントで再実行できます。
- プロファイル・口調・話題・EXPは振り返り成功時にまとめて反映します。次回の今日のひとことの質問もここで生成します。

### イベント・招待

| 優先度 | Method | Path | 概要 |
| --- | --- | --- | --- |
| M | `POST` | `/api/events` | `{ title, description, startDate, endDate, venueTemplate }` でイベントを作成。招待コードを発行し、作成者は自動で参加 |
| M | `GET` | `/api/events` | 参加中・作成したイベントの一覧（開催前/開催中/終了のフェーズ付き） |
| M | `GET` | `/api/events/{id}` | イベント詳細（参加者のみ） |
| M | `GET` | `/api/events/{id}/participants` | 会場の賑わい表示用の参加者一覧（名前とプリセットIDのみ） |
| M | `GET` | `/api/invitations/{code}` | 参加ページ用のイベント概要 |
| M | `POST` | `/api/invitations/{code}/join` | 参加してマッチングを実行。参加済みでも成功扱い |
| M | `DELETE` | `/api/events/{id}/participants/me` | 退出 |
| M | `DELETE` | `/api/events/{id}` | 参加者が作成者のみの場合に限り削除 |
| S | `POST` | `/api/events/{id}/archive` | 作成者による早期終了 |

- 招待コードは6〜8文字の英数字です。QRコードはフロントエンドで生成します。
- `/api/invitations/**` もログイン後のみ利用できます。
- 常設デモイベントはシードデータで用意します。専用APIは設けません。

### 出会いカード

| 優先度 | Method | Path | 概要 |
| --- | --- | --- | --- |
| M | `GET` | `/api/events/{id}/cards` | そのイベントでの自分宛てカード。会場ページを開いたときに1回だけ呼ぶ（ポーリングなし） |
| M | `GET` | `/api/cards` | 自分宛てカードの全件（スマートフォンのカード一覧用） |
| M | `GET` | `/api/cards/{id}` | カード詳細。開封済みの場合は保存済みの報告を含む |
| M | `POST` | `/api/cards/{id}/open` | LLMで会話報告を生成して保存。開封済みの場合は保存済みの報告を返す |

### その他

- `/actuator/health` はDBのヘルスチェックを有効にし、Supabaseのキープアライブにも使用します。
- 管理用APIは作りません。マスタ（`level_curves`、`exp_rules`、`achievement_defs`、`counter_defs`、`topic_categories`、`items`、`demo_passphrases`、`conversation_openings`）はSQLまたはシードデータで編集します。

### 追加予定のエラーコード

| HTTP status | code | 発生条件 |
| --- | --- | --- |
| `400 Bad Request` | `INVALID_API_KEY` | APIキーの有効性検証に失敗した |
| `400 Bad Request` | `INVALID_PASSPHRASE` | デモの合言葉が一致しない、または無効化されている |
| `400 Bad Request` | `CONVERSATION_TOO_SHORT` | 振り返りに必要な往復数に達していない |
| `404 Not Found` | `CONVERSATION_NOT_FOUND` / `EVENT_NOT_FOUND` / `CARD_NOT_FOUND` | 対象が存在しない、または閲覧権限がない |
| `409 Conflict` | `NPC_STATE_INVALID` | NPCの誕生状態と操作が合わない |
| `409 Conflict` | `TURN_MISMATCH` | `expectedTurn` が現在の往復数と一致しない |
| `409 Conflict` | `CONVERSATION_FINISHED` | 終了済みの会話にメッセージを送った |
| `409 Conflict` | `NPC_NOT_BORN` | NPC誕生前にイベントへ参加しようとした |
| `409 Conflict` | `EVENT_ENDED` | 終了したイベントに参加しようとした |
| `409 Conflict` | `EVENT_NOT_STARTED` | 開催前のイベントのカードを開封しようとした |
| `409 Conflict` | `EVENT_HAS_PARTICIPANTS` | 作成者以外の参加者がいるイベントを削除しようとした |
| `429 Too Many Requests` | `DEMO_LIMIT_EXCEEDED` | デモの呼び出し回数上限に達した |
| `502 Bad Gateway` | `LLM_CALL_FAILED` | LLMプロバイダの呼び出しに失敗した、またはタイムアウトした |

閲覧権限がない場合も `404 Not Found` を返し、対象の存在を知られないようにします。
