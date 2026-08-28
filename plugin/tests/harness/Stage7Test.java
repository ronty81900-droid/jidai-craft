import java.lang.reflect.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class Stage7Test {

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
        return (T) Proxy.newProxyInstance(Stage7Test.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    static int get(String obj, String entry) {
        return BOARD.getOrDefault(obj, Map.of()).getOrDefault(entry, 0);
    }
    static void set(String obj, String entry, int v) {
        BOARD.computeIfAbsent(obj, k -> new HashMap<>()).put(entry, v);
    }

    static class PlayerStub implements InvocationHandler {
        final String name; boolean full = false;
        final List<String> messages = new ArrayList<>();
        final List<Object> given = new ArrayList<>();
        PlayerStub(String name) { this.name = name; }
        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "getName": return name;
                case "sendMessage":
                    if (a != null && a.length == 1) {
                        if (a[0] instanceof String s) messages.add(s);
                        else if (a[0] instanceof net.kyori.adventure.text.Component c) {
                            messages.add(net.kyori.adventure.text.serializer.plain
                                    .PlainTextComponentSerializer.plainText().serialize(c));
                        }
                    }
                    return null;
                case "getInventory":
                    return proxy(PlayerInventory.class, (p2, m2, a2) -> {
                        switch (m2.getName()) {
                            case "firstEmpty": return full ? -1 : 0;
                            case "addItem":
                                if (a2 != null && a2.length > 0) given.add(a2[0]);
                                return new HashMap<Integer, ItemStack>();
                        }
                        return fallback(m2, p2, a2);
                    });
            }
            return fallback(m, p, a);
        }
        void clear() { messages.clear(); given.clear(); }
        String last() { return messages.isEmpty() ? "" : messages.get(messages.size() - 1); }
    }

    static String readClass(String jarPath, String entry) throws Exception {
        try (JarFile jar = new JarFile(jarPath)) {
            try (var in = jar.getInputStream(jar.getJarEntry(entry))) {
                return new String(in.readAllBytes(), "ISO-8859-1");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        Field tu = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        tu.setAccessible(true);
        Object unsafe = tu.get(null);
        Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);

        Scoreboard board = proxy(Scoreboard.class, (p, m, a) -> {
            if (!m.getName().equals("getObjective")) return fallback(m, p, a);
            String n = (String) a[0];
            if (!BOARD.containsKey(n)) return null;
            return proxy(Objective.class, (p2, m2, a2) -> {
                if (!m2.getName().equals("getScore")) return fallback(m2, p2, a2);
                String entry = (a2[0] instanceof Player pl) ? pl.getName() : String.valueOf(a2[0]);
                return proxy(Score.class, (p3, m3, a3) -> {
                    switch (m3.getName()) {
                        case "getScore": return get(n, entry);
                        case "setScore": set(n, entry, (Integer) a3[0]); return null;
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
                case "getWorlds": return new ArrayList<org.bukkit.World>();
                case "getOnlinePlayers": return new ArrayList<Player>();
                case "getLogger": return Logger.getLogger("Stage7Test");
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true); bs.set(null, server);

        System.out.println("=== 段階7: ガチャの実測 ===");
        System.out.println();

        Class<?> kaneClass = Class.forName("jidai.Kane");
        Class<?> gachaClass = Class.forName("jidai.Gacha");
        Class<?> pluginClass = Class.forName("jidai.JidaiCraft");
        Object kane = kaneClass.getDeclaredConstructor().newInstance();
        // 段階9: Gacha は JidaiCraft を受け取るようになった
        Object plugin = allocate.invoke(unsafe, pluginClass);
        Field plf = org.bukkit.plugin.java.JavaPlugin.class.getDeclaredField("logger");
        plf.setAccessible(true); plf.set(plugin, Logger.getLogger("Stage7Test"));
        Field psf = org.bukkit.plugin.java.JavaPlugin.class.getDeclaredField("server");
        psf.setAccessible(true); psf.set(plugin, server);
        Object gacha = gachaClass.getDeclaredConstructor(pluginClass).newInstance(plugin);

        // 乱数を種つきに差し替える（同じ結果が再現できるように）
        Field ranF = gachaClass.getDeclaredField("ran");
        ranF.setAccessible(true);
        ranF.set(gacha, new Random(12345));

        Method mErabu = gachaClass.getDeclaredMethod("erabu", int.class);
        mErabu.setAccessible(true);
        Method mMawasu = gachaClass.getMethod("mawasu", Player.class, kaneClass);
        Method mBotan = gachaClass.getMethod("botanKa", int.class);

        BOARD.put("kane_kojin", new HashMap<>());
        BOARD.put("chokin", new HashMap<>());
        BOARD.put("chuo", new HashMap<>());

        // ---------- A. 抽選が時代を守るか ----------
        //
        // ★★ 目印を手で書かない（2026-08-22）★★
        //   もとは {"パン×2", "焼いた牛肉×4", ...} と目印の品を並べていたが、
        //   景品表を入れ替えた時に**目印だけが古いまま**残り、
        //   中身は正しいのに4件まとめて赤くなった。
        //   表そのものから「その時代にある品」を読んで突き合わせる。
        Object[] hyou = hyou(gachaClass);

        for (int jidai : new int[]{1, 2, 3, 4}) {
            Set<String> sonoJidai = new TreeSet<>();
            for (Object k : hyou) {
                if (kazu(k, "jidai") == jidai) {
                    sonoJidai.add(namaeKazu(k));
                }
            }
            // ★ 表の中で（名前×個数）が重なっていないこと。
            //   重なっていると、この見分け方そのものが成り立たない。
            int kensu = 0;
            for (Object k : hyou) {
                if (kazu(k, "jidai") == jidai) {
                    kensu++;
                }
            }
            check("時代 " + jidai + ": 景品が（名前×個数）で見分けられる",
                    kensu == sonoJidai.size(), "表 " + kensu + " 件 / 名前は " + sonoJidai.size() + " 通り");

            Map<String, Integer> deta = new TreeMap<>();
            for (int i = 0; i < 40000; i++) {
                deta.merge(namaeKazu(mErabu.invoke(gacha, jidai)), 1, Integer::sum);
            }
            List<String> yosomono = new ArrayList<>();
            for (String key : deta.keySet()) {
                if (!sonoJidai.contains(key)) {
                    yosomono.add(key);
                }
            }
            check("勢力の時代 " + jidai + ": 他の時代の景品が混ざらない",
                    yosomono.isEmpty(), "混ざった: " + yosomono);
            check("勢力の時代 " + jidai + ": 表にある景品が全部 出る",
                    deta.size() == sonoJidai.size(),
                    "出た " + deta.size() + " / 表 " + sonoJidai.size());
            System.out.println("      " + deta);
        }

        // ---------- B. 重みが効いているか ----------
        Map<String, Integer> d1 = new TreeMap<>();
        for (int i = 0; i < 20000; i++) {
            Object k = mErabu.invoke(gacha, 1);
            d1.merge(namaeKazu(k), 1, Integer::sum);
        }
        // ★ 比べる相手も表から選ぶ。いちばん重い品と、いちばん軽い品。
        Object omoi = null, karui = null;
        for (Object k : hyou) {
            if (kazu(k, "jidai") != 1) {
                continue;
            }
            if (omoi == null || kazu(k, "omomi") > kazu(omoi, "omomi")) {
                omoi = k;
            }
            if (karui == null || kazu(k, "omomi") < kazu(karui, "omomi")) {
                karui = k;
            }
        }
        int ooi = d1.getOrDefault(namaeKazu(omoi), 0);
        int dai = d1.getOrDefault(namaeKazu(karui), 0);
        check("重みが効いている (" + namaeKazu(omoi) + " が " + namaeKazu(karui) + " より明らかに多い)",
                ooi > dai * 5, namaeKazu(omoi) + "=" + ooi + " " + namaeKazu(karui) + "=" + dai);
        check("いちばん軽い景品もちゃんと出る", dai > 0, "0回");
        System.out.println("      20000回: " + namaeKazu(omoi) + "=" + ooi
                + " " + namaeKazu(karui) + "=" + dai);

        // ---------- C. まわす時の門番 ----------
        PlayerStub h = new PlayerStub("ronty");
        Player self = proxy(Player.class, h);
        jidaiSet(1);

        // 金が足りない
        set("kane_kojin", "ronty", 4);
        h.clear();
        mawasuSafe(mMawasu, gacha, self, kane);
        check("★金が足りなければ回らない", h.last().contains("足りません"), h.last());
        check("★足りない時、金が1も動かない", get("kane_kojin", "ronty") == 4,
                "実測=" + get("kane_kojin", "ronty"));

        // 持ち物いっぱい
        set("kane_kojin", "ronty", 100);
        h.clear(); h.full = true;
        mawasuSafe(mMawasu, gacha, self, kane);
        check("★持ち物がいっぱいなら回らない", h.last().contains("持ち物がいっぱい"), h.last());
        check("★持ち物いっぱいの時、金が1も動かない", get("kane_kojin", "ronty") == 100,
                "実測=" + get("kane_kojin", "ronty"));
        h.full = false;

        // データパック未読込
        Map<String, Integer> bak = BOARD.remove("kane_kojin");
        h.clear();
        mawasuSafe(mMawasu, gacha, self, kane);
        check("データパック未読込なら落ちずに断る", h.last().contains("データパック"), h.last());
        BOARD.put("kane_kojin", bak);

        // ---------- D. 実際に回す ----------
        // 品が当たる経路はアイテム生成でこの環境の壁に当たる。
        // ただし金の出し入れはその手前で終わっているので、
        // **残高の動き方**で「正しく処理されたか」を測る。
        //   品が当たった  … ちょうど 5 減る
        //   金が当たった  … 5 減って、その分が戻る (10 なら +5、50 なら +45)
        // ★ 起こりうる差分も表から作る。手で {0, 95} と書いていたせいで、
        //   金の景品を増やした時にここだけ古くなった（2026-08-22）。
        Set<Integer> monoSa = new TreeSet<>();
        Set<Integer> kaneSa = new TreeSet<>();
        monoSa.add(-5);                               // 品が当たった＝値段ぶん減るだけ
        for (Object k : hyou) {
            if (kazu(k, "jidai") == 1 && kazu(k, "kane") > 0) {
                kaneSa.add(kazu(k, "kane") - 5);
            }
        }

        set("kane_kojin", "ronty", 100000);
        int mono = 0, kaneA = 0, hen = 0;
        List<Integer> henna = new ArrayList<>();
        for (int i2 = 0; i2 < 500; i2++) {
            int mae = get("kane_kojin", "ronty");
            h.clear();
            mawasuSafe(mMawasu, gacha, self, kane);
            int sa = get("kane_kojin", "ronty") - mae;
            if (kaneSa.contains(sa)) {
                kaneA++;
            } else if (monoSa.contains(sa)) {
                mono++;
            } else {
                hen++;
                if (henna.size() < 3) henna.add(sa);
            }
        }
        check("★500回まわして、残高の動きが必ず筋の通った値になる",
                hen == 0, "おかしかった回=" + hen + " 差分の例=" + henna);
        check("品の当たりでは ちょうど 5 引かれる", mono > 0, "0回");
        check("金の当たりでは その額が入る (5なら±0 / 100なら+95)", kaneA > 0, "0回");
        System.out.println("      500回: 品=" + mono + " 金=" + kaneA
                + " 残高 100000 → " + get("kane_kojin", "ronty"));
        System.out.println("      (品が当たった時のアイテム受け渡しは、この環境では実行できない)");

        // ---------- E. ボタン以外は反応しない ----------
        check("★中央の枠だけが「まわす」ボタン",
                (Boolean) mBotan.invoke(gacha, 4)
                        && !(Boolean) mBotan.invoke(gacha, 0)
                        && !(Boolean) mBotan.invoke(gacha, 8), "枠の判定がおかしい");

        // ---------- F. 出してはいけない物が入っていないこと ----------
        String cls = readClass(jarPath, "jidai/Gacha.class");
        String[] dame = {"CROSSBOW", "SWORD", "AXE", "BOW", "TRIDENT", "HELMET",
                "CHESTPLATE", "LEGGINGS", "BOOTS", "SHIELD", "TNT", "sekiyu"};
        List<String> mitsuketa = new ArrayList<>();
        for (String d : dame) {
            if (cls.contains(d)) mitsuketa.add(d);
        }
        check("★★勢力の戦力に影響する物が景品に入っていない (銃・防具・武器・石油)",
                mitsuketa.isEmpty(), "見つかった: " + mitsuketa);
        check("個人の金だけを扱っている (勢力の貯金に触れていない)",
                !cls.contains("seiryokuZandaka") && !cls.contains("seiryokuKousin"), "触れている");

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }

    /**
     * 景品表そのものを取り出す。
     *
     * ★★ 検証が表を写し取らないようにするため ★★
     *   期待する品名や確率をテスト側に書くと、表を入れ替えるたびに
     *   **中身は正しいのにテストだけ古くなる**。
     *   （2026-08-22 に実際そうなった。4件まとめて赤くなった）
     */
    static Object[] hyou(Class<?> gachaClass) throws Exception {
        Field f = gachaClass.getDeclaredField("KEIHIN");
        f.setAccessible(true);
        return (Object[]) f.get(null);
    }

    /** record の数値の欄を1つ取り出す（kazu / kane / omomi / jidai）。 */
    static int kazu(Object rec, String mei) throws Exception {
        Method m = rec.getClass().getMethod(mei);
        m.setAccessible(true);
        return (Integer) m.invoke(rec);
    }

    /** private record の値を取り出す。record のクラスが private なので setAccessible が要る。 */
    static String namae(Object rec) throws Exception {
        Method m = rec.getClass().getMethod("namae");
        m.setAccessible(true);
        return (String) m.invoke(rec);
    }

    /**
     * 名前と個数を組にした目印。
     *
     * ★★ なぜ組にするか（2026-08-22）★★
     *   景品の名前から個数を外した（「パン 32」→「パン」＋ kazu 32）ので、
     *   名前だけだと「パン」が時代の中で何度も出てきて区別できない。
     *   名前だけで見分ける形のままにすると、時代の混ざりを見逃す。
     */
    static String namaeKazu(Object rec) throws Exception {
        Method m = rec.getClass().getMethod("kazu");
        m.setAccessible(true);
        return namae(rec) + "×" + m.invoke(rec);
    }

    static void mawasuSafe(Method m, Object gacha, Player p, Object kane) throws Exception {
        try { m.invoke(gacha, p, kane); }
        catch (InvocationTargetException e) { /* アイテム生成の壁。金の判定はその手前 */ }
    }

    /**
     * 中央と5勢力の時代を、まとめて同じ値にする。
     *
     * ★★ 2026-08-20 に、販売所の解禁が【勢力の時代】へ変わった ★★
     *   中央(chuo/世界)だけ動かしても棚は変わらない。
     *   このテストは「時代が N の世界」を作りたいだけなので、両方そろえる。
     */
    static void jidaiSet(int n) {
        set("chuo", "世界", n);
        for (String k : new String[]{"丘陵", "森林", "川", "内海", "岩場"}) {
            set("jidai", k, n);
        }
    }
}
