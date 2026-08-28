import java.lang.reflect.*;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

/** Checks that the faction notice reaches exactly the faction, and nobody else. */
public class Stage4cTest {

    static int pass = 0, fail = 0;
    static final Map<String, Map<String, Integer>> BOARD = new HashMap<>();
    static final Map<String, String> TEAMS = new LinkedHashMap<>();
    static final Map<String, PlayerStub> ONLINE = new LinkedHashMap<>();

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
        return (T) Proxy.newProxyInstance(Stage4cTest.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    static class PlayerStub implements InvocationHandler {
        final String name;
        final List<String> messages = new ArrayList<>();
        Player self;
        PlayerStub(String name) { this.name = name; }
        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "getName": return name;
                case "sendMessage":
                    if (a != null && a.length == 1 && a[0] instanceof String s) messages.add(s);
                    return null;
                case "getInventory":
                    return proxy(org.bukkit.inventory.PlayerInventory.class, (p2, m2, a2) -> {
                        switch (m2.getName()) {
                            case "firstEmpty": return 0;
                            case "addItem": return new HashMap<Integer, org.bukkit.inventory.ItemStack>();
                        }
                        return fallback(m2, p2, a2);
                    });
            }
            return fallback(m, p, a);
        }
    }

    static PlayerStub join(String name, String team) {
        PlayerStub h = new PlayerStub(name);
        h.self = proxy(Player.class, h);
        ONLINE.put(name, h);
        if (team != null) TEAMS.put(name, team);
        return h;
    }

    static int get(String obj, String entry) {
        return BOARD.getOrDefault(obj, Map.of()).getOrDefault(entry, 0);
    }
    static void set(String obj, String entry, int v) {
        BOARD.computeIfAbsent(obj, k -> new HashMap<>()).put(entry, v);
    }

    public static void main(String[] args) throws Exception {
        Field tu = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        tu.setAccessible(true);
        Object unsafe = tu.get(null);
        Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);

        Scoreboard board = proxy(Scoreboard.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getObjective": {
                    String n = (String) a[0];
                    if (!BOARD.containsKey(n)) return null;
                    return proxy(Objective.class, (p2, m2, a2) -> {
                        if (m2.getName().equals("getScore")) {
                            String entry = String.valueOf(a2[0] instanceof String ? a2[0] : a2[0]);
                            return proxy(Score.class, (p3, m3, a3) -> {
                                switch (m3.getName()) {
                                    case "getScore": return get(n, entry);
                                    case "setScore": set(n, entry, (Integer) a3[0]); return null;
                                }
                                return fallback(m3, p3, a3);
                            });
                        }
                        return fallback(m2, p2, a2);
                    });
                }
                case "getEntryTeam": {
                    String t = TEAMS.get((String) a[0]);
                    if (t == null) return null;
                    return proxy(Team.class, (p2, m2, a2) -> {
                        switch (m2.getName()) {
                            case "getName": return t;
                            case "hasEntry": return t.equals(TEAMS.get((String) a2[0]));
                        }
                        return fallback(m2, p2, a2);
                    });
                }
            }
            return fallback(m, p, a);
        });

        ScoreboardManager mgr = proxy(ScoreboardManager.class, (p, m, a) ->
                m.getName().equals("getMainScoreboard") ? board : fallback(m, p, a));

        Server server = proxy(Server.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getScoreboardManager": return mgr;
                case "getOnlinePlayers": {
                    List<Player> l = new ArrayList<>();
                    for (PlayerStub h : ONLINE.values()) l.add(h.self);
                    return l;
                }
                case "broadcast": throw new AssertionError("Bukkit.broadcast が呼ばれた（全体通知が残っている）");
                case "getLogger": return java.util.logging.Logger.getLogger("Stage4cTest");
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true); bs.set(null, server);

        System.out.println("=== 勢力の金の知らせが「勢力の中だけ」に届くか ===");
        System.out.println();

        BOARD.put("kane_kojin", new HashMap<>());
        BOARD.put("chokin", new HashMap<>());
        BOARD.put("chuo", new HashMap<>());
        jidaiSet(4);
        set("chokin", "丘陵", 100);
        set("chokin", "森林", 100);

        // 丘陵2人 / 森林2人 / 無所属1人
        PlayerStub ronty  = join("ronty",   "kyuryo");
        PlayerStub okusan = join("okusan",  "kyuryo");
        PlayerStub teki1  = join("teki1",   "shinrin");
        PlayerStub teki2  = join("teki2",   "shinrin");
        PlayerStub kanri  = join("kanri",   null);

        Class<?> kaneClass = Class.forName("jidai.Kane");
        Object kane = kaneClass.getDeclaredConstructor().newInstance();
        Method onaji = kaneClass.getMethod("onajiSeiryoku", Player.class);

        @SuppressWarnings("unchecked")
        List<Player> nakama = (List<Player>) onaji.invoke(kane, ronty.self);
        List<String> namae = new ArrayList<>();
        for (Player p : nakama) namae.add(p.getName());
        check("同じ勢力の2人だけが選ばれる (本人を含む)",
                namae.size() == 2 && namae.contains("ronty") && namae.contains("okusan"),
                String.valueOf(namae));
        check("★他勢力の人は選ばれない",
                !namae.contains("teki1") && !namae.contains("teki2"), String.valueOf(namae));
        check("★どの勢力にも入っていない人は選ばれない",
                !namae.contains("kanri"), String.valueOf(namae));
        System.out.println("      選ばれた相手: " + namae);

        // 実際に買ってみる（購入経路まるごと）
        Class<?> shopClass = Class.forName("jidai.Shop");
        Object shop = allocate.invoke(unsafe, shopClass);
        tanaIreru(shopClass, shop, "IPPAN");
        Method kau = shopClass.getMethod("kau", Player.class, int.class, kaneClass);
        try {
            // ★ 鉄の胸当てはタブ2（防具）にある
            pageSet(shop, ronty.self, 2);
            kau.invoke(shop, ronty.self, 15, kane);
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            while (c.getCause() != null) c = c.getCause();
            System.out.println("[INFO] 途中で停止: " + c);
        }
        check("勢力の金庫が 100 から 70 になる", get("chokin", "丘陵") == 70,
                "実測=" + get("chokin", "丘陵"));

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }

    /**
     * ★ Unsafe.allocateInstance は【コンストラクタを通らない】。
     *   そのため 2026-08-20 に足した品揃え(shohin)と店名(namae)が
     *   null のままになり、買おうとすると落ちる。ここで入れてやる。
     *   hako は "IPPAN"(販売所) か "JUKIHIN"(銃器専門店)。
     */
    static void tanaIreru(Class<?> shopClass, Object shop, String hako) throws Exception {
        java.lang.reflect.Field tana = shopClass.getDeclaredField(hako);
        tana.setAccessible(true);
        java.lang.reflect.Field f = shopClass.getDeclaredField("shohin");
        f.setAccessible(true);
        f.set(shop, tana.get(null));
        java.lang.reflect.Field g = shopClass.getDeclaredField("namae");
        g.setAccessible(true);
        g.set(shop, hako.equals("JUKIHIN") ? "銃器専門店" : "販売所");
        java.lang.reflect.Field h = shopClass.getDeclaredField("shurui");
        h.setAccessible(true);
        h.set(shop, hako.equals("JUKIHIN") ? "juki" : "hanbaijo");
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

    /**
     * ★ その人が見ているタブ（ページ）を直接 決める。
     *   本番ではタブを押して切り替えるが、その処理は次の tick に回すので
     *   ハーネスでは走らない。ここで直接 入れて先へ進む。
     */
    static void pageSet(Object shop, Object hito, int page) throws Exception {
        String namae = (hito instanceof String) ? (String) hito
                : ((org.bukkit.entity.Player) hito).getName();
        java.lang.reflect.Field f = shop.getClass().getDeclaredField("pageOf");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Integer> m = (java.util.Map<String, Integer>) f.get(shop);
        if (m == null) {
            // ★ Shop は「初めて使う時に作る」形なので、まだ null のことがある
            //   （Unsafe でコンストラクタを通していないため）。ここで作る。
            m = new java.util.HashMap<>();
            f.set(shop, m);
        }
        m.put(namae, page);
    }
}
