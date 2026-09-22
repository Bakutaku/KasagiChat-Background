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

常設デモイベントは招待コードの有無だけで作成を判断するため、サンプルNPCの定義（名前・見た目のプリセットID・話題）を変えても既存のデータベースには反映されません。反映するには `events` の該当行を削除するか、データベースを作り直してください。

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
| `GET` | `/api/user/me` | 登録済み | 現在ログイン中のユーザー情報を取得 |
| `GET` | `/api/home` | 登録済み | 本人の分身・思い出の品・配置・解禁アイテムを取得 |
| `GET` | `/api/npc` | 登録済み | 本人のNPCを取得 |
| `POST` | `/api/npc` | 登録済み | 誕生前のNPCを1体作成 |
| `GET` | `/api/credentials` | 登録済み | AI利用設定を取得 |
| `GET` | `/api/credentials/options` | 登録済み | 選択できるプロバイダとモデルを取得 |
| `PUT` | `/api/credentials` | 登録済み | APIキーまたはデモの合言葉を登録 |
| `DELETE` | `/api/credentials` | 登録済み | AI利用設定を削除 |
| `GET` | `/api/daily-question` | 登録済み | 今日のひとことの質問を取得（なければ204） |
| `GET` | `/api/conversations?status=UNREVIEWED` | 登録済み | 未振り返りの会話一覧を取得 |
| `POST` | `/api/conversations` | 登録済み | 会話を開始、または未振り返りの会話を再開 |
| `GET` | `/api/conversations/{id}` | 登録済み | 会話の状態と全メッセージを取得 |
| `POST` | `/api/conversations/{id}/messages` | 登録済み | 発言を送り、NPCの応答を1件得る |
| `POST` | `/api/conversations/{id}/review` | 登録済み | 会話を終えて振り返りを実行 |
| `PUT` | `/api/home/items/{topicId}/placement` | 登録済み | 思い出の品を固定スロットへ配置・移動 |
| `DELETE` | `/api/home/items/{topicId}/placement` | 登録済み | 思い出の品を収納へ戻す |
| `POST` | `/api/events` | 登録済み | イベントを作成し、招待コードを発行（作成者は自動で参加） |
| `GET` | `/api/events` | 登録済み | 作成した、または参加中のイベント一覧 |
| `GET` | `/api/events/{id}` | 登録済み | イベントの詳細（参加者のみ） |
| `GET` | `/api/events/{id}/participants` | 登録済み | 会場の賑わい表示用の参加者一覧（名前とプリセットIDのみ） |
| `POST` | `/api/events/{id}/archive` | 登録済み | 作成者によるイベントの早期終了 |
| `DELETE` | `/api/events/{id}/participants/me` | 登録済み | イベントから退出 |
| `DELETE` | `/api/events/{id}` | 登録済み | 作成者のみのイベントを削除 |
| `GET` | `/api/invitations/{code}` | 登録済み | 招待コードからイベントの概要を取得 |
| `POST` | `/api/invitations/{code}/join` | 登録済み | 招待コードでイベントへ参加 |
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

### 家の取得・配置

