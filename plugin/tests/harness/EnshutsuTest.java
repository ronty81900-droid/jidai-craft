import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class EnshutsuTest {

    static int pass = 0, fail = 0;
    static final Map<String, Map<String, Integer>> BOARD = new HashMap<>();
    static final Map<String, PlayerStub> ONLINE = new LinkedHashMap<>();
    static final List<Runnable> YOYAKU = new ArrayList<>();   // 予約されたが未実行の処理

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
        return (T) Proxy.newProxyInstance(EnshutsuTest.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    static int get(String o, String e) { return BOARD.getOrDefault(o, Map.of()).getOrDefault(e, 0); }
    static void set(String o, String e, int v) { BOARD.computeIfAbsent(o, k -> new HashMap<>()).put(e, v); }

    static String hiraku(Component c) { return PlainTextComponentSerializer.plainText().serialize(c); }

    /** 色コード("§"+1文字)を取り除いて、画面に見える字だけにする */
    static String nuku(String s) { return s.replaceAll("§.", ""); }

    /** 頭に付いている色コードを取り出す。無ければ空文字 */
    static String atama(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i + 1 < s.length() && s.charAt(i) == '§') {
            sb.append(s, i, i + 2);
            i += 2;
        }
        return sb.toString();
    }

    static class PlayerStub implements InvocationHandler {
        final String name; Player self; boolean online = true;
        final List<String> messages = new ArrayList<>();
        final List<Object> given = new ArrayList<>();
        final List<String> oto = new ArrayList<>();
        final List<Inventory> opened = new ArrayList<>();
        int titles = 0;
        PlayerStub(String name) { this.name = name; }
        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "getName": return name;
                case "isOnline": return online;
                case "sendMessage":
                    if (a != null && a.length == 1) {
                        if (a[0] instanceof String s) messages.add(s);
                        else if (a[0] instanceof Component c) messages.add(hiraku(c));
                    }
                    return null;
                // ★ Adventure を外したので showTitle ではなく sendTitle を使う。
                //   両方数えておけば、どちらの書き方でも測れる。
                case "showTitle":
                case "sendTitle": titles++; return null;
                case "playSound":
                    if (a != null && a.length >= 2 && a[1] instanceof String s2) oto.add(s2);
                    return null;
                case "openInventory":
                    if (a != null && a.length == 1 && a[0] instanceof Inventory inv) opened.add(inv);
                    return null;
                case "getLocation": return null;
                case "getInventory":
                    return proxy(PlayerInventory.class, (p2, m2, a2) -> {
                        switch (m2.getName()) {
                            case "firstEmpty": return 0;
                            case "addItem":
                                if (a2 != null && a2.length > 0) given.add(a2[0]);
                                return new HashMap<Integer, ItemStack>();
                        }
                        return fallback(m2, p2, a2);
                    });
            }
            return fallback(m, p, a);
        }
        boolean saw(String f) { for (String s : messages) if (s.contains(f)) return true; return false; }
        void clear() { messages.clear(); given.clear(); oto.clear(); titles = 0; }
    }

    static PlayerStub join(String n) {
        PlayerStub h = new PlayerStub(n);
        h.self = proxy(Player.class, h);
        ONLINE.put(n, h);
        return h;
    }

    public static void main(String[] args) throws Exception {
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

        // ★ 予約された処理を「実行せずに貯める」時計。
        //   これで「演出が1コマも進んでいない時点」を作れる。
        BukkitScheduler sched = proxy(BukkitScheduler.class, (p, m, a) -> {
            if (m.getName().equals("runTaskLater") && a.length >= 2 && a[1] instanceof Runnable r) {
                YOYAKU.add(r);
                return null;
            }
            return fallback(m, p, a);
        });

        Server server = proxy(Server.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getScoreboardManager": return mgr;
                case "getScheduler": return sched;
                case "getLogger": return Logger.getLogger("EnshutsuTest");
                case "getWorlds": return new ArrayList<org.bukkit.World>();
                case "getOnlinePlayers": {
                    List<Player> l = new ArrayList<>();
                    for (PlayerStub h : ONLINE.values()) l.add(h.self);
                    return l;
                }
                case "createInventory":
                    return proxy(Inventory.class, (p2, m2, a2) ->
                            m2.getName().equals("getSize") ? (Object) 9
                                    : m2.getName().equals("getHolder") ? a[0]
                                    : fallback(m2, p2, a2));
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true); bs.set(null, server);

        System.out.println("=== 第2部: 演出・通知・音の実測 ===");
        System.out.println();

        Class<?> ensClass = Class.forName("jidai.Enshutsu");
        Class<?> gachaClass = Class.forName("jidai.Gacha");
        Class<?> kaneClass = Class.forName("jidai.Kane");
        Class<?> pluginClass = Class.forName("jidai.JidaiCraft");

        // ---------- A. 音IDが1箇所にまとまっているか ----------
        List<String> oto = new ArrayList<>();
        for (Field f : ensClass.getDeclaredFields()) {
            if (f.getName().startsWith("OTO_") && f.getType() == String.class) {
                f.setAccessible(true);
                oto.add((String) f.get(null));
            }
        }
        check("★音IDが Enshutsu の定数として揃っている (9種)", oto.size() == 9, "実測=" + oto.size());
        boolean katachi = true;
        for (String o : oto) {
            if (o.indexOf(46) <= 0 || !o.equals(o.toLowerCase())) katachi = false;
        }
        check("音IDがバニラの形式 (小文字とドットのみ)", katachi, String.valueOf(oto));
        System.out.println("      " + oto);

        // ソースに直書きの音IDが残っていないか
        // ★ 置き場は run_harness.py から -Djidai.root で渡る。
        //   ここに絶対パスを書くと、他の人のパソコンで動かない。
        String base = System.getProperty("jidai.root") + "/plugin/src/main/java/jidai/";
        List<String> chokusho = new ArrayList<>();
        for (String f : new String[]{"Gacha.java", "Shop.java", "JidaiCraft.java", "Basho.java"}) {
            String src = new String(Files.readAllBytes(Paths.get(base + f)), "UTF-8");
            String q = String.valueOf((char) 34);
            for (String pre : new String[]{q + "ui.", q + "entity.", q + "block.", q + "item."}) {
                if (src.contains(pre)) chokusho.add(f + " に " + pre);
            }
        }
        check("★★音IDが他のファイルに直書きされていない", chokusho.isEmpty(),
                String.valueOf(chokusho));

        // ---------- B. 通知の書式 ----------
        // ★★ 2026-08-20 に Adventure(Paper 専用)を外した ★★
        //   kazaru は Component ではなく【色コード入りの文字列】を返す。
        //   MOD と一緒に動かす土台(Arclight)が Paper API を持たないため。
        Method kazaru = ensClass.getMethod("kazaru", String.class, String.class);
        String kounyu = (String) kazaru.invoke(null, "購入", "テスト");
        String senso = (String) kazaru.invoke(null, "戦争宣誓", "テスト");
        String geko = (String) kazaru.invoke(null, "下剋上", "テスト");
        String atari = (String) kazaru.invoke(null, "大当たり", "テスト");
        check("★★戻り値が Component ではなく String (Paper 依存を外した)",
                kounyu instanceof String, kounyu.getClass().getName());
        // 色コードを取り除いた「見える字」で確かめる
        check("通知に種別ごとの記号が付く",
                nuku(kounyu).startsWith("● [購入]")
                        && nuku(senso).startsWith("✖ [戦争宣誓]")
                        && nuku(atari).startsWith("★ [大当たり]"),
                nuku(kounyu) + " / " + nuku(senso) + " / " + nuku(atari));
        check("本文がそのまま入っている",
                nuku(kounyu).endsWith("テスト"), nuku(kounyu));
        // 色は「SECTION + 1文字」。頭の色コードだけを取り出して比べる
        String iroS = atama(senso);
        String iroG = atama(geko);
        String iroK = atama(kounyu);
        check("★戦争宣誓と下剋上だけ、購入と明確に違う色",
                !iroS.isEmpty() && iroS.equals(iroG) && !iroS.equals(iroK),
                "戦争=" + iroS + " 下剋上=" + iroG + " 購入=" + iroK);
        // ★★ ここが逆戻りの見張り ★★
        //   Adventure(net.kyori)は Paper 専用。1つでも戻ると、
        //   Arclight の上でプラグインが丸ごと落ちる。全ファイルを見る。
        java.util.List<String> kyori = new ArrayList<>();
        for (java.io.File f : new java.io.File(base).listFiles()) {
            if (!f.getName().endsWith(".java")) {
                continue;
            }
            if (new String(Files.readAllBytes(f.toPath()), "UTF-8").contains("import net.kyori")) {
                kyori.add(f.getName());
            }
        }
        check("★★★Adventure(Paper 専用)を1つも使っていない", kyori.isEmpty(),
                "まだ使っている: " + kyori);
        System.out.println("      " + nuku(kounyu) + " / " + nuku(senso)
                + " / " + nuku(atari));

        // ---------- C. 演出の長さと減速 ----------
        Method nagasa = gachaClass.getMethod("enshutsuNagasa");
        int tick = (Integer) nagasa.invoke(null);
        check("★演出の長さが 3〜4秒 (60〜80 tick)", tick >= 60 && tick <= 80, "実測=" + tick + " tick");
        Field magariF = gachaClass.getDeclaredField("MAGARI");
        magariF.setAccessible(true);
        int[] magari = (int[]) magariF.get(null);
        boolean osoku = magari[magari.length - 1] >= magari[magari.length - 2]
                && magari[magari.length - 2] >= magari[magari.length - 3]
                && magari[magari.length - 3] > magari[0];
        check("★最後の3コマが一番ゆっくり", osoku,
                "末尾3つ=" + magari[magari.length - 3] + "," + magari[magari.length - 2]
                        + "," + magari[magari.length - 1]);
        System.out.println("      合計 " + tick + " tick (" + (tick / 20.0) + " 秒) / 間隔 "
                + Arrays.toString(magari));

        // ---------- D. ★順序: 景品は演出より先に渡る ----------
        //
        // 演出は最初から最後までアイテムの生成でできているため、
        // この環境（アイテム登録簿が無い）では1コマも走らせられない。
        // そこで**ソースの並び順**で、順序が崩れていないことを確かめる。
        String gsrc = new String(Files.readAllBytes(
                Paths.get(base + "Gacha.java")), "UTF-8");
        // ★★ 2026-08-18 に順序を変えた ★★
        //   前は「代金 → 渡す → 演出」だった。だが先に渡すと、
        //   まだ回っている最中に持ち物へ品が増え、
        //   **結果が出る前に当たりが分かってしまう**。
        //   今は「代金 → 演出 → 止まってから渡す」。
        //   切断された時も watasu() を通すので、渡し損ねは起きない。
        int iMawasu = gsrc.indexOf("public void mawasu(");
        int iHiku = gsrc.indexOf("int ato = mae - nedan;", iMawasu);
        int iEnshutsu = gsrc.indexOf("enshutsu(player, atari", iMawasu);
        check("★★順序: 代金を引く → 演出 の並びになっている",
                iHiku > 0 && iEnshutsu > iHiku,
                "引く=" + iHiku + " 演出=" + iEnshutsu);
        int iTeishi = gsrc.indexOf("private void teishi(");
        int iWatasuYobi = gsrc.indexOf("watasu(player, atari, ato, kane);", iTeishi);
        check("★★★渡すのは演出が止まってから（teishi の中で渡している）",
                iTeishi > 0 && iWatasuYobi > iTeishi,
                "teishi=" + iTeishi + " 渡す=" + iWatasuYobi);
        // 切断された時も同じ渡し口を通ることを、呼び出しが2か所あることで見る
        int w1 = gsrc.indexOf("watasu(player, atari, ato, kane);");
        int w2 = gsrc.indexOf("watasu(player, atari, ato, kane);", w1 + 1);
        check("★切断された時も渡す（代金だけ取られる事故を防ぐ）",
                w1 > 0 && w2 > w1, "渡し口=" + w1 + "," + w2);
        System.out.println("      (この並びなので、演出中に閉じても切断しても景品は手元にある)");

        // 画面を1人ずつ作っているか（共有していないか）
        check("★画面を1人ずつ作っている (Map で名前ごとに持っている)",
                gsrc.contains("Map<String, Inventory> gamen")
                        && gsrc.contains("gamen.put(player.getName(), inv)"),
                "共有のままかもしれない");
        check("★二重に回させない札がある", gsrc.contains("mawashichu"), "見つからない");
        check("★切断したら演出をやめる", gsrc.contains("player.isOnline()"), "見つからない");
        check("★全体に流すのは いちばん上の等級(DAI)だけ",
                gsrc.contains(".equals(DAI)") && gsrc.contains("zeninTsuchi")
                        && gsrc.contains("hanabi"), "見つからない");
        // ★★ 2026-08-22: 個人への見出しも出し分けるようになった ★★
        //   それまでは何が出ても Enshutsu.ATARI（大当たり）で知らせていた。
        //   丸石でも「大当たり」と出るので、言葉が意味を失っていた。
        check("★★個人への見出しは お金と特殊アイテムだけ「大当たり」",
                gsrc.contains("oomonoKa(atari) ? Enshutsu.ATARI : Enshutsu.GACHA"),
                "出し分けが見つからない");
        check("★特殊アイテムの見分けは Shouri を正本にしている（並べ直していない）",
                gsrc.contains("Shouri.tokushuModelData("), "Gacha 側で名前を並べ直している");

        // ---------- E. 金の出入りは実際に測れる ----------
        BOARD.put("kane_kojin", new HashMap<>());
        BOARD.put("chokin", new HashMap<>());
        BOARD.put("chuo", new HashMap<>());
        jidaiSet(1);

        Object plugin = allocate.invoke(unsafe, pluginClass);
        Field lf = JavaPlugin.class.getDeclaredField("logger");
        lf.setAccessible(true); lf.set(plugin, Logger.getLogger("EnshutsuTest"));
        Field srf = JavaPlugin.class.getDeclaredField("server");
        srf.setAccessible(true); srf.set(plugin, server);
        // 時代進行の演出が中央のビーコンの色を塗るので、場所の係も要る
        Class<?> bashoClass = Class.forName("jidai.Basho");
        Field bshf = pluginClass.getDeclaredField("basho");
        bshf.setAccessible(true);
        bshf.set(plugin, bashoClass.getDeclaredConstructor().newInstance());

        Object kane = kaneClass.getDeclaredConstructor().newInstance();
        Object gacha = gachaClass.getDeclaredConstructor(pluginClass).newInstance(plugin);
        Field ranF = gachaClass.getDeclaredField("ran");
        ranF.setAccessible(true); ranF.set(gacha, new Random(777));
        Method mMawasu = gachaClass.getMethod("mawasu", Player.class, kaneClass);

        PlayerStub a = join("ronty");
        PlayerStub b = join("okusan");

        // ★★ 起こりうる差分は【景品表から作る】★★
        //   もとは {0, 95} と手で書いていたので、金の景品を増やした時に
        //   中身は正しいのにここだけ古くなって赤くなった（2026-08-22）。
        Field keihinF = gachaClass.getDeclaredField("KEIHIN");
        keihinF.setAccessible(true);
        Object[] hyou = (Object[]) keihinF.get(null);
        Set<Integer> kaneSa = new TreeSet<>();
        for (Object k : hyou) {
            Method mj = k.getClass().getMethod("jidai");
            mj.setAccessible(true);
            Method mk = k.getClass().getMethod("kane");
            mk.setAccessible(true);
            if ((Integer) mj.invoke(k) == 1 && (Integer) mk.invoke(k) > 0) {
                kaneSa.add((Integer) mk.invoke(k) - 5);
            }
        }

        set("kane_kojin", "ronty", 100000);
        int mono = 0, kaneA = 0, hen = 0;
        for (int i2 = 0; i2 < 400; i2++) {
            int mae2 = get("kane_kojin", "ronty");
            try { mMawasu.invoke(gacha, a.self, kane); }
            catch (InvocationTargetException e) { /* アイテム生成の壁 */ }
            int sa = get("kane_kojin", "ronty") - mae2;
            if (kaneSa.contains(sa)) kaneA++;
            else if (sa == -5) mono++;
            else hen++;
        }
        check("★400回まわして、残高の動きが必ず筋の通った値になる", hen == 0, "おかしい回=" + hen);
        check("品の当たりでは 5 引かれる", mono > 0, "0回");
        check("金の当たりでは引いた分が戻る", kaneA > 0, "0回");
        System.out.println("      400回: 品=" + mono + " 金=" + kaneA
                + " 残高 100000 → " + get("kane_kojin", "ronty"));

        // ---------- F. 断る経路（演出に入らない）----------
        a.clear();
        set("kane_kojin", "ronty", 2);
        try { mMawasu.invoke(gacha, a.self, kane); } catch (InvocationTargetException e) { }
        check("金が足りなければ回らず、断りの音が鳴る",
                get("kane_kojin", "ronty") == 2 && a.oto.contains("entity.villager.no"),
                "残高=" + get("kane_kojin", "ronty") + " 音=" + a.oto);

        // ---------- G. 時代進行の検知 ----------
        jidaiSet(1);
        Object ens = ensClass.getDeclaredConstructor(pluginClass, kaneClass).newInstance(plugin, kane);
        Method byoumai = ensClass.getMethod("byoumai");
        for (PlayerStub p : ONLINE.values()) p.clear();
        byoumai.invoke(ens);
        check("★起動しただけでは時代の演出が出ない", a.titles == 0, "title=" + a.titles);

        jidaiSet(2);
        byoumai.invoke(ens);
        check("★時代が進んだら全員にタイトルが出る", a.titles == 1 && b.titles == 1,
                "a=" + a.titles + " b=" + b.titles);
        check("★重い音が全員に鳴る", a.oto.contains("entity.ender_dragon.growl"),
                String.valueOf(a.oto));
        // ★★ 2026-08-20 に、中央の通知から商品の名指しを外した ★★
        //   解禁は【勢力の時代】で決まるようになったので、中央が進んだ時に
        //   「入荷しました」と流すと、まだ達していない勢力に誤解させる。
        //   入荷の知らせはデータパック(jidai:shinko/shounin_*)が
        //   その勢力の人にだけ出している。
        check("★★中央の通知は商品を名指ししない（勢力ごとに違うため）",
                !a.saw("入荷しました"), String.valueOf(a.messages));
        check("中央が移ったことは全員に伝える", a.saw("中央が"), String.valueOf(a.messages));

        int mae = a.titles;
        byoumai.invoke(ens);
        check("★同じ時代で二重に出ない", a.titles == mae, "title=" + a.titles);

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
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
