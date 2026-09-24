// =============================================================
// Gacha.java ── ガチャ（ダイヤブロック）
//
//   個人の金で1回まわす。結果はランダム。
//   販売所の変種として作ってある。右クリック→画面→クリック、という
//   仕組みは販売所とまったく同じで、結果が抽選になるだけ。
//
//   ★ 勢力の戦力に影響する物は出さない。
//     銃・防具・武器・石油を出すと、ガチャが最適解になって
//     販売所も時代の解禁も無意味になる。
//
//   ★★ 順序を崩してはいけない ★★
//     金を引く → 抽選 → **景品を渡す** → 演出を見せる
//     演出は「もう決まった結果」を見せるだけ。演出の途中で画面を閉じても、
//     切断しても、景品は既に手元にある。金だけ引かれる事故が起きない。
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class Gacha implements InventoryHolder {

    /**
     * 1回まわす値段（個人の金）。時代ごとに変わる。★ 変えるならここ。
     * 添字は時代 1〜4（0番は使わない）。
     */
    private static final int[] NEDAN = {0, 5, 10, 20, 50};

    /** その時代の値段を返す。 */
    private static int nedan(int jidai) {
        int j = Math.max(1, Math.min(4, jidai));
        return NEDAN[j];
    }

    /**
     * 画面の枠の数。18 = 2段。
     *
     * ★ 1段目(0〜8)が景品の流れる帯。当たりは中央(4)。
     *   2段目(9〜17)は「値段と等級の説明」を絵で描くための帯（2026-08-21）。
     *   ★ 抽選の演出は今までどおり 0〜8 だけを使う。
     */
    private static final int WAKU_SUU = 18;

    /**
     * 景品が流れる帯の幅。★ 1段目の 9枠だけ。
     *
     * ★★ WAKU_SUU と分けてある ★★
     *   2026-08-21 に、説明を描くための2段目を足した。
     *   流れるのは1段目だけなので、ここで WAKU_SUU を使うと
     *   2段目まで景品が動いてしまう。
     */
    private static final int OBI_HABA = 9;

    /** 当選枠。真ん中で止まる。まわしていない間は「まわす」ボタン。 */
    private static final int BUTTON = 4;

    /**
     * 十連ボタン。2段目の中央。
     *
     * ★ 1段目は景品が流れる帯なので置けない。2段目は絵の説明帯で、
     *   物が置かれていない枠だった。ここを使う。
     */
    private static final int BUTTON_10 = 13;

    /** 直前の結果を出しておく枠。2段目の左端。 */
    private static final int KEKKA_WAKU = 9;

    /**
     * 止まった景品を見せてから、ボタンに戻すまでの間（tick）。
     *
     * ★★ なぜ戻すのか（2026-08-22 のご指摘）★★
     *   「一度まわすと、UI から出ないともう一度まわせない」
     *   原因は【止まった景品が まわすボタンの枠を占領したまま】だったこと。
     *   枠を押せば実は もう一度まわせたが、
     *   ボタンが消えて景品の名前になっているので、
     *   まわせるようには見えなかった。
     *   → 少し見せてから、必ずボタンへ戻す。
     */
    private static final int MODOSU_MADE = 30;

    // ── 十連の結果画面 ──
    /** 結果画面の枠の数。27 = 3段。★ 単発の画面（18枠）とは別の入れ物。 */
    private static final int JUREN_SUU = 27;

    /** 一度にまわす回数。 */
    private static final int JUREN_KAZU = 10;

    /**
     * 結果を並べる枠。上段に5つ、中段に5つ。
     *
     * ★★ MOD 側と必ず同じにすること ★★
     *   clientmod の JurenGamen.java が、この枠から中身を読んで描く。
     *   食い違うと MOD 側だけ空になる。GachaTest が両方を突き合わせる。
     */
    private static final int[] JUREN_WAKU = {2, 3, 4, 5, 6, 11, 12, 13, 14, 15};

    /** 下段のボタン。 */
    private static final int JUREN_MOUICHIDO = 21;
    private static final int JUREN_TOJIRU = 23;

    /** 結果画面の見出し。★ MOD 側はこの文字で見分ける。 */
    private static final String JUREN_MIDASHI = "十連の結果";

    /** 等級。止まった時の音と、全体に流すかどうかが変わる */
    private static final String HAZURE = "ハズレ";
    private static final String SHO = "小";
    private static final String CHU = "中";
    private static final String DAI = "大";

    /**
     * コマを1つ進めるまでの待ち時間（tick）。20tick = 1秒。
     *
     * ★ だんだん間隔を伸ばして減速させる。等速で止まると演出にならない。
     * ★ 最後の3コマ（8,10,12）が一番ゆっくり。ここで見ている人が息を止める。
     *
     * 合計 = 1×7 + 2×3 + 3×2 + 4+5+6+8+10+12 = 64 tick ≒ 3.2秒
     */
    private static final int[] MAGARI = {
            1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 3, 3, 4, 5, 6, 8, 10, 12};

    /**
     * 景品1つぶん。
     *
     * kane   … 0 より大きければ「アイテムではなく個人の金が返る」
     * omomi  … 当たりやすさ
     * jidai  … この中央の時代から抽選に入る
     * toukyuu… 止まった時の音と、全体に流すかどうかを決める等級
     */
    private record Keihin(String namae, Material material, int kazu,
                          int kane, int omomi, int jidai, String toukyuu) {
    }

    /**
     * 景品の一覧。★ 中身と確率を変えるのはここだけ。
     *
     * ★★ 時代ごとに【総入れ替え】する ★★
     *   v6 までは「その時代までの景品が全部混ざる」形だったが、
     *   時代が進むほど中身ごと変わる形にした(2026-08-18)。
     *   jidai の欄は「この時代だけに出る」という意味。
     *
     * ★ 金の欄は【個人の金をその額だけ増やす】。アイテムは渡さない。
     *   画面に出る金塊や金インゴットは当たりの見た目で、
     *   売却レート(金4)とは無関係。だから桁が食い違っていてもよい。
     *
     * ★ 重みは 10000 を1万分率として使う。
     *   例: 1500 = 15.00% / 50 = 0.50% / 10 = 0.10%
     *
     * ★★ 2026-08-22（四度目）: 特殊アイテムを下げた（ご指示）★★
     *   鉄器 2.3%→1.0% / 中世 3.8%→1.8% / 近代・現代 5.8%→2.5%。浮いたぶんは原木へ。
     *   さらに【勢力ごとに各1個まで】。一度その勢力に出た特殊は、以後は
     *   ふつうの当たりに引き直す（kisei）。勢力に入っていない人には出ない。
     *
     * ★★ 2026-08-22（三度目）: 実機のご指摘で微調整 ★★
     *   「松明が少し多い・パンが多い・丸石(はずれ)を5%ほど上げる」
     *   鉄器: 松明 18%→14% / パン 25%→21% / 丸石 7%→12%
     *   他の時代も同じ向きに寄せた。金の重みは触っていないので戻りは変わらない。
     *   ★ この3つの上限・下限は GachaTest が見張る。
     *
     * ★★ 2026-08-22（二度目）: もっとやわらげた ★★
     *   一度 45% まで下げたが、実機で「まだかなり渋い」とのご指摘。
     *   渋く感じる正体は【はずれの率】より、
     *   **返ってくる金が値段の 2〜3割しかない**ことだった。
     *   ありふれた品を貰っても、手に残った感じがしない。
     *
     *   そこで割り当てをこう変えた（どの時代も同じ形）:
     *
     *     はずれ（ありふれた物）  45% → **20%**
     *     ふつうの当たり（多めの物）      **51〜57%**
     *     お金                      12% → **21〜23%**
     *     特殊アイテム             1〜3% → **2.3〜5.8%**
     *     特賞（大金）                    **0.2%**
     *
     *   → 大当たりの見出しが出るのは【お金＋特殊】で 23〜29%（およそ4回に1回）。
     *
     * ★★ 金の期待値は値段の6割前後に抑えてある ★★
     *   ここを 100% に近づけると、ガチャが【金を増やす手段】になり、
     *   売って稼ぐ・時代を進めるという筋が崩れる。
     *   1回まわすごとに平均4割ぶん減り、減ったぶんは現物で返る。
     *   ★ この比率は GachaTest が毎回 計算して確かめている。手で足さないこと。
     *
     * ★★ 名前に個数を書かない ★★
     *   「パン 32」と書くと、持ち物の右下に出る数と合わさって
     *   実機で「パン 32 ×32」に見えた（ご指摘）。個数は kazu だけが持ち、
     *   表示は hyouji() が「パン ×32」の形に組み立てる。
     */
    // ★★ 内訳（はずれ／ふつう／金／特殊 の割合）はここに書かない ★★
    //   重みを調整するたびにコメントだけ古くなる。実際、2026-08-23 まで
    //   4時代とも実際の配列と食い違っていた（例: 鉄器のはずれは 2000 ではなく 2300）。
    //   **正しい割合は GachaTest が毎回 この配列から計算して出す。**
    //     python plugin/tests/run_harness.py   → 「-- A. 割り当て --」の表
    private static final Keihin[] KEIHIN = {
            //          名前            アイテム                  数  金   重み 時代 等級
            // ---- 鉄器 (5円) ----
            new Keihin("丸石",        Material.COBBLESTONE,      4,    0,  1200, 1, HAZURE),
            new Keihin("松明",        Material.TORCH,           16,    0,   500, 1, HAZURE),
            new Keihin("原木",        Material.OAK_LOG,          8,    0,  600, 1, HAZURE),
            new Keihin("パン",        Material.BREAD,            8,    0, 1000, 1, SHO),
            new Keihin("松明",        Material.TORCH,           32,    0,  900, 1, SHO),
            new Keihin("原木",        Material.OAK_LOG,         16,    0, 1030, 1, SHO),
            new Keihin("ヒッタイトの戦車", Material.PAPER,        1,    0,   80, 1, CHU),   // 遺物
            new Keihin("ロンバルディアの鉄王冠", Material.PAPER,  1,    0,   70, 1, CHU),   // 遺物
            new Keihin("パン",        Material.BREAD,           16,    0,   800, 1, SHO),
            new Keihin("羊毛",        Material.WHITE_WOOL,       8,    0,   900, 1, SHO),
            new Keihin("焼いた牛肉",  Material.COOKED_BEEF,      4,    0,   500, 1, SHO),
            new Keihin("パン",        Material.BREAD,           32,    0,  300, 1, CHU),
            new Keihin("金 5",        Material.GOLD_NUGGET,      1,    5, 1200, 1, SHO),
            new Keihin("金 10",       Material.GOLD_NUGGET,      2,   10,  600, 1, SHO),
            new Keihin("金 25",       Material.GOLD_INGOT,       1,   25,  200, 1, CHU),
            new Keihin("金 50",       Material.GOLD_INGOT,       2,   50,   50, 1, CHU),
            new Keihin("古びた護符",  Material.PAPER,            1,    0,    50, 1, CHU),
            new Keihin("金 300",      Material.GOLD_BLOCK,       1,  300,   20, 1, DAI),

            // ---- 中世 (10円) ----
            new Keihin("丸石",        Material.COBBLESTONE,      8,    0,  1200, 2, HAZURE),
            new Keihin("石レンガ",    Material.STONE_BRICKS,    16,    0,   600, 2, HAZURE),
            new Keihin("松明",        Material.TORCH,           32,    0,   400, 2, HAZURE),
            new Keihin("石レンガ",    Material.STONE_BRICKS,    32,    0, 1000, 2, SHO),
            new Keihin("原木",        Material.OAK_LOG,         32,    0, 1080, 2, SHO),
            new Keihin("聖剣エクスカリバー", Material.PAPER,     1,    0,   50, 2, CHU),   // 遺物
            new Keihin("テンプル騎士団の盾", Material.PAPER,     1,    0,   50, 2, CHU),   // 遺物
            new Keihin("武器商人の手形", Material.PAPER,         1,    0,   50, 2, CHU),   // 遺物
            new Keihin("焼いた牛肉",  Material.COOKED_BEEF,      8,    0, 1000, 2, SHO),
            new Keihin("ガラス",      Material.GLASS,           16,    0,  700, 2, SHO),
            new Keihin("焼いた牛肉",  Material.COOKED_BEEF,     16,    0,  900, 2, SHO),
            new Keihin("焼いた牛肉",  Material.COOKED_BEEF,     32,    0,   520, 2, CHU),
            new Keihin("金 10",       Material.GOLD_NUGGET,      2,   10, 1200, 2, SHO),
            new Keihin("金 20",       Material.GOLD_NUGGET,      4,   20,  700, 2, SHO),
            new Keihin("金 40",       Material.GOLD_INGOT,       2,   40,  300, 2, CHU),
            new Keihin("金 100",      Material.GOLD_INGOT,       4,  100,   80, 2, CHU),
            new Keihin("古びた護符",  Material.PAPER,            1,    0,    50, 2, CHU),
            new Keihin("聖杯の欠片",  Material.PAPER,            1,    0,    50, 2, CHU),
            new Keihin("羊皮紙の海図", Material.PAPER,           1,    0,    50, 2, CHU),
            new Keihin("金 600",      Material.GOLD_BLOCK,       1,  600,   15, 2, DAI),
            new Keihin("金 1500",     Material.GOLD_BLOCK,       2, 1500,    5, 2, DAI),

            // ---- 近代 (20円) ----
            new Keihin("丸石",        Material.COBBLESTONE,     16,    0,  1200, 3, HAZURE),
            new Keihin("石レンガ",    Material.STONE_BRICKS,    32,    0,   600, 3, HAZURE),
            new Keihin("松明",        Material.TORCH,           64,    0,   400, 3, HAZURE),
            new Keihin("石レンガ",    Material.STONE_BRICKS,    64,    0, 1000, 3, SHO),
            new Keihin("原木",        Material.OAK_LOG,         64,    0, 1280, 3, SHO),
            new Keihin("ベッセマー転炉", Material.PAPER,         1,    0,   50, 3, CHU),   // 遺物
            new Keihin("ロスチャイルドの金庫", Material.PAPER,   1,    0,   50, 3, CHU),   // 遺物
            new Keihin("死の商人の手引書", Material.PAPER,       1,    0,   50, 3, CHU),   // 遺物
            new Keihin("焼いた牛肉",  Material.COOKED_BEEF,     16,    0, 1000, 3, SHO),
            new Keihin("ガラス",      Material.GLASS,           32,    0,  700, 3, SHO),
            new Keihin("焼いた牛肉",  Material.COOKED_BEEF,     32,    0,  800, 3, SHO),
            new Keihin("焼いた牛肉",  Material.COOKED_BEEF,     64,    0,  450, 3, CHU),
            new Keihin("金 20",       Material.GOLD_NUGGET,      4,   20, 1200, 3, SHO),
            new Keihin("金 40",       Material.GOLD_NUGGET,      8,   40,  700, 3, SHO),
            new Keihin("金 80",       Material.GOLD_INGOT,       4,   80,  270, 3, CHU),
            new Keihin("金 200",      Material.GOLD_INGOT,       8,  200,   80, 3, CHU),
            new Keihin("古びた護符",  Material.PAPER,            1,    0,    50, 3, CHU),
            new Keihin("聖杯の欠片",  Material.PAPER,            1,    0,    50, 3, CHU),
            new Keihin("羊皮紙の海図", Material.PAPER,           1,    0,    50, 3, CHU),
            // ★★ 2026-08-23: 集める5種のうち「蒸気機関の歯車」「月の石」は近代に出さない ★★
            //   ご指示（クリア想定時間 §6 の案C）。近代で5種そろうため、超特殊勝利が
            //   57〜67分で決まり、本命の現代到達（2時間13分）を1時間 追い越していた。
            //   近代で引けるのは 3種（護符・聖杯・海図）まで。残り2種は現代のガチャだけ。
            //   → 超特殊勝利にも「現代へ着く」が前提になり、本命の勝ち筋と並ぶ。
            //   外した重み 50×2 は原木へ戻した（近代の合計は 10000 のまま）。
            new Keihin("金 1200",     Material.GOLD_BLOCK,       2, 1200,   15, 3, DAI),
            new Keihin("金 3000",     Material.GOLD_BLOCK,       3, 3000,    5, 3, DAI),

            // ---- 現代 (50円) ----
            new Keihin("丸石",        Material.COBBLESTONE,     32,    0,  1200, 4, HAZURE),
            new Keihin("深層岩",      Material.DEEPSLATE,       32,    0,   600, 4, HAZURE),
            new Keihin("松明",        Material.TORCH,           64,    0,   400, 4, HAZURE),
            new Keihin("石レンガ",    Material.STONE_BRICKS,    64,    0, 1000, 4, SHO),
            new Keihin("原木",        Material.OAK_LOG,         64,    0, 1180, 4, SHO),
            new Keihin("ペニシリン",  Material.PAPER,            1,    0,   50, 4, CHU),   // 遺物
            new Keihin("暗視ゴーグル", Material.PAPER,           1,    0,   50, 4, CHU),   // 遺物
            new Keihin("スマートフォン", Material.PAPER,         1,    0,   50, 4, CHU),   // 遺物
            new Keihin("焼いた牛肉",  Material.COOKED_BEEF,     32,    0, 1000, 4, SHO),
            new Keihin("ガラス",      Material.GLASS,           64,    0,  700, 4, SHO),
            new Keihin("焼いた牛肉",  Material.COOKED_BEEF,     64,    0,  800, 4, SHO),
            new Keihin("パン",        Material.BREAD,           64,    0,  450, 4, CHU),
            new Keihin("金 50",       Material.GOLD_NUGGET,      8,   50, 1200, 4, SHO),
            new Keihin("金 100",      Material.GOLD_INGOT,       4,  100,  700, 4, SHO),
            new Keihin("金 200",      Material.GOLD_INGOT,       8,  200,  270, 4, CHU),
            new Keihin("金 500",      Material.GOLD_BLOCK,       1,  500,   80, 4, CHU),
            new Keihin("古びた護符",  Material.PAPER,            1,    0,    50, 4, CHU),
            new Keihin("聖杯の欠片",  Material.PAPER,            1,    0,    50, 4, CHU),
            new Keihin("羊皮紙の海図", Material.PAPER,           1,    0,    50, 4, CHU),
            new Keihin("蒸気機関の歯車", Material.PAPER,         1,    0,    50, 4, CHU),
            new Keihin("月の石",      Material.PAPER,            1,    0,    50, 4, CHU),
            new Keihin("金 3000",     Material.GOLD_BLOCK,       3, 3000,   15, 4, DAI),
            new Keihin("金 8000",     Material.GOLD_BLOCK,       6, 8000,    5, 4, DAI),
    };

    /**
     * 画面とメッセージに出す文字。
     *
     * ★★ 個数を名前に書かない ★★
     *   名前に「パン 32」と書くと、アイテムの右下に出る個数と合わさって
     *   実機で「パン 32 ×32」に見えた（2026-08-22 のご指摘）。
     *   個数は kazu だけが持ち、見せる時にここで「×32」を足す。
     *
     * ★ お金の景品は、名前の数字が【金額】なので個数を足さない。
     *   （画面に並ぶ金塊の数は見た目だけのもの）
     */
    private static String hyouji(Keihin k) {
        if (k.kane() > 0 || k.kazu() <= 1) {
            return k.namae();
        }
        return k.namae() + " ×" + k.kazu();
    }

    /**
     * 「大当たり」の見出しを出す相手か。
     *
     * ★★ お金と、時代ごとの特殊アイテムだけ（2026-08-22 のご指示）★★
     *   それまでは何が出ても見出しが「大当たり」で、丸石でもそう出ていた。
     *
     * ★ 特殊アイテムの名前は Shouri が正本。ここで並べ直すと、
     *   片方だけ変えた時に黙って食い違う。
     */
    private static boolean oomonoKa(Keihin k) {
        if (k.kane() > 0) {
            return true;
        }
        // ★ 集める5種も遺物8種も「特殊」。見た目の番号を持つ物がそれ
        return Shouri.tokushuModelData(k.namae()) > 0;
    }

    /** 予約（演出を1コマずつ進める）に使う。 */
    private final JidaiCraft plugin;

    /** 抽選に使う乱数。 */
    private final Random ran = new Random();

    /**
     * 名前 → その人だけの画面。
     * ★ 1台を共有すると、他人の演出が自分の画面に流れてしまう。
     */
    private final Map<String, Inventory> gamen = new HashMap<>();

    /** いま回している人。演出中の二度押しを止める。 */
    private final Set<String> mawashichu = new HashSet<>();

    public Gacha(JidaiCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, WAKU_SUU, Enshutsu.gamenMei(Enshutsu.UI_GACHA, "ガチャ"));
    }

    /** ガチャの画面を開く。 */
    public void hiraku(Player player, Kane kane) {
        Inventory inv = Bukkit.createInventory(this, WAKU_SUU, Enshutsu.gamenMei(Enshutsu.UI_GACHA, "ガチャ"));
        int jidai = kane.jidaiOf(player);
        inv.setItem(BUTTON, button(jidai));
        inv.setItem(BUTTON_10, button10(jidai));
        gamen.put(player.getName(), inv);
        player.openInventory(inv);
    }

    /**
     * 中央のボタン。値段と、今の時代だけを説明に出す。
     *
     * ★★ 2026-09-18 のご指示「何が出るかは提示しなくてよい」★★
     *   それまでは、その時代の景品を全部 並べていた（20行ほど）。
     *   伏せる方が引く楽しみが残る。景品表の正本は Gacha.KEIHIN のままで、
     *   運営用の資料（docs/資料_商品一覧.md）には載っている。
     */
    private ItemStack button(int jidai) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(moji("まわす (個人の金 " + nedan(jidai) + ")"));

        List<String> setsumei = new ArrayList<>();
        setsumei.add(moji("クリックで1回まわす"));
        setsumei.add(moji("今の時代: " + Kane.jidaiMei(jidai)));
        // ★ 何が出るかは出さない（2026-09-18 のご指示）
        meta.setLore(setsumei);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 十連ボタン。値段は単発の10倍。天井があることも書いておく。
     */
    private ItemStack button10(int jidai) {
        ItemStack item = new ItemStack(Material.ENDER_EYE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(moji("十連でまわす (個人の金 " + nedan(jidai) * JUREN_KAZU + ")"));
        List<String> setsumei = new ArrayList<>();
        setsumei.add(moji("クリックで10回まとめてまわす"));
        setsumei.add(moji("★ 10回のうち必ず1つは「中」以上"));
        setsumei.add(moji("持ち物の空きが要ります"));
        meta.setLore(setsumei);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 等級の印。結果画面で名前の頭に付ける。
     *
     * ★★ なぜ色コードではなく【文字】にしたか ★★
     *   MOD を入れていない人は素のチェスト画面で結果を見る。
     *   色だけだと、その人たちには等級が伝わらない。
     *   文字なら両方に届くし、MOD 側は色の情報を
     *   サーバーから受け取らずに、この文字を見て色を決められる。
     *   （＝新しい通信を足さずに済む。この作りの土台）
     *
     * ★ 流れる帯には付けない。回っている途中で等級が見えてしまい、
     *   止まる所を見る楽しみが減るため。
     */
    static final String SHIRUSHI_DAI = "★";
    static final String SHIRUSHI_CHU = "◆";

    /** 等級の印を付けた名前。 */
    private static String shirushiTsuki(Keihin k) {
        if (k.toukyuu().equals(DAI)) {
            return SHIRUSHI_DAI + hyouji(k);
        }
        if (k.toukyuu().equals(CHU)) {
            return SHIRUSHI_CHU + hyouji(k);
        }
        return hyouji(k);
    }

    /** 結果画面に並べる1つ。印つきの名前で作る。 */
    private ItemStack kekkaItem10(Keihin k) {
        // ★ keihinItem を土台にする。特殊アイテムの絵（CustomModelData）が
        //   ここだけ抜けていて、結果画面では紙のままだった（2026-08-22 のご指摘）。
        ItemStack item = keihinItem(k);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(moji(shirushiTsuki(k)));
        item.setItemMeta(meta);
        return item;
    }

    /** めくる前の伏せ札。 */
    private static ItemStack fuseta() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(moji("？"));
        item.setItemMeta(meta);
        return item;
    }

    /** 結果画面の仕切り。押しても何も起きない枠。 */
    private static ItemStack shikiri() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    /** 結果画面の「もう一度」。 */
    private ItemStack mouichido(int jidai) {
        ItemStack item = new ItemStack(Material.ENDER_EYE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(moji("もう一度 十連 (個人の金 " + nedan(jidai) * JUREN_KAZU + ")"));
        item.setItemMeta(meta);
        return item;
    }

    /** 結果画面の「閉じる」。 */
    private static ItemStack tojiruBotan() {
        ItemStack item = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(moji("閉じる"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * ガチャの画面が押された。単発の画面と、十連の結果画面の両方を受ける。
     *
     * ★ どちらの画面かは【枠の数】で見分ける。
     *   持ち主はどちらも Gacha なので、これで分ける必要がある。
     */
    public void oshita(Player player, Inventory inv, int slot, Kane kane) {
        if (inv.getSize() == JUREN_SUU) {
            // ★ めくっている最中はボタンが無い。押しても何もしない
            Juren j = machi.get(player.getName());
            if (j != null && !j.watashita) {
                return;
            }
            if (slot == JUREN_MOUICHIDO) {
                // ★ クリックの処理中に画面を開き直すと、クライアントに
                //   新しい画面が届かないことがある（下の「閉じる」と同じ理由）。
                //   1tick 待ってから開く。
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> mawasu10(player, kane), 1L);
            } else if (slot == JUREN_TOJIRU) {
                // ★ クリックの処理中に閉じると Bukkit が嫌がる。1tick 待つ。
                //   ★ player::closeInventory と書くと、runTaskLater の
                //     Runnable 版と Consumer<BukkitTask> 版のどちらとも取れて
                //     コンパイルが通らない。引数の無いラムダなら Runnable に決まる。
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> player.closeInventory(), 1L);
            }
            return;
        }
        if (slot == BUTTON) {
            mawasu(player, kane);
        } else if (slot == BUTTON_10) {
            mawasu10(player, kane);
        }
    }

    /**
     * 1回まわす。
     *
     * ★★ この順序を崩さないこと ★★
     *   1. 断る条件をすべて確かめる（金を減らす前に）
     *   2. 金を引く
     *   3. 抽選する
     *   4. **景品を渡す**
     *   5. 演出を始める（もう決まった結果を見せるだけ）
     *
     *   4 と 5 が逆だと、演出中に画面を閉じた・切断した・サーバーが落ちた時に
     *   **金だけ引かれて景品が消える。**
     */
    public void mawasu(Player player, Kane kane) {

        if (mawashichu.contains(player.getName())) {
            player.sendMessage("[ガチャ] 今まわっています");
            return;
        }
        if (!kane.junbiOK()) {
            player.sendMessage("[ガチャ] データパックが読み込まれていません");
            return;
        }
        // 持ち物がいっぱいだと、当たった品が消える。先に断る。
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("[ガチャ] 持ち物がいっぱいです。空けてから回してください");
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }
        // ★ 値段も景品も【その勢力の時代】で決まる。
        //   先に進んだ勢力ほど高くなるが、中身も良くなる。
        int jidai = kane.jidaiOf(player);
        int nedan = nedan(jidai);

        int mae = kane.kojinZandaka(player);
        if (mae < nedan) {
            player.sendMessage("[ガチャ] 個人の金が足りません (必要 " + nedan
                    + " / 所持 " + mae + ")");
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }

        Keihin atari = kisei(erabu(jidai), player, kane, jidai, null);
        if (atari == null) {
            player.sendMessage("[ガチャ] 今は景品がありません（管理者に連絡してください）");
            return;
        }

        // --- 2) 代金を引く ---------------------------------------
        int ato = mae - nedan;
        kane.kojinKousin(player, ato);

        // --- 4) 演出 → 止まってから渡す --------------------------
        //
        // ★★ 渡すのは演出の【後】 ★★
        //   先に渡すと、まだ回っている最中に持ち物へ品が増えてしまい、
        //   「結果が出る前に当たりが分かる」ことになる。
        //   何が当たるかは既に決まっている(atari)ので、
        //   渡す時機を後ろにずらすだけで筋は通る。
        int zangaku = (atari.kane() > 0) ? ato + atari.kane() : ato;
        enshutsu(player, atari, jidai, mae, zangaku, kane);
    }

    /**
     * 横に流れて減速し、真ん中で止まる演出を始める。
     * 画面が無ければ（既に閉じている等）、結果だけ伝えて終わる。
     */
    private void enshutsu(Player player, Keihin atari, int jidai, int mae, int ato,
                          Kane kane) {
        Inventory inv = gamen.get(player.getName());
        if (inv == null) {
            teishi(player, atari, mae, ato, kane);
            return;
        }

        // 流れる帯を作る。最後のコマで、当たりがちょうど真ん中に来るようにする。
        List<ItemStack> obi = new ArrayList<>();
        int hitsuyou = MAGARI.length + OBI_HABA;
        for (int i = 0; i < hitsuyou; i++) {
            obi.add(keihinItem(kiseiMiru(erabu(jidai), player, kane, jidai)));
        }
        obi.set(MAGARI.length - 1 + BUTTON, keihinItem(atari));

        // ★ 回っている間は十連ボタンと前回の結果を引っ込める（2026-08-22 のご指摘）。
        //   出しっぱなしだと「回っている最中に十連も押せる」ように見えるし、
        //   帯の下に居座って目障りだった。modosu() が戻す。
        inv.setItem(BUTTON_10, null);
        inv.setItem(KEKKA_WAKU, null);

        mawashichu.add(player.getName());
        susumu(player, inv, obi, 0, atari, mae, ato, kane);
    }

    /**
     * 1コマ進める。進めたら、次のコマを「MAGARI[step] tick 後」に予約する。
     *
     * ★ 間隔が毎回ちがうので、繰り返しの予約ではなく
     *   「次を1回だけ予約する」を積み重ねている。
     */
    private void susumu(Player player, Inventory inv, List<ItemStack> obi,
                        int step, Keihin atari, int mae, int ato, Kane kane) {

        // ★ 途中で切断された時。渡すのを演出の後にした以上、
        //   ここで渡さないと「代金だけ取られて何も貰えない」ことになる。
        if (!player.isOnline()) {
            mawashichu.remove(player.getName());
            watasu(player, atari, ato, kane);
            return;
        }

        if (step >= MAGARI.length) {
            inv.setItem(BUTTON, keihinItem(atari));
            mawashichu.remove(player.getName());
            teishi(player, atari, mae, ato, kane);
            return;
        }

        for (int i = 0; i < OBI_HABA; i++) {
            inv.setItem(i, obi.get(step + i));
        }
        // 減速に合わせて音を少しずつ低くする（止まる感じが出る）
        float takasa = 1.4f - (0.5f * step / MAGARI.length);
        Enshutsu.oto(player, Enshutsu.OTO_KAITEN, takasa);

        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> susumu(player, inv, obi, step + 1, atari, mae, ato, kane),
                MAGARI[step]);
    }

    /**
     * 止まった時の見せ方。等級で音を変え、大当たりだけ全体に流す。
     *
     * ★ 中を全体に流すと通知が多くなりすぎて、大当たりの価値が下がる。
     *   全体に出すのは「大」だけ。これがドラマの種になる。
     */
    private void teishi(Player player, Keihin atari, int mae, int ato, Kane kane) {
        // ★★ ここで初めて渡す（演出が止まった瞬間）★★
        watasu(player, atari, ato, kane);

        // ★ 何が当たっても同じ、経験値のような高い音を鳴らす。
        //   等級で音を変えると、鳴った瞬間に中身が分かってしまい、
        //   止まるところを見る楽しみが減るため。
        //   大当たりだけは、そのあと全体通知と花火で分かる。
        Enshutsu.oto(player, Enshutsu.OTO_CHU);

        // ★★ 見出しを出し分ける（2026-08-22）★★
        //   お金と特殊アイテムだけ「大当たり」。それ以外は「ガチャ」。
        String shurui = oomonoKa(atari) ? Enshutsu.ATARI : Enshutsu.GACHA;
        player.sendMessage(Enshutsu.kazaru(shurui,
                hyouji(atari) + " が出た (個人の金 " + mae + " → " + ato + ")"));

        if (atari.toukyuu().equals(DAI)) {
            // ★ 全体チャットと花火は、いちばん上の等級だけ。
            //   ここを広げると通知が増えすぎて、大当たりの価値が下がる。
            Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.ATARI,
                    player.getName() + " がガチャで「" + hyouji(atari) + "」を引き当てた"));
            Enshutsu.hanabi(player);
        }

        // ★ 少し見せてから、まわすボタンへ戻す（続けてまわせるように）
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> modosu(player, atari, kane), MODOSU_MADE);
    }

    /**
     * まわし終わった画面を、もう一度まわせる形に戻す。
     *
     * ★★ まわしている最中なら触らない ★★
     *   景品を見せている 1.5秒 の間に もう一度押されると、
     *   次の回転が始まっている。そこへ前回の後始末が届くと、
     *   回転の途中でボタンが割り込んで演出が壊れる。
     */
    private void modosu(Player player, Keihin atari, Kane kane) {
        if (mawashichu.contains(player.getName())) {
            return;                 // もう次がまわっている
        }
        Inventory inv = gamen.get(player.getName());
        if (inv == null) {
            return;                 // もう閉じている
        }
        // 流れ終わった帯を片づける（景品が並んだままだと散らかって見える）
        for (int i = 0; i < OBI_HABA; i++) {
            inv.setItem(i, null);
        }
        int jidai = kane.jidaiOf(player);
        inv.setItem(BUTTON, button(jidai));
        inv.setItem(BUTTON_10, button10(jidai));
        inv.setItem(KEKKA_WAKU, kekkaItem(atari));
    }

    /** 直前の結果として置いておくアイテム。 */
    private ItemStack kekkaItem(Keihin k) {
        ItemStack item = keihinItem(k);
        ItemMeta meta = item.getItemMeta();
        List<String> setsumei = new ArrayList<>();
        setsumei.add(moji("さっきの結果"));
        meta.setLore(setsumei);
        item.setItemMeta(meta);
        return item;
    }

    // =========================================================
    //  十連
    // =========================================================

    /**
     * まわし終えて、まだ渡していない十連の結果。
     *
     * ★★ なぜ「渡す前」の状態を持つか（2026-08-22 のご指摘）★★
     *   「結果が出る前にアイテムが付与されてしまう」。
     *   渡すのを演出の後ろへ動かすと、演出の最中に 閉じた・切断した・
     *   サーバーが落ちた 時に【代金だけ取られる】穴が開く。
     *   そこで結果を控えておき、次の3つの出口すべてで必ず渡す:
     *     1. めくり終わった   … その場で渡す（mekuru）
     *     2. 途中で閉じた     … 閉じた瞬間に渡す（tojita）
     *     3. 途中で切断した   … 次に入った瞬間に渡す（sanka）
     *   渡したら watashita を立て、二重に渡さない。
     *   ★ サーバーそのものが落ちた時だけは残らない（約5秒の窓）。
     */
    private static final class Juren {
        final List<Keihin> deta;
        final int jidai;
        final int mae;              // まわす前の金
        final Kane kane;

        /**
         * この結果を見せている画面。
         *
         * ★★ なぜ画面を覚えるか（2026-08-23 の「めくられず停止する」）★★
         *   「もう一度 十連」を押すと、古い結果画面を閉じて新しい画面を開く。
         *   その【閉じる】が InventoryCloseEvent を呼び、tojita が
         *   「十連の途中で閉じた」と取り違えて、**まだ1枚もめくっていない
         *   新しい結果をその場で渡していた**。渡すと machi から消えるので、
         *   直後の mekuru が「もう片づいている」と見なして何もせず終わり、
         *   伏せ札がめくられないまま固まる。
         *
         *   初回の十連が動くのは、その時 閉じるのがガチャの画面（27枠ではない）で、
         *   tojita の枠数の条件に当たらないから。2回目以降だけ壊れていた。
         *
         *   → 閉じた画面が【この結果の画面そのものか】を見て決める。
         */
        final Inventory inv;
        boolean watashita;

        Juren(List<Keihin> deta, int jidai, int mae, Kane kane, Inventory inv) {
            this.deta = deta;
            this.jidai = jidai;
            this.mae = mae;
            this.kane = kane;
            this.inv = inv;
        }
    }

    /** 名前 → 渡し待ちの十連。 */
    private final Map<String, Juren> machi = new HashMap<>();

    // =========================================================
    //  特殊アイテムは勢力ごとに各1個まで（2026-08-22 のご指示）
    // =========================================================
    //
    // ★★ なぜ「勢力ごと」か ★★
    //   勝利条件は「リーダー1人が5種そろえる」。同じ勢力に同じ物が
    //   2つ出ても意味が無く、5種を別々の勢力が1つずつ持つ形が
    //   いちばん駆け引きになる。出た時点で記録し、以後その勢力には
    //   ふつうの当たりへ引き直す。
    // ★ 記録は【抽選した時点】で付ける。渡すのは後（十連）なので、
    //   渡す前に閉じても切断しても、同じ物が二度 出ることは無い。
    // ★ 勢力に入っていない人（勢力の金庫が無い人）には特殊は出ない。
    //   出しても どの勢力の1個かが決まらないため。

    /** 勢力名 → その勢力に出た特殊アイテムの名前。設定ファイルに残す。 */
    private final Map<String, Set<String>> tokushuDeta = new HashMap<>();

    private static final String KEY_TOKUSHU = "gacha_tokushu";

    /** 設定ファイルから読み戻す。★ onEnable から呼ぶ。 */
    public void yomikomi() {
        tokushuDeta.clear();
        if (plugin == null) {
            return;
        }
        var bu = plugin.getConfig().getConfigurationSection(KEY_TOKUSHU);
        if (bu == null) {
            return;
        }
        for (String kuni : bu.getKeys(false)) {
            tokushuDeta.put(kuni, new HashSet<>(plugin.getConfig().getStringList(KEY_TOKUSHU + "." + kuni)));
        }
        Ibutsu.yomikomi(tokushuDeta);
    }

    private void tokushuHozon() {
        if (plugin == null) {
            return;
        }
        plugin.getConfig().set(KEY_TOKUSHU, null);
        for (Map.Entry<String, Set<String>> e : tokushuDeta.entrySet()) {
            plugin.getConfig().set(KEY_TOKUSHU + "." + e.getKey(), new ArrayList<>(e.getValue()));
        }
        plugin.saveConfig();
    }

    /** 記録を全部消す（運営の全部リセット）。 */
    public void tokushuRisetto() {
        tokushuDeta.clear();
        Ibutsu.risetto();
        tokushuHozon();
    }

    /** その勢力に、その特殊アイテムがもう出ているか。 */
    boolean tokushuDetaKa(String kuni, String namae) {
        Set<String> d = tokushuDeta.get(kuni);
        return d != null && d.contains(namae);
    }

    /** 特殊アイテムか。★ 名前の正本は Shouri。 */
    private static boolean tokushuKa(Keihin k) {
        return k.kane() <= 0 && Shouri.tokushuModelData(k.namae()) > 0;
    }

    /**
     * 集める5種を手に入れた時、全勢力へ知らせる（2026-08-31 のご指示）。
     *
     * ★★ 渡す時に呼ぶ。抽選の時ではない ★★
     *   十連は伏せ札を1枚ずつめくる演出なので、抽選時に流すと結果がばれる。
     *   遺物の知らせ（Ibutsu.teniireta）と同じ場所・同じ理由。
     *
     * ★ 遺物はここでは扱わない。遺物は Ibutsu 側が勢力の中へ効果まで伝える。
     *   ここは【集める5種】だけ。勝敗そのものなので全勢力に見せる。
     */
    private void tokushuTsuchi(Player player, Kane kane, String namae) {
        boolean atsumeru = false;
        for (String m : Shouri.tokushuMei()) {
            if (m.equals(namae)) {
                atsumeru = true;
            }
        }
        if (!atsumeru) {
            return;
        }
        String kuni = kane.kinkoMei(player);
        if (kuni == null) {
            return;
        }
        // 何個目かを添える。超特殊勝利は5種そろえる勝ち方なので、
        // 「あと何個か」が全員に見えると駆け引きになる。
        Set<String> motteru = tokushuDeta.get(kuni);
        int kazu = 0;
        for (String m : Shouri.tokushuMei()) {
            if (motteru != null && motteru.contains(m)) {
                kazu++;
            }
        }
        Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.ATARI,
                kuni + " が " + namae + " を入手しました（"
                        + kazu + "/" + Shouri.tokushuKazu() + "）"));
    }

    /**
     * 引いた景品に【勢力ごとに各1個まで】の決まりを当てる。
     *   特殊でない        … そのまま
     *   勢力に居ない      … ふつうの当たりに引き直す
     *   もう出ている      … ふつうの当たりに引き直す
     *   まだ出ていない    … 記録して、そのまま
     * @param konkai この十連の中で既に出た特殊（同じ十連に2つ出さないため）。null でもよい
     */
    private Keihin kisei(Keihin k, Player player, Kane kane, int jidai, Set<String> konkai) {
        if (k == null || !tokushuKa(k)) {
            return k;
        }
        String kuni = kane.kinkoMei(player);
        if (kuni == null || tokushuDetaKa(kuni, k.namae())
                || (konkai != null && konkai.contains(k.namae()))) {
            Keihin kawari = erabuFutsuu(jidai);
            return kawari == null ? k : kawari;
        }
        tokushuDeta.computeIfAbsent(kuni, x -> new HashSet<>()).add(k.namae());
        Ibutsu.kiroku(kuni, k.namae());           // 遺物なら、この瞬間から効果が付く
        if (konkai != null) {
            konkai.add(k.namae());
        }
        tokushuHozon();
        return k;
    }

    /** 帯に流すだけの時。記録せず、出せない特殊だけ差し替える。 */
    private Keihin kiseiMiru(Keihin k, Player player, Kane kane, int jidai) {
        if (k == null || !tokushuKa(k)) {
            return k;
        }
        String kuni = kane.kinkoMei(player);
        if (kuni == null || tokushuDetaKa(kuni, k.namae())) {
            Keihin kawari = erabuFutsuu(jidai);
            return kawari == null ? k : kawari;
        }
        return k;
    }

    /** 特殊アイテム以外から、重みで1つ選ぶ（引き直し用）。 */
    private Keihin erabuFutsuu(int jidai) {
        int goukei = 0;
        for (Keihin k : KEIHIN) {
            if (k.jidai() == jidai && !tokushuKa(k)) {
                goukei += k.omomi();
            }
        }
        if (goukei <= 0) {
            return null;
        }
        int hiita = ran.nextInt(goukei);
        for (Keihin k : KEIHIN) {
            if (k.jidai() != jidai || tokushuKa(k)) {
                continue;
            }
            hiita -= k.omomi();
            if (hiita < 0) {
                return k;
            }
        }
        return null;
    }

    /**
     * めくる間隔（tick）。[i] は i 枚目をめくるまでの待ち、[10] は最後からボタンまで。
     *
     * ★ 合計 98tick ≒ 4.9秒（ご指示「5秒程度」）。
     *   頭に溜めを置き、最後の2枚だけ遅くして「止まる感じ」を出す。
     *   ★ 合計が 4〜6秒に収まることを GachaTest が見張る。
     */
    private static final int[] JUREN_MA = {12, 7, 7, 7, 7, 7, 7, 7, 9, 12, 16};

    /**
     * 10回まとめてまわす。
     *
     * ★★ 順序 ★★
     *   1. 断る条件を確かめる
     *   2. **先に10回ぶん抽選する**（まだ何も見せない）
     *   3. 持ち物の空きが足りるか数える。足りなければ【金を引かずに】中止
     *   4. 金を引く → 結果を控える → 伏せ札の画面を開く
     *   5. 1枚ずつめくる → めくり終わってから渡す（mekuru）
     *
     *   3 を抽選の後に置いているのは、金だけの当たりは枠を使わないため。
     *   先に「10枠 空けろ」と断ると、ほとんどの人が弾かれてしまう。
     */
    public void mawasu10(Player player, Kane kane) {
        if (mawashichu.contains(player.getName())) {
            player.sendMessage("[ガチャ] 今まわっています");
            return;
        }
        if (!kane.junbiOK()) {
            player.sendMessage("[ガチャ] データパックが読み込まれていません");
            return;
        }
        // ★ 渡し損ねが残っていれば、先にそれを片づける
        sanka(player);

        int jidai = kane.jidaiOf(player);
        int nedan = nedan(jidai) * JUREN_KAZU;

        int mae = kane.kojinZandaka(player);
        if (mae < nedan) {
            player.sendMessage("[ガチャ] 個人の金が足りません (必要 " + nedan
                    + " / 所持 " + mae + ")");
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }

        List<Keihin> deta = juren(jidai, player, kane);
        if (deta == null) {
            player.sendMessage("[ガチャ] 今は景品がありません（管理者に連絡してください）");
            return;
        }

        int iru = 0;
        for (Keihin k : deta) {
            if (k.kane() <= 0) {
                iru++;
            }
        }
        if (akiWaku(player) < iru) {
            player.sendMessage("[ガチャ] 持ち物の空きが足りません (必要 " + iru
                    + " 枠)。空けてから もう一度どうぞ");
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;         // ★ まだ金は引いていない
        }

        // --- 金を引いて、結果を控える ------------------------
        int ato = mae - nedan;
        kane.kojinKousin(player, ato);

        // --- 伏せ札の画面を作る --------------------------------
        //   ★ 画面を【作ってから】控える。控えてから作ると、
        //     Juren に画面を渡せない（上の Juren の説明を参照）。
        Inventory inv = Bukkit.createInventory(this, JUREN_SUU, JUREN_MIDASHI);
        for (int i = 0; i < JUREN_SUU; i++) {
            inv.setItem(i, shikiri());
        }
        for (int i = 0; i < JUREN_KAZU; i++) {
            inv.setItem(JUREN_WAKU[i], fuseta());
        }
        machi.put(player.getName(), new Juren(deta, jidai, mae, kane, inv));
        mawashichu.add(player.getName());

        // --- 開いて、めくり始める ------------------------------
        player.openInventory(inv);
        Enshutsu.oto(player, Enshutsu.OTO_KAITEN, 0.8f);
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> mekuru(player, inv, 0), JUREN_MA[0]);
    }

    /**
     * 伏せ札を1枚めくる。最後までめくったらボタンを出し、ここで初めて渡す。
     *
     * ★ 途中で閉じられていれば machi から消えているので、何もせず終わる。
     * ★ 切断されていれば、渡すのは sanka（次に入った時）に任せる。
     */
    private void mekuru(Player player, Inventory inv, int i) {
        Juren j = machi.get(player.getName());
        if (j == null || j.watashita) {
            return;                             // もう片づいている
        }
        if (!player.isOnline()) {
            mawashichu.remove(player.getName());
            return;                             // sanka が渡す
        }

        if (i >= JUREN_KAZU) {
            inv.setItem(JUREN_MOUICHIDO, mouichido(j.jidai));
            inv.setItem(JUREN_TOJIRU, tojiruBotan());
            mawashichu.remove(player.getName());
            watasu10(player, j);                // ★★ 渡すのはここ（めくり終えた後）★★
            return;
        }

        Keihin k = j.deta.get(i);
        inv.setItem(JUREN_WAKU[i], kekkaItem10(k));

        // ★ 音で等級が分かるようにする（止まる所を見る楽しみを邪魔しない程度に）
        if (k.toukyuu().equals(DAI)) {
            Enshutsu.oto(player, Enshutsu.OTO_CHU);
        } else if (k.toukyuu().equals(CHU)) {
            Enshutsu.oto(player, Enshutsu.OTO_MEKURI_CHU, 1.3f);
        } else {
            Enshutsu.oto(player, Enshutsu.OTO_SHO, 0.9f + 0.07f * i);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> mekuru(player, inv, i + 1), JUREN_MA[i + 1]);
    }

    /**
     * 十連の結果を実際に渡す。3つの出口（mekuru / tojita / sanka）が全部ここへ来る。
     *
     * ★ 金は【今の残高】に足す。控えた時の残高に足すと、
     *   めくっている5秒の間に動いた金（拾った・買った）を消してしまう。
     * ★ 持ち物に入り切らなければ足元へ落とす。空きは先に数えてあるが、
     *   めくっている間に拾い物をすると埋まることがある。
     */
    private void watasu10(Player player, Juren j) {
        if (j.watashita) {
            return;
        }
        j.watashita = true;
        machi.remove(player.getName());

        int zangaku = j.kane.kojinZandaka(player);
        for (Keihin k : j.deta) {
            if (k.kane() > 0) {
                zangaku += k.kane();
            } else {
                Map<Integer, ItemStack> amari = player.getInventory().addItem(watasuMono(k));
                for (ItemStack nokori : amari.values()) {
                    player.getWorld().dropItem(player.getLocation(), nokori);
                }
                Ibutsu.teniireta(player, j.kane, k.namae());     // 遺物なら勢力に知らせる
                tokushuTsuchi(player, j.kane, k.namae());        // 集める5種なら全勢力へ
            }
        }
        j.kane.kojinKousin(player, zangaku);

        shirase10(player, j.deta, j.mae, zangaku);
    }

    /** 十連の結果を文字で知らせる。大当たりは全体にも。 */
    private void shirase10(Player player, List<Keihin> deta, int mae, int ato) {
        StringBuilder b = new StringBuilder();
        boolean dai = false;
        for (Keihin k : deta) {
            if (oomonoKa(k)) {
                if (b.length() > 0) {
                    b.append(" / ");
                }
                b.append(hyouji(k));
            }
            if (k.toukyuu().equals(DAI)) {
                dai = true;
            }
        }
        String naka = (b.length() == 0) ? "大当たりなし" : b.toString();
        player.sendMessage(Enshutsu.kazaru(dai ? Enshutsu.ATARI : Enshutsu.GACHA,
                "十連: " + naka + " (個人の金 " + mae + " → " + ato + ")"));

        if (dai) {
            Enshutsu.oto(player, Enshutsu.OTO_DAI);
            Enshutsu.hanabi(player);
            for (Keihin k : deta) {
                if (k.toukyuu().equals(DAI)) {
                    Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.ATARI,
                            player.getName() + " がガチャで「" + hyouji(k) + "」を引き当てた"));
                }
            }
        } else {
            Enshutsu.oto(player, Enshutsu.OTO_CHU);
        }
    }

    /**
     * 10回ぶん抽選する。
     *
     * ★★ 天井（2026-08-22 のご指示「渋い」への答えの一つ）★★
     *   10回とも「小」以下だと、まとめてまわした意味がない。
     *   1つも「中」以上が無かった時だけ、最後の1つを
     *   【中以上だけの抽選】で引き直す。
     *   確率をいじるのではなく、下振れの底を上げるやり方。
     */
    private List<Keihin> juren(int jidai) {
        return juren(jidai, null, null);
    }

    /**
     * 10回ぶん抽選して、勢力ごとの決まり（特殊は各1個まで）を当てる。
     * ★ player が null なら決まりを当てない（検証用）。
     * ★ 天井の引き直しは特殊を対象にしない（決まりを迂回しないため）。
     */
    private List<Keihin> juren(int jidai, Player player, Kane kane) {
        List<Keihin> deta = new ArrayList<>();
        Set<String> konkai = new HashSet<>();
        for (int i = 0; i < JUREN_KAZU; i++) {
            Keihin k = erabu(jidai);
            if (k == null) {
                return null;
            }
            if (player != null) {
                k = kisei(k, player, kane, jidai, konkai);
            }
            deta.add(k);
        }
        boolean atari = false;
        for (Keihin k : deta) {
            if (chuIjou(k)) {
                atari = true;
            }
        }
        if (!atari) {
            Keihin k = erabuChuIjou(jidai);
            if (k != null) {
                deta.set(JUREN_KAZU - 1, k);
            }
        }
        return deta;
    }

    /** 「中」以上か。天井の判定に使う。 */
    private static boolean chuIjou(Keihin k) {
        return k.toukyuu().equals(CHU) || k.toukyuu().equals(DAI);
    }

    /** 持ち物の空き枠の数。★ 分からない時は 0 とみなす（安全側）。 */
    private static int akiWaku(Player player) {
        ItemStack[] naka = player.getInventory().getStorageContents();
        if (naka == null) {
            return 0;
        }
        int aki = 0;
        for (ItemStack it : naka) {
            if (it == null || it.getType() == Material.AIR) {
                aki++;
            }
        }
        return aki;
    }

    /** めくる演出の合計の長さ（tick）。検証で使う。 */
    public static int jurenNagasa() {
        int goukei = 0;
        for (int m : JUREN_MA) {
            goukei += m;
        }
        return goukei;
    }

    /** 景品を、画面に並べる見た目のアイテムにする。 */
    private ItemStack keihinItem(Keihin k) {
        ItemStack item = new ItemStack(k.material(), Math.max(1, k.kazu()));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(moji(hyouji(k)));
        int moderu = Shouri.tokushuModelData(k.namae());
        if (moderu > 0) {
            meta.setCustomModelData(moderu);     // MOD が絵を差し替える目印
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 実際に手渡す物を作る。
     *
     * ★★★ 特殊アイテムだけ、名前を付けて渡す ★★★
     *   勝利判定（Shouri.motteru）は【紙の表示名】で見分けている。
     *   ここで名前の無い紙を渡すと、5種そろえても永久に勝てない。
     *   2026-08-22 まで、まさにそうなっていた（渡す所だけ素の
     *   new ItemStack(...) で、名前を付けていなかった）。
     *
     * ★ ふつうの景品に名前を付けてはいけない。
     *   パンに名前を付けると、ふつうのパンと重ならなくなり
     *   持ち物がすぐ埋まる。名前を付けるのは特殊アイテムだけ。
     */
    private static ItemStack watasuMono(Keihin k) {
        ItemStack item = new ItemStack(k.material(), Math.max(1, k.kazu()));
        int moderu = Shouri.tokushuModelData(k.namae());
        if (moderu == 0) {
            return item;                         // ふつうの景品は素のまま
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(moji("§b" + k.namae()));
        meta.setLore(setsumeiGyou(k.namae()));
        meta.setCustomModelData(moderu);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 特殊アイテムの説明文。
     *
     * ★★ 集める5種と遺物では、書くことがまるで違う（2026-08-23 のご指摘）★★
     *   集める5種 … リーダーが5種そろえると【勝ち】（勝利条件そのもの）
     *   遺物      … 勝利条件には入らない。手に入れた【勢力】に効果が付く
     *
     *   2026-08-23 まで、遺物にも「リーダーが5種そろえると勝ち」と書いていた。
     *   持ち主は集めれば勝てると思い込み、いつまでも勝てない。説明が嘘になる。
     *
     * ★ 効果の文は Shouri.IBUTSU_HYOU が正本。ここで書き直さない。
     */
    static List<String> setsumeiGyou(String namae) {
        List<String> setsumei = new ArrayList<>();
        String kouka = Shouri.ibutsuSetsumei(namae);
        if (kouka != null) {
            setsumei.add(moji("§7" + Kane.jidaiMei(Shouri.ibutsuJidai(namae)) + "の遺物"));
            setsumei.add(moji("§b" + kouka));
            setsumei.add(moji("§7手に入れた勢力の全員に効果が付く"));
            setsumei.add(moji("§8集めても勝ちにはならない"));
            return setsumei;
        }
        setsumei.add(moji("§7時代の特殊アイテム"));
        setsumei.add(moji("§7リーダーが5種そろえると勝ち"));
        return setsumei;
    }

    /**
     * 今の時代で出せる景品の中から、重みに応じて1つ選ぶ。
     *
     * 重みの選び方:
     *   1. 出せる景品の重みを全部足す (例: 101)
     *   2. 0以上 101未満 の数をひとつ引く
     *   3. 先頭から重みを引いていき、0未満になったところが当たり
     */
    private Keihin erabu(int jidai) {
        int goukei = 0;
        for (Keihin k : KEIHIN) {
            if (k.jidai() == jidai) {
                goukei += k.omomi();
            }
        }
        if (goukei <= 0) {
            return null;
        }
        int hiita = ran.nextInt(goukei);
        for (Keihin k : KEIHIN) {
            if (k.jidai() != jidai) {
                continue;
            }
            hiita -= k.omomi();
            if (hiita < 0) {
                return k;
            }
        }
        return null;   // ここには来ない
    }

    /**
     * 「中」以上だけを対象に、重みで1つ選ぶ。十連の天井に使う。
     *
     * ★ 重みの比はそのまま使う。中の中でも出やすさの差は残る。
     */
    private Keihin erabuChuIjou(int jidai) {
        // ★ 特殊は対象にしない。天井で「勢力ごとに各1個まで」を迂回させないため
        int goukei = 0;
        for (Keihin k : KEIHIN) {
            if (k.jidai() == jidai && chuIjou(k) && !tokushuKa(k)) {
                goukei += k.omomi();
            }
        }
        if (goukei <= 0) {
            return null;
        }
        int hiita = ran.nextInt(goukei);
        for (Keihin k : KEIHIN) {
            if (k.jidai() != jidai || !chuIjou(k) || tokushuKa(k)) {
                continue;
            }
            hiita -= k.omomi();
            if (hiita < 0) {
                return k;
            }
        }
        return null;   // ここには来ない
    }

    /** クリックされた枠が「まわすボタン（単発）」か。 */
    public boolean botanKa(int slot) {
        return slot == BUTTON;
    }

    /** 十連の結果画面の枠の並び。★ MOD 側との突き合わせに使う。 */
    public static int[] jurenWaku() {
        return JUREN_WAKU.clone();
    }

    /** 十連の結果画面の見出し。★ MOD 側との突き合わせに使う。 */
    public static String jurenMidashi() {
        return JUREN_MIDASHI;
    }

    /** 画面を閉じた人を忘れる。開きっぱなしの記録を残さないため。 */
    public void tojita(String namae, Inventory inv) {
        gamen.remove(namae);

        // ★ 十連の結果を見ている途中で閉じた → その場で渡す（渡し損ねの出口その2）
        // ★ 閉じた画面が【その結果の画面そのもの】の時だけ渡す。
        //   「もう一度 十連」で開き直す時にも閉じる知らせが来るが、
        //   そちらは新しい結果の画面なので、ここで渡してはいけない（2026-08-23）。
        if (inv != null && inv.getSize() == JUREN_SUU) {
            Juren j = machi.get(namae);
            Player p = Bukkit.getPlayerExact(namae);
            if (j != null && p != null && !j.watashita && j.inv == inv) {
                mawashichu.remove(namae);
                watasu10(p, j);
            }
        }
    }

    /**
     * サーバーに入った時。十連の途中で切断した人に、渡し損ねた結果を渡す
     * （渡し損ねの出口その3）。★ JidaiCraft.onJoin から呼ぶ。
     */
    public void sanka(Player player) {
        Juren j = machi.get(player.getName());
        if (j != null && !j.watashita) {
            mawashichu.remove(player.getName());
            watasu10(player, j);
            player.sendMessage(Enshutsu.kazaru(Enshutsu.GACHA,
                    "切断中だった十連の結果を渡しました"));
        }
    }

    /**
     * 当たった物を渡す。
     *
     * ★ 金はアイテムを渡さず、個人の金を増やすだけ。
     *   金額はガチャの当たり用の値で、売却レートとは無関係。
     * ★ 演出の後に呼ぶ。切断された時もここを通す（渡し損ねを防ぐため）。
     */
    private void watasu(Player player, Keihin atari, int ato, Kane kane) {
        if (atari.kane() > 0) {
            kane.kojinKousin(player, ato);
            return;
        }
        if (player.isOnline()) {
            player.getInventory().addItem(watasuMono(atari));
            Ibutsu.teniireta(player, kane, atari.namae());     // 遺物なら勢力に知らせる
            tokushuTsuchi(player, kane, atari.namae());        // 集める5種なら全勢力へ
        } else {
            // 離れている人の持ち物は触れない。取りこぼしを記録だけ残す。
            plugin.getLogger().warning("ガチャの景品を渡せませんでした（切断）: "
                    + player.getName() + " / " + atari.namae());
        }
    }

    /** 演出の合計の長さ（tick）。検証で使う。 */
    public static int enshutsuNagasa() {
        int goukei = 0;
        for (int m : MAGARI) {
            goukei += m;
        }
        return goukei;
    }

    /**
     * 文字列を画面に出せる形に変換する。
     * ★ 何もしないとアイテムの名前と説明は【斜体】で出る。
     *   Enshutsu.MODOSU を頭に付けて、まっすぐな字に戻している。
     */
    private static String moji(String s) {
        return Enshutsu.MODOSU + s;
    }
}
