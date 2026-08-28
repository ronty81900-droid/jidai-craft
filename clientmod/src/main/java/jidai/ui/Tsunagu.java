package jidai.ui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * サーバーが開いたチェスト画面を、こちらの画面に差し替える。
 *
 * ★★ なぜこの形にしたか ★★
 *   買い物の中身をサーバーとやり取りする方法は2つあった。
 *
 *     1. 独自の通信を作る（プラグイン側も改造して、値段や残高を送る）
 *     2. 【今までどおりチェスト画面を開かせて、その上に絵を被せる】
 *
 *   2 を選んだ。理由は3つ。
 *     ・サーバーを1行も変えずに済む（655項目で確かめた買う処理をそのまま使える）
 *     ・MOD を入れていない人は、今までどおりチェスト画面で買える
 *     ・解禁や在庫の判定を二重に持たなくてよい。
 *       プラグインは未解禁の枠を灰色の板ガラスで塞ぐので、
 *       こちらは【中身を見るだけ】で状態が分かる
 *
 * ★★ 2026-08-22: うちの画面はすべて古地図にした ★★
 *   販売所・銃器専門店（下地の絵つき）／十連の結果／
 *   ガチャ・銀行・時代を進める・宣戦（共通の画面）。
 *   素のチェスト画面のまま残っている物は もう無い。
 */
@Mod.EventBusSubscriber(modid = JidaiUi.ID, value = Dist.CLIENT)
public final class Tsunagu {

    private Tsunagu() {
        // イベントを受けるだけなので、実体は作らない
    }

    /** 見出しでどの店かを見分ける。プラグインは §r の後ろにこの名前を置く。 */
    private static final String MISE = "販売所";
    private static final String JUKI = "銃器専門店";

    /** 十連の結果画面。★ プラグインの Gacha.JUREN_MIDASHI と同じ文字。 */
    static final String JUREN = "十連の結果";

    /**
     * 共通の画面で描くもの。
     *   { 見出しに含まれる文字, パックの絵の文字, 画面に出す名前, 回転帯を描くか }
     *
     * ★★ 絵の文字も見ている理由 ★★
     *   `jidai ui on` にすると、見出しが【絵そのものの文字】だけになり、
     *   日本語が1文字も入らなくなる。名前だけで見分けていると、
     *   その瞬間にどの画面も判別できなくなる（＝古地図に化けなくなる）。
     *   両方を見ておけば、パックを配っても配らなくても同じように動く。
     */
    private static final String[][] KYOTSU = {
            {"ガチャ",       String.valueOf((char) 0xEDE1), "ガチャ",             "1"},
            {"銀行",         String.valueOf((char) 0xEDE4), "銀行",               "0"},
            {"時代を進める", String.valueOf((char) 0xEDE3), "時代を進める",       "0"},
            {"宣戦",         String.valueOf((char) 0xEDE6), "宣戦する相手を選ぶ", "0"},
    };

    /** 販売所と銃器専門店の絵の文字（タブごとに2枚ずつある）。 */
    private static final String[] E_MISE = {
            String.valueOf((char) 0xEDE2), String.valueOf((char) 0xEDE7)};
    private static final String[] E_JUKI = {
            String.valueOf((char) 0xEDE5), String.valueOf((char) 0xEDE8)};

    /** タブは1段目の左から並ぶ。販売所は3枚、銃器専門店は2枚。 */
    private static final int TAB_KAZU = 3;

    @SubscribeEvent
    public static void hirakareta(ScreenEvent.Opening e) {
        Screen atarashii = e.getNewScreen();
        if (!(atarashii instanceof AbstractContainerScreen<?> yoki)) {
            return;
        }
        // ★ menu フィールドは protected なので触れない。
        //   公開されている MenuAccess#getMenu() から取る。
        String midashi = yoki.m_96636_().getString();      // Screen#getTitle()

        // ★ 十連の結果は、買い物の画面とは作りが違うので別の画面へ
        if (midashi.contains(JUREN)) {
            e.setNewScreen(new JurenGamen(yoki.m_6262_()));
            return;
        }

        int shu = shurui(midashi);
        if (shu >= 0) {
            e.setNewScreen(new UiGamen(shu, yoki.m_6262_()));
            return;
        }

        // ★ ガチャ・銀行・時代を進める・宣戦は、中身を読んでそのまま並べる
        for (String[] r : KYOTSU) {
            if (midashi.contains(r[0]) || midashi.contains(r[1])) {
                e.setNewScreen(new KyotsuGamen(yoki.m_6262_(), r[2], r[3].equals("1")));
                return;
            }
        }
        // うちの画面ではない。そのままチェスト画面を出す
    }

    /** 見出しから店の種類を返す。0=販売所 / 1=銃器専門店 / -1=うちの店ではない */
    static int shurui(String midashi) {
        if (fukumu(midashi, MISE, E_MISE)) {
            return 0;
        }
        if (fukumu(midashi, JUKI, E_JUKI)) {
            return 1;
        }
        return -1;
    }

    /** 日本語の名前か、パックの絵の文字のどちらかを含むか。 */
    private static boolean fukumu(String midashi, String mei, String[] e) {
        if (midashi.contains(mei)) {
            return true;
        }
        for (String s : e) {
            if (midashi.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 店の種類と、いま選ばれているタブから、ページ番号を返す。
     *
     * ★★ 毎フレーム 呼び直すこと ★★
     *   画面が開いた【その瞬間】は、まだ中身が届いていない。
     *   サーバーは「画面を開け」の合図と「中身」を別々に送るため、
     *   開いた時に1回だけ見ると、タブの印がまだ無くて必ず1枚目と判定してしまう。
     *   （実機で「防具タブにしても見出しが生活のまま」になった原因がこれ）
     */
    static int pageBan(int shurui, AbstractContainerMenu menu) {
        int tab = erandeiruTab(menu);
        String id;
        if (shurui == 0) {
            id = (tab == 2) ? "shop_weapon" : (tab == 1 ? "shop_armor" : "shop_life");
        } else {
            id = (tab == 1) ? "gunshop_ammo" : "gunshop_guns";
        }
        for (int i = 0; i < Hyou.GAMEN.length; i++) {
            if (Hyou.GAMEN[i].id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * いま選ばれているタブ。
     *
     * ★ プラグインは、選ばれているタブの名前の頭に「▶」を付けている
     *   （`Shop.tabItem`）。それを目印にする。
     *   ページ番号を別に送ってもらう必要がない。
     */
    private static int erandeiruTab(AbstractContainerMenu menu) {
        for (int waku = 0; waku < TAB_KAZU; waku++) {
            ItemStack mono = Mc.wakuNoMono(menu, waku);
            if (mono != null && !mono.m_41619_()
                    && mono.m_41786_().getString().startsWith("▶")) {
                return waku;
            }
        }
        return 0;
    }
}
