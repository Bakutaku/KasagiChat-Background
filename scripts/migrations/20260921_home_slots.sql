-- PostgreSQL / 既存のtopicsテーブルに対する一度限りの手動マイグレーション。
-- アプリ停止中、更新版の起動前に適用する。Flyway等による自動実行は未導入。
-- 既存行のhome_slot_idはNULL（収納）。既存データの削除・自動配置はしない。
BEGIN;

ALTER TABLE topics ADD COLUMN home_slot_id varchar(30);

ALTER TABLE topics ADD CONSTRAINT uk_topics_npc_home_slot
    UNIQUE (npc_id, home_slot_id);

ALTER TABLE topics ADD CONSTRAINT ck_topics_home_slot CHECK (
    home_slot_id IS NULL
    OR (category_id IS NOT NULL AND home_slot_id IN (
        'DISPLAY_1', 'DISPLAY_2', 'DISPLAY_3', 'DISPLAY_4', 'DISPLAY_5', 'DISPLAY_6'
    ))
    OR (category_id IS NULL AND home_slot_id IN (
        'BOOKSHELF_1', 'BOOKSHELF_2', 'BOOKSHELF_3', 'BOOKSHELF_4', 'BOOKSHELF_5', 'BOOKSHELF_6'
    ))
);

COMMIT;
