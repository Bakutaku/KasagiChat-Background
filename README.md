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

`prod` では起動前に、JPAエンティティに対応するテーブルと `SPRING_SESSION` テーブルを構築しておく必要があります。

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

`debug` では起動時にHibernateがテーブルを作成・更新し、Spring Sessionのテーブルも初期化します。本登録には、現在有効な規約データがデータベースに1件以上必要です。規約データが存在しない場合、本登録は `TERMS_AGREEMENT_REQUIRED` で拒否されます。

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

入力バリデーション違反、CSRFエラー、未認証、権限不足など、Spring SecurityまたはSpring MVCが直接返すエラーは上記のアプリケーション固有形式とは異なる場合があります。

## 実装上の補足

- `/api/terms/required` を除く `/api/**` は、原則として登録済みユーザー権限が必要です。仮登録ユーザーには `/api/registrations/**` だけが許可されます。
- 規約は `effectiveAt` が現在時刻以前のものだけが有効です。本登録時には規約種別ごとの最新規約すべてへの同意が必要です。
- `debug` ではDDL更新とSQLログを有効にし、`prod` ではスキーマ検証のみを行います。本番スキーマはマイグレーションなどで管理してください。
