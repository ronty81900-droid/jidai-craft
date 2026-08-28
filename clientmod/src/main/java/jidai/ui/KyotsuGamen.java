package jidai.ui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * ガチャ・銀行・時代を進める・宣戦を、古地図の顔で描く共通の画面。
 *
 * ★★ なぜ画面ごとに作らないか（2026-08-22「すべて古地図化」）★★
 *   この4つは、作りがどれも同じ:
 *     「名前と説明の付いたアイテムが数個ならぶ。押すと何かが起きる」
 *   1画面ずつ専用に書くと、同じ物を4回書くうえ、
 *   プラグイン側で枠を1つ足すたびに MOD も直すことになる。
 *
 *   そこで**中身を読んで、そのまま並べる**形にした。
 *   プラグインが枠を足しても、MOD は何も直さずに追随する。
 *
 * ★★ 値段や条件は「説明文」から取る ★★
 *   プラグインは値段・必要量・残高をアイテムの説明文に書いている。
 *   古地図の板に全部を並べる余地は無いので、
 *   カーソルを乗せた時だけ吹き出しで出す。
 *
 * ★★ 中身は毎フレーム読み直す ★★
 *   画面が開いた その瞬間は、まだ中身が届いていない
 *   （「画面を開け」と「中身」は別々に送られてくる）。
 *   開いた時に1回だけ読むと、いつまでも空の画面になる。
 */
public class KyotsuGamen extends Screen {

    /** チェストの1段の枠数。 */
    private static final int DAN = 9;

    /**
     * ひと枠の大きさ。横は画面に合わせて縮む。
     *
     * ★★ 下限を 44 にしていて溢れた ★★
     *   Minecraft の自動 GUI 倍率では、画面の論理幅が 320 まで小さくなる。
     *   9枠 × 44 = 396 で、320 に収まらない。板だけ画面幅に丸めていたので、
     *   **枠が板からはみ出す**形だった（掲示板と同じ轍）。
     *   絵は 16px なので、最低 20 あれば置ける。そこまで縮めてよい。
     */
    private static final int MASU_HIROI = 68;
    private static final int MASU_SEBAI = 20;
    private static final int MASU_TAKASA = 44;

    /** 回転帯として描くかの目安。これ以上 埋まっていたら「回っている」。 */
    private static final int MAWATTERU = 3;

    private final AbstractContainerMenu menu;
    private final String midashi;

    /**
     * 1段目を回転帯として描いてよい画面か（＝ガチャ）。
     * ★ 銀行などで帯にすると、ただ横に伸びるだけで読みにくい。
     */
    private final boolean obiAri;

    /** 枠ごとの当たり判定。毎フレーム作り直す。 */
    private final List<int[]> atari = new ArrayList<>();

    public KyotsuGamen(AbstractContainerMenu menu, String midashi, boolean obiAri) {
        super(Mc.moji(midashi));
        this.menu = menu;
        this.midashi = midashi;
        this.obiAri = obiAri;
    }

    /** Screen#isPauseScreen */
    @Override
    public boolean m_7043_() {
        return false;
    }

    /** その段に物が入っている枠の番号。 */
    private List<Integer> danNoWaku(int dan, int kazu) {
        List<Integer> r = new ArrayList<>();
        for (int i = dan * DAN; i < Math.min((dan + 1) * DAN, kazu); i++) {
            ItemStack mono = Mc.wakuNoMono(menu, i);
            if (mono != null && !mono.m_41619_()) {
                r.add(i);
            }
        }
        return r;
    }

