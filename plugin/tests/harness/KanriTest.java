// =============================================================
// KanriTest ── 運営（コマンドブロック）の操作を測る
//
//   ★★ なぜ足したか（2026-08-22）★★
//     ゲームマスターがコマンドブロックから「開始・停止・リセット・
//     テレポ・リスポーン」を打てるようにした。
//     実機でしか分からない事（テレポで本当に動くか）は人が見るしかないが、
//     次の4つは機械で測れる:
//       A. リセットが【書くべきスコアだけ】を書き、金や徴収の世代に触らないこと
//       C. 座標の読み取り
//       D. 配線（イベントとコマンドが本当につながっているか）
// =============================================================

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class KanriTest {

    static int pass = 0, fail = 0;

    /** 目的名 → (保持者 → 値)。スコアボードの偽物。 */
    static final Map<String, Map<String, Integer>> BOARD = new LinkedHashMap<>();

    static void check(String label, boolean cond, String detail) {
        if (cond) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + detail); }
    }

    static Object fallback(Method m, Object proxy, Object[] args) {
        switch (m.getName()) {
            case "toString": return "stub";
            case "hashCode": return System.identityHashCode(proxy);
            case "equals": return proxy == args[0];
        }
        Class<?> r = m.getReturnType();
        if (r == boolean.class) return false;
        if (r == int.class) return 0;
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> iface, InvocationHandler h) {
        return (T) Proxy.newProxyInstance(KanriTest.class.getClassLoader(),
                new Class<?>[]{iface}, h);
    }

    static int get(String obj, String entry) {
        return BOARD.getOrDefault(obj, Map.of()).getOrDefault(entry, 0);
    }

    static void set(String obj, String entry, int v) {
        BOARD.computeIfAbsent(obj, k -> new LinkedHashMap<>()).put(entry, v);
    }

    static String src(String... michi) throws Exception {
        for (String q : michi) {
            Path w = Paths.get(q);
            if (Files.exists(w)) {
                return Files.readString(w);
            }
        }
        return null;
    }

    /** プロジェクトの根からの相対パスを、どこから走らせても引く。 */
    static java.nio.file.Path sagasu(String michi) {
        java.nio.file.Path[] ne = {java.nio.file.Paths.get("..", ".."),
                java.nio.file.Paths.get("."), java.nio.file.Paths.get("..")};
        for (java.nio.file.Path n : ne) {
            java.nio.file.Path p = n.resolve(michi);
            if (java.nio.file.Files.exists(p)) return p;
        }
        throw new IllegalStateException(michi + " が見つからない");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 運営の操作: 実測 ===");
        System.out.println();

        // --- スコアボードの偽物 --------------------------------------
        Scoreboard board = proxy(Scoreboard.class, (p, m, a) -> {
            if (m.getName().equals("getEntries")) {
                Set<String> zen = new LinkedHashSet<>();
                for (Map<String, Integer> v : BOARD.values()) {
                    zen.addAll(v.keySet());
                }
                return zen;
            }
            if (!m.getName().equals("getObjective")) return fallback(m, p, a);
            String n = (String) a[0];
            if (!BOARD.containsKey(n)) return null;
            return proxy(Objective.class, (p2, m2, a2) -> {
                if (!m2.getName().equals("getScore")) return fallback(m2, p2, a2);
                String entry = String.valueOf(a2[0]);
                return proxy(Score.class, (p3, m3, a3) -> {
                    switch (m3.getName()) {
                        case "getScore": return get(n, entry);
                        case "setScore": set(n, entry, (Integer) a3[0]); return null;
                        case "isScoreSet": return BOARD.get(n).containsKey(entry);
                    }
                    return fallback(m3, p3, a3);
                });
            });
        });
        ScoreboardManager mgr = proxy(ScoreboardManager.class, (p, m, a) ->
                m.getName().equals("getMainScoreboard") ? board : fallback(m, p, a));
        Server server = proxy(Server.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getScoreboardManager": return mgr;
                case "getLogger": return java.util.logging.Logger.getLogger("KanriTest");
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true);
        bs.set(null, server);

        Class<?> kc = Class.forName("jidai.Kanri");
        Class<?> kaneClass = Class.forName("jidai.Kane");
        Object kane = kaneClass.getDeclaredConstructor().newInstance();
        Object kanri = unsafeNew(kc);
        ire(kanri, "kane", kane);

        // =========================================================
        //  A. リセット
        // =========================================================
        System.out.println("-- A. リセットが書くもの・書かないもの --");

        String[] kuni = {"丘陵", "森林", "川", "内海", "岩場"};
        for (String obj : new String[]{"chuo", "wakidashi", "jidai", "chokin", "kane_kojin",
                "sekiyu", "senryou", "gekokujo", "shokuminchi", "senkou", "shouri", "choshu",
                "settei"}) {
            BOARD.put(obj, new LinkedHashMap<>());
        }
        set("chuo", "世界", 3);
        set("wakidashi", "世界", 1200);
        set("shouri", "世界", 2);
        set("choshu", "世界", 2);
        for (String k : kuni) {
            set("jidai", k, 3);
            set("chokin", k, 12345);
            set("senryou", k, 1);
            set("gekokujo", k, 1);
            set("shokuminchi", k, 2);
            set("senkou", k, 1);
        }
        set("kane_kojin", "ronty", 500);
        set("kane_kojin", "okusan", 300);
        set("sekiyu", "ronty", 40);
        set("choshu", "ronty", 2);
        set("settei", "石油_停止", 0);
        // ★ 2026-09-09: ビーコンの案内が読む設定。置かないと読み出しで落ちる
        set("settei", "占領_必要回数", 100);

        Method mReset = kc.getDeclaredMethod("scoreRisetto", boolean.class);
        mReset.setAccessible(true);

        // --- 時代だけ ---
        int n1 = (Integer) mReset.invoke(kanri, false);
        check("reset: 中央の時代が 1 に戻る", get("chuo", "世界") == 1, "実際=" + get("chuo", "世界"));
        check("reset: 湧いた本数が 0 に戻る", get("wakidashi", "世界") == 0, "実際=" + get("wakidashi", "世界"));
        boolean zenJidai = true;
        for (String k : kuni) {
            zenJidai &= get("jidai", k) == 1;
        }
        check("reset: 5勢力の時代が全部 1 に戻る", zenJidai, "戻っていない勢力がある");
        check("★reset: 貯金には触らない", get("chokin", "丘陵") == 12345, "実際=" + get("chokin", "丘陵"));
        check("★reset: 個人の金には触らない", get("kane_kojin", "ronty") == 500, "実際=" + get("kane_kojin", "ronty"));
        check("★reset: 戦争（占領・植民地）には触らない",
                get("senryou", "丘陵") == 1 && get("shokuminchi", "丘陵") == 2, "触っている");
        check("★★reset: 徴収の世代（choshu）には触らない（触ると全員の石油が4割 取られる）",
                get("choshu", "世界") == 2 && get("choshu", "ronty") == 2, "触っている");
        check("reset: 書いた数 = 2 + 勢力5", n1 == 7, "実際=" + n1);

        // --- 全部 ---
        set("chuo", "世界", 3);
        int n2 = (Integer) mReset.invoke(kanri, true);
        check("reset zenbu: 貯金が 0", get("chokin", "丘陵") == 0 && get("chokin", "岩場") == 0, "残っている");
        check("reset zenbu: 個人の金が 0（持っている人 全員）",
                get("kane_kojin", "ronty") == 0 && get("kane_kojin", "okusan") == 0, "残っている");
        check("reset zenbu: 個人の石油が 0", get("sekiyu", "ronty") == 0, "残っている");
        check("★reset zenbu: 石油を持っていなかった人に、石油のスコアを作らない",
                !BOARD.get("sekiyu").containsKey("okusan"), "作っている");
        check("★reset zenbu: 勢力名や設定項目に、個人の金のスコアを作らない",
                !BOARD.get("kane_kojin").containsKey("丘陵")
                        && !BOARD.get("kane_kojin").containsKey("石油_停止"), "作っている");
        check("reset zenbu: 占領・下剋上・植民地・先行・勝利が 0",
                get("senryou", "丘陵") == 0 && get("gekokujo", "丘陵") == 0
                        && get("shokuminchi", "丘陵") == 0 && get("senkou", "丘陵") == 0
                        && get("shouri", "世界") == 0, "残っている");
        check("★★reset zenbu でも 徴収の世代には触らない", get("choshu", "世界") == 2, "触っている");
        check("reset zenbu: 書いた数 = 2 + 5×6 + 1 + 金2 + 石油1", n2 == 2 + 30 + 1 + 3, "実際=" + n2);

        // 目的が無い世界（データパック未読込）でも落ちない
        BOARD.remove("jidai");
        int n3 = (Integer) mReset.invoke(kanri, false);
        check("目的が無ければ書かずに飛ばす（落ちない）", n3 == 2, "実際=" + n3);

        // =========================================================
        //  C. 座標の読み取り
        // =========================================================
        System.out.println();
        System.out.println("-- C. 座標 --");
        Method mTen = kc.getDeclaredMethod("tenkai", String.class);
        mTen.setAccessible(true);
        int[] a1 = (int[]) mTen.invoke(null, "40 176 -927");
        check("\"40 176 -927\" が読める", a1 != null && a1[0] == 40 && a1[1] == 176 && a1[2] == -927,
                Arrays.toString(a1));
        check("読点区切りも読める", Arrays.equals((int[]) mTen.invoke(null, "1,2,3"), new int[]{1, 2, 3}), "駄目");
        check("数が足りなければ null", mTen.invoke(null, "1 2") == null, "通ってしまう");
        check("数字でなければ null", mTen.invoke(null, "a b c") == null, "通ってしまう");
        String kitei = (String) teisuu(kc, "HAJIME_NO_RISU");
        check("★既定の初期リスポーンが ご指示の座標 (40 176 -927)", kitei.equals("40 176 -927"), kitei);

        // =========================================================
        //  D. 配線
        // =========================================================
        System.out.println();
        System.out.println("-- D. 配線 --");
        String jsrc = src("plugin/src/main/java/jidai/JidaiCraft.java",
                          "../src/main/java/jidai/JidaiCraft.java",
                          "src/main/java/jidai/JidaiCraft.java");
        check("JidaiCraft.java が読めた", jsrc != null, "見つからない");
        if (jsrc != null) {
            check("★(字) /jidai game が Kanri へつながっている",
                    jsrc.contains("if (args[0].equals(\"game\")) {\n            return kanri.command(sender, args);"),
                    "配線が無い");
            check("★(字) game は【プレイヤー限定の門番より前】にある（コマンドブロックから打てる）",
                    jsrc.indexOf("args[0].equals(\"game\")")
                            < jsrc.indexOf("add と remove はゲーム内から実行してください"),
                    "門番の後ろ＝コマンドブロックで弾かれる");
            check("(字) 生き返る時に Kanri が拠点へ戻す", jsrc.contains("kanri.ikikaeru(event);"), "配線が無い");
            check("(字) 起動時に初期リスポーンを世界へ書く", jsrc.contains("kanri.spawnTekiyou();"), "配線が無い");
            check("★(字) ガチャを閉じた時、入れ物ごと渡している（十連の途中で閉じた人に渡すため）",
                    jsrc.contains("gacha.tojita(event.getPlayer().getName(), event.getInventory());"),
                    "名前だけ渡している");
            check("★(字) 入った時に渡し損ねの十連を渡す", jsrc.contains("gacha.sanka(event.getPlayer());"), "配線が無い");
            check("(字) 補完に game がある", jsrc.contains("\"tsuruhashi\", \"ui\", \"game\""), "無い");
        }

        // ---- リセットと特殊アイテム（2026-08-23 のご指示）----
        //   「時代を最初に戻した」のに『もう出た』の記録だけ残っていると、
        //   二度目の催しで特殊アイテムが1個も出ない。
        String rsrc = src("plugin/src/main/java/jidai/Kanri.java",
                          "../src/main/java/jidai/Kanri.java",
                          "src/main/java/jidai/Kanri.java");
        check("Kanri.java が読めた（リセットの検査用）", rsrc != null, "見つからない");
        if (rsrc != null) {
            String rst = kiridashi(rsrc, "private boolean reset(CommandSender sender, boolean zenbu)",
                                   "int scoreRisetto(boolean zenbu)");
            check("★★★(字) reset は zenbu でなくても特殊アイテムの記録を戻す",
                    rst.indexOf("gacha.tokushuRisetto();") >= 0
                            && rst.indexOf("gacha.tokushuRisetto();") < rst.indexOf("if (zenbu) {"),
                    "zenbu の中にある＝時代だけ戻すと特殊が二度と出ない");
            // ★★ 2026-08-24 のご指示で、zenbu でなくても取り上げる形へ変えた ★★
            //   記録だけ消して紙が残ると、リーダーが5種そろえたまま＝即 勝利になり、
            //   遺物も「紙はあるのに効果は無い」状態になる。
            check("★★(字) reset は zenbu でなくても手元の特殊アイテムを取り上げる",
                    rst.indexOf("int kaishu = Shouri.kaishu();") >= 0
                            && rst.indexOf("int kaishu = Shouri.kaishu();") < rst.indexOf("if (zenbu) {"),
                    "zenbu の中にある＝時代だけ戻すと紙が残る");
        }

        // ---- 取り上げる物の見分け（実際に呼ぶ）----
        Class<?> shouri = Class.forName("jidai.Shouri");
        Method mNamae = shouri.getDeclaredMethod("tokushuNoNamaeKa", String.class);
        mNamae.setAccessible(true);
        String[] atsumeru = (String[]) shouri.getMethod("tokushuMei").invoke(null);
        String[][] ibutsu = (String[][]) shouri.getMethod("ibutsuHyou").invoke(null);
        int mitsuketa = 0;
        for (String mei : atsumeru) {
            if ((Boolean) mNamae.invoke(null, "§b" + mei)) {
                mitsuketa++;
            }
        }
        for (String[] ib : ibutsu) {
            if ((Boolean) mNamae.invoke(null, "§b" + ib[0])) {
                mitsuketa++;
            }
        }
        check("★取り上げる対象が 集める5種＋遺物 の " + (atsumeru.length + ibutsu.length) + " 種すべて",
                mitsuketa == atsumeru.length + ibutsu.length, "見分けられた=" + mitsuketa);
        check("★ふつうの品は取り上げない（パン・丸石・空）",
                !(Boolean) mNamae.invoke(null, "§bパン")
                        && !(Boolean) mNamae.invoke(null, "丸石 ×32")
                        && !(Boolean) mNamae.invoke(null, (Object) null),
                "ふつうの品まで取り上げる");
        // ---- 制圧（ビーコンを落とす）----
        if (jsrc != null) {
            Class<?> jcls = Class.forName("jidai.JidaiCraft");
            // ★★ 2026-09-09: 条件が「相手の貯金 100以下」から
            //   「相手の銀行を 占領_必要回数 回 壊した」へ替わった（ご指示）。
            //   1回1%の略奪では貯金が 0 にならないので、貯金では永久に落とせない。
            int yobi = (Integer) teisuu(jcls, "BEACON_KAISU_YOBI");
            check("★★ビーコンを壊せる回数の予備の値は 100", yobi == 100, "実際=" + yobi);
            check("★★(字) 必要回数はデータパックの settei が正本（プラグインは覚えない）",
                    jsrc.contains("kane.settei(\"占領_必要回数\")"), "プラグインが数字を持っている");
            check("★(字) 壊した回数が足りなければ落とせない判定",
                    jsrc.contains("if (kaisu < hitsuyou) {"), "判定が古い");
            check("★(字) 回数はマーカーから写してもらってから読む（kai_yomu）",
                    jsrc.contains("function jidai:sensou/kai_yomu")
                            && jsrc.contains("kane.sagyou(\"#q_kai\")"), "読み方が違う");
            check("★★(字) 制圧したら戦争を【再戦禁止】へ進める（マーカーは読まず命令で）",
                    jsrc.contains("run scoreboard players set @s sensou 3")
                            && jsrc.contains("sensou_byou = 戦争_禁止秒 settei")
                            && jsrc.contains("function jidai:sensou/youyaku"), "戦争が終わらない");
            check("★(字) 制圧の演出: 花火5発 ＋ 全員に「終戦」のタイトル",
                    (Integer) teisuu(jcls, "SEIATSU_HANABI") == 5
                            && jsrc.contains("p.sendTitle(\"§c§l終戦\""), "演出が無い");
            check("★(字) 制圧の演出は【ビーコンを落とした時だけ】（時間切れの交戦終了では呼ばない）",
                    jsrc.indexOf("seiatsuEnshutsu(") > 0
                            && kiridashi(jsrc, "private boolean beaconKowaseruKa(", "void seiatsuEnshutsu(")
                                    .contains("seiatsuEnshutsu(block.getLocation()"),
                    "呼び場所が違う");
        }

        String yml = src("plugin/src/main/resources/plugin.yml",
                         "../src/main/resources/plugin.yml",
                         "src/main/resources/plugin.yml");
        check("(字) plugin.yml の usage に game がある", yml != null && yml.contains("| game"), "無い");

        String ksrc = src("plugin/src/main/java/jidai/Kanri.java",
                          "../src/main/java/jidai/Kanri.java",
                          "src/main/java/jidai/Kanri.java");
        if (ksrc != null) {
            check("★(字) 停止は石油の栓を閉め、開始は開ける", ksrc.contains("kane.sekiyuTomeru(!ugokasu);"), "栓を触っていない");
            // ★★ 2026-08-24 に tokushu / ibutsu を足した ★★
            //   この2つは【確認用】で、出す相手と勢力が要るのでプレイヤー限定でよい。
            //   当日 使う start / stop / reset / tp / risu / beacon は
            //   コマンドブロックから打てないと困るので、そちらだけを見る。
            String unei = kiridashi(ksrc, "private boolean kirikae(", "private boolean tokushu(");
            check("★(字) 当日 使う処理はコマンドブロックから打てる（プレイヤー限定にしていない）",
                    unei.indexOf("instanceof Player") < 0,
                    "コマンドブロックから打てない処理がある");
            check("★(字) 確認用の tokushu / ibutsu はプレイヤー限定でよい",
                    kiridashi(ksrc, "private boolean tokushu(", "private int kaku(")
                            .indexOf("instanceof Player") > 0, "限定していない");
            check("★(字) リスポーンは force=true（ベッドが無くても拠点に戻る）",
                    ksrc.contains("p.setBedSpawnLocation(l, true);"), "force が無い");
            check("★(字) 拠点の後方にビーコンを置く口がある（beacon）", ksrc.contains("private boolean beacon("), "無い");
            check("★(字) ビーコンの形は Basho が守る範囲と同じ（本体・上のガラス・下の鉄3x3）",
                    ksrc.contains("setType(Material.IRON_BLOCK)") && ksrc.contains("setType(Material.BEACON)")
                            && ksrc.contains("setType(garasu(team))"), "形が違う");
            check("★(字) 置いたビーコンを登録している（tsuika ＋ beaconKuniTouroku ＋ hozon）",
                    ksrc.contains("basho.tsuika(Basho.BEACON, kagi)")
                            && ksrc.contains("basho.beaconKuniTouroku(kagi, team)")
                            && ksrc.contains("basho.hozon(plugin);"), "登録が足りない");
            int ushiro = (Integer) teisuu(kc, "BEACON_USHIRO");
            check("ビーコンは立ち位置の 16 ブロック後方（-z）", ushiro == 16, "実際=" + ushiro);
        }

        System.out.println();
        System.out.println("================================");
        // =========================================================
        //  ★★ 掃除の漏れ（2026-08-24・drift-audit で発見）★★
        // =========================================================
        System.out.println();

        System.out.println();
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }

    /** package-private の static 定数を読む。 */
    static Object teisuu(Class<?> c, String mei) throws Exception {
        Field f = c.getDeclaredField(mei);
        f.setAccessible(true);
        return f.get(null);
    }

    static String kiridashi(String src, String kara, String made) {
        int i = src.indexOf(kara);
        int j = src.indexOf(made, i < 0 ? 0 : i);
        if (i < 0 || j < 0 || j < i) {
            return "";
        }
        return src.substring(i, j);
    }

    static void ire(Object o, String mei, Object atai) throws Exception {
        Field f = o.getClass().getDeclaredField(mei);
        f.setAccessible(true);
        f.set(o, atai);
    }

    static Object unsafeNew(Class<?> c) throws Exception {
        Class<?> u = Class.forName("sun.misc.Unsafe");
        Field f = u.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method m = u.getMethod("allocateInstance", Class.class);
        return m.invoke(unsafe, c);
    }
}
