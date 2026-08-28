package jidai.ui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;

/**
 * 文字が枠に収まるかを、実機の書体で測って見せる画面。
 *
 * ★★ なぜ本番より先にこれを作るか ★★
 *   設計書では、商品名の欄は 62px × 2行しかない。
 *   「コルト M1851 リボルバー」が本当に入るかは、
 *   Minecraft の書体で測るまで【誰にも分からない】。
 *   ここで測らずに本番を書くと、135件ぶん描いたあとで
 *   「はみ出す」と分かって作り直すことになる。
 *
 *   測るのは Minecraft 自身の折り返し（Font#split）。
 *   自分で折り返しを書くと、本体と1文字ずれただけで
 *   「測った時は収まっていたのに、実際ははみ出す」が起きる。
 */
public class SokuteiGamen extends Screen {

    // ── 色（Navigation 案のパレットから）──
    private static final int JI       = 0xF0102329;
    private static final int MOJI     = 0xFFF5E8C8;
    private static final int USUI     = 0xFFAEB8AE;
    private static final int AKA      = 0xFFC15A49;
    private static final int MIDORI   = 0xFF77B789;
    private static final int KIN      = 0xFFC4934B;

    /** Minecraft の1行の高さ。書体の決め打ち。 */
    private static final int GYOU_TAKASA = 9;

    /** 画面に並べられる件数。これを超えたぶんは件数だけ知らせる。 */
    private static final int MISERU = 12;

    /** 測った結果ひとつぶん。 */
    private static final class Kekka {
        final String id;
        final String moji;
        final int gyou;       // 実際に折り返した行数
        final int kagiri;     // 許された行数
        final int hitsuyou;   // 1行で書いた時に要る幅（px・拡大後）
        final int wakuHaba;   // 枠の幅
        final boolean takasaFusoku;

        Kekka(String id, String moji, int gyou, int kagiri,
              int hitsuyou, int wakuHaba, boolean takasaFusoku) {
            this.id = id;
            this.moji = moji;
            this.gyou = gyou;
            this.kagiri = kagiri;
            this.hitsuyou = hitsuyou;
            this.wakuHaba = wakuHaba;
            this.takasaFusoku = takasaFusoku;
        }

        boolean warui() {
            return gyou > kagiri || takasaFusoku;
        }
    }

    private List<Kekka> kekka;
    private int zenbu;
    private int afureta;

    public SokuteiGamen() {
        super(Mc.moji("文字の収まりを測る"));
    }

    /** Screen#isPauseScreen() */
    @Override
    public boolean m_7043_() {
        return false;
    }

    /** Screen#render(GuiGraphics, マウスx, マウスy, 経過) */
    @Override
    public void m_88315_(GuiGraphics g, int mausuX, int mausuY, float keika) {
        m_280273_(g);                       // renderBackground
        if (kekka == null) {
            hakaru();
        }
        egaku(g);
        super.m_88315_(g, mausuX, mausuY, keika);
    }

    /**
     * 135件を測る。1回だけ走ればよいので、結果を持っておく。
     *
     * ★ その場で変わる値（{value} など）は、いちばん長くなる形に置き換えて測る。
     *   短い値で測ると「本番で桁が増えた時だけはみ出す」を見逃す。
     */
    private void hakaru() {
        kekka = new ArrayList<>();
        zenbu = 0;
        afureta = 0;
        for (Hyou.Moji m : Hyou.ZENBU) {
            String s = Hyou.atehameru(m.moji);
            float bai = m.bairitsu();

            // 枠の幅は画面の px。書体は拡大前で測るので、割って渡す。
            int mojiHaba = Math.max(1, Math.round(m.w / bai));
            List<FormattedCharSequence> gyou = Mc.oru(s, mojiHaba);

            int hitsuyou = Math.round(Mc.haba(s) * bai);
            boolean takasaFusoku = Math.round(m.gyouSuu * GYOU_TAKASA * bai) > m.h;

            Kekka k = new Kekka(m.gamen + "/" + m.id, s, gyou.size(), m.gyouSuu,
                                hitsuyou, m.w, takasaFusoku);
            zenbu++;
            if (k.warui()) {
                afureta++;
                kekka.add(k);
            }
            // ログにも全件 出す。画面に載りきらないぶんはここで読める。
            System.out.println(String.format(
                    "[時代クラフトUI] %-28s %-2d/%-2d行 幅%3d/%3d %s  %s",
                    k.id, k.gyou, k.kagiri, k.hitsuyou, k.wakuHaba,
                    k.warui() ? "★あふれる" : "ok", s));
        }
        System.out.println("[時代クラフトUI] 測定おわり: 全 " + zenbu
                + " 件 / あふれ " + afureta + " 件");
    }

    private void egaku(GuiGraphics g) {
        int haba = 300;
        int x0 = (Mc.gamenHaba(this) - haba) / 2;
        int y0 = 16;

        Mc.nuru(g, x0 - 6, y0 - 6, x0 + haba + 6, Mc.gamenTakasa(this) - 10, JI);

        Mc.kakuOokiku(g, "文字の収まりを測りました", x0, y0, MOJI, 1.5F);

        String matome = "全 " + zenbu + " 件 / あふれ " + afureta + " 件";
        Mc.kaku(g, matome, x0, y0 + 20, afureta == 0 ? MIDORI : AKA);

        if (afureta == 0) {
            Mc.kakuOokiku(g, "すべて枠に収まりました", x0, y0 + 40, MIDORI, 1.5F);
            Mc.kaku(g, "設計どおりに描いて問題ありません。", x0, y0 + 62, USUI);
            Mc.kaku(g, "内訳は latest.log にも出しています。", x0, y0 + 74, USUI);
            return;
        }

        Mc.kaku(g, "はみ出すもの（行数 実際/上限・幅 必要/枠）", x0, y0 + 36, KIN);
        int y = y0 + 50;
        int dashita = 0;
        for (Kekka k : kekka) {
            if (dashita >= MISERU) {
                break;
            }
            String migi = k.gyou + "/" + k.kagiri + "行  " + k.hitsuyou + "/" + k.wakuHaba + "px"
                    + (k.takasaFusoku ? "  高さ不足" : "");
            Mc.kaku(g, k.id, x0, y, USUI);
            Mc.kaku(g, migi, x0 + haba - Mc.haba(migi), y, AKA);
            Mc.kaku(g, k.moji, x0 + 8, y + 10, MOJI);
            y += 22;
            dashita++;
        }
        if (kekka.size() > MISERU) {
            Mc.kaku(g, "……ほか " + (kekka.size() - MISERU) + " 件。全部は latest.log に出ています。",
                    x0, y, USUI);
        }
    }
}
