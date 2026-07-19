# KasagiChat Backend (Spring Boot)

KasagiChat のバックエンドAPI。

- フロントエンド / ドキュメント: [KasagiChat リポジトリ](https://github.com/Bakutaku/KasagiChat)
- 起動方法・認証実験の解説: KasagiChat リポジトリの `docs/auth-experiment.md` 参照
  (Docker Compose は KasagiChat リポジトリ側にあり、両リポジトリを並べて置く前提)

## 構成

- Spring Boot 4.x / Java 21 / Gradle
- Spring Security OAuth2 Client(GitHub / Google)+ セッションCookie認証
- PostgreSQL(Spring Data JPA)

## パッケージ

| パッケージ | 内容 |
|---|---|
| `com.kasagichat.api.security` | SecurityFilterChain・CSRF・OAuthログイン成功ハンドラ |
| `com.kasagichat.api.user` | users テーブル(エンティティ/リポジトリ/サービス)・/api/me |
