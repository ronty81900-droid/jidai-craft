package jidai.ui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * 古地図の顔を描くための、共通の部品。
 *
 * ★★ なぜ1か所にまとめるか ★★
 *   十連の結果・ガチャ・銀行・時代を進める・宣戦 ── 画面が増えるたびに
 *   同じ「板」「枠線」「ボタン」を書き写していくと、
 *   色を1つ変えたい時に**直し忘れた画面だけ浮く**。
 *   見た目の決まりごとは、ここだけを見れば分かるようにする。
 *
 * ★ 座標はすべて画面の実寸（論理px）。倍率はここでは扱わない。
 */
public final class Kazari {

    private Kazari() {
        // 部品置き場なので、実体は作らない
    }

    // ── 色（Navigation 案のパレット）──
    /** いちばん外の地。 */
    public static final int SOTO = 0xFF102329;
    /** 板の面。少しだけ透ける。 */
    public static final int JI = 0xF017313A;
    /** 真鍮。枠と罫。 */
    public static final int SHINCHU = 0xFFC4934B;
    /** ふつうの字。 */
    public static final int MOJI = 0xFFF5E8C8;
    /** 補助の字。 */
    public static final int USUI = 0xFFAEB8AE;
    /** 見出し。 */
    public static final int MIZU = 0xFF4FA1B2;
    /** ひと枠の底。 */
    public static final int MASU = 0xFF0E1D22;
    /** 大当たり。 */
    public static final int KIN = 0xFFE8B44A;
    /** 中当たり。 */
    public static final int AOI = 0xFF6FD3E0;

    /** Minecraft の1行の高さ。書体の決め打ち。 */
    public static final int GYOU = 9;

    /** 1px の枠線。 */
    public static void waku(GuiGraphics g, int x, int y, int w, int h, int iro) {
        Mc.nuru(g, x, y, x + w, y + 1, iro);
        Mc.nuru(g, x, y + h - 1, x + w, y + h, iro);
        Mc.nuru(g, x, y, x + 1, y + h, iro);
        Mc.nuru(g, x + w - 1, y, x + w, y + h, iro);
    }

