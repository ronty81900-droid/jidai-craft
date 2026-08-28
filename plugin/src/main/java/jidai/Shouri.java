// =============================================================
// Shouri.java ── 勝利条件のうち、プラグインが見る1つ
//
//   ★★ 勝ち方は4つある ★★
//     1 戦争勝利     … 全勢力を植民地にする → データパック(sensou/shouri)
//     3 経済勝利     … 貯金 50万           → データパック(sensou/keizai)
//     4 超特殊勝利   … リーダーが特殊5種    → **ここ**
//     5 未来到達勝利 … 現代から未来へ進む   → データパック(shinko/shounin_*)
//
//   ★ 番号 2 は「人望勝利（傭兵の過半数）」だった。傭兵を止めた
//     2026-08-26 から使っていない。番号は詰めていない（データパックの
//     何箇所かに数字で書いてあり、付け替えると黙って食い違うため）。
//     経緯は docs/蛮族と傭兵（休止中）.md。
//
//   1・3・5 は「スコアを見るだけ」なのでデータパックが持っている。
//   4 は「持ち物の中身」を見る必要があり、データパックでは数えられない
//   （1.20.1 に execute if items が無い）。だからプラグイン側にある。
//
//   ★ 勝ちを宣言する所は1か所にまとめてある。
//     jidai:sensou/shouri_kakutei を呼ぶだけで、
//     二重に祝わない見張りも、通知も、花火も向こうがやる。
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 超特殊勝利を見張る係。1秒に1回 呼ばれる。
 */
public final class Shouri {

    /**
     * ガチャから出る特殊アイテムの5種。**ここが名前の正本。**
     *
     *   { 名前, 絵のファイル名, 見た目の番号 }
     *
     * ★★ 名前で見分けている ★★
     *   金床が使えない世界なので、参加者はアイテムの名前を変えられない。
     *   （鍛冶台・金床は塞いである。JidaiCraft.TSUKURU_DAI）
     *   そのため「名前が一致する紙」は、ガチャから出た物だけになる。
     * ★ 中身を変える時は Gacha.java の景品表と必ずそろえること。
     *
     * ★★ 見た目の番号（CustomModelData）★★
     *   中身はただの紙のまま、**クライアント MOD が絵だけ差し替える**。
     *   MOD を入れていない人には紙のまま見えるが、名前で分かるので困らない。
     *   サーバーに MOD を入れる必要も、独自アイテムを登録する必要もない。
     *   絵の対応表（minecraft/models/item/paper.json）は
     *   `clientmod/tools/tokushu_moderu.py` が **この表から作る**。手で写さない。
     *
     * ★ 2026-08-22: 各時代の有名な物へ改名（鉄器のみ据え置き）。
     */
    private static final String[][] TOKUSHU_HYOU = {
            {"古びた護符",     "gofu",        "8201"},   // 鉄器
            {"聖杯の欠片",     "seihai",      "8202"},   // 中世
            {"羊皮紙の海図",   "kaizu",       "8203"},   // 中世
            {"蒸気機関の歯車", "haguruma",    "8204"},   // 近代
            {"月の石",         "tsukinoishi", "8205"},   // 現代
    };

    /**
     * 遺物 11種（2026-08-22 のご指示・二度目の表）。**手に入れた勢力に効果が付く。勝利条件には入らない。**
     *
     *   { 名前, 絵のファイル名, 見た目の番号, 時代, 効果の種類, 説明 }
     *
     * ★ 効果の本体は Ibutsu.java。種類の名前は Ibutsu の定数と同じ字。
     * ★ 時代ごとの数: 鉄器2 / 中世3 / 近代3 / 現代3。
     * ★ 絵は 64×64 で依頼中（Codex 依頼 第6回）。無い間は紙の絵のまま出る。
     */
    private static final String[][] IBUTSU_HYOU = {
            {"ヒッタイトの戦車",       "sensha",     "8211", "1", "SPEED",      "勢力全員の移動速度が 1.2倍になる"},
            {"ロンバルディアの鉄王冠", "oukan",      "8212", "1", "HORI",       "石を掘った時に貴金属が出る確率が 1.2倍になる"},
            {"聖剣エクスカリバー",     "excalibur",  "8213", "2", "STRENGTH",   "勢力全員の近接の攻撃力が 10% 上がる"},
            {"テンプル騎士団の盾",     "tate",       "8214", "2", "RESISTANCE", "勢力全員の受けるダメージが 15% 減る"},
            {"武器商人の手形",         "tegata",     "8215", "2", "JUKI",       "銃器専門店が貯金 5,500 で開く（通常 10,000）"},
            {"ベッセマー転炉",         "tenro",      "8216", "3", "HASTE",      "勢力全員の採掘が速くなる（貴金属が早く集まる）"},
            {"ロスチャイルドの金庫",   "kinko",      "8217", "3", "URI",        "貴金属の売値が 1.2倍になる"},
            {"死の商人の手引書",       "tebiki",     "8218", "3", "NEBIKI",     "銃器専門店で買うと代金の 10% が戻る"},
            {"ペニシリン",             "penicillin", "8219", "4", "REGEN",      "勢力全員の自然回復が 20% 増える"},
            {"暗視ゴーグル",           "goggle",     "8220", "4", "NIGHT",      "勢力全員が夜や洞窟でも見える（暗視）"},
            {"スマートフォン",         "sumaho",     "8221", "4", "JOHO",       "/joho で他勢力の貯金・石油・時代が見える"},
    };

