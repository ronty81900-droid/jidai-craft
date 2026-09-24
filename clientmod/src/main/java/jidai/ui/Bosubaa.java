package jidai.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「世界の時代」のボスバーを、古地図の顔で描き直す。
 *
 * ★★ なぜ MOD で描くか ★★
 *   バニラのボスバーの絵は 182×5px しかない。配信で一番上に大きく映る帯なのに、
 *   ここだけ素の Minecraft の見た目が残る。
 *   絵（clientmod/ui/assets/.../bar_*.png）は 4倍の解像度で用意してあるので、
 *   バニラに描かせず、こちらで貼る。
 *
 * ★★ 仕様の正本は clientmod/ui/BOSSBAR_SPEC.md の「§3 レンダラ契約」★★
 *   枠 728×32 を 182×8 論理px に描く（9分割しない）。中身は枠の (1,1) へ、
 *   左端から `180 × 進み具合` 論理px。バニラの 182 を使うと右端が枠にかかる。
 *
 * ★★ キャンセルすると【バーと名前の両方】が飛ぶ ★★
 *   Forge が当てた BossHealthOverlay を javap で読んで確かめた。
 *     onCustomizeBossEventProgress → isCanceled → ifne で
 *     drawBar と drawString(名前) を丸ごと飛ばし、getIncrement だけ使う。
 *   つまり名前もこちらで描かないと消える。バニラと同じ位置に出す。
 *
 * ★ 触るのは【うちのバーだけ】。
 *   他の MOD やプラグインが出すボスバーに手を出すと、原因の分からない
 *   不具合になる。名前に「世界の時代」が入っている物だけを描き直す。
 *
 * ★ SRG 名（m_18861_ など）の意味は Mc.java の作法どおり、
 *   呼ぶ所にコメントで本来の名前を残してある。
 */
@Mod.EventBusSubscriber(modid = JidaiUi.ID, value = Dist.CLIENT)
public final class Bosubaa {

    private Bosubaa() {
        // イベントを受けるだけなので、実体は作らない
    }

    /**
     * うちのバーを見分ける目印。
     *
     * ★ データパックの jidai:shinko/bar が名前に必ず入れている字。
     *   ここを変える時は bar.mcfunction も一緒に変えること
     *   （tests/bar_kakunin.py が両方を突き合わせている）。
     */
    static final String MEJIRUSHI = "世界の時代";

    // ── 絵の実寸（テクスチャpx）★ BOSSBAR_SPEC.md §3 ──
    private static final int WAKU_TEX_W = 728;
    private static final int WAKU_TEX_H = 32;
    private static final int NAKA_TEX_W = 720;
    private static final int NAKA_TEX_H = 24;

    // ── 画面に出す大きさ（論理px）──
    /** 枠の横。バニラのボスバーと同じ 182（＝728 ÷ 4） */
    private static final int WAKU_W = 182;
    /** 枠の縦。バニラは 5 だが、こちらは縁を持つので 8（＝32 ÷ 4） */
    private static final int WAKU_H = 8;
    /** 溝（中身が入る所）の横。★ バニラの 182 ではない。182 だと右端が枠にかかる */
    private static final int MIZO_W = 180;
    /** 溝の縦（＝24 ÷ 4） */
    private static final int MIZO_H = 6;
    /** 枠の縁。中身はここだけ内側へずらして置く（テクスチャpx の (4,4)） */
    private static final int FUCHI = 1;

    /** 中身の 論理px 1つ ＝ テクスチャpx いくつか。720 ÷ 180 = 4 */
    private static final int BAI = NAKA_TEX_W / MIZO_W;

    /** バニラのバーの高さ。次のバーの間隔を、こちらの高さぶん広げるために使う */
    private static final int BANIRA_H = 5;

    /** 名前の色。バニラと同じ白 */
    private static final int MOJI_IRO = 0xFFFFFF;