    /**
     * 台紙。四隅に測量の飾りを付ける（古地図らしさ）。
     */
    public static void ita(GuiGraphics g, int x, int y, int w, int h) {
        Mc.nuru(g, x, y, x + w, y + h, SOTO);
        Mc.nuru(g, x + 1, y + 1, x + w - 1, y + h - 1, JI);
        waku(g, x, y, w, h, SHINCHU);
        for (int[] d : new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}}) {
            int cx = (d[0] == 0) ? x + 2 : x + w - 8;
            int cy = (d[1] == 0) ? y + 2 : y + h - 8;
            Mc.nuru(g, cx, cy, cx + 6, cy + 1, SHINCHU);
            Mc.nuru(g, cx, cy, cx + 1, cy + 6, SHINCHU);
        }
    }

    /** 見出しと、その下の罫。見出しの下端の y を返す。 */
    public static int midashi(GuiGraphics g, int x, int y, int w, String s) {
        Mc.kaku(g, s, x + (w - Mc.haba(s)) / 2, y + 7, MIZU);
        Mc.nuru(g, x + 8, y + 18, x + w - 8, y + 19, SHINCHU);
        return y + 19;
    }

    /** 押しボタン。 */
    public static void botan(GuiGraphics g, int x, int y, int w, int h, String moji,
                             boolean ue) {
        Mc.nuru(g, x, y, x + w, y + h, ue ? SHINCHU : MASU);
        waku(g, x, y, w, h, SHINCHU);
        Mc.kaku(g, moji, x + (w - Mc.haba(moji)) / 2, y + (h - GYOU) / 2 + 1,
                ue ? SOTO : MOJI);
    }

    /** その点が四角の中か。 */
    public static boolean atari(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /**
     * 説明の吹き出し。
     *
     * ★ 値段や条件は、プラグインがアイテムの説明文に書いている。
     *   古地図の画面では文字を並べる余地が無いので、
     *   カーソルを乗せた時だけ ここに出す。
     *
     * ★ 画面からはみ出さないように、右や下で折り返す。
     */
    public static void fukidashi(GuiGraphics g, List<String> gyou, int mx, int my,
                                 int gamenHaba, int gamenTakasa) {
        if (gyou == null || gyou.isEmpty()) {
            return;
        }
        int haba = 0;
        for (String s : gyou) {
            haba = Math.max(haba, Mc.haba(s));
        }
        haba += 10;
        int takasa = 6 + gyou.size() * GYOU;

        int x = mx + 10;
        int y = my - 6;
        if (x + haba > gamenHaba) {
            x = mx - 10 - haba;
        }
        if (x < 2) {
            x = 2;
        }
        if (y + takasa > gamenTakasa) {
            y = gamenTakasa - takasa - 2;
        }
        if (y < 2) {
            y = 2;
        }

        Mc.nuru(g, x, y, x + haba, y + takasa, SOTO);
        Mc.nuru(g, x + 1, y + 1, x + haba - 1, y + takasa - 1, JI);
        waku(g, x, y, haba, takasa, SHINCHU);
        int ty = y + 4;
        boolean atama = true;
        for (String s : gyou) {
            Mc.kaku(g, s, x + 5, ty, atama ? MOJI : USUI);
            atama = false;
            ty += GYOU;
        }
    }

    /** 私用領域の始まりと終わり。ここに入る文字は「絵」なので描かない。 */
    private static final char SHIYOU_HAJIME = 0xE000;
    private static final char SHIYOU_OWARI = 0xF8FF;

    /**
     * 見出しから、リソースパックの絵の文字を取り除く。
     *
     * ★★ なぜ要るか ★★
     *   `jidai ui on` にすると、画面の見出しが【絵そのものの文字】になる。
     *   （私用領域 U+E000〜U+F8FF の文字。リソースパックが絵を割り当てている）
     *   そのまま描くと、こちらの画面では「□」に見える。
     */
    public static String midashiSouji(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= SHIYOU_HAJIME && c <= SHIYOU_OWARI) {
                continue;               // 絵の文字
            }
            b.append(c);
        }
        return b.toString().trim();
    }

    /**
     * アイテムの絵を1つ描く。
     *
     * ★★ 特殊アイテムは【名前で】見分けて、絵を直接貼る ★★
     *   バニラの描画（renderItem）は paper.json の対応表が効いて初めて
     *   特殊アイテムの絵になる。こちらの画面では対応表に頼らず、
     *   Tokushu の表（Shouri.java から作る）で名前から絵を引いて貼る。
     *   「ガチャ上では紙のまま」（2026-08-22 のご指摘）を、
     *   対応表の効き方に左右されずに潰すため。
     */
    public static void aitem(GuiGraphics g, ItemStack mono, int x, int y) {
        if (mono == null || mono.m_41619_()) {
            return;
        }
        String namae = mono.m_41786_().getString();              // ItemStack#getHoverName
        String e = Tokushu.e(namae);
        if (e != null) {
            int[] o = Tokushu.ookisa(namae);
            Mc.haruOokisa(g, e, x, y, 16, 16, o[0], o[1]);
            return;
        }
        Mc.aitemWoEgaku(g, mono, x, y);
        Mc.aitemNoKazu(g, mono, x, y);
    }

    /** 文字を、幅に収まるように折って描く。使った行数を返す。 */
    public static int oriteKaku(GuiGraphics g, String s, int x, int y, int haba,
                                int gyouSuu, int iro) {
        List<FormattedCharSequence> gyou = Mc.oru(s, haba);
        int n = Math.min(gyou.size(), gyouSuu);
        for (int i = 0; i < n; i++) {
            FormattedCharSequence r = gyou.get(i);
            Mc.kakuGyou(g, r, x + (haba - Mc.habaGyou(r)) / 2, y + i * GYOU, iro);
        }
        return n;
    }
}
