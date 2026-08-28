import java.lang.reflect.*;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class Stage4Test {

    static int pass = 0, fail = 0;
    static Object unsafe; static Method allocate;

    static final Map<String, Map<String, Integer>> BOARD = new HashMap<>();
    static final Map<String, String> TEAMS = new HashMap<>();
    static final List<String> BROADCASTS = new ArrayList<>();

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
        return (T) Proxy.newProxyInstance(Stage4Test.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    static int get(String obj, String entry) {
        return BOARD.getOrDefault(obj, Map.of()).getOrDefault(entry, 0);
    }
    static void set(String obj, String entry, int v) {
        BOARD.computeIfAbsent(obj, k -> new HashMap<>()).put(entry, v);
    }

    static Score score(String obj, String entry) {
        return proxy(Score.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getScore": return get(obj, entry);
                case "setScore": set(obj, entry, (Integer) a[0]); return null;
                case "getEntry": return entry;
                case "isScoreSet": return BOARD.getOrDefault(obj, Map.of()).containsKey(entry);
            }
            return fallback(m, p, a);
        });
    }

    static Objective objective(String name) {
        return proxy(Objective.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getName": return name;
                case "getScore":
                    if (a[0] instanceof String s) return score(name, s);
                    return score(name, String.valueOf(a[0]));
            }
            return fallback(m, p, a);
        });
    }

    static Scoreboard board() {
        return proxy(Scoreboard.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getObjective":
                    String n = (String) a[0];
                    return BOARD.containsKey(n) ? objective(n) : null;
                case "getEntryTeam":
                    String t = TEAMS.get((String) a[0]);
                    if (t == null) return null;
                    return proxy(Team.class, (p2, m2, a2) ->
                            m2.getName().equals("getName") ? t : fallback(m2, p2, a2));
            }
            return fallback(m, p, a);
        });
    }

    static class PlayerStub implements InvocationHandler {
        final String name; final boolean full;
        final List<String> messages = new ArrayList<>();
        final List<Object> given = new ArrayList<>();
        PlayerStub(String name, boolean full) { this.name = name; this.full = full; }

        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "getName": return name;
                case "sendMessage":
                    if (a != null && a.length == 1 && a[0] instanceof String s) messages.add(s);
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
    }

    static Player player(PlayerStub h) { return proxy(Player.class, h); }

    public static void main(String[] args) throws Exception {
        Field tu = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        tu.setAccessible(true); unsafe = tu.get(null);
        allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);

        ScoreboardManager mgr = proxy(ScoreboardManager.class, (p, m, a) ->
                m.getName().equals("getMainScoreboard") ? board() : fallback(m, p, a));
        Server server = proxy(Server.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getScoreboardManager": return mgr;
                case "broadcast":
                    BROADCASTS.add(String.valueOf(a[0]));
                    return 1;
                case "getLogger": return java.util.logging.Logger.getLogger("Stage4Test");
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true); bs.set(null, server);

        System.out.println("=== 段階4: 金の出し入れと購入の実測 ===");
        System.out.println();

        Class<?> kaneClass = Class.forName("jidai.Kane");
        Object kane = kaneClass.getDeclaredConstructor().newInstance();

        Method junbiOK = kaneClass.getMethod("junbiOK");
        check("データパック未読込なら junbiOK は false", !(Boolean) junbiOK.invoke(kane), "true になった");

        BOARD.put("kane_kojin", new HashMap<>());
        BOARD.put("chokin", new HashMap<>());
        // 段階6: 商品に解禁時代が付いた。ここでは全部解禁された状態で試す
        BOARD.put("chuo", new HashMap<>());
        jidaiSet(4);
        check("目的が揃えば junbiOK は true", (Boolean) junbiOK.invoke(kane), "false のまま");

        Method kojinZandaka = kaneClass.getMethod("kojinZandaka", Player.class);
        Method kojinKousin = kaneClass.getMethod("kojinKousin", Player.class, int.class);
        Method kinkoMei = kaneClass.getMethod("kinkoMei", Player.class);
        Method seiZandaka = kaneClass.getMethod("seiryokuZandaka", String.class);

        PlayerStub rontyH = new PlayerStub("ronty", false);
        Player ronty = player(rontyH);
        set("kane_kojin", "ronty", 50);
        check("個人の残高を正しく読む", (Integer) kojinZandaka.invoke(kane, ronty) == 50,
                "実測=" + kojinZandaka.invoke(kane, ronty));
        kojinKousin.invoke(kane, ronty, 33);
        check("個人の残高を正しく書く", get("kane_kojin", "ronty") == 33, "実測=" + get("kane_kojin", "ronty"));

        TEAMS.put("ronty", "kyuryo");
        check("kyuryo は 丘陵", "丘陵".equals(kinkoMei.invoke(kane, ronty)),
                "実測=" + kinkoMei.invoke(kane, ronty));
        TEAMS.put("ronty", "shinrin");
        check("shinrin は 森林", "森林".equals(kinkoMei.invoke(kane, ronty)),
                "実測=" + kinkoMei.invoke(kane, ronty));
        TEAMS.remove("ronty");
        check("勢力未所属なら null", kinkoMei.invoke(kane, ronty) == null,
                "実測=" + kinkoMei.invoke(kane, ronty));
        TEAMS.put("ronty", "yoso");
        check("対応表に無いチームなら null (勝手に金庫を作らない)", kinkoMei.invoke(kane, ronty) == null,
                "実測=" + kinkoMei.invoke(kane, ronty));
        TEAMS.remove("ronty");

        set("chokin", "丘陵", 100);
        set("chokin", "森林", 55);
        check("金庫ごとに別々の残高を読む",
                (Integer) seiZandaka.invoke(kane, "丘陵") == 100
                        && (Integer) seiZandaka.invoke(kane, "森林") == 55, "取り違え");

        Class<?> shopClass = Class.forName("jidai.Shop");
        Object shop = allocate.invoke(unsafe, shopClass);
        tanaIreru(shopClass, shop, "IPPAN");
        Method kau = shopClass.getMethod("kau", Player.class, int.class, kaneClass);

        PlayerStub p1 = new PlayerStub("ronty", false);
        set("kane_kojin", "ronty", 100);
        // ★ 本当に空いている枠を押す。
        //   2026-08-22 に剣を2本 足したので、タブ1は 9〜14 が埋まった。
        //   空いているのは 15〜17。
        //   （タブ2の 15 は鉄の胸当てなので、ページを間違えると
        //     「商品がある」ことになってしまう）
        pageSet(shop, player(p1), 1); kau.invoke(shop, player(p1), 15, kane);
        check("商品の無い枠: 断り、金は動かない",
                p1.messages.size() == 1 && p1.messages.get(0).contains("商品がありません")
                        && get("kane_kojin", "ronty") == 100,
                p1.messages + " 残高=" + get("kane_kojin", "ronty"));

        PlayerStub p2 = new PlayerStub("ronty", false);
        set("kane_kojin", "ronty", 1);
        pageSet(shop, player(p2), 1); kau.invoke(shop, player(p2), 9, kane);
        check("個人・残高不足: 金もアイテムも動かない",
                get("kane_kojin", "ronty") == 1 && p2.given.isEmpty(),
                "残高=" + get("kane_kojin", "ronty") + " 渡した=" + p2.given.size());
        check("個人・残高不足: 必要額と所持額を伝える",
                p2.messages.size() == 1 && p2.messages.get(0).contains("必要 2")
                        && p2.messages.get(0).contains("所持 1"), String.valueOf(p2.messages));
        System.out.println("      文面: " + p2.messages.get(0));

        PlayerStub p3 = new PlayerStub("ronty", false);
        TEAMS.remove("ronty");
        set("chokin", "丘陵", 100);
        pageSet(shop, player(p3), 2); kau.invoke(shop, player(p3), 15, kane);
        check("勢力・未所属: どの金庫も動かない",
                get("chokin", "丘陵") == 100 && get("chokin", "森林") == 55
                        && p3.given.isEmpty(), String.valueOf(p3.messages));
        // 勢力に入っていない人は勢力の金を使えない
        //   どちらにせよ勢力の金庫は1円も動かない、が守りたいこと。
        check("勢力・未所属: 勢力の金は使えないと断られる",
                p3.messages.size() == 1 && p3.messages.get(0).contains("勢力の金は使えません"),
                String.valueOf(p3.messages));
        System.out.println("      文面: " + p3.messages.get(0));

        PlayerStub p4 = new PlayerStub("ronty", false);
        TEAMS.put("ronty", "shinrin");
        set("chokin", "森林", 29);
        set("chokin", "丘陵", 100);
        pageSet(shop, player(p4), 2); kau.invoke(shop, player(p4), 15, kane);
        check("勢力・金庫不足: 金もアイテムも動かない",
                get("chokin", "森林") == 29 && p4.given.isEmpty(),
                "金庫=" + get("chokin", "森林"));
        check("勢力・金庫不足: 他勢力の金庫に手を出さない",
                get("chokin", "丘陵") == 100, "丘陵=" + get("chokin", "丘陵"));
        check("勢力・金庫不足: 必要額と金庫残高を伝える",
                p4.messages.size() == 1 && p4.messages.get(0).contains("必要 30")
                        && p4.messages.get(0).contains("金庫 29"), String.valueOf(p4.messages));
        System.out.println("      文面: " + p4.messages.get(0));

        PlayerStub p5 = new PlayerStub("ronty", true);
        set("kane_kojin", "ronty", 999);
        pageSet(shop, player(p5), 1); kau.invoke(shop, player(p5), 9, kane);
        check("持ち物いっぱい: 金が減らない (最悪の事故を防ぐ)",
                get("kane_kojin", "ronty") == 999 && p5.given.isEmpty(),
                "残高=" + get("kane_kojin", "ronty"));
        check("持ち物いっぱい: 理由を伝える",
                p5.messages.size() == 1 && p5.messages.get(0).contains("持ち物がいっぱい"),
                String.valueOf(p5.messages));

        Map<String, Integer> kojinBak = BOARD.remove("kane_kojin");
        PlayerStub p6 = new PlayerStub("ronty", false);
        pageSet(shop, player(p6), 1); kau.invoke(shop, player(p6), 9, kane);
        check("データパック未読込: 落ちずに断る",
                p6.messages.size() == 1 && p6.messages.get(0).contains("データパック"),
                String.valueOf(p6.messages));
        BOARD.put("kane_kojin", kojinBak);

        System.out.println();
        System.out.println("--- 購入成立の経路 ---");
        PlayerStub p7 = new PlayerStub("ronty", false);
        set("kane_kojin", "ronty", 10);
        String err = null;
        try {
            pageSet(shop, player(p7), 1); kau.invoke(shop, player(p7), 9, kane);
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            while (c.getCause() != null) c = c.getCause();
            err = c.toString();
        }
        if (err != null) System.out.println("[INFO] 途中で停止: " + err);
        check("購入成立(個人): 残高が 10 から 8 になる",
                get("kane_kojin", "ronty") == 8, "実測=" + get("kane_kojin", "ronty"));

        PlayerStub p8 = new PlayerStub("ronty", false);
        TEAMS.put("ronty", "kyuryo");
        set("chokin", "丘陵", 100);
        set("chokin", "森林", 55);
        try { pageSet(shop, player(p8), 2); kau.invoke(shop, player(p8), 15, kane); } catch (InvocationTargetException ignored) { }
        check("購入成立(勢力): 丘陵が 100 から 70 になる",
                get("chokin", "丘陵") == 70, "実測=" + get("chokin", "丘陵"));
        check("購入成立(勢力): 森林は 55 のまま (取り違えない)",
                get("chokin", "森林") == 55, "実測=" + get("chokin", "森林"));

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
