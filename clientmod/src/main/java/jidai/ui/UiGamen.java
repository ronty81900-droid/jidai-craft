package jidai.ui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 買い物などの画面を、設計どおりに描く。
 *
 * ★★ 描く順番（設計書の layer_order のとおり）★★
 *   1. 下地の絵（`page_*.png`）。ふつうの状態のカードまで、この1枚に描かれている
 *   2. 状態が変わったカードだけ、その位置へ上から貼り替える
 *   3. 文字
 *
 *   毎フレーム部品を組み立て直さないので軽く、作りも素直になる。
 *
 * ★ 座標はすべて【設計の 320x240 の中の値】。
 *   画面の実寸に合わせるのは、いちばん外側でまとめて拡大するだけ。
 *   個々の座標に倍率を掛けて回ると、必ずどこかで掛け忘れる。
 */
public class UiGamen extends Screen {

    /** 設計の大きさ。すべての座標はこの中の値。 */
    private static final int SEKKEI_HABA = 320;
    private static final int SEKKEI_TAKASA = 240;

    /** Minecraft の1行の高さ。書体の決め打ち。 */
    private static final int GYOU_TAKASA = 9;

    // ── GLFW のキー番号 ──
    private static final int KEY_HIDARI = 263;
    private static final int KEY_MIGI = 262;

    /** いま何枚目の画面を出しているか（Hyou.GAMEN の番号） */
    private int ima;

    /** カーソルが乗っているカードの名前。乗っていなければ null */
    private String kasoruNoKado;

    /**
     * サーバーが開いているチェスト画面。
     *
     * ★ null なら「ただ絵を見ているだけ」（J キーで開いた時）。
     *   その時は買えないし、左右の矢印で全画面を見て回れる。
     * ★ null でなければ本物の店。押すと本当に買う。
     */
    private final AbstractContainerMenu menu;

    /** 絵だけを見る時（J キー）。 */
    public UiGamen(int ima) {
        super(Mc.moji("時代クラフト"));
        this.shurui = -1;
        this.menu = null;
        this.ima = ima;
    }

    /**
     * 店の種類。0=販売所 / 1=銃器専門店 / -1=絵を見ているだけ。
     * ★ ページ番号ではなく種類を持つ。ページはタブで変わるので、
     *   毎フレーム 中身から求め直す（下の m_88315_ を参照）。
     */
    private final int shurui;

    /** サーバーが開いた店に被せる時。 */
    public UiGamen(int shurui, AbstractContainerMenu menu) {
        super(Mc.moji("時代クラフト"));
        this.shurui = shurui;
        this.menu = menu;
        this.ima = 0;
    }

    /** 本物の店に被さっているか。 */
    private boolean honmono() {
        return menu != null;
    }

    /** Screen#isPauseScreen() ── 開いてもゲームを止めない */
    @Override
    public boolean m_7043_() {
        return false;
    }

    /**
     * 何倍で描くか。
     *
     * ★★ 整数倍だけにする ★★
     *   1.5倍のような半端な倍率だと、ドット絵の1画素が1.5画素に伸びて
     *   線の太さがまだらになる。設計は 320x240 で、Minecraft の
     *   自動スケールはここを下回らないので、1倍なら必ず収まる。
     *   広い時だけ2倍・3倍にする。
     */
    private int bairitsu() {
        int yoko = Mc.gamenHaba(this) / SEKKEI_HABA;
        int tate = Mc.gamenTakasa(this) / SEKKEI_TAKASA;
        return Math.max(1, Math.min(yoko, tate));
    }

    /** Screen#render(GuiGraphics, マウスx, マウスy, 経過) */
    @Override
    public void m_88315_(GuiGraphics g, int mausuX, int mausuY, float keika) {
        m_280273_(g);                       // renderBackground ── 後ろを暗くする

        // ★★ どのタブを見ているかは【毎フレーム 求め直す】★★
        //   画面が開いた瞬間は、まだ中身が届いていない。
        //   サーバーは「画面を開け」の合図と「中身」を別々に送るため、
        //   開いた時に1回だけ見ると、タブの印がまだ無くて必ず1枚目になる。
        //   （実機で「防具タブにしても見出しが生活のまま」になった原因）
        if (honmono()) {
            int p = Tsunagu.pageBan(shurui, menu);
            if (p >= 0) {
                ima = p;
            }
        }

        int bai = bairitsu();
        int ox = (Mc.gamenHaba(this) - SEKKEI_HABA * bai) / 2;
        int oy = (Mc.gamenTakasa(this) - SEKKEI_TAKASA * bai) / 2;

        // マウスの位置も、設計の座標へ直しておく。
        // ★ ここを直し忘れると、拡大した時だけ当たり判定がずれる。
        int sx = (mausuX - ox) / bai;
        int sy = (mausuY - oy) / bai;

        Mc.tsumu(g, ox, oy, bai);
        egaku(g, sx, sy);
        Mc.orosu(g);

        annai(g);
        super.m_88315_(g, mausuX, mausuY, keika);
    }