    /** Screen#render */
    @Override
    public void m_88315_(GuiGraphics g, int mx, int my, float bu) {
        m_280273_(g);                       // Screen#renderBackground
        atari.clear();

        int kazu = (menu == null) ? 0 : menu.m_38927_().size();
        // ★ 下段（自分の持ち物 36枠）は描かない。上の画面ぶんだけ。
        int uwadan = Math.max(0, kazu - 36);
        int danSuu = Math.max(1, (uwadan + DAN - 1) / DAN);

        // ── 1段目がタブなら、タブとして上に描く ──
        //   ★ プラグインは、選ばれているタブの名前の頭に「▶」を付ける（Shop.tabItem）。
        //     1段目にそれが1つでもあれば、その段はタブの列。
        List<Integer> tabu = new ArrayList<>();
        for (int i : danNoWaku(0, uwadan)) {
            ItemStack mono = Mc.wakuNoMono(menu, i);
            if (mono != null && !mono.m_41619_()
                    && mono.m_41786_().getString().startsWith("▶")) {
                tabu = danNoWaku(0, uwadan);
                break;
            }
        }

        // ── 段ごとに、何をどう並べるか決める ──
        List<List<Integer>> dan = new ArrayList<>();
        for (int d = tabu.isEmpty() ? 0 : 1; d < danSuu; d++) {
            List<Integer> w = danNoWaku(d, uwadan);
            // ★ ガチャは、まわしている間 2段目（十連ボタン）を引っ込める。
            //   空の段を詰めると そのたびに板の高さが変わって落ち着かないので、
            //   回転帯のある画面では空の段も場所だけ取っておく。
            if (!w.isEmpty() || obiAri) {
                dan.add(w);
            }
        }
        if (dan.isEmpty()) {
            dan.add(new ArrayList<>());     // 中身が届く前。枠だけ出しておく
        }

        int ooi = 1;
        for (List<Integer> w : dan) {
            ooi = Math.max(ooi, w.size());
        }
        // ★ ガチャは回っている間だけ 9枠 埋まる。そのたびに板の大きさが
        //   変わると、画面が伸び縮みして落ち着かない。always 9枠ぶん取る。
        if (obiAri) {
            ooi = Math.max(ooi, DAN);
        }

        int gamenHaba = Mc.gamenHaba(this);
        int gamenTakasa = Mc.gamenTakasa(this);

        // ★ 画面に収まるように、ひと枠の横幅を決める。
        //   ★ 板の方を丸めてはいけない。板だけ縮めても枠は縮まないので、
        //     枠が板からはみ出す。先に枠を決めて、板をそれに合わせる。
        int masuHaba = Math.max(MASU_SEBAI,
                Math.min(MASU_HIROI, (gamenHaba - 40) / ooi));

        int tabuTakasa = tabu.isEmpty() ? 0 : 18;
        int haba = Math.max(masuHaba * ooi + 16, tabu.isEmpty() ? 0 : tabu.size() * 52 + 16);
        int takasa = 24 + tabuTakasa + dan.size() * MASU_TAKASA + 10;
        int x1 = (gamenHaba - haba) / 2;
        int y1 = (gamenTakasa - takasa) / 2;

        Kazari.ita(g, x1, y1, haba, takasa);
        Kazari.midashi(g, x1, y1, haba, midashi);

        int y = y1 + 24;

        // ── タブ ──
        if (!tabu.isEmpty()) {
            int tw = (haba - 16 - (tabu.size() - 1) * 4) / tabu.size();
            int tx = x1 + 8;
            for (int waku : tabu) {
                ItemStack mono = Mc.wakuNoMono(menu, waku);
                String na = mono.m_41786_().getString();
                boolean erabu = na.startsWith("▶");
                String moji = erabu ? na.substring(1).trim() : na;
                boolean ue = Kazari.atari(mx, my, tx, y, tw, 14);
                Kazari.botan(g, tx, y, tw, 14, moji, erabu || ue);
                atari.add(new int[]{waku, tx, y, tw, 14});
                tx += tw + 4;
            }
            y += tabuTakasa;
        }
        for (List<Integer> w : dan) {
            boolean obi = obiAri && w.size() >= MAWATTERU;
            hitoDan(g, w, x1, y, haba, masuHaba, obi, mx, my);
            y += MASU_TAKASA;
        }

        // ── カーソルの下にある物の説明 ──
        //   ★ 板や枠を全部 描き終えてから出す。先に出すと下敷きになる。
        for (int[] a : atari) {
            if (Kazari.atari(mx, my, a[1], a[2], a[3], a[4])) {
                Kazari.fukidashi(g, Mc.setsumei(Mc.wakuNoMono(menu, a[0])),
                        mx, my, gamenHaba, gamenTakasa);
                break;
            }
        }
    }