    /**
     * ボスバーを1本 描く直前に、Forge が呼ぶ。
     *
     * ★ e.getX() / e.getY() は、バニラがこのバーを描こうとしていた場所。
     *   位置決めはバニラに任せる（画面の幅や本数で変わるため）。
     */
    @SubscribeEvent
    public static void egaku(CustomizeGuiOverlayEvent.BossEventProgress e) {
        LerpingBossEvent bar = e.getBossEvent();
        String mei = bar.m_18861_().getString();          // BossEvent#getName
        if (!mei.contains(MEJIRUSHI)) {
            return;                 // 他のボスバーには触らない
        }
        String naka = nakami(bar.m_18862_());             // BossEvent#getColor
        if (naka == null) {
            // 知らない色。絵が無いので、バニラに描かせる。
            // ★ 何も出ないより、素の帯でも出ている方が事故が小さい。
            return;
        }

        // ★ ここから先はバニラに描かせない。名前も含めて全部こちらで描く
        e.setCanceled(true);
        // こちらのバーはバニラより (8-5)=3px 高い。次のバーが重ならないようずらす
        e.setIncrement(e.getIncrement() + WAKU_H - BANIRA_H);

        GuiGraphics g = e.getGuiGraphics();
        int x = e.getX();
        int y = e.getY();

        // --- 枠（空の状態）。728×32 をそのまま 182×8 へ縮める。9分割しない ---
        Mc.haruOokisa(g, "textures/gui/bar_frame.png", x, y, WAKU_W, WAKU_H,
                WAKU_TEX_W, WAKU_TEX_H);

        // --- 中身。左端から 進み具合ぶんだけ切り出して、枠の (1,1) へ ---
        //
        // ★★ 先に論理px で丸めてから 4倍する ★★
        //   テクスチャ側を先に丸めると、切り口が画素の途中に来て縁がにじむ。
        //   論理px を 4倍すれば、必ずテクスチャの画素の境目に乗る。
        //   満タン6段階なら 0/30/60/90/120/150/180 論理px
        //   ＝ 0/120/240/360/480/600/720 テクスチャpx（目盛りの位置と一致する）。
        float susumi = bar.m_142717_();                   // BossEvent#getProgress
        if (susumi < 0.0F) {
            susumi = 0.0F;
        }
        if (susumi > 1.0F) {
            susumi = 1.0F;
        }
        int haba = Math.round(MIZO_W * susumi);
        if (haba > 0) {
            Mc.haruKiri(g, "textures/gui/" + naka, x + FUCHI, y + FUCHI, haba, MIZO_H,
                    0.0F, 0.0F, haba * BAI, NAKA_TEX_H, NAKA_TEX_W, NAKA_TEX_H);
        }

        // --- 名前。バニラと同じ位置（真ん中そろえ・バーの1行ぶん上）---
        Mc.kaku(g, mei, x + (WAKU_W - Mc.haba(mei)) / 2, y - Mc.gyouTakasa(), MOJI_IRO);
    }

    /**
     * バニラのボスバーの色 → 時代の絵。
     *
     * ★ 対応は BOSSBAR_SPEC.md §3 の表。データパックの jidai:shinko/bar が
     *   中央の時代に合わせてこの色を送っている。**勝手に入れ替えない。**
     * ★ 列挙の定数名（WHITE など）は SRG でも変わらない
     *   （tools/namae.py で引いて確かめた）。
     */
    static String nakami(BossEvent.BossBarColor iro) {
        if (iro == BossEvent.BossBarColor.WHITE) {
            return "bar_fill_tekki.png";      // 鉄器
        }
        if (iro == BossEvent.BossBarColor.YELLOW) {
            return "bar_fill_chusei.png";     // 中世
        }
        if (iro == BossEvent.BossBarColor.BLUE) {
            return "bar_fill_kindai.png";     // 近代
        }
        if (iro == BossEvent.BossBarColor.PURPLE) {
            return "bar_fill_gendai.png";     // 現代
        }
        return null;                          // 知らない色
    }
}
