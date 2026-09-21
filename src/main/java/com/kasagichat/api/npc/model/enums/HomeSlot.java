package com.kasagichat.api.npc.model.enums;

import java.util.Arrays;
import java.util.Optional;

/** 家の正規スロット定義。画面座標はフロントが管理し、IDと互換性はサーバーが管理する。 */
public enum HomeSlot {
    DISPLAY_1(HomeItemKind.SOUVENIR),
    DISPLAY_2(HomeItemKind.SOUVENIR),
    DISPLAY_3(HomeItemKind.SOUVENIR),
    DISPLAY_4(HomeItemKind.SOUVENIR),
    DISPLAY_5(HomeItemKind.SOUVENIR),
    DISPLAY_6(HomeItemKind.SOUVENIR),
    BOOKSHELF_1(HomeItemKind.BOOK),
    BOOKSHELF_2(HomeItemKind.BOOK),
    BOOKSHELF_3(HomeItemKind.BOOK),
    BOOKSHELF_4(HomeItemKind.BOOK),
    BOOKSHELF_5(HomeItemKind.BOOK),
    BOOKSHELF_6(HomeItemKind.BOOK);

    // debugでのDDL生成にも本番の手動マイグレーションと同じ制約を適用する。
    public static final String CHECK_CONSTRAINT = "home_slot_id is null or "
        + "(category_id is not null and home_slot_id in "
        + "('DISPLAY_1','DISPLAY_2','DISPLAY_3','DISPLAY_4','DISPLAY_5','DISPLAY_6')) or "
        + "(category_id is null and home_slot_id in "
        + "('BOOKSHELF_1','BOOKSHELF_2','BOOKSHELF_3','BOOKSHELF_4','BOOKSHELF_5','BOOKSHELF_6'))";

    private final HomeItemKind acceptedKind;

    HomeSlot(HomeItemKind acceptedKind) {
        this.acceptedKind = acceptedKind;
    }

    public HomeItemKind acceptedKind() {
        return acceptedKind;
    }

    public static Optional<HomeSlot> fromId(String id) {
        return Arrays.stream(values()).filter(slot -> slot.name().equals(id)).findFirst();
    }
}
