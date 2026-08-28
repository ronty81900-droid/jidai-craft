import java.io.File;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

/**
 * Runs onEnable() itself — the path every other harness skipped.
 *
 * onEnable cannot complete headlessly: new Shop() builds ItemStacks, which
 * need the server's item registry. So the contract this test enforces is:
 *   "onEnable must fail ONLY at that known wall, and nowhere earlier."
 * Anything else that throws is a real startup bug.
 */
public class OnEnableTest {

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
        if (r == float.class) return 0.0f;
        if (r == long.class) return 0L;
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> iface, InvocationHandler h) {
        return (T) Proxy.newProxyInstance(OnEnableTest.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    static Throwable root(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t;
    }

    /** 組み立て済みかを見るために、private の欄をのぞく。 */
    static Object f(Object o, String name) throws Exception {
        Field fl = o.getClass().getDeclaredField(name);
        fl.setAccessible(true);
        return fl.get(o);
    }

    public static void main(String[] args) throws Exception {
        String jarPath = args[0];

        Field tu = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        tu.setAccessible(true);
        Object unsafe = tu.get(null);
        Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);

        BOARD.put("kane_kojin", new HashMap<>());
        BOARD.put("chokin", new HashMap<>());
        BOARD.put("sekiyu", new HashMap<>());
        BOARD.put("settei", new HashMap<>());

        Scoreboard board = proxy(Scoreboard.class, (p, m, a) -> {
            if (m.getName().equals("getObjective")) {
                String n = (String) a[0];
                if (!BOARD.containsKey(n)) return null;
                return proxy(Objective.class, (p2, m2, a2) ->
                        m2.getName().equals("getScore")
                                ? proxy(Score.class, (p3, m3, a3) -> fallback(m3, p3, a3))
                                : fallback(m2, p2, a2));
            }
            return fallback(m, p, a);
        });
        ScoreboardManager mgr = proxy(ScoreboardManager.class, (p, m, a) ->
                m.getName().equals("getMainScoreboard") ? board : fallback(m, p, a));
        BukkitScheduler sched = proxy(BukkitScheduler.class, (p, m, a) -> fallback(m, p, a));
        PluginManager pm = proxy(PluginManager.class, (p, m, a) -> fallback(m, p, a));

        Server server = proxy(Server.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getScoreboardManager": return mgr;
                case "getScheduler": return sched;
                case "getPluginManager": return pm;
                case "recipeIterator":
                    // ★ 本物は精錬のレシピ一覧を返す。ここでは空で足りる。
                    //   null を返すと、それはそれで実装の弱さを突く検査になる。
                    return java.util.Collections.emptyIterator();
                case "getLogger": return Logger.getLogger("OnEnableTest");
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true); bs.set(null, server);

        System.out.println("=== 起動処理(onEnable)の実測 ===");
        System.out.println();

        // ---------- 1) 初回起動そのもの ----------
        // 実機の初回起動を再現する: プラグインのフォルダごと存在しない状態
        File dir = new File(System.getProperty("java.io.tmpdir"), "jidai_onenable_test");
        deleteAll(dir);
        check("初回起動の再現: プラグインのフォルダが存在しない", !dir.exists(), "残っている");
        File cfg = new File(dir, "config.yml");

        Class<?> pluginClass = Class.forName("jidai.JidaiCraft");
        Object plugin = allocate.invoke(unsafe, pluginClass);
        Field lf = JavaPlugin.class.getDeclaredField("logger");
        lf.setAccessible(true); lf.set(plugin, Logger.getLogger("OnEnableTest"));
        Field cf = JavaPlugin.class.getDeclaredField("configFile");
        cf.setAccessible(true); cf.set(plugin, cfg);
        Field df = JavaPlugin.class.getDeclaredField("dataFolder");
        df.setAccessible(true); df.set(plugin, dir);
        Field cl = JavaPlugin.class.getDeclaredField("classLoader");
        cl.setAccessible(true); cl.set(plugin, OnEnableTest.class.getClassLoader());
        Field sf = JavaPlugin.class.getDeclaredField("server");
        sf.setAccessible(true); sf.set(plugin, server);

        Throwable caught = null;
        try {
            pluginClass.getMethod("onEnable").invoke(plugin);
        } catch (InvocationTargetException e) {
            caught = root(e.getCause());
        }

        // ★ ここが今回の不具合を捕まえる判定
        boolean shippaiWaNashi = caught == null;
        boolean kizonNoKabe = caught instanceof IllegalStateException
                && String.valueOf(caught.getMessage()).contains("RegistryAccess");
        check("★onEnable が『既知の壁』以外で落ちない",
                shippaiWaNashi || kizonNoKabe,
                "落ちた原因: " + caught);
        // 段階6で、棚に並べる処理がコンストラクタから「開く直前」へ移った。
        // そのおかげで onEnable はアイテム生成に触れなくなり、最後まで通るようになった。
        check("★onEnable が最後まで通る", shippaiWaNashi, "途中で止まった: " + caught);

        // ★★ データパックの確認を onEnable でやってはいけない ★★
        //   onEnable はまだ1tickも進んでいない。データパックの jidai:load は
        //   #minecraft:load で【最初の tick】に走るので、この時点では
        //   目的が1つも無い。ここで調べると、正しく入っていても
        //   「読み込まれていません」と出てしまう（実機の Arclight で発生）。
        String jsrc = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
                // ★ 置き場は run_harness.py から -Djidai.root で渡る
                System.getProperty("jidai.root")
                + "/plugin/src/main/java/jidai/JidaiCraft.java")), "UTF-8");
        String onEnableBody = jsrc.substring(jsrc.indexOf("public void onEnable()"),
                jsrc.indexOf("private void datapackKakunin"));
        check("★★★データパックの確認を onEnable の中でしていない",
                !onEnableBody.contains("datapackKakunin()")
                        && !onEnableBody.contains("kane.junbiOK()"),
                "onEnable の中で調べている（まっさらな世界で誤警告になる）");
        check("★★確認は5秒後の jidoScan の中でしている",
                jsrc.contains("private void jidoScan()")
                        && jsrc.substring(jsrc.indexOf("private void jidoScan()"))
                                .startsWith("private void jidoScan()")
                        && jsrc.indexOf("datapackKakunin();",
                                jsrc.indexOf("private void jidoScan()")) > 0,
                "jidoScan が呼んでいない");
        check("★駄目だった時に、確かめる手順を出している",
                jsrc.contains("datapack list enabled")
                        && jsrc.contains("function jidai:load"),
                "対処の案内が無い");
        if (caught != null) {
            System.out.println("      到達点: " + caught);
        }

        // 壁の手前までは確実に済んでいるか（フィールドが埋まっているかで見る）
        check("設定ファイルの読み戻しが完了している (basho が組み立て済み)",
                readField(plugin, pluginClass, "basho") != null, "basho が null");

        // ★ 配線の確認。onEnable が通っても、渡し忘れていれば実機で落ちる。
        //   (v0.5.0 で「起動すらしない」を出したので、組み立て後を必ず見る)
        Object sensenF = f(plugin, "sensen");
        check("★宣戦の画面が組み立て済み (sensen が用意されている)", sensenF != null, "null");
        Object shopF = f(plugin, "shop");
        check("★★販売所に宣戦の画面が渡っている (渡し忘れると戦争宣誓が死ぬ)",
                shopF != null && f(shopF, "sensen") != null, "Shop.sensen が null");
        check("★★販売所に勢力が渡っている (渡し忘れると下剋上の権利が増えない)",
                shopF != null && f(shopF, "seiryoku") != null, "Shop.seiryoku が null");

        // ---------- 2) 同梱リソースの静的検査 ----------
        // 「.jar に同梱していないリソースを要求するコード」が無いことを、
        // バイトコードと .jar の中身を突き合わせて確かめる。
        boolean yobu = classCallsResource(jarPath);
        boolean aru = jarHasEntry(jarPath, "config.yml");
        check("★同梱 config.yml を要求するコードが無い（あるなら .jar に同梱されている）",
                !yobu || aru,
                "saveDefaultConfig/saveResource を呼ぶのに .jar に config.yml が無い");
        System.out.println("      saveResource 系の呼び出し: " + (yobu ? "あり" : "なし")
                + " / .jar 内の config.yml: " + (aru ? "あり" : "なし"));

        // ---------- 3) 設定ファイルが無くても保存できるか ----------
        // saveDefaultConfig を消した以上、最初の書き込みが
        // フォルダごと作れなければ登録が保存されない。
        deleteAll(dir);
        Object p2 = allocate.invoke(unsafe, pluginClass);
        lf.set(p2, Logger.getLogger("OnEnableTest"));
        cf.set(p2, cfg);
        df.set(p2, dir);
        cl.set(p2, OnEnableTest.class.getClassLoader());

        Method getConfig = JavaPlugin.class.getMethod("getConfig");
        Object conf = getConfig.invoke(p2);
        check("フォルダが無くても getConfig() が使える", conf != null, "null が返った");

        Method set = conf.getClass().getMethod("set", String.class, Object.class);
        set.invoke(conf, "hanbaijo", new ArrayList<>(List.of("world,1,2,3")));
        JavaPlugin.class.getMethod("saveConfig").invoke(p2);
        check("★フォルダごと無くても saveConfig() がファイルを作れる", cfg.exists(),
                "config.yml が作られなかった");
        if (cfg.exists()) {
            System.out.println("      作られた中身: "
                    + new String(Files.readAllBytes(cfg.toPath()), "UTF-8").trim().replace("\n", " / "));
        }

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }

    static Object readField(Object o, Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static void deleteAll(File f) {
        if (!f.exists()) return;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteAll(k);
        f.delete();
    }

    /** Does any plugin class reference saveDefaultConfig / saveResource? */
    static boolean classCallsResource(String jarPath) throws Exception {
        try (JarFile jar = new JarFile(jarPath)) {
            var it = jar.entries();
            while (it.hasMoreElements()) {
                var e = it.nextElement();
                if (!e.getName().endsWith(".class")) continue;
                byte[] b;
                try (var in = jar.getInputStream(e)) { b = in.readAllBytes(); }
                String s = new String(b, "ISO-8859-1");
                if (s.contains("saveDefaultConfig") || s.contains("saveResource")) return true;
            }
        }
        return false;
    }

    static boolean jarHasEntry(String jarPath, String name) throws Exception {
        try (JarFile jar = new JarFile(jarPath)) {
            return jar.getJarEntry(name) != null;
        }
    }
}
