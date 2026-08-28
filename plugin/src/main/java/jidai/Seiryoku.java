// =============================================================
// Seiryoku.java ── 勢力そのものの状態
//
//   リーダー・代行・下剋上の権利・占領されているか・直近の戦争。
//
//   ★★ 戦争そのものはデータパックが持つ ★★
//     占領・略奪・開戦・終了はすべてデータパック側。
//     このファイルは、その結果（スコア）を**読んで判断するだけ**。
//     1つも書き込まない。
//     データパックがまだスコアを作っていない間は、どれも 0 が返るので、
//     「占領されていない・再戦禁止なし・平時」として自然に動く。
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class Seiryoku {

    /** 設定ファイルの見出し */
    private static final String KEY_LEADER = "leader";
    private static final String KEY_DAIKOU = "daikou";

    /**
     * 勢力の一覧。データパックのチーム名・日本語名・番号と揃える。
     *
     * ★★ 番号はここに書かない ★★
     *   勢力の番号を配るのはデータパック（load.mcfunction の bangou）。
     *   両方に書くと必ずずれるので、番号が要る時は毎回スコアを読む。
     *
     * ★ この5つは、データパックの load.mcfunction が作るチーム名と
     *   1対1で対応しています（2026-08-18 に5勢力そろいました）。
     *   データパック側の名前が変わったら、この表の1列目を直します。
     */
    private static final String[][] SEIRYOKU = {
            //  チーム名    日本語名
            {"kyuryo",  "丘陵"},
            {"shinrin", "森林"},
            {"kawa",    "川"},
            {"naikai",  "内海"},
            {"iwaba",   "岩場"},
    };

    /** "チーム名=プレイヤー名" の並びで持つ。設定ファイルにそのまま書ける形 */
    private List<String> leader = new ArrayList<>();
    private List<String> daikou = new ArrayList<>();

    /** 保存先。書き換えたらその場でファイルに書き出すために持つ */
    private final JidaiCraft plugin;

    public Seiryoku(JidaiCraft plugin) {
        this.plugin = plugin;
    }

    // --- 勢力の名前 -----------------------------------------------

    /** チーム名の全部。 */
    public static String[] zenTeam() {
        String[] t = new String[SEIRYOKU.length];
        for (int i = 0; i < SEIRYOKU.length; i++) {
            t[i] = SEIRYOKU[i][0];
        }
        return t;
    }

    /** チーム名 → 勢力の番号。知らないチームなら 0。 */
    /**
     * 勢力の番号。★ データパックの bangou を読むだけ。
     *   まだ配られていない勢力は 0 が返る。
     */
    public static int bangou(String team, Kane kane) {
        return kane.bangou(mei(team));
    }

    /**
     * 番号 → チーム名。占領しているのが誰かを知るために使う。
     * 見つからなければ null。
     */
    public static String teamKara(int bangou, Kane kane) {
        if (bangou <= 0) {
            return null;
        }
        for (String[] g : SEIRYOKU) {
            if (kane.bangou(g[1]) == bangou) {
                return g[0];
            }
        }
        return null;
    }

    /** チーム名 → 日本語名。知らないチームならそのまま返す。 */
    public static String mei(String team) {
        for (String[] g : SEIRYOKU) {
            if (g[0].equals(team)) {
                return g[1];
            }
        }
        return team;
    }

    /** 知っている勢力か。 */
    public static boolean seiryokuKa(String team) {
        for (String[] g : SEIRYOKU) {
            if (g[0].equals(team)) {
                return true;
            }
        }
        return false;
    }

    // --- 表の読み書き（"鍵=値" の並びを扱う小道具） ----------------

    private static String hiku(List<String> hyou, String kagi) {
        for (String gyou : hyou) {
            int i = gyou.indexOf('=');
            if (i > 0 && gyou.substring(0, i).equals(kagi)) {
                return gyou.substring(i + 1);
            }
        }
        return null;
    }

    private static void ireru(List<String> hyou, String kagi, String atai) {
        hyou.removeIf(g -> {
            int i = g.indexOf('=');
            return i > 0 && g.substring(0, i).equals(kagi);
        });
        hyou.add(kagi + "=" + atai);
    }

    // --- リーダー -------------------------------------------------

    /** 勢力のリーダーの名前。決まっていなければ null。 */
    public String leaderMei(String team) {
        return hiku(leader, team);
    }

    /** 勢力の代行の名前。決まっていなければ null。 */
    public String daikouMei(String team) {
        return hiku(daikou, team);
    }

    /** リーダーを決める。運営が使う。 */
    /**
     * データパックが見る目印を付け直す。
     *
     * ★★ これが無いと下剋上が絶対に発動しない ★★
     *   データパックの gekokujo_tsukau は `@s[tag=jidai_leader]` で
     *   リーダーを見分けている。目印を付けるのはプラグインの仕事だと
     *   向こうのコメントに書いてある。付け忘れると、条件を満たしていても
     *   「行使できるのはリーダーだけです」で必ず断られる。
     *
     * ★ 目印はプレイヤーに保存されるので、外す方も必ずやる。
     *   今つながっている人だけを見る（離れている人は入り直した時に付け直す）。
     */
    public void mejirushiNaosu() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            mejirushi(p);
        }
    }

    /** 1人ぶん。リーダーか代行なら付ける、違えば外す。 */
    public void mejirushi(Player p) {
        boolean leader = false;
        for (String t : zenTeam()) {
            if (leaderKa(t, p.getName())) {
                leader = true;
                break;
            }
        }
        String cmd = "tag " + p.getName() + (leader ? " add " : " remove ") + "jidai_leader";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    public void leaderKimeru(String team, String namae) {
        ireru(leader, team, namae);
        hozon();
        mejirushiNaosu();
    }

    /** 代行を決める。リーダーが1名だけ指名できる。 */
    public void daikouKimeru(String team, String namae) {
        ireru(daikou, team, namae);
        hozon();
        mejirushiNaosu();
    }

    /**
     * その人が、その勢力のリーダー（か代行）か。
     * ★ 下剋上を宣言できるのはここが true の人だけ。
     *   戦争宣誓は全員が押せる（そちらでは使わない）。
     */
    public boolean leaderKa(String team, String namae) {
        return namae.equals(leaderMei(team)) || namae.equals(daikouMei(team));
    }

    // --- 下剋上の権利 ---------------------------------------------
    //
    // ★★ 権利はプラグインが持たない ★★
    //   買うのも消費するのもデータパック（gekokujo_kau / gekokujo_tsukau）。
    //   プラグインが別に数を持つと、必ず食い違う。だから読むだけにする。

    /** その勢力が下剋上の権利を持っているか。 */
    public boolean kenriAru(String team, Kane kane) {
        return kane.gekokujoKenri(mei(team)) >= 1;
    }

    // --- 占領と再戦禁止（データパックのスコアを読むだけ） ----------

    /**
     * その勢力を占領している勢力のチーム名。占領されていなければ null。
     *
     * ★ データパックの `senryou`（保持者＝勢力の日本語名）を読む。
     *   中身は「占領している勢力の**番号**」なので、番号から名前に戻す。
     */
    public String senryoShiteiru(String team, Kane kane) {
        return teamKara(kane.senryou(mei(team)), kane);
    }

        /**
     * その勢力が今、どこかと戦争中（準備または交戦）か。
     *
     * ★ 1.20.1 移行で、状態が「勢力ごと」に持たれるようになった。
     *   以前は「組ごと」だったので相手になり得る勢力を総当たりしていたが、
     *   いまは自分の分を1回読むだけで足りる。
     */
    public boolean sensouChu(String team, Kane kane) {
        int j2 = kane.sensouJotai(mei(team));
        return j2 == 1 || j2 == 2;
    }

    // --- 宣戦の相手に選べるか -------------------------------------

    /**
     * 自勢力から見て、その相手を宣戦の相手に選べるか。
     * 選べない時はその理由を返す。選べるなら null を返す。
     *
     * ★ 画面では、選べない相手を灰色にして、この理由をそのまま出す。
     *   「押せない」だけだと、なぜ押せないのか分からないため。
     *
     * 断る条件は4つ（指示書どおりの並び）。
     *   1. 勢力でない
     *   2. 自分の勢力
     *   3. 再戦禁止が残っている
     *   4. 相手がすでに戦争中
     * ★ 5つ目「自勢力がすでに戦争中」は指示書に無いが足してある。
     *   戦争中に宣戦しても意味が無く、100 を捨てるだけになるため。
     *   要らなければ、その1文を消せば元どおり。
     */
    public String senseniDekiruka(String jibun, String aite, Kane kane) {
        if (jibun == null || !seiryokuKa(jibun)) {
            // どこにも入っていない人（運営・見学者）がここに落ちる
            return "勢力に入っていません";
        }
        if (aite == null || !seiryokuKa(aite)) {
            return "勢力ではありません";
        }
        if (aite.equals(jibun)) {
            return "自分の勢力です";
        }
        int aida = kane.sensou(mei(jibun), mei(aite));
        if (aida == 1 || aida == 2) {
            return "その勢力とはもう戦争中です";
        }
        if (aida == 3) {
            int nokori = kane.sensouByou(mei(jibun), mei(aite));
            return "再戦禁止 (あと " + (nokori / 60) + "分" + (nokori % 60) + "秒)";
        }
        if (sensouChu(aite, kane)) {
            return "すでに戦争中です";
        }
        if (sensouChu(jibun, kane)) {
            return "自勢力がすでに戦争中です";
        }
        return null;
    }

    /** 自勢力から見て、今えらべる相手の一覧。 */
    public List<String> erabreruAite(String jibun, Kane kane) {
        List<String> aite = new ArrayList<>();
        for (String t : zenTeam()) {
            if (senseniDekiruka(jibun, t, kane) == null) {
                aite.add(t);
            }
        }
        return aite;
    }

    // --- 保存と読み戻し -------------------------------------------

    public void yomikomi() {
        leader = new ArrayList<>(plugin.getConfig().getStringList(KEY_LEADER));
        daikou = new ArrayList<>(plugin.getConfig().getStringList(KEY_DAIKOU));
    }

    public void hozon() {
        if (plugin == null) {
            return;   // 検証で単体で使う時
        }
        plugin.getConfig().set(KEY_LEADER, leader);
        plugin.getConfig().set(KEY_DAIKOU, daikou);
        plugin.saveConfig();
    }

    // --- 下剋上の行使 ---------------------------------------------

    /**
     * 下剋上を宣言できるかを、指示書の順番どおりに確かめる。
     * 断る時はその理由を返す。発動してよければ null を返す。
     *
     *   1. 勢力に入っているか
     *   2. リーダー（か代行）か
     *   3. 権利を持っているか
     *   4. 自勢力が占領されているか
     */
    public String gekokujoDekiruka(Player player, Kane kane) {
        String team = kane.teamMei(player);
        if (team == null || !seiryokuKa(team)) {
            return "勢力に入っていません";
        }
        if (!leaderKa(team, player.getName())) {
            return "リーダー（か代行）だけが宣言できます (今のリーダー: "
                    + (leaderMei(team) == null ? "未設定" : leaderMei(team)) + ")";
        }
        if (!kenriAru(team, kane)) {
            return "下剋上の権利がありません (販売所で購入してください)";
        }
        if (senryoShiteiru(team, kane) == null) {
            return "自勢力は占領されていません";
        }
        return null;
    }
}