    /** 名前だけを取り出した物。★ 上の表から作る（二重に書かない）。 */
    private static final String[] TOKUSHU = mei();

    private static String[] mei() {
        String[] a = new String[TOKUSHU_HYOU.length];
        for (int i = 0; i < a.length; i++) {
            a[i] = TOKUSHU_HYOU[i][0];
        }
        return a;
    }

    private final JidaiCraft plugin;

    public Shouri(JidaiCraft plugin) {
        this.plugin = plugin;
    }

    /** 1秒に1回。勝ちが出ていないかを見る。 */
    public void byoumai(Kane kane, Seiryoku seiryoku) {
        if (!kane.junbiOK()) {
            return;      // データパックがまだ
        }
        if (kane.shouriSumi()) {
            return;      // もう決着している
        }
        chotokushu(kane, seiryoku);
    }

    // =========================================================
    //  勝利条件その4「超特殊勝利」
    // =========================================================

    /**
     * どこかの勢力のリーダーが、特殊アイテムを5種すべて持っていれば勝ち。
     *
     * ★ 見るのはリーダー1人の持ち物だけ。
     *   「勢力の誰かが持っていればよい」にすると、
     *   5人で1つずつ持って集まらないまま勝ててしまう。
     *   1人に集めさせることで、集める過程そのものが動きになる。
     */
    private void chotokushu(Kane kane, Seiryoku seiryoku) {
        for (String team : Seiryoku.zenTeam()) {
            String namae = seiryoku.leaderMei(team);
            if (namae == null) {
                continue;
            }
            Player leader = Bukkit.getPlayerExact(namae);
            if (leader == null) {
                continue;      // 居ない人の持ち物は見られない
            }
            if (zensyu(leader)) {
                plugin.getLogger().info("超特殊勝利: " + Seiryoku.mei(team)
                        + " (リーダー " + namae + ")");
                kakutei(Seiryoku.bangou(team, kane), 4);
                return;
            }
        }
    }

    /** その人が特殊アイテムを5種すべて持っているか。 */
    public boolean zensyu(Player player) {
        for (String mei : TOKUSHU) {
            if (!motteru(player, mei)) {
                return false;
            }
        }
        return true;
    }

    /**
     * その人が、その名前の紙を実際に持っているか（誰でも呼べる）。
     *
     * ★★ 2026-08-24 の実機の指摘で足した ★★
     *   「スマホを持っているのに情報が見れない」。
     *   遺物の効果は【勢力の獲得記録】だけを見ていて、
     *   手元の紙は1度も見ていなかった。
     *   使う側が自分で打つ物（/joho）は、持っている本人にも効くべき。
     */
    public static boolean kamiMotteru(Player player, String mei) {
        for (ItemStack it : player.getInventory().getContents()) {
            if (it == null || it.getType() != Material.PAPER) {
                continue;
            }
            ItemMeta m = it.getItemMeta();
            if (m != null && m.hasDisplayName() && m.getDisplayName().contains(mei)) {
                return true;
            }
        }
        return false;
    }

    /** その名前の紙を持っているか。 */
    private boolean motteru(Player player, String mei) {
        for (ItemStack it : player.getInventory().getContents()) {
            if (it == null || it.getType() != Material.PAPER) {
                continue;
            }
            ItemMeta m = it.getItemMeta();
            if (m != null && m.hasDisplayName()
                    && m.getDisplayName().contains(mei)) {
                return true;
            }
        }
        return false;
    }

