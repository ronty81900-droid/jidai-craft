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
import org.bukkit.scoreboard.Team;

public class Stage6Test {

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
        return (T) Proxy.newProxyInstance(Stage6Test.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    static int get(String obj, String entry) {
        return BOARD.getOrDefault(obj, Map.of()).getOrDefault(entry, 0);
    }
    static void set(String obj, String entry, int v) {
        BOARD.computeIfAbsent(obj, k -> new HashMap<>()).put(entry, v);
    }

    static class PlayerStub implements InvocationHandler {
        final String name;
        final List<String> messages = new ArrayList<>();
        PlayerStub(String name) { this.name = name; }
        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "getName": return name;
                case "sendMessage":
                    if (a != null && a.length == 1 && a[0] instanceof String s) messages.add(s);
                    return null;
                case "getInventory":
                    return proxy(PlayerInventory.class, (p2, m2, a2) -> {
                        switch (m2.getName()) {
                            case "firstEmpty": return 0;
                            case "addItem": return new HashMap<Integer, ItemStack>();
                        }
                        return fallback(m2, p2, a2);
                    });
            }
            return fallback(m, p, a);
        }
        void clear() { messages.clear(); }
        String last() { return messages.isEmpty() ? "" : messages.get(messages.size() - 1); }
    }

    static Object shop, kane; static Method mKau;

    /** kau() を呼び、最後に返ってきた文面を返す。 */
    static String kau(PlayerStub h, Player self, int slot) throws Exception {
        h.clear();
        try { mKau.invoke(shop, self, slot, kane); }
        catch (InvocationTargetException e) { /* アイテム生成の壁。金の判定はその手前 */ }
        return h.last();
    }

    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
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
                case "getLogger": return Logger.getLogger("Stage6Test");
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true); bs.set(null, server);

        System.out.println("=== 段階6: 商品5種と未開放表示の実測 ===");
        System.out.println();

        Class<?> kaneClass = Class.forName("jidai.Kane");
        Class<?> shopClass = Class.forName("jidai.Shop");
        kane = kaneClass.getDeclaredConstructor().newInstance();
        shop = allocate.invoke(unsafe, shopClass);
        tanaIreru(shopClass, shop, "IPPAN");
        mKau = shopClass.getMethod("kau", Player.class, int.class, kaneClass);
        Method mChuo = kaneClass.getMethod("chuoJidai");
        Method mMei = kaneClass.getMethod("jidaiMei", int.class);

        // ---------- A. 中央の時代の読み取り ----------
        check("★データパックが無ければ 中央の時代 は 1 とみなす (安全側)",
                (Integer) mChuo.invoke(kane) == 1, "実測=" + mChuo.invoke(kane));

        BOARD.put("kane_kojin", new HashMap<>());
        BOARD.put("chokin", new HashMap<>());
        BOARD.put("chuo", new HashMap<>());
        jidaiSet(3);
        check("★中央の時代を 世界/chuo から読む", (Integer) mChuo.invoke(kane) == 3,
                "実測=" + mChuo.invoke(kane));
        check("時代の呼び名 (1=鉄器 2=中世 3=近代 4=現代)",
                "鉄器".equals(mMei.invoke(null, 1)) && "中世".equals(mMei.invoke(null, 2))
                        && "近代".equals(mMei.invoke(null, 3)) && "現代".equals(mMei.invoke(null, 4)),
                mMei.invoke(null, 1) + "/" + mMei.invoke(null, 2) + "/"
                        + mMei.invoke(null, 3) + "/" + mMei.invoke(null, 4));

        // ---------- B. 時代ごとに買えるものが変わる ----------
        PlayerStub h = new PlayerStub("ronty");
        Player self = proxy(Player.class, h);
        TEAMS.put("ronty", "kyuryo");
        // 所持金は 0 にしておく。解禁されていれば「足りません」、
        // 解禁されていなければ「まだ解禁されていません」で見分ける。
        set("kane_kojin", "ronty", 0);
        set("chokin", "丘陵", 0);

        // ★★ 2026-08-20 に店を2軒に分けた ★★
        //   販売所(この shop)  … 生活・ピッケル・防具
        //   銃器専門店(別の店) … 銃9丁と弾6種  ← BukiTest が見る
        //   ここでは販売所の側だけを、時代の境目にあたる品で見る。
        // ★ ピッケルの販売は取りやめた（枠9〜12 は空）。
        // ★ 2026-08-21: タブを入れた。1段目(0〜8)はタブ専用、商品は2段目から。
        //   枠番号はタブごとに振り直されるので、ページも一緒に持つ。
        // ★ 2026-08-22: 剣を専用のタブ3「武器」へ移した。
        //   タブ1は 9〜12、タブ3は 9〜10。
        // ★ 2026-08-22: 生活タブに6品 足した。タブ1は 9〜13 と 27〜31。
        int[] waku     = {  9,  10,  11,  12,  13,  27,  28,  29,  30,  31,
                            9,  10,  11,  12,  13,   9,  12,  14,  15,  28,  27,  30};
        int[] page     = {  1,   1,   1,   1,   1,   1,   1,   1,   1,   1,
                            3,   3,   3,   3,   3,   2,   2,   2,   2,   2,   2,   2};
        String[] namae = {"パン", "石炭", "原木", "石レンガ", "松明", "焼肉", "ガラス", "戦争宣誓", "金リンゴ", "下剋上",
                          "鉄剣", "弓", "矢", "ダイヤ剣", "クロスボウ",
                          "チェーン兜", "チェーン靴", "鉄兜", "鉄胸",
                          "ダイヤ胸", "ダイヤ兜", "ダイヤ靴"};
        int[] kaikin   = {  1,   1,   1,   1,   1,   2,   2,   2,   3,   3,
                            1,   1,   1,   2,   2,   1,   1,   2,   2,   3,   4,   4};

        for (int jidai = 1; jidai <= 4; jidai++) {
            jidaiSet(jidai);
            StringBuilder mieru = new StringBuilder();
            boolean ok = true;
            for (int i = 0; i < waku.length; i++) {
                pageSet(shop, "ronty", page[i]);
                String msg = kau(h, self, waku[i]);
                boolean kaeru = !msg.contains("まだ解禁されていません");
                boolean hazu = jidai >= kaikin[i];
                if (kaeru != hazu) ok = false;
                mieru.append(namae[i]).append(kaeru ? "○ " : "× ");
            }
            check("中央の時代 " + jidai + " (" + mMei.invoke(null, jidai) + ") で買えるものが正しい",
                    ok, mieru.toString());
            System.out.println("      " + mieru);
        }

        // ★ 銃は販売所から消え、銃器専門店へ移った
        jidaiSet(4);
        // ★ 1段目(0〜8)はタブ。銃は別の店なので、ここには無い。
        pageSet(shop, "ronty", 1);
        check("★★銃は販売所からは買えない (タブ1の 15・16 は空)",
                kau(h, self, 15).contains("そこには商品がありません")
                        && kau(h, self, 16).contains("そこには商品がありません"),
                kau(h, self, 15) + " / " + kau(h, self, 16));
        check("★★ピッケルはもう売っていない (タブ1の 17 も空)",
                kau(h, self, 17).contains("そこには商品がありません")
                        && !kau(h, self, 17).contains("ピッケル"),
                kau(h, self, 17));

        // ---------- C. 未解禁は金が動かない ----------
        jidaiSet(1);
        set("kane_kojin", "ronty", 999);
        set("chokin", "丘陵", 999);
        pageSet(shop, "ronty", 2);
        String msg = kau(h, self, 28);   // ダイヤの胸当て (近代で解禁)
        check("★未解禁の商品は、金があっても買えない",
                msg.contains("まだ解禁されていません"), msg);
        check("★未解禁で断った時、金が1も動かない",
                get("kane_kojin", "ronty") == 999 && get("chokin", "丘陵") == 999,
                "個人=" + get("kane_kojin", "ronty") + " 勢力=" + get("chokin", "丘陵"));
        check("断り文面に、いつ解禁されるかが入る", msg.contains("近代"), msg);
        System.out.println("      文面: " + msg);

        // ---------- D. 解禁されれば通る ----------
        jidaiSet(3);
        set("kane_kojin", "ronty", 0);
        set("chokin", "丘陵", 0);
        pageSet(shop, "ronty", 2);
        String m2 = kau(h, self, 28);
        check("★解禁後は解禁の判定を通り、金の判定まで進む",
                m2.contains("足りません"), m2);
        System.out.println("      文面: " + m2);

        // ---------- E. 未開放表示の中身（バイトコード検査） ----------
        // 画面に並ぶアイテムはサーバーのアイテム登録簿が要るため実行できない。
        // 代わりに「灰色のガラス板」と「??? 未開放」が実装に入っていることを確かめる。
        String cls = readClass(jarPath, "jidai/Shop.class");
        check("★未開放の枠に 灰色のガラス板 を使っている",
                cls.contains("GRAY_STAINED_GLASS_PANE"), "見つからない");
        check("★未開放の枠の名前が「？？」",
                cls.contains("？？"), "見つからない");
        check("未開放の説明に解禁時代を出している",
                cls.contains("で解禁"), "見つからない");
        check("商品5種がそろっている (食料/防具/銃/戦争宣誓/下剋上)",
                cls.contains("パン") && cls.contains("鉄の胸当て") && cls.contains("銃")
                        && cls.contains("戦争宣誓") && cls.contains("下剋上"), "足りない");
        check("開くたびに並べ直す入口 hiraku がある",
                cls.contains("hiraku"), "見つからない");

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }

    static String readClass(String jarPath, String entry) throws Exception {
        try (JarFile jar = new JarFile(jarPath)) {
            var e = jar.getJarEntry(entry);
            try (var in = jar.getInputStream(e)) {
                return new String(in.readAllBytes(), "UTF-8");
            }
        }
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
