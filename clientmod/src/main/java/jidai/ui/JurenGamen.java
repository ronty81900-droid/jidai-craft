package jidai.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 十連ガチャの結果を、古地図の顔で見せる画面。
 *
 * ★★ なぜ絵を頼まずに書けるか ★★
 *   販売所の画面は Codex に描いてもらった下地の絵を貼っているが、
 *   ここは【線と塗りだけ】で組んである。掲示板（Keiji）と同じやり方。
 *   絵を待たずに出せて、後から下地の絵に差し替えることもできる。
 *
 * ★★ サーバーとの新しいやり取りは一切していない ★★
 *   プラグインは 27枠のチェスト画面を開くだけ。
 *   こちらは【その中身を読んで描き直している】。
 *   MOD を入れていない人には、素のチェスト画面として
 *   同じ10個がちゃんと並ぶ。
 *
 * ★★ 中身は毎フレーム読み直すこと ★★
 *   画面が開いた その瞬間は、まだ中身が届いていない。
 *   サーバーは「画面を開け」の合図と「中身」を別々に送るため。
 *   開いた時に1回だけ読むと、いつまでも空の画面になる。
 *   （販売所のタブで同じ間違いをして、実機で気づいた）
 */
public class JurenGamen extends Screen {

    // ★ 色と板・枠・ボタンは Kazari が持つ。ここでは決めない
    //   （画面が増えた時に、直し忘れた画面だけ浮くのを防ぐ）

    /**
     * 結果の枠の並び。★ プラグインの Gacha.JUREN_WAKU と同じ値。
     *   食い違うと、この画面だけ空になる。GachaTest が両方を突き合わせる。
     */
    static final int[] WAKU = {2, 3, 4, 5, 6, 11, 12, 13, 14, 15};

    /** 下段のボタンの枠。★ こちらも Gacha と同じ値。 */
    static final int MOUICHIDO = 21;
    static final int TOJIRU = 23;

    /** 等級の印。★ プラグインが名前の頭に付ける（Gacha.SHIRUSHI_*）。 */
    private static final String SHIRUSHI_DAI = "★";
    private static final String SHIRUSHI_CHU = "◆";

    /** ひと枠の大きさ。 */
    private static final int MASU_HABA = 46;
    private static final int MASU_TAKASA = 40;

    /** 横に並べる数。10個を 5×2 で置く。 */
    private static final int YOKO = 5;

    /** サーバーが開いているチェスト画面。 */
    private final AbstractContainerMenu menu;

    /** ボタンの当たり判定。描く時に入れて、押された時に使う。 */
    private int mouichidoX, mouichidoY, mouichidoW, mouichidoH;
    private int tojiruX, tojiruY, tojiruW, tojiruH;

    public JurenGamen(AbstractContainerMenu menu) {
        super(Mc.moji("十連の結果"));
        this.menu = menu;
    }

    /** Screen#isPauseScreen。開いても時間は止めない（マルチなので当然）。 */
    @Override
    public boolean m_7043_() {
        return false;
    }

    /** Screen#render */
    @Override
    public void m_88315_(GuiGraphics g, int mx, int my, float bu) {
        m_280273_(g);                       // Screen#renderBackground（暗くする）

        int haba = MASU_HABA * YOKO + 16;
        int takasa = 22 + MASU_TAKASA * 2 + 26;
        int x1 = (Mc.gamenHaba(this) - haba) / 2;
        int y1 = (Mc.gamenTakasa(this) - takasa) / 2;

        Kazari.ita(g, x1, y1, haba, takasa);
        Kazari.midashi(g, x1, y1, haba, "十連の結果");

        // ── 10個 ──
        int atari = 0;
        for (int i = 0; i < WAKU.length; i++) {
            int cx = x1 + 8 + (i % YOKO) * MASU_HABA;
            int cy = y1 + 22 + (i / YOKO) * MASU_TAKASA;
            ItemStack mono = Mc.wakuNoMono(menu, WAKU[i]);
            if (hitotsu(g, cx, cy, mono)) {
                atari++;
            }
        }

        // ── 下の帯（当たりの数とボタン）──
        int shita = y1 + 22 + MASU_TAKASA * 2 + 4;
        Mc.nuru(g, x1 + 8, shita, x1 + haba - 8, shita + 1, Kazari.SHINCHU);

        String kazu = "当たり " + atari + " / 10";
        Mc.kaku(g, kazu, x1 + 10, shita + 7, Kazari.USUI);

        // ★★ ボタンは、サーバーが枠に置いてから出す ★★
        //   めくっている最中はサーバー側に無い（押しても効かない）。
        //   無いのに描くと「押したのに反応しない」に見える。
        ItemStack m1 = Mc.wakuNoMono(menu, MOUICHIDO);
        ItemStack m2 = Mc.wakuNoMono(menu, TOJIRU);
        boolean botanAri = m1 != null && !m1.m_41619_() && !Mc.mikaikinKa(m1)
                && m2 != null && !m2.m_41619_() && !Mc.mikaikinKa(m2);

        mouichidoW = Mc.haba("もう一度 十連") + 12;
        mouichidoH = 14;
        mouichidoX = x1 + haba - 10 - mouichidoW - 8 - (Mc.haba("閉じる") + 12);
        mouichidoY = shita + 4;
        tojiruW = Mc.haba("閉じる") + 12;
        tojiruH = 14;
        tojiruX = x1 + haba - 10 - tojiruW;
        tojiruY = shita + 4;

        if (botanAri) {
            Kazari.botan(g, mouichidoX, mouichidoY, mouichidoW, mouichidoH, "もう一度 十連",
                    Kazari.atari(mx, my, mouichidoX, mouichidoY, mouichidoW, mouichidoH));
            Kazari.botan(g, tojiruX, tojiruY, tojiruW, tojiruH, "閉じる",
                    Kazari.atari(mx, my, tojiruX, tojiruY, tojiruW, tojiruH));
        } else {
            // めくっている最中。当たり判定も無しにする
            mouichidoW = 0;
            tojiruW = 0;
            String ima = "めくっています…";
            Mc.kaku(g, ima, x1 + haba - 10 - Mc.haba(ima), shita + 7, Kazari.USUI);
        }
    }

