-- PostgreSQL / psql 専用: ユーザーと、そのユーザーに紐づく全データを物理削除する。
-- 実行例:
--   psql "$DATABASE_URL" -v public_id='00000000-0000-0000-0000-000000000000' \
--     -f scripts/delete-user-hard.sql
--
-- 注意: 作成者が対象ユーザーであるイベントは、参加者・カードを含めて削除する。
--       そのため、そのイベントに参加した他ユーザーの当該イベント上のデータも削除される。

\if :{?public_id}
\else
  \quit 'public_id を -v public_id=<UUID> で指定してください。'
\endif

BEGIN;

-- 対象をロックし、UUIDの誤指定の場合は何も削除せず失敗させる。
-- 論理削除済みのユーザーも、完全削除の対象にできる。
CREATE TEMP TABLE delete_target_user (
  id bigint PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO delete_target_user (id)
SELECT id
FROM users
WHERE public_id = :'public_id'::uuid
FOR UPDATE;

DO $$
BEGIN
  IF (SELECT count(*) FROM delete_target_user) <> 1 THEN
    RAISE EXCEPTION '削除対象のユーザーが1件ではありません';
  END IF;
END
$$;

CREATE TEMP TABLE delete_target_conversation ON COMMIT DROP AS
SELECT id
FROM conversation
WHERE user_id IN (SELECT id FROM delete_target_user);

CREATE TEMP TABLE delete_target_npc ON COMMIT DROP AS
SELECT id
FROM npc
WHERE user_id IN (SELECT id FROM delete_target_user);

CREATE TEMP TABLE delete_target_topic ON COMMIT DROP AS
SELECT id
FROM topic
WHERE npc_id IN (SELECT id FROM delete_target_npc)
   OR source_conversation_id IN (SELECT id FROM delete_target_conversation);

CREATE TEMP TABLE delete_target_event ON COMMIT DROP AS
SELECT id
FROM event
WHERE creator_user_id IN (SELECT id FROM delete_target_user);

CREATE TEMP TABLE delete_target_daily_question ON COMMIT DROP AS
SELECT id
FROM daily_question
WHERE user_id IN (SELECT id FROM delete_target_user)
   OR source_conversation_id IN (SELECT id FROM delete_target_conversation);

-- conversation -> daily_question の参照を先に外す。
UPDATE conversation
SET daily_question_id = NULL
WHERE daily_question_id IN (SELECT id FROM delete_target_daily_question);

-- 子テーブルから順に削除する。
DELETE FROM memory
WHERE topic_id IN (SELECT id FROM delete_target_topic)
   OR source_conversation_id IN (SELECT id FROM delete_target_conversation);

DELETE FROM message
WHERE conversation_id IN (SELECT id FROM delete_target_conversation);

DELETE FROM daily_question
WHERE id IN (SELECT id FROM delete_target_daily_question);

DELETE FROM topic
WHERE id IN (SELECT id FROM delete_target_topic);

DELETE FROM unlocked_item
WHERE user_id IN (SELECT id FROM delete_target_user)
   OR achievement_id IN (
     SELECT id FROM achievement WHERE user_id IN (SELECT id FROM delete_target_user)
   );

DELETE FROM achievement
WHERE user_id IN (SELECT id FROM delete_target_user);

DELETE FROM achievement_counter
WHERE user_id IN (SELECT id FROM delete_target_user);

DELETE FROM growth_event
WHERE user_id IN (SELECT id FROM delete_target_user);

DELETE FROM npc
WHERE id IN (SELECT id FROM delete_target_npc);

DELETE FROM api_credential
WHERE user_id IN (SELECT id FROM delete_target_user);

DELETE FROM demo_usage
WHERE user_id IN (SELECT id FROM delete_target_user);

-- 対象が受取人・相手であるカード、および対象が作成したイベントのカードを削除する。
DELETE FROM card
WHERE recipient_user_id IN (SELECT id FROM delete_target_user)
   OR partner_user_id IN (SELECT id FROM delete_target_user)
   OR event_id IN (SELECT id FROM delete_target_event);

DELETE FROM event_participant
WHERE user_id IN (SELECT id FROM delete_target_user)
   OR event_id IN (SELECT id FROM delete_target_event);

DELETE FROM event
WHERE id IN (SELECT id FROM delete_target_event);

DELETE FROM conversation
WHERE id IN (SELECT id FROM delete_target_conversation);

DELETE FROM user_terms_agreement
WHERE user_id IN (SELECT id FROM delete_target_user);

-- 未登録状態の同一OAuth識別子が残っていた場合も削除する。
DELETE FROM pending_users
WHERE (provider, subject) IN (
  SELECT provider, subject
  FROM user_auth
  WHERE user_id IN (SELECT id FROM delete_target_user)
);

DELETE FROM user_auth
WHERE user_id IN (SELECT id FROM delete_target_user);

DELETE FROM users
WHERE id IN (SELECT id FROM delete_target_user);

COMMIT;
