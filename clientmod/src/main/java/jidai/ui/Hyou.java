package jidai.ui;

/**
 * 画面に描く文字の表。
 *
 * ★★ このファイルは手で直さない ★★
 *   `ui/manifest.json` から `python tools/hyou_tsukuru.py` で作り直す。
 *   文字は 149 件あり、1件ずつ座標・大きさ・色・揃えを持つ。
 *   手で写すと必ずどこかで1桁間違えるが、コンパイルは通ってしまうので、
 *   実機で開くまで気付けない。だから正本を manifest.json ひとつにしている。
 *
 *   直したい時は manifest.json を直して、この道具を走らせ直すこと。
 */
public final class Hyou {

    private Hyou() {
        // 表だけの置き場なので、実体は作らない
    }

    /** 画面に描く文字ひとつぶん。 */
    public static final class Moji {

        /** どの画面か（gunshop_guns など） */
        public final String gamen;
        /** 画面の中での名前（item_1_name など） */
        public final String id;
        /** 出す文字。{...} の部分は、その場で変わる値に差し替える */
        public final String moji;
        /** 文字を置く枠。320x240 の中の座標 */
        public final int x;
        public final int y;
        public final int w;
        public final int h;
        /** 文字の大きさ。8 が素の大きさ / 12 は1.5倍 / 16 は2倍 */
        public final int px;
        /** 折り返してよい行数の上限 */
        public final int gyouSuu;
        /** 色。0xRRGGBB */
        public final int iro;
        /** 寄せ方。L=左 C=中央 R=右 */
        public final char yose;
        /** その場で変わる値を含むか */
        public final boolean ugoku;

        Moji(String gamen, String id, String moji, int x, int y, int w, int h,
             int px, int gyouSuu, int iro, char yose, boolean ugoku) {
            this.gamen = gamen;
            this.id = id;
            this.moji = moji;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.px = px;
            this.gyouSuu = gyouSuu;
            this.iro = iro;
            this.yose = yose;
            this.ugoku = ugoku;
        }

        /** 文字の大きさから、拡大率を出す。8px が 1.0 倍。 */
        public float bairitsu() {
            return px / 8.0F;
        }
    }