API契約、フロント向けTypeScript型、スロット互換性、エラーとDB適用手順は [家API](docs/home-api.md) を参照してください。
思い出の品は既存の話題から導出し、配置先のみ `topics.home_slot_id` へ保存します。
既存DBには更新版起動前に [手動マイグレーション](scripts/migrations/20260921_home_slots.sql) が必要です。

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
  "avatarUrl": "https://example.com/avatar.png"
}
```

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
| `400 Bad Request` | `INVALID_CONVERSATION_REQUEST` | 会話種別とシーンの組み合わせが不正 |
| `400 Bad Request` | `CONVERSATION_TOO_SHORT` | 振り返りに必要な往復数へ達していない（`detail`の往復数は種別ごとに変わる。`BIRTH`は3、それ以外は1） |
| `404 Not Found` | `CONVERSATION_NOT_FOUND` | 会話が存在しない、または本人の会話ではない |
| `409 Conflict` | `CONVERSATION_FINISHED` | 終了済みの会話へ発言しようとした |
| `409 Conflict` | `TURN_MISMATCH` | クライアントの往復数がサーバーの状態と一致しない |
| `409 Conflict` | `NPC_STATE_INVALID` | NPCの誕生状態が会話種別の前提と合わない |
| `409 Conflict` | `DAILY_QUESTION_NOT_AVAILABLE` | 今日のひとことに使える未消化の質問がない |
| `503 Service Unavailable` | `CONVERSATION_CONFIGURATION_ERROR` | 会話の冒頭マスタが登録されていない |
| `400 Bad Request` | `INVALID_EVENT_PERIOD` | イベントの終了日時が開始日時より後になっていない |
| `404 Not Found` | `EVENT_NOT_FOUND` | イベントが存在しない、または閲覧権限がない（存在を秘匿するため同じコードで返す） |
| `409 Conflict` | `NPC_NOT_BORN` | 分身の誕生前にイベントを作成・参加しようとした |
| `409 Conflict` | `EVENT_ENDED` | 終了したイベントに参加しようとした |
| `409 Conflict` | `EVENT_HAS_PARTICIPANTS` | 作成者以外の参加記録があるイベントを削除しようとした |
| `500 Internal Server Error` | `INVITE_CODE_GENERATION_FAILED` | 未使用の招待コードを発行できなかった |

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

### 既存APIの変更

| 優先度 | Method | Path | 変更内容 |
| --- | --- | --- | --- |
| M | `GET` | `/api/user/me` | レスポンスに `onboarding: { credentialConfigured, npcCreated, npcBorn }` を追加。フロントエンドが初回フロー（APIキー設定 → NPC誕生）の遷移先を決めるために使用 |

### 設定（APIキー・デモキー）

| 優先度 | Method | Path | 概要 |
| --- | --- | --- | --- |
| M | `GET` | `/api/credentials` | 現在の設定。プロバイダ、マスク済みキー、DEMOの場合はデモ残り回数 |
| M | `GET` | `/api/credentials/options` | 選択可能なBYOKモデルと、運営固定のDEMOモデル |
| M | `PUT` | `/api/credentials` | 登録・変更。`provider` によって入力を切り替える |
| M | `DELETE` | `/api/credentials` | 削除 |

`PUT /api/credentials` のリクエスト例:

```json
{ "provider": "OPENAI", "model": "gpt-5-mini", "apiKey": "sk-..." }
```

```json
{ "provider": "DEMO", "passphrase": "HOSHI26" }
```

- `OPENAI` / `ANTHROPIC`: 保存前にモデル一覧取得などで有効性を1回検証します。無効な場合は `INVALID_API_KEY` を返します。キーは暗号化して保存します。
- `DEMO`: 合言葉を `demo_passphrases` と照合します。一致しない場合は `INVALID_PASSPHRASE` を返します。呼び出し回数はアカウント単位で数えます。
- OpenAI互換エンドポイント（base URL指定）は運営設定のみとし、APIからは指定できません（SSRF対策）。

`GET /api/credentials` は未設定時に `{"configured":false,...}`、設定済み時に `provider`、`model`、`maskedApiKey` を返します。生のAPIキーと合言葉は返しません。DEMOでは `demoUsage: {used, limit, remaining}` も返します。

手動で環境へ設定するLLM関連変数（秘密値をリポジトリへ保存しないこと）:

- `API_CREDENTIAL_ENCRYPTION_KEY`: Base64形式の32バイトAES鍵
- `DEMO_LLM_API_KEY`: デモで使用する運営APIキー
- `DEMO_LLM_PROVIDER`: `OPENAI` または `ANTHROPIC`
- `DEMO_LLM_MODEL`: デモで固定するモデルID
- `OPENAI_ALLOWED_MODELS`: BYOKで選択可能なモデルIDのカンマ区切り一覧（任意）
- `ANTHROPIC_ALLOWED_MODELS`: BYOKで選択可能なモデルIDのカンマ区切り一覧（任意）

`prod` はHibernateの `ddl-auto: validate` を使うため、既存DBにはデプロイ前に `api_credentials.model_name varchar(100)` を追加してください（デバッグ環境は `ddl-auto: update`）。

### NPC・プロフィール帳

NPCの見た目は固定プリセットです。プリセット一覧APIは設けず、フロントエンドとサーバーで同じプリセットIDを定義します（サーバーはenumで検証）。

| 優先度 | Method | Path | 概要 |
| --- | --- | --- | --- |
| M | `POST` | `/api/npc` | `{ presetId, name }` で未誕生状態のNPCを作成。誕生会話の振り返り成功で誕生済みになる |
| M | `GET` | `/api/npc` | 名前、プリセットID、レベル、EXP、次レベルまでの必要EXP、人格文書、口調、口調反映ON/OFF、統計 |
| M | `PATCH` | `/api/npc` | `{ profile?, speechStyle?, speechStyleEnabled? }`。文字数上限のみ検証 |
| M | `GET` | `/api/npc/topics` | 話題一覧。カテゴリと品物画像を含む（思い出の品はここから導出） |
| M | `PATCH` | `/api/npc/topics/{id}` | `{ public }` で公開/非公開を切り替え |
| M | `DELETE` | `/api/npc/topics/{id}` | 話題を削除。対応する思い出の品も表示されなくなる |

口調は発言の引用ではなく、振り返り時にLLMが更新する文章（`speechStyle`）として持ちます。

### 成長演出・実績（優先度低）

| 優先度 | Method | Path | 概要 |
| --- | --- | --- | --- |
| S | `GET` | `/api/growth-events?unread=true` | 実績・成長の通知ログ（未読分） |
| S | `POST` | `/api/growth-events/read` | 一括既読 |
| S | `GET` | `/api/achievements?claimed=false` | 未受取の報酬箱 |
| S | `POST` | `/api/achievements/{id}/claim` | 報酬を受け取り、`unlocked_items` へ反映 |

### 会話（NPC誕生・練習・今日のひとこと）

**このセクションは実装済みです**（`GET /api/conversations` の会話履歴を除く）。稼働中の一覧は「エンドポイント一覧」を参照してください。

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
| C | `GET` | `/api/conversations` | 会話履歴（未実装） |

`?status=UNREVIEWED` は進行中と終了済みの両方を新しい順に返します。1件あたりの形は次のとおりで、メッセージ本文は含みません（行ごとに読み込むとN+1になるため、本文は詳細取得へ委ねます）。

```json
[
  {
    "id": "0b8f6c1e-2d4a-4f7b-9c3e-5a1d2e3f4a5b",
    "type": "PRACTICE",
    "scene": "CAFE",
    "status": "IN_PROGRESS",
    "turn": 2,
    "canFinish": true,
    "startedAt": "2026-09-21T12:34:56Z"
  }
]
```

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
- 質問は未消化のものが残っていない場合だけ追加します。練習は往復数に上限がないため、毎回生成すると質問が積み上がり、「今日のひとこと」が常に在庫を抱えた状態になるためです。保留は最大1件です。
- 話題は `話題名|カテゴリコード` の形式で生成させ、`topic_categories` のコードへ解決します。未知のコードと未指定は分類なし（家では本棚）として保存します。
- 話題名は大文字小文字を無視して既存と突き合わせます。区別して比較すると一意制約違反で振り返り全体が巻き戻り、再実行しても同じ理由で失敗し続けるためです。

### イベント・招待

| 優先度 | Method | Path | 概要 |
| --- | --- | --- | --- |
| M | `POST` | `/api/events` | `{ title, description, startsAt, endsAt, venueTemplate }` でイベントを作成。招待コードを発行し、作成者は自動で参加 |
| M | `GET` | `/api/events` | 参加中・作成したイベントの一覧（開催前/開催中/終了のフェーズ付き） |
| M | `GET` | `/api/events/{id}` | イベント詳細（参加者のみ） |
| M | `GET` | `/api/events/{id}/participants` | 会場の賑わい表示用の参加者一覧（名前とプリセットIDのみ） |
| M | `GET` | `/api/invitations/{code}` | 参加ページ用のイベント概要 |
| M | `POST` | `/api/invitations/{code}/join` | 参加する。参加済みでも成功扱い。参加のたびにマッチングを再計算する |
| M | `DELETE` | `/api/events/{id}/participants/me` | 退出 |
| M | `DELETE` | `/api/events/{id}` | 参加者が作成者のみの場合に限り削除 |
| S | `POST` | `/api/events/{id}/archive` | 作成者による早期終了 |

- 招待コードは6〜8文字の英数字です。QRコードはフロントエンドで生成します。
- `/api/invitations/**` もログイン後のみ利用できます。
- 常設デモイベントはシードデータで用意します。専用APIは設けません。
- 招待コードは作成者にだけ返します（`EventResponse.inviteCode`）。誰を招くかを主催者が決められるようにするためです。
- 参加時はタグ一致マッチングを実行し、出会いカードを作り直します。対象はそのイベントに現在参加中の全参加者です。

### 出会いカード

| 優先度 | Method | Path | 概要 |
| --- | --- | --- | --- |
| M | `GET` | `/api/events/{id}/cards` | そのイベントでの自分宛てカード。会場ページを開いたときに1回だけ呼ぶ（ポーリングなし） |
| M | `GET` | `/api/cards` | 自分宛てカードの全件（スマートフォンのカード一覧用） |
| M | `GET` | `/api/cards/{id}` | カード詳細。開封済みの場合は保存済みの報告を含む |
| M | `POST` | `/api/cards/{id}/open` | LLMで会話報告を生成して保存。開封済みの場合は保存済みの報告を返す |

レスポンス例:

```json
{
  "id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "eventId": "11111111-2222-3333-4444-555555555555",
  "eventTitle": "交流会",
  "partnerName": "ハル",
  "partnerPresetId": "cool-girl",
  "score": 45,
  "commonTags": ["ゲーム", "コーヒー"],
  "opened": true,
  "openedAt": "2026-03-01T10:00:00Z",
  "report": "ハルさんの分身と、ゲームの話で盛り上がってきたよ。",
  "recommendedTopics": ["最近遊んだゲームの話", "おすすめのコーヒー豆の話"]
}
```

- 未開封のカードでは `openedAt`・`report`・`recommendedTopics` を返しません。カード表面（名前・見た目・共通タグ・相性スコア）はマッチングの結果だけで組み立てるため、LLMを呼ばずに表示できます。
- 他人宛てのカードは、存在を秘匿するため `CARD_NOT_FOUND` を返します。

#### マッチング

- すべてサーバー内で完結する決定的な計算です。LLMもembeddingも使いません。
- 相性スコアは公開話題（`topics.is_public`）のカテゴリ一致で決めます。カテゴリが一致するたびに `2 × 互いの興味度の最大値の積` を加点し、同じカテゴリの中で話題名が部分一致していれば `+5` します。分類なしの話題は共通タグにできないため対象外です。
- カードを作る相手は各参加者の相性上位3人です。相性0の相手も候補に含めるため、参加者が2人以上いれば全員が最低1枚受け取ります。相互保証があり、片側の上位3人に入っていれば両方にカードを作るため、3枚を超えることがあります。
- 同点は「相手の公開話題の興味度合計 → 共通タグの数 → ユーザーID」の順で機械的に決めます。最後にIDを見ることで、同じ入力からは必ず同じ結果になります。
- 再計算は同じ (event, recipient, partner) のカードを更新します（upsert）。更新するのは相性スコアと共通タグだけで、開封状態と会話報告は残します。選ばれなくなった組み合わせのカードも削除しません。
- 同時参加でカードが二重に作られないよう、イベント行を排他ロックして再計算を直列化します。

#### 開封

- 会話報告とおすすめ話題は、両者のプロファイルを入れた1回の呼び出しでまとめて生成します。開いた本人のAI接続設定で実行し、結果を共有しません。
- プロンプトへ入れる相手側のデータは「公開話題 + 人格文書」までです。非公開話題はイベントでは一切使いません。人格文書と話題には、文中の命令に従わないための注意書きと、個人的な事情を書かせないためのガードを添えています。
- 口調は開いた本人の分だけを、`speechStyleEnabled` がONの場合に渡します。報告するのは自分の分身であり、相手が喋る場面ではないためです。
- 生成は `<report>` と `<recommended_topics>` のタグ形式で受け取ります。`<report>` が取れない場合は `LLM_CALL_FAILED` を返し、カードは未開封のまま残るため同じエンドポイントで開き直せます。おすすめ話題は改行区切りで最大3件です。
- 開封済みの場合はLLMを呼ばず、保存済みの内容をそのまま返します。保存時は `cards.version` の楽観ロックで、同時開封による二重生成を弾きます。
- 開催前（`UPCOMING`）のイベントのカードは開封できず、`EVENT_NOT_STARTED` を返します。

### その他

| 優先度 | Method | Path | 概要 |
| --- | --- | --- | --- |
| S | `GET` | `/api/usage/summary` | コスト概算の表示 |

- `/actuator/health` はDBのヘルスチェックを有効にし、Supabaseのキープアライブにも使用します。
- 管理用APIは作りません。マスタ（`level_curves`、`exp_rules`、`achievement_defs`、`counter_defs`、`topic_categories`、`items`、`demo_passphrases`、`conversation_openings`）はSQLまたはシードデータで編集します。

### 追加予定のエラーコード

| HTTP status | code | 発生条件 |
| --- | --- | --- |
| `400 Bad Request` | `INVALID_API_KEY` | APIキーの有効性検証に失敗した |
| `400 Bad Request` | `INVALID_PASSPHRASE` | デモの合言葉が一致しない、または無効化されている |
| `400 Bad Request` | `CONVERSATION_TOO_SHORT` | 振り返りに必要な往復数に達していない |
| `404 Not Found` | `CONVERSATION_NOT_FOUND` / `CARD_NOT_FOUND` | 対象が存在しない、または閲覧権限がない |
| `409 Conflict` | `NPC_STATE_INVALID` | NPCの誕生状態と操作が合わない |
| `409 Conflict` | `TURN_MISMATCH` | `expectedTurn` が現在の往復数と一致しない |
| `409 Conflict` | `CONVERSATION_FINISHED` | 終了済みの会話にメッセージを送った |
| `409 Conflict` | `EVENT_NOT_STARTED` | 開催前のイベントのカードを開封しようとした |
| `429 Too Many Requests` | `DEMO_LIMIT_EXCEEDED` | デモの呼び出し回数上限に達した |
| `502 Bad Gateway` | `LLM_CALL_FAILED` | LLMプロバイダの呼び出しに失敗した、またはタイムアウトした |

閲覧権限がない場合も `404 Not Found` を返し、対象の存在を知られないようにします。
