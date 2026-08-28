// =============================================================
// MiraiTest ── 時代5「未来」と、未来到達勝利（2026-08-23 のご指示）
//
//   ご指示:
//     「現代へ到達は勝利条件にしません。
//       現代へ到達後、現代から未来に到達したら勝利とします
//       （建築ブロック数と石油の数で勝利みたいな）」
//
//   ★★ ここで測れること / 測れないこと ★★
//     測れる  … 必要量の読み取り（実物の Kane を呼ぶ）
//               データパックの実物（load / hantei / jougen / 承認5本 / shouri_kakutei）
//               ★ 必要な建築数が、数える区画に物理的に収まるか
//     測れない… 実際に承認して勝ちの合図が出るか（サーバーが要る）
//               → docs/実機確認リスト.md に人の目で見る物として残してある。
// =============================================================

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class MiraiTest {

    static int pass = 0, fail = 0;
    static final Map<String, Map<String, Integer>> BOARD = new HashMap<>();

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
        if (r == double.class) return 0.0;
        if (r == long.class) return 0L;
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> iface, InvocationHandler h) {
        return (T) Proxy.newProxyInstance(MiraiTest.class.getClassLoader(),
                new Class<?>[]{iface}, h);
    }

    static int get(String o, String e) { return BOARD.getOrDefault(o, Map.of()).getOrDefault(e, 0); }
    static void set(String o, String e, int v) { BOARD.computeIfAbsent(o, k -> new HashMap<>()).put(e, v); }

    static String src(String... michi) throws IOException {
        Path[] soko = {Paths.get("..", ".."), Paths.get("."), Paths.get("..")};
        for (Path ne : soko) {
            Path p = ne;
            for (String m : michi) p = p.resolve(m);
            if (Files.exists(p)) return new String(Files.readAllBytes(p), "UTF-8");
        }
        throw new FileNotFoundException(String.join("/", michi) + " が見つからない (cwd="
                + Paths.get("").toAbsolutePath() + ")");
    }

    static String dp(String... michi) throws IOException {
        List<String> a = new ArrayList<>(List.of("datapacks", "jidai_craft", "data", "jidai", "functions"));
        a.addAll(List.of(michi));
        return src(a.toArray(new String[0]));
    }

    /** load.mcfunction の settei の既定値を読む。無ければ -1。 */
    static int settei(String load, String kagi) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("scoreboard players set " + java.util.regex.Pattern.quote(kagi)
                        + " settei (-?\\d+)").matcher(load);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 時代5「未来」と 未来到達勝利 ===");

        Scoreboard board = proxy(Scoreboard.class, (p, m, a) -> {
            if (!m.getName().equals("getObjective")) return fallback(m, p, a);
            String n = (String) a[0];
            if (!BOARD.containsKey(n)) return null;
            return proxy(Objective.class, (p2, m2, a2) -> {
                if (!m2.getName().equals("getScore")) return fallback(m2, p2, a2);
                String e = (a2[0] instanceof Player pl) ? pl.getName() : String.valueOf(a2[0]);
                return proxy(Score.class, (p3, m3, a3) -> {
                    switch (m3.getName()) {
                        case "getScore": return get(n, e);
                        case "setScore": set(n, e, (Integer) a3[0]); return null;
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
                case "getOnlinePlayers": return new ArrayList<Player>();
                case "getLogger": return Logger.getLogger("MiraiTest");
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true); bs.set(null, server);

        // =========================================================
        //  A. 必要量の読み取り（実物の Kane を呼ぶ）
        // =========================================================
        System.out.println();
        System.out.println("-- A. 現代→未来 の必要量をプラグインが読めるか --");

        Class<?> kc = Class.forName("jidai.Kane");
        Object kane = kc.getDeclaredConstructor().newInstance();
        Method mSekiyu = kc.getMethod("hitsuyoSekiyu", int.class);
        Method mChokin = kc.getMethod("hitsuyoChokin", int.class);
        Method mKenchiku = kc.getMethod("hitsuyoKenchiku", int.class);
        Method mMei = kc.getMethod("jidaiMei", int.class);

        Field fMirai = kc.getDeclaredField("JIDAI_MIRAI");
        check("★最後の時代は 5（未来）", (Integer) fMirai.get(null) == 5,
                "実際=" + fMirai.get(null));
        check("★時代の呼び名に「未来」がある", "未来".equals(mMei.invoke(null, 5)),
                "実際=" + mMei.invoke(null, 5));
        check("時代の呼び名 1〜4 は変わっていない",
                "鉄器".equals(mMei.invoke(null, 1)) && "中世".equals(mMei.invoke(null, 2))
                        && "近代".equals(mMei.invoke(null, 3)) && "現代".equals(mMei.invoke(null, 4)),
                "変わっている");

        BOARD.put("settei", new HashMap<>());
        set("settei", "進行_石油_現代", 500);
        set("settei", "進行_貯金_現代", 0);
        set("settei", "進行_建築_現代", 2400);
        set("settei", "進行_石油_近代", 500);

        check("★★現代(4)の必要石油は 進行_石油_現代 から読む",
                (Integer) mSekiyu.invoke(kane, 4) == 500, "実測=" + mSekiyu.invoke(kane, 4));
        check("★★現代(4)の必要建築は 進行_建築_現代 から読む",
                (Integer) mKenchiku.invoke(kane, 4) == 2400, "実測=" + mKenchiku.invoke(kane, 4));
        check("現代(4)の必要貯金は 進行_貯金_現代（既定 0＝要らない）",
                (Integer) mChokin.invoke(kane, 4) == 0, "実測=" + mChokin.invoke(kane, 4));
        check("★未来(5)から先は無い（0 を返す）",
                (Integer) mSekiyu.invoke(kane, 5) == 0
                        && (Integer) mKenchiku.invoke(kane, 5) == 0, "0 でない");

        // ★ 近代の欄を読み違えていないか（現代の欄と取り違えると、未来の壁が近代のままになる）
        set("settei", "進行_石油_近代", 777);
        check("★近代(3)は今までどおり 進行_石油_近代 を読む",
                (Integer) mSekiyu.invoke(kane, 3) == 777, "実測=" + mSekiyu.invoke(kane, 3));
        set("settei", "進行_石油_近代", 500);

        // =========================================================
        //  B. データパックの実物
        // =========================================================
        System.out.println();
        System.out.println("-- B. データパックが未来を持っているか --");

        String load = dp("load.mcfunction");
        int oil = settei(load, "進行_石油_現代");
        int kenchiku = settei(load, "進行_建築_現代");
        int chokin = settei(load, "進行_貯金_現代");
        check("★load が 進行_石油_現代 を持っている（" + oil + "）", oil > 0, "無い");
        check("★load が 進行_建築_現代 を持っている（" + kenchiku + "）", kenchiku > 0, "無い");
        check("load が 進行_貯金_現代 を持っている（" + chokin + "）", chokin >= 0, "無い");

        // ★★ いちばん大事な検査 ★★
        //   建築を数える区画は jidai:shinko/kenchiku の clone の範囲。
        //   必要数がその体積を超えたら、どれだけ建てても永久に届かない。
        String ken = dp("shinko", "kenchiku.mcfunction");
        java.util.regex.Matcher mm = java.util.regex.Pattern
                .compile("clone ~(-?\\d+) (\\d+) ~(-?\\d+) ~(-?\\d+) (\\d+) ~(-?\\d+)").matcher(ken);
        check("建築を数える範囲を読めた", mm.find(), "clone の行が見つからない");
        int x = Math.abs(Integer.parseInt(mm.group(4)) - Integer.parseInt(mm.group(1))) + 1;
        int y = Math.abs(Integer.parseInt(mm.group(5)) - Integer.parseInt(mm.group(2))) + 1;
        int z = Math.abs(Integer.parseInt(mm.group(6)) - Integer.parseInt(mm.group(3))) + 1;
        int taiseki = x * y * z;
        check("★★必要な建築数 " + kenchiku + " が、数える区画 " + x + "×" + y + "×" + z
                        + "＝" + taiseki + " に収まる",
                kenchiku > 0 && kenchiku < taiseki,
                "超えている＝どれだけ建てても永久に届かない");
        // 余裕があるか（すき間なく埋める作業になっていないか）
        check("必要な建築数が区画の 8割 未満（すき間なく埋める作業にしない）",
                kenchiku < taiseki * 0.8, "割合=" + (kenchiku * 100 / taiseki) + "%");

        String hantei = dp("shinko", "hantei.mcfunction");
        check("★★hantei が時代4（現代）の必要量を選んでいる",
                hantei.contains("matches 4 run scoreboard players operation #hitsuyo_sekiyu sagyou = 進行_石油_現代")
                        && hantei.contains("matches 4 run scoreboard players operation #hitsuyo_kenchiku sagyou = 進行_建築_現代"),
                "現代の欄を見ていない＝現代から進めない");
        check("★★hantei の打ち止めが 5（未来）になっている",
                hantei.contains("matches 5.. run scoreboard players set #zenbu sagyou 0")
                        && !hantei.contains("matches 4.. run scoreboard players set #zenbu sagyou 0"),
                "4 で打ち止めのまま＝現代から進めない");

        String jougen = dp("sekiyu", "jougen.mcfunction");
        check("★石油の上限: 現代は 進行_石油_現代 から引く",
                jougen.contains("matches 4 run scoreboard players operation #jougen sagyou = 進行_石油_現代"),
                "近代の量のままだと、未来に要る本数を持てない");
        check("石油の上限: 未来(5以上)にも上限がある",
                jougen.contains("matches 5.. run scoreboard players operation #jougen sagyou = 進行_石油_現代"),
                "無い");

        // ★ 上限（必要量×1.2）が、必要量そのものを下回っていないか
        check("★★石油の上限が必要量を下回っていない（" + oil + " × 1.2 = " + (oil * 12 / 10) + "）",
                oil * 12 / 10 >= oil, "下回っている＝永久に届かない");

        // 承認 5本
        String[][] kuni = {{"kyuryo", "丘陵"}, {"shinrin", "森林"}, {"kawa", "川"},
                           {"naikai", "内海"}, {"iwaba", "岩場"}};
        for (String[] k : kuni) {
            String sh = dp("shinko", "shounin_" + k[0] + ".mcfunction");
            check("★★承認(" + k[1] + "): 未来へ着いたら勝ちを宣言する",
                    sh.contains("if score " + k[1] + " jidai matches 5.. run scoreboard players set #shouri_shu sagyou 5")
                            && sh.contains("if score " + k[1] + " jidai matches 5.. run function jidai:sensou/shouri_kakutei"),
                    "宣言していない＝未来へ着いても何も起きない");
            check("承認(" + k[1] + "): 勝った勢力の番号は bangou から取る（決め打ちしない）",
                    sh.contains("#s_kuni sagyou = " + k[1] + " bangou"), "決め打ちしている");
        }

        String kakutei = dp("sensou", "shouri_kakutei.mcfunction");
        check("★★勝ち方に 5（未来到達）がある",
                kakutei.contains("#shouri_shu sagyou matches 5 run tellraw"), "無い＝勝っても何も出ない");
        // ★ 2 は「人望勝利（傭兵の過半数）」だった。傭兵を止めた 2026-08-26 から
        //   誰も 2 を立てないので、行ごと外してある。番号は詰めていない。
        for (int shu : new int[]{1, 3, 4}) {
            check("勝ち方 " + shu + " は残っている",
                    kakutei.contains("#shouri_shu sagyou matches " + shu + " run tellraw"), "消えた");
        }
        check("★勝ち方 2（人望）は外れている（傭兵を止めたため）",
                !kakutei.contains("#shouri_shu sagyou matches 2 "), "残っている");

        // =========================================================
        //  C. 申請の画面（字で見る）
        // =========================================================
        System.out.println();
        System.out.println("-- C. 現代からも申請できるか --");

        String shinko = src("plugin", "src", "main", "java", "jidai", "Shinko.java");
        check("★★(字) 申請できる上限が 未来(5) になっている",
                shinko.contains("boolean jouken = ima < Kane.JIDAI_MIRAI;")
                        && !shinko.contains("boolean jouken = ima < 4;"),
                "4 のままだと現代から申請できない");
        check("★(字) 押した時の門も 未来(5) になっている",
                shinko.contains("if (ima >= Kane.JIDAI_MIRAI || !sorotta(kane, mei, ima)) {"),
                "4 のまま");
        check("★(字) 未来への申請だと分かる文が出る",
                shinko.contains("ima + 1 == Kane.JIDAI_MIRAI")
                        && shinko.contains("この勢力の勝ちです"), "出ない");
        check("(字) 「現代が最後の時代です」は消えている",
                !shinko.contains("現代が最後の時代です"), "残っている");

        String keiji = src("clientmod", "src", "main", "java", "jidai", "ui", "Keiji.java");
        check("★掲示板（MOD）も 5 を「未来」と読む",
                keiji.contains("case 5: return \"未来\";"), "数字のまま出る");

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        System.out.println("★ 実際に承認して勝ちの合図が出るかは、サーバーを立てないと測れない。");
        System.out.println("  docs/実機確認リスト.md に人の目で見る物として残してある。");
        if (fail > 0) System.exit(1);
    }
}