    /** 1段ぶんを並べる。 */
    private void hitoDan(GuiGraphics g, List<Integer> waku, int x1, int y,
                         int haba, int masuHaba, boolean obi, int mx, int my) {
        if (waku.isEmpty()) {
            return;
        }
        int zenHaba = masuHaba * waku.size();
        int x = x1 + (haba - zenHaba) / 2;

        if (obi) {
            // ★ ガチャが回っている間。真ん中で止まるので、そこを指し示す。
            Kazari.waku(g, x - 2, y - 1, zenHaba + 4, MASU_TAKASA - 2, Kazari.SHINCHU);
        }

        for (int i = 0; i < waku.size(); i++) {
            int cx = x + i * masuHaba;
            boolean mannaka = obi && (waku.get(i) % DAN == 4);
            hitotsu(g, waku.get(i), cx, y, masuHaba, mannaka, mx, my);
            atari.add(new int[]{waku.get(i), cx, y, masuHaba, MASU_TAKASA - 4});
        }
    }

    /** ひと枠ぶん。 */
    private void hitotsu(GuiGraphics g, int wakuBan, int x, int y, int w,
                         boolean mannaka, int mx, int my) {
        ItemStack mono = Mc.wakuNoMono(menu, wakuBan);
        boolean ue = Kazari.atari(mx, my, x, y, w, MASU_TAKASA - 4);

        Mc.nuru(g, x + 2, y, x + w - 2, y + MASU_TAKASA - 4, Kazari.MASU);
        if (mannaka) {
            Kazari.waku(g, x + 2, y, w - 4, MASU_TAKASA - 4, Kazari.KIN);
        } else if (ue) {
            Kazari.waku(g, x + 2, y, w - 4, MASU_TAKASA - 4, Kazari.SHINCHU);
        }
        if (mono == null || mono.m_41619_()) {
            return;
        }

        int ex = x + (w - 16) / 2;
        Kazari.aitem(g, mono, ex, y + 3);

        // ★ 未開放の枠（灰色の板ガラス）は名前を出さない。
        //   販売所と同じ考え方で「？？」にする。
        String namae = Mc.mikaikinKa(mono) ? "？？" : mono.m_41786_().getString();
        Kazari.oriteKaku(g, namae, x + 3, y + 22, w - 6, 2,
                ue ? Kazari.MOJI : Kazari.USUI);
    }

    /** GuiEventListener#mouseClicked */
    @Override
    public boolean m_6375_(double x, double y, int botan) {
        int mx = (int) x;
        int my = (int) y;
        for (int[] a : atari) {
            if (Kazari.atari(mx, my, a[1], a[2], a[3], a[4])) {
                Mc.wakuWoOsu(menu, a[0]);
                return true;
            }
        }
        return super.m_6375_(x, y, botan);
    }

    /**
     * Screen#keyPressed。
     * ★ 持ち物のキー（既定は E）でも閉じられるようにする。販売所と同じ。
     */
    @Override
    public boolean m_7933_(int kii, int sukyan, int shuushoku) {
        if (Mc.shimauKiKa(kii, sukyan)) {
            Mc.tojiru();
            return true;
        }
        return super.m_7933_(kii, sukyan, shuushoku);
    }

    /** Screen#onClose */
    @Override
    public void m_7379_() {
        Mc.tojiru();
    }
}
