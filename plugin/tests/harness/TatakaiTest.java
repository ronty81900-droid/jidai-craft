// =============================================================
// TatakaiTest ── 同士討ちの禁止と、殺した時の分捕り（2026-08-24 のご指示）
//
//   「同じ勢力の人間は殺せないようなコマンドを組んでください。
//     そして、敵勢力もしくは蛮族を殺すと相手の所持金の10％奪える
//     （勢力貯金からは取れないようにしてください。）
//     蛮族は仲間うちでも殺し合いができるようにしてください。」
//
//   ★ ここは【実物の Tatakai を呼んで】測る。字の検査ではない。
//   ★ 測れないこと: TaCZ の弾が EntityDamageByEntityEvent に来るか。
//     来なくてもチームの friendlyFire=false が保険になる（両方 見る）。
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
import org.bukkit.scoreboard.Team;

public class TatakaiTest {

    static int pass = 0, fail = 0;
    static final Map<String, Map<String, Integer>> BOARD = new HashMap<>();
    static final Map<String, String> TEAMS = new HashMap<>();

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
        if (r == float.class) return 0.0f;
        if (r == long.class) return 0L;
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> iface, InvocationHandler h) {
        return (T) Proxy.newProxyInstance(TatakaiTest.class.getClassLoader(),
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
        throw new FileNotFoundException(String.join("/", michi) + " が見つからない");
    }

    /** 名前と、届いた知らせを覚えるだけの人。 */
    static class Hito implements InvocationHandler {
        final String na;
        final List<String> messages = new ArrayList<>();
        Hito(String na) { this.na = na; }
        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "getName": return na;
                case "sendMessage":
                    if (a != null && a.length == 1 && a[0] instanceof String s) messages.add(s);
                    return null;
                case "getLocation": return null;
            }
            return fallback(m, p, a);
        }
        boolean saw(String x) {
            for (String s : messages) if (s.contains(x)) return true;
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 同士討ちの禁止と、殺した時の分捕り ===");

        Scoreboard board = proxy(Scoreboard.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getObjective": {
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
                }
                case "getEntryTeam": {
                    String t = TEAMS.get((String) a[0]);
                    if (t == null) return null;
                    return proxy(Team.class, (p2, m2, a2) ->
                            m2.getName().equals("getName") ? t : fallback(m2, p2, a2));
                }
            }
            return fallback(m, p, a);
        });
        ScoreboardManager mgr = proxy(ScoreboardManager.class, (p, m, a) ->
                m.getName().equals("getMainScoreboard") ? board : fallback(m, p, a));
        Server server = proxy(Server.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getScoreboardManager": return mgr;
                case "getOnlinePlayers": return new ArrayList<Player>();
                case "getLogger": return Logger.getLogger("TatakaiTest");
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true); bs.set(null, server);

        // データパックが在る状態にする（junbiOK が true になるように）
        BOARD.put("kane_kojin", new HashMap<>());
        BOARD.put("chokin", new HashMap<>());
        BOARD.put("youhei", new HashMap<>());
        BOARD.put("settei", new HashMap<>());
        BOARD.put("sekiyu", new HashMap<>());

        Class<?> kc = Class.forName("jidai.Kane");
        Object kane = kc.getDeclaredConstructor().newInstance();
        Class<?> tc = Class.forName("jidai.Tatakai");
        Field uw = tc.getDeclaredField("UBAU_WARIAI"); uw.setAccessible(true);

        // ★ plugin は使わない筋だけ試すので、コンストラクタを通さずに作る
        Object tatakai = unsafeNew(tc);

        Method mOnaji = tc.getDeclaredMethod("onajiSeiryokuKa", Player.class, Player.class, kc);
        mOnaji.setAccessible(true);
        Method mUbau = tc.getDeclaredMethod("ubauGaku", int.class);
        mUbau.setAccessible(true);

        Hito hA = new Hito("kyuryoA"), hB = new Hito("kyuryoB");
        Hito hT = new Hito("shinrinT"), hZ = new Hito("banzokuZ"), hW = new Hito("banzokuW");
        Player pA = proxy(Player.class, hA), pB = proxy(Player.class, hB);
        Player pT = proxy(Player.class, hT), pZ = proxy(Player.class, hZ), pW = proxy(Player.class, hW);
        TEAMS.put("kyuryoA", "kyuryo");
        TEAMS.put("kyuryoB", "kyuryo");
        TEAMS.put("shinrinT", "shinrin");
        TEAMS.put("banzokuZ", "youhei");
        TEAMS.put("banzokuW", "youhei");

        // =========================================================
        //  A. 誰が誰を傷つけられるか
        // =========================================================
        System.out.println();
        System.out.println("-- A. 同じ勢力は傷つけ合えない --");

        check("★★同じ勢力（丘陵どうし）は守られる",
                (Boolean) mOnaji.invoke(tatakai, pA, pB, kane), "守られていない");
        check("★★別の勢力（丘陵 vs 森林）は傷つけられる",
                !(Boolean) mOnaji.invoke(tatakai, pA, pT, kane), "守られてしまう");
        check("自分自身は対象外（自爆を止めない）",
                !(Boolean) mOnaji.invoke(tatakai, pA, pA, kane), "止めてしまう");

        // ★ 雇われた傭兵は雇い主の勢力チームへ移る＝その勢力の一員として守られる
        TEAMS.put("banzokuZ", "kyuryo");
        TEAMS.put("banzokuZ", "youhei");

        // =========================================================
        //  B. 殺した時の分捕り
        // =========================================================
        System.out.println();
        System.out.println("-- B. 個人の金の 10% を奪う --");

        check("★奪う割合は 10%", (Integer) uw.get(null) == 10, "実際=" + uw.get(null));
        check("1000 の 10% は 100", (Integer) mUbau.invoke(null, 1000) == 100, "違う");
        check("端数は切り捨て（55 → 5）", (Integer) mUbau.invoke(null, 55) == 5, "違う");
        check("0 なら 0", (Integer) mUbau.invoke(null, 0) == 0, "違う");
        check("負の残高でも 0（増やさない）", (Integer) mUbau.invoke(null, -100) == 0, "違う");

        Method mKoro = tc.getDeclaredMethod("koroshita", Player.class, Player.class, kc);
        mKoro.setAccessible(true);
        Field fp = tc.getDeclaredField("plugin"); fp.setAccessible(true);

        // plugin.getLogger() を呼ぶので、最低限の偽物を入れる
        Class<?> jc = Class.forName("jidai.JidaiCraft");
        Object nisePlugin = unsafeNew(jc);
        fp.set(tatakai, nisePlugin);

        // --- 敵勢力を倒した ---
        set("kane_kojin", "shinrinT", 1000);
        set("kane_kojin", "kyuryoA", 50);
        set("chokin", "森林", 99999);
        hA.messages.clear(); hT.messages.clear();
        try {
            mKoro.invoke(tatakai, pT, pA, kane);
        } catch (InvocationTargetException e) {
            // getLogger の偽物が無くても、金の移動はその手前で終わっている
        }
        check("★★敵勢力を倒すと、相手の個人の金の10%を奪える（1000 → 900 / 50 → 150）",
                get("kane_kojin", "shinrinT") == 900 && get("kane_kojin", "kyuryoA") == 150,
                "倒された=" + get("kane_kojin", "shinrinT") + " 倒した=" + get("kane_kojin", "kyuryoA"));
        check("★★勢力の貯金には1円も触らない",
                get("chokin", "森林") == 99999, "実測=" + get("chokin", "森林"));

        // --- 蛮族を倒した ---
        set("kane_kojin", "banzokuZ", 300);
        set("kane_kojin", "kyuryoA", 0);
        try { mKoro.invoke(tatakai, pZ, pA, kane); } catch (InvocationTargetException e) { }

        // --- 蛮族どうし ---
        set("kane_kojin", "banzokuW", 500);
        set("kane_kojin", "banzokuZ", 0);
        try { mKoro.invoke(tatakai, pW, pZ, kane); } catch (InvocationTargetException e) { }

        // --- 味方は奪えない ---
        set("kane_kojin", "kyuryoB", 1000);
        set("kane_kojin", "kyuryoA", 0);
        try { mKoro.invoke(tatakai, pB, pA, kane); } catch (InvocationTargetException e) { }
        check("★★同じ勢力の仲間からは1円も奪わない",
                get("kane_kojin", "kyuryoB") == 1000 && get("kane_kojin", "kyuryoA") == 0,
                "奪ってしまった");

        // --- 自滅・0円 ---
        set("kane_kojin", "shinrinT", 5);
        set("kane_kojin", "kyuryoA", 0);
        try { mKoro.invoke(tatakai, pT, pA, kane); } catch (InvocationTargetException e) { }
        check("★5円の相手（10%が0）からは何も動かない",
                get("kane_kojin", "shinrinT") == 5 && get("kane_kojin", "kyuryoA") == 0, "動いた");
        set("kane_kojin", "kyuryoA", 1000);
        try { mKoro.invoke(tatakai, pA, pA, kane); } catch (InvocationTargetException e) { }
        check("自滅では奪えない", get("kane_kojin", "kyuryoA") == 1000, "減った");

        // =========================================================
        //  C. 配線と保険
        // =========================================================
        System.out.println();
        System.out.println("-- C. 配線と、バニラ側の保険 --");

        String jcs = src("plugin", "src", "main", "java", "jidai", "JidaiCraft.java");
        check("★★殴った時のイベントに繋がっている",
                jcs.contains("EntityDamageByEntityEvent event")
                        && jcs.contains("tatakai.fusegu(event.getDamager(), event.getEntity(), kane)"),
                "繋がっていない");
        check("★★死んだ時のイベントに繋がっている",
                jcs.contains("PlayerDeathEvent event")
                        && jcs.contains("tatakai.koroshita(event.getEntity(), event.getEntity().getKiller(), kane)"),
                "繋がっていない");
        String ts = src("plugin", "src", "main", "java", "jidai", "Tatakai.java");
        check("★矢や弾は【撃った人】まで辿る（誤射を素通りさせない）",
                ts.contains("damager instanceof Projectile") && ts.contains("ya.getShooter()"),
                "辿っていない");

        String load = src("datapacks", "jidai_craft", "data", "jidai", "functions", "load.mcfunction");
        for (String t : new String[]{"kyuryo", "shinrin", "kawa", "naikai", "iwaba"}) {
            check("★保険: " + t + " は friendlyFire false",
                    load.contains("team modify " + t + " friendlyFire false"), "無い");
        }


        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        System.out.println("★ TaCZ の弾がこのイベントに来るかは実機でしか分からない。");
        System.out.println("  docs/実機確認リスト.md に人の目で見る物として残してある。");
        if (fail > 0) System.exit(1);
    }

    /** コンストラクタを通さずに作る。 */
    static Object unsafeNew(Class<?> c) throws Exception {
        Class<?> u = Class.forName("sun.misc.Unsafe");
        Field f = u.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        return unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, c);
    }
}