    /** 特殊アイテムの種類数。文書と検査で使う。 */
    public static int tokushuKazu() {
        return TOKUSHU.length;
    }

    /** 特殊アイテムの名前。 */
    public static String[] tokushuMei() {
        return TOKUSHU.clone();
    }

    /**
     * その表示名が、特殊アイテム（集める5種か遺物）の物か。
     *
     * ★ 色の記号（§b）や印が頭に付くので、含むかどうかで見る。
     *   motteru と同じ見分け方にそろえてある。
     */
    static boolean tokushuNoNamaeKa(String hyoujiMei) {
        if (hyoujiMei == null) {
            return false;
        }
        for (String mei : TOKUSHU) {
            if (hyoujiMei.contains(mei)) {
                return true;
            }
        }
        for (String[] r : IBUTSU_HYOU) {
            if (hyoujiMei.contains(r[0])) {
                return true;
            }
        }
        return false;
    }

    /**
     * 居る人 全員の持ち物から、特殊アイテム（名前の付いた紙）を取り上げる。
     *
     * ★★ なぜ運営の全部リセットで要るか（2026-08-23 のご指示）★★
     *   「勢力ごとに各1個」の記録だけ消しても、参加者の手元に紙が残っていると、
     *   リセットした直後にリーダーが5種そろえたまま＝**即 勝利**になる。
     *   遺物も同じで、紙が残っていると次の催しへ持ち越される。
     *   記録と現物を同時に戻して、はじめて「最初に戻した」と言える。
     *
     * ★ 離れている人の持ち物は触れない。戻ってきた時に紙を持っている場合があるが、
     *   その時は記録が空なので、もう一度 出ることはある（二重には増えない）。
     *
     * @return 取り上げた数
     */
    public static int kaishu() {
        int kazu = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack[] naka = p.getInventory().getContents();
            for (int i = 0; i < naka.length; i++) {
                ItemStack it = naka[i];
                if (it == null || it.getType() != Material.PAPER) {
                    continue;
                }
                ItemMeta m = it.getItemMeta();
                if (m == null || !m.hasDisplayName()
                        || !tokushuNoNamaeKa(m.getDisplayName())) {
                    continue;
                }
                kazu += it.getAmount();
                p.getInventory().setItem(i, null);
            }
        }
        // ★★ 2026-08-26 の監査で見つけた掃除の漏れ ★★
        //   死んで まだ蘇っていない人の紙は【預かり】に入っていて、
        //   ここは「今 生きている人の持ち物」しか見ていなかった。
        //   残すと、リセットの後で蘇った瞬間に特殊アイテムが戻ってくる。
        for (List<ItemStack> nokori : AZUKARI.values()) {
            for (ItemStack it : nokori) {
                kazu += it.getAmount();
            }
        }
        AZUKARI.clear();
        return kazu;
    }

    /**
     * その効果を持つ遺物の名前。★ 表から引く（名前を2か所に書かないため）。
     *   無ければ空文字（null を返すと、うっかり使った時に落ちる）。
     */
    static String ibutsuNoMei(String kouka) {
        for (String[] r : IBUTSU_HYOU) {
            if (r[4].equals(kouka)) {
                return r[0];
            }
        }
        return "";
    }

    /**
     * 特殊アイテムの紙を1枚 作る（運営が確かめる用・2026-08-24）。
     *
     * ★ ガチャが渡す物とまったく同じ形にする。
     *   名前・説明・見た目の番号を別に組み立てると、
     *   「運営が出した物では絵が出るのに、ガチャの物では出ない」が起きる。
     *   番号は tokushuModelData、説明は Gacha.setsumeiGyou が正本。
     *
     * @return 知らない名前なら null
     */
    public static ItemStack tsukuruKami(String mei) {
        int moderu = tokushuModelData(mei);
        if (moderu == 0) {
            return null;
        }
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(Enshutsu.MODOSU + "§b" + mei);
        m.setLore(Gacha.setsumeiGyou(mei));
        m.setCustomModelData(moderu);
        item.setItemMeta(m);
        return item;
    }

    /**
     * 死んだ時に落ちないよう、特殊アイテムを落とし物から抜いて預かる。
     *
     * ★★ 2026-08-24 のご指示「特殊アイテムも死んでも落ちないように」★★
     *   時代のツルハシと同じ形（Tsuruhashi.shinda / ikikaetta）。
     *   落とし物から抜くだけだと消えてしまうので、預かって生き返った時に返す。
     *
     * ★ 集める5種も遺物も どちらも守る。集める5種は勝敗そのものだし、
     *   遺物の紙は戦利品なので、死んだだけで消えると意味が分からない。
     *
     * @return 抜いた数
     */
    public static int shinda(String namae, List<ItemStack> ochirumono) {
        List<ItemStack> azukari = new ArrayList<>();
        for (int i = ochirumono.size() - 1; i >= 0; i--) {
            ItemStack it = ochirumono.get(i);
            if (it == null || it.getType() != Material.PAPER) {
                continue;
            }
            ItemMeta m = it.getItemMeta();
            if (m == null || !m.hasDisplayName()
                    || !tokushuNoNamaeKa(m.getDisplayName())) {
                continue;
            }
            azukari.add(it.clone());
            ochirumono.remove(i);
        }
        if (!azukari.isEmpty()) {
            AZUKARI.put(namae, azukari);
        }
        return azukari.size();
    }

    /**
     * 生き返った時に、預かっていた特殊アイテムを返す。
     *
     * ★ 持ち物がいっぱいなら足元へ落とす（預かった物を消さないため）。
     */
    public static int ikikaetta(Player player) {
        List<ItemStack> azukari = AZUKARI.remove(player.getName());
        if (azukari == null || azukari.isEmpty()) {
            return 0;
        }
        for (ItemStack it : azukari) {
            for (ItemStack nokori : player.getInventory().addItem(it).values()) {
                player.getWorld().dropItem(player.getLocation(), nokori);
            }
        }
        return azukari.size();
    }

    /**
     * 死んだ人 → 預かっている特殊アイテム。
     *
     * ★ 生き返るまでの数秒だけ持つ。サーバーが落ちるとここは消えるが、
     *   落ちた時は落とし物も残らないので、どちらにせよ同じこと。
     */
    private static final Map<String, List<ItemStack>> AZUKARI = new HashMap<>();

    /**
     * 特殊アイテムの見た目の番号。特殊でなければ 0。
     *
     * ★ 0 かどうかで「特殊アイテムか」を見分けられるので、
     *   名前の一覧を別の所へ写さずに済む。
     */
    public static int tokushuModelData(String namae) {
        for (String[] r : TOKUSHU_HYOU) {
            if (r[0].equals(namae)) {
                return Integer.parseInt(r[2]);
            }
        }
        // ★ 遺物も「特殊アイテム」。勢力ごとに各1個・絵の差し替えは同じ仕組みに乗る
        for (String[] r : IBUTSU_HYOU) {
            if (r[0].equals(namae)) {
                return Integer.parseInt(r[2]);
            }
        }
        return 0;
    }

    /** 遺物か。 */
    public static boolean ibutsuKa(String namae) {
        return ibutsuKouka(namae) != null;
    }

    /** 遺物の効果の種類（Ibutsu の定数と同じ字）。遺物でなければ null。 */
    public static String ibutsuKouka(String namae) {
        for (String[] r : IBUTSU_HYOU) {
            if (r[0].equals(namae)) {
                return r[4];
            }
        }
        return null;
    }

    /** 遺物の説明。遺物でなければ null。 */
    public static String ibutsuSetsumei(String namae) {
        for (String[] r : IBUTSU_HYOU) {
            if (r[0].equals(namae)) {
                return r[5];
            }
        }
        return null;
    }

    /** 遺物の出る時代。遺物でなければ 0。 */
    public static int ibutsuJidai(String namae) {
        for (String[] r : IBUTSU_HYOU) {
            if (r[0].equals(namae)) {
                return Integer.parseInt(r[3]);
            }
        }
        return 0;
    }

    /** 遺物の表。MOD 側の模型を作る道具と、検証が読む。 */
    public static String[][] ibutsuHyou() {
        String[][] c = new String[IBUTSU_HYOU.length][];
        for (int i = 0; i < c.length; i++) {
            c[i] = IBUTSU_HYOU[i].clone();
        }
        return c;
    }

    /** 名前・絵・番号の表。MOD 側の模型を作る道具と、検証が読む。 */
    public static String[][] tokushuHyou() {
        String[][] c = new String[TOKUSHU_HYOU.length][];
        for (int i = 0; i < c.length; i++) {
            c[i] = TOKUSHU_HYOU[i].clone();
        }
        return c;
    }

    // =========================================================
    //  略奪で「集める5種」を奪う（2026-08-23 のご指示・案D）
    // =========================================================

    /**
     * 略奪が成立した時、相手勢力のリーダーから「集める5種」の紙を1枚 奪う。
     *
     * ★★ なぜリーダーから奪うか ★★
     *   勝ちになるのは【リーダー1人が5種そろえた】時だけ（chotokushu）。
     *   紙は誰でも持てるが、勝敗に関わるのはリーダーの手元にある物だけなので、
     *   そこを狙えないと「奪える」ことに意味が出ない。
     *
     * ★★ なぜ遺物を奪わないか ★★
     *   遺物の効果は【手に入れた勢力】に付いていて、紙を渡しても移らない
     *   （Ibutsu.java）。奪っても何も起きないのに、奪えた気になってしまう。
     *   奪えるのは勝敗が動く「集める5種」だけにする。
     *
     * ★ 石油の略奪（jidai:sensou/sekiyu_ubau）と同じ考え方。
     *   個人が持っている物は、個人から奪って、全員の前で名指しで出す。
     *
     * ★ リーダーが居ない（落ちている）時は何も起きない。
     *   離れている人の持ち物には触れないため。
     *
     * @param ubauHito 略奪した人
     * @param aiteTeam 略奪された勢力のチーム名
     * @param seiryoku リーダーを引くため
     * @return 奪った物の名前。奪えなければ null
     */
    public static String ryakudatsuDeUbau(Player ubauHito, String aiteTeam, Seiryoku seiryoku) {
        String leaderMei = seiryoku.leaderMei(aiteTeam);
        if (leaderMei == null) {
            return null;                     // リーダーが決まっていない
        }
        Player leader = Bukkit.getPlayerExact(leaderMei);
        if (leader == null) {
            return null;                     // 居ない人の持ち物は見られない
        }

        // ★ 先に受け取れるかを見る。受け取れないのに取り上げると、紙が消える。
        if (ubauHito.getInventory().firstEmpty() < 0) {
            ubauHito.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO,
                    "持ち物がいっぱいで、特殊アイテムを奪えなかった"));
            Enshutsu.oto(ubauHito, Enshutsu.OTO_DAME);
            return null;
        }

        ItemStack[] naka = leader.getInventory().getContents();
        for (int i = 0; i < naka.length; i++) {
            ItemStack it = naka[i];
            if (it == null || it.getType() != Material.PAPER) {
                continue;
            }
            ItemMeta m = it.getItemMeta();
            if (m == null || !m.hasDisplayName()) {
                continue;
            }
            String mei = atsumeruNoNamae(m.getDisplayName());
            if (mei == null) {
                continue;                    // 遺物や、ただの名前付きの紙
            }

            // 1枚だけ移す。★ 同じ紙が2枚 重なることはないが、数で見て安全に減らす。
            ItemStack ichimai = it.clone();
            ichimai.setAmount(1);
            if (it.getAmount() <= 1) {
                leader.getInventory().setItem(i, null);
            } else {
                it.setAmount(it.getAmount() - 1);
            }
            ubauHito.getInventory().addItem(ichimai);

            leader.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO,
                    "§c" + ubauHito.getName() + " に「" + mei + "」を奪われた"));
            Enshutsu.oto(leader, Enshutsu.OTO_DAME);
            Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.SENSO,
                    ubauHito.getName() + " が " + Seiryoku.mei(aiteTeam)
                            + " のリーダーから「" + mei + "」を奪った"));
            Enshutsu.otoZenin(Enshutsu.OTO_DAI);
            return mei;
        }
        return null;                          // リーダーは1枚も持っていなかった
    }

    /**
     * その表示名が「集める5種」の物なら、その名前を返す。違えば null。
     *
     * ★ tokushuNoNamaeKa は遺物も真を返す。奪う側はそれでは困るので分けてある。
     */
    private static String atsumeruNoNamae(String hyoujiMei) {
        if (hyoujiMei == null) {
            return null;
        }
        for (String mei : TOKUSHU) {
            if (hyoujiMei.contains(mei)) {
                return mei;
            }
        }
        return null;
    }

    // =========================================================

    /**
     * 勝ちを宣言する。中身はデータパックに任せる。
     *   shu … 1=戦争 / 3=経済 / 4=超特殊 / 5=未来到達
     *         ★ 2（人望）は傭兵ごと止めた。番号は詰めていない。
     */
    private void kakutei(int bangou, int shu) {
        if (bangou <= 0) {
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "scoreboard players set #s_kuni sagyou " + bangou);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "scoreboard players set #shouri_shu sagyou " + shu);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "function jidai:sensou/shouri_kakutei");
    }
}