    private void egaku(GuiGraphics g, int sx, int sy) {
        Hyou.Gamen gm = Hyou.GAMEN[ima];

        // 1) 下地
        Mc.haru(g, gm.shita, 0, 0, SEKKEI_HABA, SEKKEI_TAKASA);

        // 2) 状態が変わったカードを貼り替える
        //    ★ 未解禁かどうかは【サーバーが開いた画面の中身】を見て決める。
        //      プラグインが灰色の板ガラスで塞いでいるので、条件を二重に持たない。
        kasoruNoKado = null;
        for (Hyou.Kado k : Hyou.KADO) {
            if (!k.gamen.equals(gm.id)) {
                continue;
            }
            boolean kasoru = k.naka(sx, sy);
            if (kasoru) {
                kasoruNoKado = k.id;
            }
            String moto = null;
            if (honmono() && Mc.mikaikinKa(Mc.wakuNoMono(menu, k.waku))) {
                moto = k.mikaikin;
            } else if (kasoru) {
                moto = k.kasoru;
            }
            if (moto != null) {
                Mc.haru(g, moto, k.x, k.y, k.w, k.h);
            }
        }

        // 3) アイテムの絵（ゲームに描かせる）
        for (Hyou.Kado k : Hyou.KADO) {
            if (k.gamen.equals(gm.id) && k.aitem != null) {
                aikon(g, k);
            }
        }

        // 4) 文字
        for (Hyou.Moji m : Hyou.ZENBU) {
            if (m.gamen.equals(gm.id)) {
                moji(g, m);
            }
        }
    }

    /**
     * その文字が付いているカード。カードの文字でなければ null。
     * （item_1_name → item_1。item_1 と item_12 を取り違えない形で探す）
     */
    private Hyou.Kado mojiNoKado(Hyou.Moji m) {
        for (Hyou.Kado k : Hyou.KADO) {
            if (k.gamen.equals(m.gamen) && m.id.startsWith(k.id + "_")) {
                return k;
            }
        }
        return null;
    }

    /**
     * カードのアイテムを描く。
     *
     * ★ アイテムの絵は【ゲームが 16x16 で】描く。大きさは変えられないので、
     *   32x32 の枠に置く時はこちらで2倍に拡大する。
     * ★ 何のアイテムかは Hyou（＝プラグインの Shop.java から生成）が持っている。
     *   ここで名前を書くと、店の中身と絵が食い違う。
     */
    private void aikon(GuiGraphics g, Hyou.Kado k) {
        // ★ 本物の店なら、サーバーが置いた実物を描く。
        //   未解禁の板ガラスもそのまま出るので、見た目と中身が必ず一致する。
        // ★ 絵を選ぶ順番。上から先に決まったものを使う。
        //   1) まだ買えない → サーバーが置いた板ガラスをそのまま出す
        //      （状態と絵が食い違わないよう、ここを最優先にする）
        //   2) MOD のアイテム（銃・弾）→ 本物の見た目
        //   3) サーバーが置いた物
        //   4) 代用品（MOD が入っていない人のため）
        ItemStack shita = honmono() ? Mc.wakuNoMono(menu, k.waku) : null;
        ItemStack mono = null;
        if (Mc.mikaikinKa(shita)) {
            mono = shita;
        }
        if (mono == null && k.modAitem != null) {
            mono = Mc.modAitem(k.modAitem, k.aitemKazu);
        }
        if (mono == null && shita != null && !shita.m_41619_()) {
            mono = shita;
        }
        if (mono == null) {
            mono = Mc.aitem(k.aitem, k.aitemKazu);
        }
        if (k.aw == 16) {
            Mc.aitemWoEgaku(g, mono, k.ax, k.ay);
            Mc.aitemNoKazu(g, mono, k.ax, k.ay);
            return;
        }
        Mc.tsumu(g, k.ax, k.ay, k.aw / 16.0F);
        Mc.aitemWoEgaku(g, mono, 0, 0);
        Mc.aitemNoKazu(g, mono, 0, 0);
        Mc.orosu(g);
    }