    /** 全部の文字。149 件。 */
    public static final Moji[] ZENBU = {
        // ---- shop_life ----
        new Moji("shop_life", "title", "販売所", 20, 14, 90, 18, 16, 1, 0xF5E8C8, 'L', false),
        new Moji("shop_life", "wallet_personal", "個人 {value}", 116, 17, 90, 10, 8, 1, 0x4FA1B2, 'R', true),
        new Moji("shop_life", "wallet_faction", "勢力 {value}", 210, 17, 90, 10, 8, 1, 0x4FA1B2, 'R', true),
        new Moji("shop_life", "tab_1_label", "生活", 26, 42, 50, 10, 8, 1, 0xF5E8C8, 'C', false),
        new Moji("shop_life", "tab_2_label", "防具", 90, 42, 50, 10, 8, 1, 0xAEB8AE, 'C', false),
        new Moji("shop_life", "tab_3_label", "武器", 154, 42, 50, 10, 8, 1, 0xAEB8AE, 'C', false),
        new Moji("shop_life", "item_1_name", "パン ×3", 20, 89, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_life", "item_1_price", "個人2円", 38, 64, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_life", "item_1_unlock", "鉄器", 40, 76, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_life", "item_2_name", "石炭", 93, 89, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_life", "item_2_price", "個人5円", 111, 64, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_life", "item_2_unlock", "鉄器", 113, 76, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_life", "item_3_name", "オークの原木 ×2", 166, 89, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_life", "item_3_price", "個人10円", 184, 64, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_life", "item_3_unlock", "鉄器", 186, 76, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_life", "item_4_name", "石レンガ ×5", 239, 89, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_life", "item_4_price", "個人12円", 257, 64, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_life", "item_4_unlock", "鉄器", 259, 76, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_life", "item_5_name", "松明 ×16", 20, 143, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_life", "item_5_price", "個人5円", 38, 118, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_life", "item_5_unlock", "鉄器", 40, 130, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_life", "item_6_name", "焼肉 ×5", 93, 143, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_life", "item_6_price", "個人30円", 111, 118, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_life", "item_6_unlock", "中世", 113, 130, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_life", "item_7_name", "ガラス ×5", 166, 143, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_life", "item_7_price", "個人25円", 184, 118, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_life", "item_7_unlock", "中世", 186, 130, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_life", "item_8_name", "戦争宣誓", 239, 143, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_life", "item_8_price", "勢力100円", 257, 118, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_life", "item_8_unlock", "中世", 259, 130, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_life", "item_9_name", "金リンゴ", 20, 197, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_life", "item_9_price", "個人100円", 38, 172, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_life", "item_9_unlock", "近代", 40, 184, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_life", "item_10_name", "下剋上", 93, 197, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_life", "item_10_price", "勢力150円", 111, 172, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_life", "item_10_unlock", "近代", 113, 184, 41, 10, 8, 1, 0xAEB8AE, 'L', false),

        // ---- shop_armor ----
        new Moji("shop_armor", "title", "販売所", 20, 14, 90, 18, 16, 1, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "wallet_faction", "勢力 {value}", 192, 17, 108, 10, 8, 1, 0x4FA1B2, 'R', true),
        new Moji("shop_armor", "tab_1_label", "生活", 26, 42, 50, 10, 8, 1, 0xAEB8AE, 'C', false),
        new Moji("shop_armor", "tab_2_label", "防具", 90, 42, 50, 10, 8, 1, 0xF5E8C8, 'C', false),
        new Moji("shop_armor", "tab_3_label", "武器", 154, 42, 50, 10, 8, 1, 0xAEB8AE, 'C', false),
        new Moji("shop_armor", "item_1_name", "チェーンの兜", 20, 89, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "item_1_price", "勢力15円", 38, 64, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_armor", "item_1_unlock", "鉄器", 40, 76, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_armor", "item_2_name", "チェーンの胸当て", 93, 89, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "item_2_price", "勢力25円", 111, 64, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_armor", "item_2_unlock", "鉄器", 113, 76, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_armor", "item_3_name", "チェーンの脚当て", 166, 89, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "item_3_price", "勢力20円", 184, 64, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_armor", "item_3_unlock", "鉄器", 186, 76, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_armor", "item_4_name", "チェーンの靴", 239, 89, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "item_4_price", "勢力10円", 257, 64, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_armor", "item_4_unlock", "鉄器", 259, 76, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_armor", "item_5_name", "鉄の兜", 20, 143, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "item_5_price", "勢力25円", 38, 118, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_armor", "item_5_unlock", "中世", 40, 130, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_armor", "item_6_name", "鉄の胸当て", 93, 143, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "item_6_price", "勢力30円", 111, 118, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_armor", "item_6_unlock", "中世", 113, 130, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_armor", "item_7_name", "鉄の脚当て", 166, 143, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "item_7_price", "勢力30円", 184, 118, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_armor", "item_7_unlock", "中世", 186, 130, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_armor", "item_8_name", "鉄の靴", 239, 143, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "item_8_price", "勢力20円", 257, 118, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_armor", "item_8_unlock", "中世", 259, 130, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_armor", "item_9_name", "ダイヤの兜", 20, 197, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "item_9_price", "勢力300円", 38, 172, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_armor", "item_9_unlock", "現代", 40, 184, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_armor", "item_10_name", "ダイヤの胸当て", 93, 197, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "item_10_price", "勢力500円", 111, 172, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_armor", "item_10_unlock", "近代", 113, 184, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_armor", "item_11_name", "ダイヤの脚当て", 166, 197, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "item_11_price", "勢力400円", 184, 172, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_armor", "item_11_unlock", "現代", 186, 184, 41, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_armor", "item_12_name", "ダイヤの靴", 239, 197, 61, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_armor", "item_12_price", "勢力200円", 257, 172, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_armor", "item_12_unlock", "現代", 259, 184, 41, 10, 8, 1, 0xAEB8AE, 'L', false),

        // ---- gunshop_guns ----
        new Moji("gunshop_guns", "title", "銃器専門店", 20, 16, 132, 14, 12, 1, 0xF5E8C8, 'L', false),
        new Moji("gunshop_guns", "wallet_faction", "勢力 {value}", 192, 17, 108, 10, 8, 1, 0x4FA1B2, 'R', true),
        new Moji("gunshop_guns", "tab_1_label", "銃", 26, 42, 50, 10, 8, 1, 0xF5E8C8, 'C', false),
        new Moji("gunshop_guns", "tab_2_label", "弾", 90, 42, 50, 10, 8, 1, 0xAEB8AE, 'C', false),
        new Moji("gunshop_guns", "item_1_name", "拳銃", 43, 64, 62, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("gunshop_guns", "item_1_price", "勢力800円", 43, 87, 62, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("gunshop_guns", "item_1_unlock", "中世", 21, 89, 18, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("gunshop_guns", "item_2_name", "小銃", 140, 64, 62, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("gunshop_guns", "item_2_price", "勢力3,000円", 140, 87, 62, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("gunshop_guns", "item_2_unlock", "近代", 118, 89, 18, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("gunshop_guns", "item_3_name", "連射銃", 237, 64, 62, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("gunshop_guns", "item_3_price", "勢力5,000円", 237, 87, 62, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("gunshop_guns", "item_3_unlock", "近代", 215, 89, 18, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("gunshop_guns", "item_4_name", "自動小銃", 43, 118, 62, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("gunshop_guns", "item_4_price", "勢力8,000円", 43, 141, 62, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("gunshop_guns", "item_4_unlock", "現代", 21, 143, 18, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("gunshop_guns", "item_5_name", "狙撃銃", 140, 118, 62, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("gunshop_guns", "item_5_price", "勢力12,000円", 140, 141, 62, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("gunshop_guns", "item_5_unlock", "現代", 118, 143, 18, 10, 8, 1, 0xAEB8AE, 'L', false),

        // ---- gunshop_ammo ----
        new Moji("gunshop_ammo", "title", "銃器専門店", 20, 16, 132, 14, 12, 1, 0xF5E8C8, 'L', false),
        new Moji("gunshop_ammo", "wallet_personal", "個人 {value}", 192, 17, 108, 10, 8, 1, 0x4FA1B2, 'R', true),
        new Moji("gunshop_ammo", "tab_1_label", "銃", 26, 42, 50, 10, 8, 1, 0xAEB8AE, 'C', false),
        new Moji("gunshop_ammo", "tab_2_label", "弾", 90, 42, 50, 10, 8, 1, 0xF5E8C8, 'C', false),
        new Moji("gunshop_ammo", "item_1_name", "弾 30発", 59, 67, 46, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("gunshop_ammo", "item_1_price", "個人20円", 59, 91, 46, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("gunshop_ammo", "item_1_unlock", "中世", 59, 107, 46, 10, 8, 1, 0xAEB8AE, 'L', false),

        // ---- gacha ----
        new Moji("gacha", "title", "ガチャ", 20, 14, 90, 18, 16, 1, 0xF5E8C8, 'L', false),
        new Moji("gacha", "gacha_price", "個人{price}円", 72, 159, 176, 14, 12, 1, 0xC4934B, 'C', true),

        // ---- bank ----
        new Moji("bank", "title", "銀行", 20, 14, 90, 18, 16, 1, 0xF5E8C8, 'L', false),
        new Moji("bank", "bank_1_name", "10 預ける", 59, 55, 46, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("bank", "bank_2_name", "50 預ける", 156, 55, 46, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("bank", "bank_3_name", "全部 預ける", 253, 55, 46, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("bank", "bank_4_name", "貴金属を売る", 59, 139, 46, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("bank", "bank_5_name", "石油を預ける", 156, 139, 46, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("bank", "bank_6_name", "残高", 253, 139, 46, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("bank", "bank_6_balance", "{balance}", 253, 163, 46, 10, 8, 1, 0x4FA1B2, 'L', true),

        // ---- advance ----
        new Moji("advance", "title", "時代を進める", 20, 16, 160, 14, 12, 1, 0xF5E8C8, 'L', false),
        new Moji("advance", "condition_1_name", "石油", 66, 57, 84, 14, 12, 1, 0xF5E8C8, 'L', false),
        new Moji("advance", "condition_1_value", "{value}", 66, 79, 84, 20, 8, 2, 0x4FA1B2, 'L', true),
        new Moji("advance", "condition_2_name", "貯金", 212, 57, 84, 14, 12, 1, 0xF5E8C8, 'L', false),
        new Moji("advance", "condition_2_value", "{value}", 212, 79, 84, 20, 8, 2, 0x4FA1B2, 'L', true),
        new Moji("advance", "condition_3_name", "建築", 66, 141, 84, 14, 12, 1, 0xF5E8C8, 'L', false),
        new Moji("advance", "condition_3_value", "{value}", 66, 163, 84, 20, 8, 2, 0x4FA1B2, 'L', true),
        new Moji("advance", "apply_name", "申請", 212, 140, 84, 28, 12, 1, 0xF5E8C8, 'L', false),
        new Moji("advance", "apply_locked_reason", "{reason}", 212, 189, 84, 10, 8, 1, 0xAEB8AE, 'L', true),

        // ---- declare_war ----
        new Moji("declare_war", "title", "宣戦する相手を選ぶ", 20, 16, 172, 14, 12, 1, 0xF5E8C8, 'L', false),
        new Moji("declare_war", "faction_1_name", "{faction_name_1}", 24, 100, 126, 20, 8, 2, 0xF5E8C8, 'L', true),
        new Moji("declare_war", "faction_2_name", "{faction_name_2}", 170, 100, 126, 20, 8, 2, 0xF5E8C8, 'L', true),
        new Moji("declare_war", "faction_3_name", "{faction_name_3}", 24, 182, 126, 20, 8, 2, 0xF5E8C8, 'L', true),
        new Moji("declare_war", "faction_4_name", "{faction_name_4}", 170, 182, 126, 20, 8, 2, 0xF5E8C8, 'L', true),
        new Moji("declare_war", "irreversible_warning_text", "取り返しがつかない操作", 22, 216, 276, 10, 8, 1, 0xC15A49, 'C', false),

        // ---- shop_weapon ----
        new Moji("shop_weapon", "title", "販売所", 20, 14, 90, 18, 16, 1, 0xF5E8C8, 'L', false),
        new Moji("shop_weapon", "wallet_personal", "個人 {value}", 116, 17, 90, 10, 8, 1, 0x4FA1B2, 'R', true),
        new Moji("shop_weapon", "wallet_faction", "勢力 {value}", 210, 17, 90, 10, 8, 1, 0x4FA1B2, 'R', true),
        new Moji("shop_weapon", "tab_1_label", "生活", 26, 42, 50, 10, 8, 1, 0xAEB8AE, 'C', false),
        new Moji("shop_weapon", "tab_2_label", "防具", 90, 42, 50, 10, 8, 1, 0xAEB8AE, 'C', false),
        new Moji("shop_weapon", "tab_3_label", "武器", 154, 42, 50, 10, 8, 1, 0xF5E8C8, 'C', false),
        new Moji("shop_weapon", "item_1_name", "鉄の剣", 43, 64, 62, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_weapon", "item_1_price", "勢力100円", 43, 87, 62, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_weapon", "item_1_unlock", "鉄器", 21, 89, 18, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_weapon", "item_2_name", "弓", 140, 64, 62, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_weapon", "item_2_price", "勢力150円", 140, 87, 62, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_weapon", "item_2_unlock", "鉄器", 118, 89, 18, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_weapon", "item_3_name", "矢 ×10", 237, 64, 62, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_weapon", "item_3_price", "個人30円", 237, 87, 62, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_weapon", "item_3_unlock", "鉄器", 215, 89, 18, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_weapon", "item_4_name", "ダイヤの剣", 43, 118, 62, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_weapon", "item_4_price", "勢力500円", 43, 141, 62, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_weapon", "item_4_unlock", "中世", 21, 143, 18, 10, 8, 1, 0xAEB8AE, 'L', false),
        new Moji("shop_weapon", "item_5_name", "クロスボウ", 140, 118, 62, 20, 8, 2, 0xF5E8C8, 'L', false),
        new Moji("shop_weapon", "item_5_price", "勢力150円", 140, 141, 62, 10, 8, 1, 0xC4934B, 'L', false),
        new Moji("shop_weapon", "item_5_unlock", "中世", 118, 143, 18, 10, 8, 1, 0xAEB8AE, 'L', false),
    };

    /**
     * {value} のような差し込みを、仮の値へ置き換える。
     *
     * ★ まだサーバーと繋いでいないので、【いちばん長くなる形】を入れる。
     *   短い値で測ると「本番で桁が増えた時だけはみ出す」を見逃す。
     *   繋いだら、ここを本物の値に差し替える。
     */
    public static String atehameru(String s) {
        if (s.indexOf('{') < 0) {
            return s;
        }
        return s.replaceAll("[{]faction_name[^}]*[}]", "内海")
                .replaceAll("[{][^}]*[}]", "999,999");
    }

    /** 画面ひとつぶん。 */
    public static final class Gamen {

        /** 画面の名前（gunshop_guns など） */
        public final String id;
        /** 下地の絵。これ1枚に、ふつうの状態のカードまで描かれている */
        public final String shita;
        /** 人が読む見出し */
        public final String midashi;

        Gamen(String id, String shita, String midashi) {
            this.id = id;
            this.shita = shita;
            this.midashi = midashi;
        }
    }

    /** 画面は8つ。 */
    public static final Gamen[] GAMEN = {
        new Gamen("shop_life", "textures/gui/page_shop_life.png", "販売所"),
        new Gamen("shop_armor", "textures/gui/page_shop_armor.png", "販売所"),
        new Gamen("gunshop_guns", "textures/gui/page_gunshop_guns.png", "銃器専門店"),
        new Gamen("gunshop_ammo", "textures/gui/page_gunshop_ammo.png", "銃器専門店"),
        new Gamen("gacha", "textures/gui/page_gacha.png", "ガチャ"),
        new Gamen("bank", "textures/gui/page_bank.png", "銀行"),
        new Gamen("advance", "textures/gui/page_advance.png", "時代を進める"),
        new Gamen("declare_war", "textures/gui/page_declare_war.png", "宣戦する相手を選ぶ"),
        new Gamen("shop_weapon", "textures/gui/page_shop_weapon.png", "販売所"),
    };

    /**
     * カードひとつぶん。
     *
     * ★ ふつうの状態は【下地の絵にもう描かれている】ので、素材を持たない。
     *   カーソルが乗った時などに、その状態の絵をこの位置へ上から貼る。
     */
    public static final class Kado {

        public final String gamen;
        public final String id;
        public final int x;
        public final int y;
        public final int w;
        public final int h;
        /** カーソルが乗っている時の絵。無ければ null（押せないカード） */
        public final String kasoru;
        /** 押している時 */
        public final String oshita;
        /** まだ買えない時 */
        public final String mikaikin;
        /** お金が足りない時 */
        public final String fusoku;
        /** ゲームがアイテムの絵を置く場所 */
        public final int ax;
        public final int ay;
        public final int aw;
        public final int ah;
        /**
         * そこに置くアイテム（minecraft:bread など）。無ければ null。
         * ★ プラグインの Shop.java が正本。ここで並べ直すと
         *   「絵と中身が違う店」になるので、必ず生成し直すこと。
         */
        public final String aitem;
        /** そのアイテムを何個ぶんとして見せるか */
        public final int aitemKazu;
        /**
         * サーバーが開いているチェスト画面の【何番目の枠】か。
         *
         * ★★ ここが通信の要 ★★
         *   この MOD はサーバーと独自のやり取りをしない。
         *   プラグインが今までどおりチェスト画面を開き、その上に絵を被せる。
         *   カードを押したら、この枠を左クリックしたことにして送る。
         *   買う処理はプラグイン側のまま（655項目で確かめてあるもの）が動く。
         *
         *   商品の無いカード（銀行など）は -1。
         */
        public final int waku;
        /**
         * MOD のアイテム指定。銃と弾だけ。無ければ null。
         *
         * ★★ これがあると、代用品ではなく【本物の銃の見た目】が出る ★★
         *   サーバー（Bukkit）は MOD のアイテムを作れないので、
         *   チェスト画面にはクロスボウなどの代用品しか置けない。
         *   クライアント側には TaCZ が入っているので、
         *   こちらで組み立てれば本物の絵が描ける。
         *   正本は plugin の Shop.java の modItem 欄。
         */
        public final String modAitem;

        Kado(String gamen, String id, int x, int y, int w, int h,
             String kasoru, String oshita, String mikaikin, String fusoku,
             int ax, int ay, int aw, int ah, String aitem, int aitemKazu, int waku,
             String modAitem) {
            this.gamen = gamen;
            this.id = id;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.kasoru = kasoru;
            this.oshita = oshita;
            this.mikaikin = mikaikin;
            this.fusoku = fusoku;
            this.ax = ax;
            this.ay = ay;
            this.aw = aw;
            this.ah = ah;
            this.aitem = aitem;
            this.aitemKazu = aitemKazu;
            this.waku = waku;
            this.modAitem = modAitem;
        }

        /** 中に点が入っているか（カーソル判定） */
        public boolean naka(int px, int py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    /** 全部のカード。44 件。 */
    public static final Kado[] KADO = {
        // ---- shop_life ----
        new Kado("shop_life", "item_1", 16, 60, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 21, 66, 16, 16, "minecraft:bread", 3, 9, null),
        new Kado("shop_life", "item_2", 89, 60, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 94, 66, 16, 16, "minecraft:coal", 1, 10, null),
        new Kado("shop_life", "item_3", 162, 60, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 167, 66, 16, 16, "minecraft:oak_log", 2, 11, null),
        new Kado("shop_life", "item_4", 235, 60, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 240, 66, 16, 16, "minecraft:stone_bricks", 5, 12, null),
        new Kado("shop_life", "item_5", 16, 114, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 21, 120, 16, 16, "minecraft:torch", 16, 13, null),
        new Kado("shop_life", "item_6", 89, 114, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 94, 120, 16, 16, "minecraft:cooked_beef", 5, 27, null),
        new Kado("shop_life", "item_7", 162, 114, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 167, 120, 16, 16, "minecraft:glass", 5, 28, null),
        new Kado("shop_life", "item_8", 235, 114, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 240, 120, 16, 16, "minecraft:red_banner", 1, 29, null),
        new Kado("shop_life", "item_9", 16, 168, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 21, 174, 16, 16, "minecraft:golden_apple", 1, 30, null),
        new Kado("shop_life", "item_10", 89, 168, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 94, 174, 16, 16, "minecraft:white_banner", 1, 31, null),

        // ---- shop_armor ----
        new Kado("shop_armor", "item_1", 16, 60, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 21, 66, 16, 16, "minecraft:chainmail_helmet", 1, 9, null),
        new Kado("shop_armor", "item_2", 89, 60, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 94, 66, 16, 16, "minecraft:chainmail_chestplate", 1, 10, null),
        new Kado("shop_armor", "item_3", 162, 60, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 167, 66, 16, 16, "minecraft:chainmail_leggings", 1, 11, null),
        new Kado("shop_armor", "item_4", 235, 60, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 240, 66, 16, 16, "minecraft:chainmail_boots", 1, 12, null),
        new Kado("shop_armor", "item_5", 16, 114, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 21, 120, 16, 16, "minecraft:iron_helmet", 1, 14, null),
        new Kado("shop_armor", "item_6", 89, 114, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 94, 120, 16, 16, "minecraft:iron_chestplate", 1, 15, null),
        new Kado("shop_armor", "item_7", 162, 114, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 167, 120, 16, 16, "minecraft:iron_leggings", 1, 16, null),
        new Kado("shop_armor", "item_8", 235, 114, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 240, 120, 16, 16, "minecraft:iron_boots", 1, 17, null),
        new Kado("shop_armor", "item_9", 16, 168, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 21, 174, 16, 16, "minecraft:diamond_helmet", 1, 27, null),
        new Kado("shop_armor", "item_10", 89, 168, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 94, 174, 16, 16, "minecraft:diamond_chestplate", 1, 28, null),
        new Kado("shop_armor", "item_11", 162, 168, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 167, 174, 16, 16, "minecraft:diamond_leggings", 1, 29, null),
        new Kado("shop_armor", "item_12", 235, 168, 69, 52, "textures/gui/card_micro_hover.png", "textures/gui/card_micro_pressed.png", "textures/gui/card_micro_locked.png", "textures/gui/card_micro_insufficient.png", 240, 174, 16, 16, "minecraft:diamond_boots", 1, 30, null),

        // ---- gunshop_guns ----
        new Kado("gunshop_guns", "item_1", 16, 60, 94, 52, "textures/gui/card_compact_hover.png", "textures/gui/card_compact_pressed.png", "textures/gui/card_compact_locked.png", "textures/gui/card_compact_insufficient.png", 22, 66, 16, 16, "minecraft:crossbow", 1, 9, "tacz:modern_kinetic_gun{GunId:\"hamster:nagantm1895\"}"),
        new Kado("gunshop_guns", "item_2", 113, 60, 94, 52, "textures/gui/card_compact_hover.png", "textures/gui/card_compact_pressed.png", "textures/gui/card_compact_locked.png", "textures/gui/card_compact_insufficient.png", 119, 66, 16, 16, "minecraft:bow", 1, 10, "tacz:modern_kinetic_gun{GunId:\"hamster:sks\"}"),
        new Kado("gunshop_guns", "item_3", 210, 60, 94, 52, "textures/gui/card_compact_hover.png", "textures/gui/card_compact_pressed.png", "textures/gui/card_compact_locked.png", "textures/gui/card_compact_insufficient.png", 216, 66, 16, 16, "minecraft:trident", 1, 11, "tacz:modern_kinetic_gun{GunId:\"hamster:mp18\"}"),
        new Kado("gunshop_guns", "item_4", 16, 114, 94, 52, "textures/gui/card_compact_hover.png", "textures/gui/card_compact_pressed.png", "textures/gui/card_compact_locked.png", "textures/gui/card_compact_insufficient.png", 22, 120, 16, 16, "minecraft:trident", 1, 12, "tacz:modern_kinetic_gun{GunId:\"tacz:m4a1\"}"),
        new Kado("gunshop_guns", "item_5", 113, 114, 94, 52, "textures/gui/card_compact_hover.png", "textures/gui/card_compact_pressed.png", "textures/gui/card_compact_locked.png", "textures/gui/card_compact_insufficient.png", 119, 120, 16, 16, "minecraft:spyglass", 1, 13, "tacz:modern_kinetic_gun{GunId:\"tacz:ai_awp\"}"),

        // ---- gunshop_ammo ----
        new Kado("gunshop_ammo", "item_1", 16, 60, 94, 76, "textures/gui/card_tall_hover.png", "textures/gui/card_tall_pressed.png", "textures/gui/card_tall_locked.png", "textures/gui/card_tall_insufficient.png", 22, 71, 32, 32, "minecraft:arrow", 30, 9, "tacz:ammo{AmmoId:\"tacz:556x45\"}"),

        // ---- gacha ----

        // ---- bank ----
        new Kado("bank", "bank_1", 16, 48, 94, 76, "textures/gui/card_tall_hover.png", "textures/gui/card_tall_pressed.png", "textures/gui/card_tall_locked.png", "textures/gui/card_tall_insufficient.png", 22, 59, 32, 32, null, 0, -1, null),
        new Kado("bank", "bank_2", 113, 48, 94, 76, "textures/gui/card_tall_hover.png", "textures/gui/card_tall_pressed.png", "textures/gui/card_tall_locked.png", "textures/gui/card_tall_insufficient.png", 119, 59, 32, 32, null, 0, -1, null),
        new Kado("bank", "bank_3", 210, 48, 94, 76, "textures/gui/card_tall_hover.png", "textures/gui/card_tall_pressed.png", "textures/gui/card_tall_locked.png", "textures/gui/card_tall_insufficient.png", 216, 59, 32, 32, null, 0, -1, null),
        new Kado("bank", "bank_4", 16, 132, 94, 76, "textures/gui/card_tall_hover.png", "textures/gui/card_tall_pressed.png", "textures/gui/card_tall_locked.png", "textures/gui/card_tall_insufficient.png", 22, 143, 32, 32, null, 0, -1, null),
        new Kado("bank", "bank_5", 113, 132, 94, 76, "textures/gui/card_tall_hover.png", "textures/gui/card_tall_pressed.png", "textures/gui/card_tall_locked.png", "textures/gui/card_tall_insufficient.png", 119, 143, 32, 32, null, 0, -1, null),
        new Kado("bank", "bank_6", 210, 132, 94, 76, null, null, null, null, 216, 143, 32, 32, null, 0, -1, null),

        // ---- advance ----
        new Kado("advance", "apply", 162, 132, 142, 76, "textures/gui/card_large_hover.png", "textures/gui/card_large_pressed.png", "textures/gui/card_large_locked.png", "textures/gui/card_large_insufficient.png", 171, 145, 32, 32, null, 0, -1, null),

        // ---- declare_war ----
        new Kado("declare_war", "faction_1", 16, 52, 142, 76, "textures/gui/card_danger_hover.png", "textures/gui/card_danger_pressed.png", "textures/gui/card_danger_locked.png", "textures/gui/card_danger_insufficient.png", 25, 61, 32, 32, null, 0, -1, null),
        new Kado("declare_war", "faction_2", 162, 52, 142, 76, "textures/gui/card_danger_hover.png", "textures/gui/card_danger_pressed.png", "textures/gui/card_danger_locked.png", "textures/gui/card_danger_insufficient.png", 171, 61, 32, 32, null, 0, -1, null),
        new Kado("declare_war", "faction_3", 16, 134, 142, 76, "textures/gui/card_danger_hover.png", "textures/gui/card_danger_pressed.png", "textures/gui/card_danger_locked.png", "textures/gui/card_danger_insufficient.png", 25, 143, 32, 32, null, 0, -1, null),
        new Kado("declare_war", "faction_4", 162, 134, 142, 76, "textures/gui/card_danger_hover.png", "textures/gui/card_danger_pressed.png", "textures/gui/card_danger_locked.png", "textures/gui/card_danger_insufficient.png", 171, 143, 32, 32, null, 0, -1, null),

        // ---- shop_weapon ----
        new Kado("shop_weapon", "item_1", 16, 60, 94, 52, "textures/gui/card_compact_hover.png", "textures/gui/card_compact_pressed.png", "textures/gui/card_compact_locked.png", "textures/gui/card_compact_insufficient.png", 22, 66, 16, 16, "minecraft:iron_sword", 1, 9, null),
        new Kado("shop_weapon", "item_2", 113, 60, 94, 52, "textures/gui/card_compact_hover.png", "textures/gui/card_compact_pressed.png", "textures/gui/card_compact_locked.png", "textures/gui/card_compact_insufficient.png", 119, 66, 16, 16, "minecraft:bow", 1, 10, null),
        new Kado("shop_weapon", "item_3", 210, 60, 94, 52, "textures/gui/card_compact_hover.png", "textures/gui/card_compact_pressed.png", "textures/gui/card_compact_locked.png", "textures/gui/card_compact_insufficient.png", 216, 66, 16, 16, "minecraft:arrow", 10, 11, null),
        new Kado("shop_weapon", "item_4", 16, 114, 94, 52, "textures/gui/card_compact_hover.png", "textures/gui/card_compact_pressed.png", "textures/gui/card_compact_locked.png", "textures/gui/card_compact_insufficient.png", 22, 120, 16, 16, "minecraft:diamond_sword", 1, 12, null),
        new Kado("shop_weapon", "item_5", 113, 114, 94, 52, "textures/gui/card_compact_hover.png", "textures/gui/card_compact_pressed.png", "textures/gui/card_compact_locked.png", "textures/gui/card_compact_insufficient.png", 119, 120, 16, 16, "minecraft:crossbow", 1, 13, null),
    };

    /**
     * タブひとつぶん。
     *
     * ★ タブはチェスト画面の 1段目（枠0・枠1）に置かれている。
     *   押したらその枠を左クリックしたことにして送ると、
     *   プラグインが画面を開き直してページが変わる。
     */
    public static final class Tabu {

        public final String gamen;
        /** 何枚目のタブか（0 から） */
        public final int ban;
        public final int x;
        public final int y;
        public final int w;
        public final int h;

        Tabu(String gamen, int ban, int x, int y, int w, int h) {
            this.gamen = gamen;
            this.ban = ban;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public boolean naka(int px, int py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        /** チェスト画面での枠番号。タブは 1段目に左から並ぶ。 */
        public int waku() {
            return ban;
        }
    }

    /** 全部のタブ。13 件。 */
    public static final Tabu[] TABU = {
        new Tabu("shop_life", 0, 20, 38, 62, 18),
        new Tabu("shop_life", 1, 84, 38, 62, 18),
        new Tabu("shop_life", 2, 148, 38, 62, 18),
        new Tabu("shop_armor", 0, 20, 38, 62, 18),
        new Tabu("shop_armor", 1, 84, 38, 62, 18),
        new Tabu("shop_armor", 2, 148, 38, 62, 18),
        new Tabu("gunshop_guns", 0, 20, 38, 62, 18),
        new Tabu("gunshop_guns", 1, 84, 38, 62, 18),
        new Tabu("gunshop_ammo", 0, 20, 38, 62, 18),
        new Tabu("gunshop_ammo", 1, 84, 38, 62, 18),
        new Tabu("shop_weapon", 0, 20, 38, 62, 18),
        new Tabu("shop_weapon", 1, 84, 38, 62, 18),
        new Tabu("shop_weapon", 2, 148, 38, 62, 18),
    };
}
