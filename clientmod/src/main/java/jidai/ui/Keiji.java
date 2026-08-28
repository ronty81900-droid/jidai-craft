package jidai.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * サイドバー（スコアボード）を、古地図の顔で描き直す掲示板。
 *
 * ★★ なぜ MOD で描くか（2026-08-22 のご指示）★★
 *   バニラのサイドバーは【数字の列に文字を足せない】。
 *   「5,969円」「12ℓ」「中世」のような表記は、仕組みの上で不可能。
 *   （行の名前は文字にできるが、右の値は点数そのもの）
 *   MOD なら自由に描けるので、ここで単位と時代名を付ける。
 *
 * ★★ 中身はサーバーのサイドバーそのもの ★★
 *   勢力ごとの目的（hyouji_N）はサイドバーに出ているので、
 *   その中身はクライアントへ届いている。ここは【読んで描き直すだけ】。
 *   行を足す・減らすのはデータパック側の仕事のまま。
 *
 * ★★ バニラのサイドバーは【止める】★★
 *   はじめは「同じ場所に不透明で被せる」形にしたが、
 *   バニラのサイドバーは画面の真ん中ではなく
 *   「行の下端 = 画面の半分 + 行数×3」という独特の位置決めで、
 *   被せる大きさを合わせにいく限り、必ずどこかで はみ出す。
 *   Forge には VanillaGuiOverlay.SCOREBOARD を取り消す口があるので、
 *   そもそも描かせない。こうすれば位置を合わせる必要がなくなる。
 */
@Mod.EventBusSubscriber(modid = JidaiUi.ID, value = Dist.CLIENT)
public final class Keiji {

    private Keiji() {
        // イベントを受けるだけなので、実体は作らない
    }

    // ── 色（Navigation 案のパレット）──
    private static final int SOTO   = 0xFF102329;   // いちばん外の地
    private static final int JI     = 0xF017313A;   // 板の面
    private static final int SHINCHU = 0xFFC4934B;  // 真鍮（枠と目盛り）
    private static final int MOJI   = 0xFFF5E8C8;   // 明るい字
    private static final int USUI   = 0xFFAEB8AE;   // 補助の字
    private static final int MIZU   = 0xFF4FA1B2;   // 見出し

    /** 行の並び。この順で上から出す。知らない行は後ろに付く。 */
    private static final String[] JUN = {"貯金", "石油", "中央の時代", "勢力の時代"};

    /**
     * バニラのサイドバーを描かせない。
     *
     * ★ 出す物がある時だけ止める。うちの掲示板が出ない場面
     *   （勢力に入っていない・サイドバーが無い）で止めてしまうと、
     *   何も出なくなって かえって不便になる。
     */
    @SubscribeEvent
    public static void vanillaWoTomeru(RenderGuiOverlayEvent.Pre e) {
        if (e.getOverlay() != VanillaGuiOverlay.SCOREBOARD.type()) {
            return;
        }
        if (Mc.sidebarMokuteki() != null) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void egaku(RenderGuiEvent.Post e) {
        Objective moku = Mc.sidebarMokuteki();
        if (moku == null) {
            return;                    // サイドバーが無いなら何もしない
        }
        Map<String, Integer> gyou = Mc.mokutekiGyou(moku);
        if (gyou.isEmpty()) {
            return;
        }

        GuiGraphics g = e.getGuiGraphics();
        String midashi = Mc.mokutekiMidashi(moku);

        // ── 行を「名前 → 見せる文字」へ直し、決めた順に並べる ──
        List<String[]> retsu = new ArrayList<>();
        for (String na : JUN) {
            Integer v = gyou.remove(na);
            if (v != null) {
                retsu.add(new String[]{na, kazari(na, v)});
            }
        }
        for (Map.Entry<String, Integer> nokori : gyou.entrySet()) {
            retsu.add(new String[]{nokori.getKey(), kazari(nokori.getKey(), nokori.getValue())});
        }

        // ── 大きさを決める ──
        //   ★ バニラのサイドバーを覆い隠すため、
        //     こちらの中身と【バニラの描く幅】の大きい方に合わせる。
        int nakaHaba = Mc.haba(midashi);
        for (String[] r : retsu) {
            nakaHaba = Math.max(nakaHaba, Mc.haba(r[0]) + 10 + Mc.haba(r[1]));
        }
        int haba = nakaHaba + 14;                              // 縁と余白ぶん
        int takasa = 4 + 9 + 3 + retsu.size() * 10 + 4;

        int gamenHaba = e.getWindow().m_85445_();              // 論理の横幅
        int gamenTakasa = e.getWindow().m_85446_();
        int x2 = gamenHaba;                                    // 右端いっぱい（バニラと同じ側）
        int x1 = x2 - haba;

        // ★ バニラは止めてあるので、位置合わせは不要。素直に真ん中へ。
        int y1 = (gamenTakasa - takasa) / 2;

        // ── 板 ──
        Mc.nuru(g, x1, y1, x2, y1 + takasa, SOTO);
        Mc.nuru(g, x1 + 1, y1 + 1, x2, y1 + takasa - 1, JI);
        Mc.nuru(g, x1, y1, x1 + 1, y1 + takasa, SHINCHU);      // 左の縁だけ真鍮
        // 目盛り（古地図の測量らしさ。左縁に 8px ごとの刻み）
        for (int y = y1 + 4; y < y1 + takasa - 4; y += 8) {
            Mc.nuru(g, x1 + 1, y, x1 + 3, y + 1, SHINCHU);
        }

        // ── 見出しと行 ──
        int moji_x = x1 + 6;
        int y = y1 + 4;
        Mc.kaku(g, midashi, x1 + (haba - Mc.haba(midashi)) / 2, y, MIZU);
        y += 9;
        Mc.nuru(g, moji_x, y, x2 - 4, y + 1, SHINCHU);          // 見出しの下の罫
        y += 3;
        for (String[] r : retsu) {
            Mc.kaku(g, r[0], moji_x, y + 1, USUI);
            Mc.kaku(g, r[1], x2 - 4 - Mc.haba(r[1]), y + 1, MOJI);
            y += 10;
        }
    }

    /**
     * 値に単位を付ける（2026-08-22 のご指示）。
     *   貯金 → 5,969円 / 石油 → 12ℓ / 時代 → 鉄器・中世・近代・現代
     */
    static String kazari(String namae, int v) {
        if (namae.contains("時代")) {
            return jidaiMei(v);
        }
        if (namae.contains("貯金") || namae.contains("金")) {
            return kugiri(v) + "円";
        }
        if (namae.contains("石油")) {
            return kugiri(v) + "ℓ";
        }
        return kugiri(v);
    }

    /** 時代の番号 → 名前。知らない番号は数字のまま。 */
    static String jidaiMei(int v) {
        switch (v) {
            case 1: return "鉄器";
            case 2: return "中世";
            case 3: return "近代";
            case 4: return "現代";
            case 5: return "未来";   // ★ 2026-08-23: ここへ着いた勢力が勝つ
            default: return String.valueOf(v);
        }
    }

    /** 1,234 のように3桁ごとに区切る。 */
    static String kugiri(int n) {
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
}