    /**
     * 文字ひとつぶんを、枠に合わせて描く。
     *
     * ★ 折り返しは Minecraft 本体に任せる（Mc.oru）。
     *   自分で折り返すと、本体と1文字ずれただけではみ出す。
     */
    private void moji(GuiGraphics g, Hyou.Moji m) {
        // ★★ 未開放のカードは、品名を伏せる（2026-08-22 のご指摘）★★
        //   何が来るかは見せず「？？」だけ。値段も出さない。
        //   解禁の時代だけは残す（貯める・進める動機になるため）。
        Hyou.Kado kado = mojiNoKado(m);
        if (kado != null && honmono()
                && Mc.mikaikinKa(Mc.wakuNoMono(menu, kado.waku))) {
            if (m.id.endsWith("_price")) {
                return;                          // 値段は描かない
            }
            if (m.id.endsWith("_name")) {
                Mc.tsumu(g, m.x, m.y, m.bairitsu());
                Mc.kakuGyou(g, Mc.oru("？？", m.w).get(0), 0, 0, 0xFF000000 | m.iro);
                Mc.orosu(g);
                return;
            }
        }
        String s = nakami(m);
        float bai = m.bairitsu();

        // 枠の幅は画面の px。書体は拡大前で測るので、倍率で割って渡す。
        int hakoHaba = Math.max(1, Math.round(m.w / bai));
        List<FormattedCharSequence> gyou = Mc.oru(s, hakoHaba);

        Mc.tsumu(g, m.x, m.y, bai);
        int kazu = Math.min(gyou.size(), m.gyouSuu);
        for (int i = 0; i < kazu; i++) {
            FormattedCharSequence hitotsu = gyou.get(i);
            int zure = 0;
            if (m.yose == 'C') {
                zure = (hakoHaba - Mc.habaGyou(hitotsu)) / 2;
            } else if (m.yose == 'R') {
                zure = hakoHaba - Mc.habaGyou(hitotsu);
            }
            // 色は 0xRRGGBB で持っているので、不透明にして渡す
            Mc.kakuGyou(g, hitotsu, zure, i * GYOU_TAKASA, 0xFF000000 | m.iro);
        }
        Mc.orosu(g);
    }

    /**
     * その文字に、いま出すべき中身を入れる。
     *
     * ★ 個人の金だけは本物が読める（スコアボードの kane_kojin）。
     * ★ 勢力の金は【読めない】。サイドバーに文字の行として出ているだけで、
     *   目的そのものがクライアントへ来ないため。
     *   分からない物に数字を出すと嘘になるので、「?」を出す。
     */
    private String nakami(Hyou.Moji m) {
        if (!honmono() || !m.ugoku) {
            return Hyou.atehameru(m.moji);
        }
        if (m.id.equals("wallet_personal")) {
            Integer kane = Mc.tensuu(Mc.jibunNoNamae(), "kane_kojin");
            return kane == null ? "個人 ?" : "個人 " + kugiri(kane);
        }
        if (m.id.equals("wallet_faction")) {
            // ★ まずサイドバー（hyouji_N の「貯金」行）から読む。
            //   これはクライアントへ必ず届いている。枠8の品は控え
            //   （サイドバーがまだ無い時や、勢力に入っていない時のため）。
            var moku = Mc.sidebarMokuteki();
            if (moku != null) {
                Integer v = Mc.mokutekiGyou(moku).get("貯金");
                if (v != null) {
                    return "勢力 " + kugiri(v);
                }
            }
            Integer kane = seiryokuNoKane();
            return kane == null ? "勢力 ?" : "勢力 " + kugiri(kane);
        }
        return Hyou.atehameru(m.moji);
    }

    /**
     * 勢力の金。
     *
     * ★★ スコアボードからは読めない ★★
     *   個人の金（kane_kojin）は Tab の一覧に出ているのでクライアントに届くが、
     *   勢力の金（chokin）はサイドバーに【文字の行として】出ているだけで、
     *   目的そのものが届かない。
     *   そこでプラグイン側が、画面の【枠8】に金額を書いた品を置いている
     *   （`Shop.kaneItem`）。ここはその名前から数字を読むだけ。
     */
    private Integer seiryokuNoKane() {
        ItemStack mono = Mc.wakuNoMono(menu, 8);
        if (mono == null || mono.m_41619_()) {
            return null;
        }
        String namae = mono.m_41786_().getString();
        StringBuilder suji = new StringBuilder();
        for (int i = 0; i < namae.length(); i++) {
            char c = namae.charAt(i);
            if (c >= '0' && c <= '9') {
                suji.append(c);
            }
        }
        if (suji.length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(suji.toString());
        } catch (NumberFormatException e) {
            return null;      // 桁が多すぎる等。数字が出せないなら「?」のまま
        }
    }

