package jidai.ui;

/**
 * 特殊アイテムの表。★★ tools/tokushu_moderu.py が Shouri.java から作る。手で直さない ★★
 *   { 名前, 絵のファイル名, 見た目の番号 }
 */
public final class Tokushu {

    private Tokushu() {
        // 表を置くだけなので、実体は作らない
    }

    public static final String[][] HYOU = {
            {"古びた護符", "gofu", "8201"},
            {"聖杯の欠片", "seihai", "8202"},
            {"羊皮紙の海図", "kaizu", "8203"},
            {"蒸気機関の歯車", "haguruma", "8204"},
            {"月の石", "tsukinoishi", "8205"},
            {"ヒッタイトの戦車", "sensha", "8211"},
            {"ロンバルディアの鉄王冠", "oukan", "8212"},
            {"聖剣エクスカリバー", "excalibur", "8213"},
            {"テンプル騎士団の盾", "tate", "8214"},
            {"武器商人の手形", "tegata", "8215"},
            {"ベッセマー転炉", "tenro", "8216"},
            {"ロスチャイルドの金庫", "kinko", "8217"},
            {"死の商人の手引書", "tebiki", "8218"},
            {"ペニシリン", "penicillin", "8219"},
            {"暗視ゴーグル", "goggle", "8220"},
            {"スマートフォン", "sumaho", "8221"},
    };

    /** 絵の実寸（HYOU と同じ並び）。MOD はこれを 16×16 に縮めて貼る。 */
    public static final int[][] OOKISA = {
            {64, 64},
            {64, 64},
            {16, 16},
            {16, 16},
            {16, 16},
            {64, 64},
            {64, 64},
            {64, 64},
            {64, 64},
            {64, 64},
            {64, 64},
            {64, 64},
            {64, 64},
            {64, 64},
            {64, 64},
            {64, 64},
    };

    /** 印と色の記号を取り除いた名前。 */
    private static String souji(String namae) {
        String s = namae;
        while (!s.isEmpty() && (s.charAt(0) == '★' || s.charAt(0) == '◆')) {
            s = s.substring(1);
        }
        return s.replaceAll("§.", "").trim();
    }

    /**
     * 名前から絵の道筋を引く。特殊アイテムでなければ null。
     * ★ 頭の等級の印（★ ◆）と色の記号は取り除いてから比べる。
     */
    public static String e(String namae) {
        if (namae == null) {
            return null;
        }
        String s = souji(namae);
        for (String[] r : HYOU) {
            if (r[0].equals(s)) {
                return "textures/item/" + r[1] + ".png";
            }
        }
        return null;
    }

    /** 名前から絵の実寸を引く。知らなければ 16×16。 */
    public static int[] ookisa(String namae) {
        if (namae == null) {
            return new int[]{16, 16};
        }
        String s = souji(namae);
        for (int i = 0; i < HYOU.length; i++) {
            if (HYOU[i][0].equals(s)) {
                return OOKISA[i];
            }
        }
        return new int[]{16, 16};
    }
}