    /**
     * 結果ひとつぶんを描く。大当たり（★か◆）なら true を返す。
     *
     * ★ 枠が空でも落ちない。中身が届く前の1〜2フレームは必ず空になる。
     */
    private boolean hitotsu(GuiGraphics g, int x, int y, ItemStack mono) {
        Mc.nuru(g, x, y, x + MASU_HABA - 4, y + MASU_TAKASA - 4, Kazari.MASU);

        if (mono == null || mono.m_41619_()) {
            return false;
        }
        // ★ まだめくっていない札（灰色の板ガラス）。大きな「？」だけ出す
        if (Mc.mikaikinKa(mono)) {
            Mc.kakuOokiku(g, "？", x + (MASU_HABA - 4) / 2 - 6, y + (MASU_TAKASA - 4) / 2 - 9,
                    Kazari.USUI, 2.0f);
            return false;
        }
        String namae = mono.m_41786_().getString();      // ItemStack#getHoverName

        int iro = Kazari.MOJI;
        boolean ooatari = false;
        if (namae.startsWith(SHIRUSHI_DAI)) {
            iro = Kazari.KIN;
            ooatari = true;
            namae = namae.substring(SHIRUSHI_DAI.length());
        } else if (namae.startsWith(SHIRUSHI_CHU)) {
            iro = Kazari.AOI;
            ooatari = true;
            namae = namae.substring(SHIRUSHI_CHU.length());
        }
        if (ooatari) {
            // 当たりの枠だけ、縁を光らせる
            Kazari.waku(g, x, y, MASU_HABA - 4, MASU_TAKASA - 4, iro);
        }

        // 絵は枠の上寄せ・中央
        int ex = x + (MASU_HABA - 4 - 16) / 2;
        Kazari.aitem(g, mono, ex, y + 3);

        // 名前は下に2行まで。★ 折り返しは Minecraft 自身に任せる
        Kazari.oriteKaku(g, namae, x, y + 21, MASU_HABA - 4, 2, iro);
        return ooatari;
    }

    /** GuiEventListener#mouseClicked */
    @Override
    public boolean m_6375_(double x, double y, int botan) {
        int mx = (int) x;
        int my = (int) y;
        if (Kazari.atari(mx, my, mouichidoX, mouichidoY, mouichidoW, mouichidoH)) {
            Mc.wakuWoOsu(menu, MOUICHIDO);
            return true;
        }
        if (Kazari.atari(mx, my, tojiruX, tojiruY, tojiruW, tojiruH)) {
            Mc.wakuWoOsu(menu, TOJIRU);
            return true;
        }
        return super.m_6375_(x, y, botan);
    }

    /**
     * Screen#keyPressed。
     * ★ 販売所と同じく、持ち物のキー（既定は E）でも閉じられるようにする。
     */
    @Override
    public boolean m_7933_(int kii, int sukyan, int shuushoku) {
        if (Mc.shimauKiKa(kii, sukyan)) {
            Mc.tojiru();
            return true;
        }
        return super.m_7933_(kii, sukyan, shuushoku);
    }

    /** Screen#onClose。閉じる時はサーバーにも伝える。 */
    @Override
    public void m_7379_() {
        Mc.tojiru();
    }
}