    /** 1,234 のように3桁ごとに区切る。 */
    private static String kugiri(int n) {
        String s = String.valueOf(Math.abs(n));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) {
                b.append(',');
            }
            b.append(s.charAt(i));
        }
        return (n < 0 ? "-" : "") + b;
    }

    /** 下に、いま何を見ているかと操作の説明を出す。★ 本番では外す。 */
    private void annai(GuiGraphics g) {
        Hyou.Gamen gm = Hyou.GAMEN[ima];
        String s = honmono()
                ? gm.midashi + "（" + gm.id + "）   カードを押すと買えます / E か Esc で閉じる"
                : (ima + 1) + " / " + Hyou.GAMEN.length + "   "
                        + gm.midashi + "（" + gm.id + "）"
                        + "   ← → で画面を切り替え / E か Esc で閉じる（絵の確認だけ）";
        Mc.kaku(g, s, (Mc.gamenHaba(this) - Mc.haba(s)) / 2,
                Mc.gamenTakasa(this) - 12, 0xFFAEB8AE);
    }

    /** Screen#keyPressed(キー, スキャン, 修飾) */
    @Override
    public boolean m_7933_(int kii, int sukyan, int shushoku) {
        // ★ E（持ち物キー）でも閉じられるようにする（2026-08-22 のご指摘）。
        //   チェスト画面は E で閉じるのが体の癖になっているため。
        //   番号 69 と直に比べず、設定（キーを替えている人）に従う。
        if (Mc.shimauKiKa(kii, sukyan)) {
            m_7379_();          // onClose ── 本物の店ならサーバーへも伝わる
            return true;
        }
        // ★ 本物の店では矢印を効かせない。
        //   絵だけ別のページに変わると、押した時に【別の店の枠】へ送ってしまう。
        //   店ではタブを押して切り替える。
        if (honmono()) {
            return super.m_7933_(kii, sukyan, shushoku);
        }
        if (kii == KEY_MIGI) {
            susumu(1);
            return true;
        }
        if (kii == KEY_HIDARI) {
            susumu(-1);
            return true;
        }
        return super.m_7933_(kii, sukyan, shushoku);
    }

    private void susumu(int muki) {
        ima = (ima + muki + Hyou.GAMEN.length) % Hyou.GAMEN.length;
    }

    /**
     * GuiEventListener#mouseClicked(x, y, ボタン)
     *
     * ★ 本物の店なら、その枠を左クリックしたことにしてサーバーへ送る。
     *   買う処理はプラグイン側のまま動く。
     */
    @Override
    public boolean m_6375_(double x, double y, int botan) {
        if (!honmono()) {
            return super.m_6375_(x, y, botan);
        }
        int bai = bairitsu();
        int sx = ((int) x - (Mc.gamenHaba(this) - SEKKEI_HABA * bai) / 2) / bai;
        int sy = ((int) y - (Mc.gamenTakasa(this) - SEKKEI_TAKASA * bai) / 2) / bai;
        String gamen = Hyou.GAMEN[ima].id;

        // タブを押したら、プラグインが画面を開き直してページが変わる
        for (Hyou.Tabu t : Hyou.TABU) {
            if (t.gamen.equals(gamen) && t.naka(sx, sy)) {
                Mc.wakuWoOsu(menu, t.waku());
                return true;
            }
        }
        for (Hyou.Kado k : Hyou.KADO) {
            if (k.gamen.equals(gamen) && k.waku >= 0 && k.naka(sx, sy)) {
                Mc.wakuWoOsu(menu, k.waku);
                return true;
            }
        }
        return super.m_6375_(x, y, botan);
    }

    /**
     * Screen#onClose() ── 閉じる時。
     *
     * ★★ 本物の店に被さっている時は、サーバーへ「閉じた」と伝える ★★
     *   ふつうの画面として閉じるだけだと、サーバー側は画面が開いたままだと思い続ける。
     */
    @Override
    public void m_7379_() {
        if (honmono()) {
            Mc.tojiru();
        }
        super.m_7379_();
    }
}
