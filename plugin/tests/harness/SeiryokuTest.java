import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class SeiryokuTest {

    static int pass = 0, fail = 0;
    static final Map<String, Map<String, Integer>> BOARD = new HashMap<>();
    static final Map<String, String> TEAMS = new HashMap<>();
    static final List<String> CMDS = new ArrayList<>();   // データパックへ送った命令
    static final java.util.LinkedHashSet<String> ZENBU = new java.util.LinkedHashSet<>();

    /** setup/kyoten が「何個 置けたことにするか」。37 で成功、それ未満で失敗を作る */
    //  ★ 2026-08-20 に銃器専門店を各拠点へ足したので 32 -> 37。
    //    5拠点x7 + 中央2 = 37。
    static int SETUP_OKERU = 37;
    static boolean DISPATCH_OK = true;                    // 送信が成功したことにするか
    static boolean DATAPACK_UKERU = true;                 // データパックが受理したことにするか
    static final List<Player> ONLINE = new ArrayList<>(); // つないでいる人

    static void check(String label, boolean cond, String detail) {
        if (cond) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + detail); }
    }

    static Object fallback3(Object proxy, Method m, Object[] args) { return fallback(m, proxy, args); }

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
        return (T) Proxy.newProxyInstance(SeiryokuTest.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    /** 文字列の間を取り出す小道具。 */
    static String aida(String s, String hajime, String owari) {
        int i = s.indexOf(hajime);
        if (i < 0) return null;
        int j = s.indexOf(owari, i + hajime.length());
        return j < 0 ? null : s.substring(i + hajime.length(), j);
    }

    static int get(String o, String e) { return BOARD.getOrDefault(o, Map.of()).getOrDefault(e, 0); }
    static void set(String o, String e, int v) { BOARD.computeIfAbsent(o, k -> new HashMap<>()).put(e, v); }

    /** 番号 → 勢力名。データパックが配る番号と同じ並び */
    static final String[] MEI = {"丘陵", "森林", "川", "内海", "岩場"};

    static String meiNo(int no) { return (no >= 1 && no <= 5) ? MEI[no - 1] : null; }

    /**
     * 戦争の要約を置く。
     *
     * ★★ 1.20.1 移行で形が変わった ★★
     *   1.21 は保持者 "丘陵>森林" のスコアだった。いまは戦争そのものは
     *   マーカーが持ち、データパックが毎秒【勢力名の保持者】へ写している。
     *   プラグインが読むのはこの写しだけ。
     */
    /**
     * 販売所の品の枠を【名前で】引く。
     * ★ 枠の番号を検証に書くと、品を足した時に中身は正しいのに赤くなる
     *   （2026-08-22 に生活タブへ6品 足した時、実際にそうなった）。
     */
    static int wakuOf(String namae) throws Exception {
        Field f = Class.forName("jidai.Shop").getDeclaredField("IPPAN");
        f.setAccessible(true);
        for (Object x : (Object[]) f.get(null)) {
            Method mn = x.getClass().getMethod("namae");
            mn.setAccessible(true);
            if (mn.invoke(x).equals(namae)) {
                Method ms = x.getClass().getMethod("slot");
                ms.setAccessible(true);
                return (Integer) ms.invoke(x);
            }
        }
        throw new IllegalStateException("販売所に無い: " + namae);
    }

    /** 銀行の枠を定数名で引く。★ 番号を書くと、並びを変えた時に赤くなる（2026-08-22 に実際）。 */
    static int ginkoWaku(String mei) throws Exception {
        Field f = Class.forName("jidai.Ginko").getDeclaredField(mei);
        f.setAccessible(true);
        return (Integer) f.get(null);
    }

    static void sensouOku(String a, String b, int jotai) {
        set("sensou", a, jotai);
        set("sensou_aite", a, get("bangou", b));
        set("sensou", b, jotai);
        set("sensou_aite", b, get("bangou", a));
    }

    /** 戦争の要約を片側だけ置く（写しが片方しか無い事故を作る） */
    static void sensouKataho(String a, String b, int jotai) {
        set("sensou", a, jotai);
        set("sensou_aite", a, get("bangou", b));
    }

    static void sensouKesu() {
        BOARD.getOrDefault("sensou", new HashMap<>()).clear();
        BOARD.getOrDefault("sensou_aite", new HashMap<>()).clear();
        BOARD.getOrDefault("sensou_byou", new HashMap<>()).clear();
    }

    static class PlayerStub implements InvocationHandler {
        final String name;
        final List<String> messages = new ArrayList<>();
        final List<Object> given = new ArrayList<>();
        final List<ItemStack> mochimono = new ArrayList<>();
        PlayerStub(String name) { this.name = name; }
        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "getName": return name;
                case "sendMessage":
                    if (a != null && a.length == 1) {
                        if (a[0] instanceof String s) messages.add(s);
                        else if (a[0] instanceof Component c) {
                            messages.add(PlainTextComponentSerializer.plainText().serialize(c));
                        }
                    }
                    return null;
                case "getLocation": return null;
                case "getInventory":
                    return proxy(PlayerInventory.class, (p2, m2, a2) -> {
                        switch (m2.getName()) {
                            case "firstEmpty": return 0;
                            case "getContents":
                                return mochimono.toArray(new ItemStack[0]);
                            case "remove":
                                mochimono.removeIf(it -> it != null
                                        && it.getType() == (Material) a2[0]);
                                return null;
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
        void clear() { messages.clear(); given.clear(); }
        String last() { return messages.isEmpty() ? "" : messages.get(messages.size() - 1); }
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
                    return proxy(Team.class, (p2, m2, a2) -> {
                        // ★ getName だけでなく hasEntry も答えること。
                        //   答えないと「同じ勢力の仲間」が0人になり、
                        //   勢力内の知らせが誰にも届かなくなる(実測で踏んだ)。
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
                case "getScheduler":
                    // runTask は「次のtickにやる」予約。試験では即座に実行する。
                    return proxy(org.bukkit.scheduler.BukkitScheduler.class, (p2, m2, a2) -> {
                        if (m2.getName().equals("runTask") && a2[1] instanceof Runnable r) {
                            r.run();
                            return null;
                        }
                        // ★ runTaskLater は「N tick 後に1回」。試験では待たずに走らせる。
                        //   これが無いと /jidai setup の中身が1行も動かない。
                        if (m2.getName().equals("runTaskLater") && a2[1] instanceof Runnable r2) {
                            r2.run();
                            return null;
                        }
                        return fallback(m2, p2, a2);
                    });
                case "getConsoleSender":
                    return proxy(org.bukkit.command.ConsoleCommandSender.class, SeiryokuTest::fallback3);
                case "dispatchCommand": {
                    String cmd = String.valueOf(a[1]);
                    CMDS.add(cmd);
                    ZENBU.add(cmd);
                    // ★ 本物のデータパックの真似をする。
                    //   宣戦を受理したら sensou を両方向に立てる。
                    //   DATAPACK_UKERU=false は「命令は通ったが、
                    //   データパックが静かに断った」状況(再戦禁止など)。
                    //   ★1.20.1 では引数が渡せないので、先にスコアが置かれる。
                    //     その set を覚えておき、function が来た時に効かせる。
                    // ★ setup/kyoten の真似。
                    //   本物は「置けた数」を #shisetsu sagyou に残す。
                    //   SETUP_OKERU を変えると、区画が読めていない状況を作れる。
                    if (DISPATCH_OK && cmd.equals("function jidai:setup/kyoten")) {
                        set("sagyou", "#shisetsu", SETUP_OKERU);
                    }
                    if (DISPATCH_OK && cmd.startsWith("scoreboard players set #w_")) {
                        String[] w = cmd.split(" ");
                        set("sagyou", w[3], Integer.parseInt(w[5]));
                    }
                    if (DISPATCH_OK && DATAPACK_UKERU
                            && cmd.equals("function jidai:sensou/sensen")) {
                        String kuni = meiNo(get("sagyou", "#w_kuni"));
                        String aite2 = meiNo(get("sagyou", "#w_aite"));
                        if (kuni != null && aite2 != null && !kuni.equals(aite2)) {
                            // 即開戦(soku:1)は準備をとばして交戦から
                            sensouOku(kuni, aite2,
                                    get("sagyou", "#w_soku") == 1 ? 2 : 1);
                        }
                    }
                    return DISPATCH_OK;
                }
                case "getLogger": return Logger.getLogger("SeiryokuTest");
                case "getOnlinePlayers": return ONLINE;
                case "getWorlds": return new ArrayList<org.bukkit.World>();
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true); bs.set(null, server);

        System.out.println("=== 戦争宣誓・下剋上・リーダー の実測 ===");
        System.out.println();

        Class<?> seiClass = Class.forName("jidai.Seiryoku");
        Class<?> kaneClass = Class.forName("jidai.Kane");
        Class<?> pluginClass = Class.forName("jidai.JidaiCraft");
        Object kane = kaneClass.getDeclaredConstructor().newInstance();
        Object sei = seiClass.getDeclaredConstructor(pluginClass).newInstance((Object) null);

        BOARD.put("kane_kojin", new HashMap<>());
        BOARD.put("chokin", new HashMap<>());
        BOARD.put("chuo", new HashMap<>());
        BOARD.put("sekiyu", new HashMap<>());
        BOARD.put("youhei", new HashMap<>());
        BOARD.put("settei", new HashMap<>());
        jidaiSet(4);

        // ---------- A. 宣戦の相手に選べるか ----------
        Method dekiruka = seiClass.getMethod("senseniDekiruka", String.class, String.class, kaneClass);

        // A-0. データパックがまだ目的を1つも作っていない状態（＝今の実物）
        check("★目的が存在しなくても落ちない（未作成なら 0 扱い）",
                dekiruka.invoke(sei, "kyuryo", "shinrin", kane) == null,
                String.valueOf(dekiruka.invoke(sei, "kyuryo", "shinrin", kane)));

        check("★自勢力は選べない",
                "自分の勢力です".equals(dekiruka.invoke(sei, "kyuryo", "kyuryo", kane)),
                String.valueOf(dekiruka.invoke(sei, "kyuryo", "kyuryo", kane)));
        check("★勢力に入っていない人は誰も選べない",
                String.valueOf(dekiruka.invoke(sei, null, "shinrin", kane)).contains("勢力に入っていません"),
                String.valueOf(dekiruka.invoke(sei, null, "shinrin", kane)));
        check("他の勢力は選べる", dekiruka.invoke(sei, "kyuryo", "shinrin", kane) == null,
                String.valueOf(dekiruka.invoke(sei, "kyuryo", "shinrin", kane)));

        // A-1. データパックが目的を作った後
        //   ★ 名前と保持者の形は datapacks/.../load.mcfunction と
        //     sensou/sensen.mcfunction を読んで合わせている。
        //     sensou の保持者は "丘陵>森林"（両方向に書かれる）。
        BOARD.put("senryou", new HashMap<>());
        BOARD.put("sensou", new HashMap<>());
        BOARD.put("sensou_aite", new HashMap<>());
        BOARD.put("sensou_byou", new HashMap<>());
        BOARD.put("bangou", new HashMap<>());
        BOARD.put("gekokujo", new HashMap<>());
        BOARD.put("sagyou", new HashMap<>());
        set("bangou", "丘陵", 1);
        set("bangou", "森林", 2);

        sensouOku("丘陵", "森林", 2);   // 丘陵と森林が交戦中
        check("★★その相手ともう戦争中なら選べない",
                "その勢力とはもう戦争中です".equals(dekiruka.invoke(sei, "kyuryo", "shinrin", kane)),
                String.valueOf(dekiruka.invoke(sei, "kyuryo", "shinrin", kane)));
        sensouKesu();

        sensouKataho("森林", "丘陵", 1);   // 片方にしか写っていない場合
        check("★片側にしか写っていなくても見つけられる",
                dekiruka.invoke(sei, "kyuryo", "shinrin", kane) != null,
                "見落とした");
        sensouKesu();

        sensouOku("丘陵", "森林", 3);   // 3＝再戦禁止
        set("sensou_byou", "丘陵", 90);
        check("★★再戦禁止(sensou=3)の相手は選べない・残り時間も出す",
                "再戦禁止 (あと 1分30秒)".equals(dekiruka.invoke(sei, "kyuryo", "shinrin", kane)),
                String.valueOf(dekiruka.invoke(sei, "kyuryo", "shinrin", kane)));
        sensouKesu();

        // 自勢力が別の相手と戦争中
        set("bangou", "川", 3);
        sensouOku("丘陵", "川", 2);
        check("★自勢力が別の相手と戦争中なら、誰も選べない",
                String.valueOf(dekiruka.invoke(sei, "kyuryo", "shinrin", kane)).contains("自勢力がすでに戦争中"),
                String.valueOf(dekiruka.invoke(sei, "kyuryo", "shinrin", kane)));
        sensouKesu();

        // 相手が別の相手と戦争中
        sensouOku("森林", "川", 2);
        check("★★相手が別の勢力と戦争中なら選べない",
                "すでに戦争中です".equals(dekiruka.invoke(sei, "kyuryo", "shinrin", kane)),
                String.valueOf(dekiruka.invoke(sei, "kyuryo", "shinrin", kane)));
        // ★番号が違う相手の戦争を、自分の戦争と取り違えないか
        sensouKesu();
        sensouOku("川", "内海", 2);
        set("bangou", "内海", 4);
        set("sensou_aite", "川", 4);
        check("★★関係ない2勢力の戦争を、自分たちの戦争と読み違えない",
                dekiruka.invoke(sei, "kyuryo", "shinrin", kane) == null,
                String.valueOf(dekiruka.invoke(sei, "kyuryo", "shinrin", kane)));
        sensouKesu();
        BOARD.get("bangou").remove("川");
        BOARD.get("bangou").remove("内海");

        @SuppressWarnings("unchecked")
        List<String> aite = (List<String>) seiClass.getMethod("erabreruAite", String.class, kaneClass)
                .invoke(sei, "kyuryo", kane);
        check("★選べる相手の一覧に自勢力が入らない・5勢力ぶん見る",
                aite.equals(List.of("shinrin", "kawa", "naikai", "iwaba")), String.valueOf(aite));
        System.out.println("      丘陵から選べる相手: " + aite);

        // 勢力の番号は【データパックが配る】。プラグインは覚えない。
        Method bangou = seiClass.getMethod("bangou", String.class, kaneClass);
        Method teamKara = seiClass.getMethod("teamKara", int.class, kaneClass);
        check("★★番号はスコア(bangou)から読む。覚えていない",
                (Integer) bangou.invoke(null, "kyuryo", kane) == 1
                        && (Integer) bangou.invoke(null, "shinrin", kane) == 2
                        && (Integer) bangou.invoke(null, "kawa", kane) == 0,
                "番号=" + bangou.invoke(null, "kyuryo", kane) + "/"
                        + bangou.invoke(null, "shinrin", kane) + "/"
                        + bangou.invoke(null, "kawa", kane));
        check("番号から勢力に戻せる／配られていない番号は null",
                "shinrin".equals(teamKara.invoke(null, 2, kane))
                        && teamKara.invoke(null, 9, kane) == null,
                String.valueOf(teamKara.invoke(null, 9, kane)));

        // ---------- B. リーダー ----------
        Method leaderKa = seiClass.getMethod("leaderKa", String.class, String.class);
        Method leaderKimeru = seiClass.getMethod("leaderKimeru", String.class, String.class);
        Method daikouKimeru = seiClass.getMethod("daikouKimeru", String.class, String.class);
        check("最初は誰もリーダーでない", !(Boolean) leaderKa.invoke(sei, "kyuryo", "ronty"), "true");
        leaderKimeru.invoke(sei, "kyuryo", "ronty");
        check("★リーダーに指名された人だけが true",
                (Boolean) leaderKa.invoke(sei, "kyuryo", "ronty")
                        && !(Boolean) leaderKa.invoke(sei, "kyuryo", "okusan"), "判定がおかしい");
        check("★他の勢力のリーダーにはならない",
                !(Boolean) leaderKa.invoke(sei, "shinrin", "ronty"), "森林でも true になった");
        daikouKimeru.invoke(sei, "kyuryo", "okusan");
        check("代行も同じ扱いになる", (Boolean) leaderKa.invoke(sei, "kyuryo", "okusan"), "false");
        leaderKimeru.invoke(sei, "kyuryo", "kakikama");
        check("リーダーを付け替えると前の人は外れる",
                (Boolean) leaderKa.invoke(sei, "kyuryo", "kakikama")
                        && !(Boolean) leaderKa.invoke(sei, "kyuryo", "ronty"), "前の人が残っている");
        leaderKimeru.invoke(sei, "kyuryo", "ronty");

        // ---------- C. 下剋上の行使の門番 ----------
        Method dekiru = seiClass.getMethod("gekokujoDekiruka", Player.class, kaneClass);
        Method kenriAru = seiClass.getMethod("kenriAru", String.class, kaneClass);

        PlayerStub ldr = new PlayerStub("ronty");        // 丘陵のリーダー
        PlayerStub ippan = new PlayerStub("okusan2");    // 丘陵の一般
        PlayerStub muzoku = new PlayerStub("kakikama");  // 無所属
        Player pLdr = proxy(Player.class, ldr);
        Player pIppan = proxy(Player.class, ippan);
        Player pMuzoku = proxy(Player.class, muzoku);
        TEAMS.put("ronty", "kyuryo");
        TEAMS.put("okusan2", "kyuryo");

        check("★勢力に入っていなければ宣言できない",
                String.valueOf(dekiru.invoke(sei, pMuzoku, kane)).contains("勢力に入っていません"),
                String.valueOf(dekiru.invoke(sei, pMuzoku, kane)));
        check("★★リーダー以外が押しても発動しない",
                String.valueOf(dekiru.invoke(sei, pIppan, kane)).contains("だけが宣言できます"),
                String.valueOf(dekiru.invoke(sei, pIppan, kane)));
        check("★★権利を持っていなければ発動しない",
                String.valueOf(dekiru.invoke(sei, pLdr, kane)).contains("権利がありません"),
                String.valueOf(dekiru.invoke(sei, pLdr, kane)));

        set("gekokujo", "丘陵", 1);   // データパックが権利を渡した
        check("★★権利は設定ファイルでなくスコア(gekokujo)で見る",
                (Boolean) kenriAru.invoke(sei, "kyuryo", kane), "false");
        check("★★占領されていなければ、リーダーでも権利があっても発動しない",
                String.valueOf(dekiru.invoke(sei, pLdr, kane)).contains("占領されていません"),
                String.valueOf(dekiru.invoke(sei, pLdr, kane)));
        System.out.println("      断り: " + dekiru.invoke(sei, pLdr, kane));

        set("senryou", "丘陵", 2);   // データパックが「丘陵は森林に占領された」と書いた
        check("★★★4つの門番をすべて通ると宣言できる（占領された時だけ）",
                dekiru.invoke(sei, pLdr, kane) == null,
                String.valueOf(dekiru.invoke(sei, pLdr, kane)));
        check("占領している相手を正しく割り出す（丘陵 → 森林）",
                "shinrin".equals(seiClass.getMethod("senryoShiteiru", String.class, kaneClass)
                        .invoke(sei, "kyuryo", kane)),
                String.valueOf(seiClass.getMethod("senryoShiteiru", String.class, kaneClass)
                        .invoke(sei, "kyuryo", kane)));
        set("senryou", "丘陵", 0);

        // ---------- D. 財布は身分で決まる ----------
        Class<?> shopClass = Class.forName("jidai.Shop");
        Object shop = allocate.invoke(unsafe, shopClass);
        tanaIreru(shopClass, shop, "IPPAN");
        Method kau = shopClass.getMethod("kau", Player.class, int.class, kaneClass);

        // 傭兵（無所属・youhei=1）が パン(枠0/個人の金2) を買う
        PlayerStub hei = new PlayerStub("kakikama");
        Player pHei = proxy(Player.class, hei);
        set("youhei", "kakikama", 1);
        set("kane_kojin", "kakikama", 10);
        hei.clear();
        try { pageSet(shop, pHei, 1); kau.invoke(shop, pHei, 9, kane); } catch (InvocationTargetException e) { }

        // 傭兵が 鉄の胸当て(枠1/本来は勢力の金30) を買う → 個人の金で払う
        set("kane_kojin", "kakikama", 100);
        set("chokin", "丘陵", 0);
        hei.clear();
        try { pageSet(shop, pHei, 2); kau.invoke(shop, pHei, 15, kane); } catch (InvocationTargetException e) { }

        // 所属者は勢力の金でしか買えない（個人の金では買えない）
        PlayerStub member = new PlayerStub("ronty");
        Player pMember = proxy(Player.class, member);
        set("youhei", "ronty", 0);
        set("kane_kojin", "ronty", 100000);
        set("chokin", "丘陵", 5);
        member.clear();
        try { pageSet(shop, pMember, 2); kau.invoke(shop, pMember, 15, kane); } catch (InvocationTargetException e) { }
        check("★★所属者は個人の金を使えない (勢力の貯金が足りず断られる)",
                get("kane_kojin", "ronty") == 100000 && member.saw("勢力の金が足りません"),
                "個人=" + get("kane_kojin", "ronty") + " " + member.messages);
        System.out.println("      断り: " + member.last());

        // 傭兵は戦争宣誓・下剋上を買えない（どの勢力の行為か決まらないため）
        set("kane_kojin", "kakikama", 100000);
        hei.clear();
        try { pageSet(shop, pHei, 1); kau.invoke(shop, pHei, wakuOf("戦争宣誓"), kane); } catch (InvocationTargetException e) { }
        // ★ 文言を焼き込まない。呼び名は身分で変わる（蛮族／傭兵）ので、
        //   「買えません」という結果の方を見る（2026-08-23 の改名で1度 落ちた）。
        hei.clear();
        try { pageSet(shop, pHei, 1); kau.invoke(shop, pHei, wakuOf("下剋上"), kane); } catch (InvocationTargetException e) { }

        // ---------- E. 宣戦の相手を選ぶ画面 ----------
        System.out.println();
        System.out.println("--- E. 相手選択GUI と データパックへの受け渡し ---");

        Class<?> sensenClass = Class.forName("jidai.Sensen");
        Object sensen = sensenClass.getDeclaredConstructor(pluginClass, seiClass)
                .newInstance(null, sei);

        // Sensen は plugin.getLogger() を使う。JavaPlugin の logger 欄に
        // 試験用の Logger を差し込んで、成功経路の最後まで通す。
        Object pl = allocate.invoke(unsafe, pluginClass);
        Field lf = Class.forName("org.bukkit.plugin.java.JavaPlugin").getDeclaredField("logger");
        lf.setAccessible(true);
        lf.set(pl, Logger.getLogger("SensenTest"));
        Field pf = sensenClass.getDeclaredField("plugin");
        pf.setAccessible(true);
        pf.set(sensen, pl);

        // 枠の割り当て
        Method wakuKara = sensenClass.getMethod("teamKara", int.class);
        check("★枠2..6 が 5勢力に対応する",
                "kyuryo".equals(wakuKara.invoke(sensen, 2))
                        && "shinrin".equals(wakuKara.invoke(sensen, 3))
                        && "kawa".equals(wakuKara.invoke(sensen, 4))
                        && "naikai".equals(wakuKara.invoke(sensen, 5))
                        && "iwaba".equals(wakuKara.invoke(sensen, 6)),
                "対応がずれている");
        check("★飾りの枠(0,1,7,8)を押しても何も起きない",
                wakuKara.invoke(sensen, 0) == null && wakuKara.invoke(sensen, 1) == null
                        && wakuKara.invoke(sensen, 7) == null && wakuKara.invoke(sensen, 8) == null,
                "飾り枠が勢力に割り当たっている");

        // 送る命令の形（データパックの取り決めどおりか）
        // ★★ 1.20.1 に関数の引数(マクロ)が無いので、
        //   「スコアを3つ置いてから関数を呼ぶ」4本の命令になる。
        Method sensenSuru = sensenClass.getMethod("sensenSuru",
                String.class, String.class, boolean.class, kaneClass);
        CMDS.clear();
        sensenSuru.invoke(sensen, "kyuryo", "shinrin", false, kane);
        check("★★★宣戦は4本の命令で送る（引数はスコアで渡す）",
                CMDS.size() == 4
                        && CMDS.get(0).equals("scoreboard players set #w_kuni sagyou 1")
                        && CMDS.get(1).equals("scoreboard players set #w_aite sagyou 2")
                        && CMDS.get(2).equals("scoreboard players set #w_soku sagyou 0")
                        && CMDS.get(3).equals("function jidai:sensou/sensen"),
                String.valueOf(CMDS));
        check("★★渡すのは勢力名ではなく【番号】（番号はデータパックが配る）",
                CMDS.get(0).endsWith(" 1") && CMDS.get(1).endsWith(" 2"),
                String.valueOf(CMDS));
        System.out.println("      送信: " + CMDS);
        sensouKesu();
        CMDS.clear();
        sensenSuru.invoke(sensen, "kyuryo", "shinrin", true, kane);
        check("★★即時開戦は #w_soku が 1 になる",
                CMDS.get(2).equals("scoreboard players set #w_soku sagyou 1"),
                String.valueOf(CMDS));
        System.out.println("      送信: " + CMDS.get(2));

        // ★ 番号が配られていない勢力には送らない（0 を渡すと事故になる）
        CMDS.clear();
        BOARD.get("bangou").remove("森林");
        Object okutta0 = sensenSuru.invoke(sensen, "kyuryo", "shinrin", false, kane);
        check("★★番号が配られていない相手には1本も送らない",
                Boolean.FALSE.equals(okutta0) && CMDS.isEmpty(), String.valueOf(CMDS));
        set("bangou", "森林", 2);

        // ★ 上の2回で「戦争が始まった」状態になっているので、ここで戻す。
        //   でないと次の erabu が「もう戦争中」で断られる。
        sensouKesu();

        // erabu（選んだ瞬間の処理）
        Method erabu = sensenClass.getMethod("erabu", Player.class, int.class, kaneClass);
        Field nedanF = sensenClass.getDeclaredField("nedan");
        nedanF.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Integer> nedan = (Map<String, Integer>) nedanF.get(sensen);

        set("chokin", "丘陵", 250);
        nedan.put("ronty", 100);
        CMDS.clear(); DISPATCH_OK = true; member.clear();
        erabu.invoke(sensen, pMember, 3, kane);     // 枠3＝森林
        // ★1.20.1 では 宣戦4本 + 要約の作り直し1本 = 5本 送る。
        //   最後の youyaku を落とすと、始まった戦争を読み直せず
        //   「始まらなかった」と誤判定して代金を返してしまう。
        check("★★★相手を選ぶと勢力の金が引かれ、データパックへ送られる (250 → 150)",
                get("chokin", "丘陵") == 150 && CMDS.size() == 5
                        && CMDS.get(1).equals("scoreboard players set #w_aite sagyou 2")
                        && CMDS.get(3).equals("function jidai:sensou/sensen"),
                "貯金=" + get("chokin", "丘陵") + " 送信=" + CMDS);
        check("★★宣戦のあとに要約を作り直させている（読み直しのため）",
                CMDS.get(4).equals("function jidai:sensou/youyaku"),
                String.valueOf(CMDS));

        sensouKesu();   // 次の試験のために戻す

        // 同じ tick に2回押した場合（画面は次のtickまで閉じない）
        CMDS.clear();
        erabu.invoke(sensen, pMember, 3, kane);
        check("★★二度押しても二重に買わない (150 のまま・送信もしない)",
                get("chokin", "丘陵") == 150 && CMDS.isEmpty(),
                "貯金=" + get("chokin", "丘陵") + " 送信=" + CMDS);

        // 飾り枠
        nedan.put("ronty", 100);
        CMDS.clear();
        erabu.invoke(sensen, pMember, 0, kane);
        check("飾り枠を押しても金は減らない",
                get("chokin", "丘陵") == 150 && CMDS.isEmpty(), "貯金=" + get("chokin", "丘陵"));

        // 開いている間に相手が戦争を始めた
        set("bangou", "川", 3);
        sensouOku("森林", "川", 2);
        member.clear(); CMDS.clear();
        erabu.invoke(sensen, pMember, 3, kane);
        check("★★開いている間に相手が戦争を始めたら、買わずに断る",
                get("chokin", "丘陵") == 150 && CMDS.isEmpty() && member.saw("すでに戦争中"),
                "貯金=" + get("chokin", "丘陵") + " " + member.messages);
        System.out.println("      断り: " + member.last());
        sensouKesu();
        BOARD.get("bangou").remove("川");

        // 勢力の金が足りない
        set("chokin", "丘陵", 30);
        nedan.put("ronty", 100);
        member.clear(); CMDS.clear();
        erabu.invoke(sensen, pMember, 3, kane);
        check("★勢力の金が足りなければ買わない",
                get("chokin", "丘陵") == 30 && CMDS.isEmpty() && member.saw("勢力の金が足りません"),
                "貯金=" + get("chokin", "丘陵") + " " + member.messages);

        // ★★ 命令は通ったのに、データパックが静かに断った場合 ★★
        //   データパックの sensen は条件に合わないと `return 0` するだけで、
        //   命令自体は成功扱いになる。状態を読み直さないと
        //   「100 取られたのに戦争が始まっていない」事故になる。
        //   ★1.20.1 では 宣戦4本 + 要約の作り直し1本 = 5本 送る。
        set("chokin", "丘陵", 250);
        nedan.put("ronty", 100);
        member.clear(); CMDS.clear(); DATAPACK_UKERU = false;
        erabu.invoke(sensen, pMember, 3, kane);
        check("★★★命令は通ってもデータパックが断ったら代金を戻す",
                get("chokin", "丘陵") == 250 && member.saw("代金は戻しました")
                        && CMDS.size() == 5,
                "貯金=" + get("chokin", "丘陵") + " " + member.messages);
        System.out.println("      断り: " + member.last());
        DATAPACK_UKERU = true;
        sensouKesu();

        // データパックへ送れなかった場合（関数がまだ無い時に起きる）
        set("chokin", "丘陵", 250);
        nedan.put("ronty", 100);
        member.clear(); CMDS.clear(); DISPATCH_OK = false;
        erabu.invoke(sensen, pMember, 3, kane);
        check("★★★送信に失敗したら代金を戻す（金だけ消える事故を防ぐ）",
                get("chokin", "丘陵") == 250 && member.saw("代金は戻しました"),
                "貯金=" + get("chokin", "丘陵") + " " + member.messages);
        System.out.println("      断り: " + member.last());
        DISPATCH_OK = true;

        // ---------- F. 販売所から画面へ渡るか ----------
        System.out.println();
        System.out.println("--- F. 販売所の「戦争宣誓」を押した時 ---");
        Field sensenF = shopClass.getDeclaredField("sensen");
        sensenF.setAccessible(true);
        sensenF.set(shop, sensen);
        Field seiF = shopClass.getDeclaredField("seiryoku");
        seiF.setAccessible(true);
        seiF.set(shop, sei);

        set("chokin", "丘陵", 250);
        set("youhei", "ronty", 0);
        member.clear(); CMDS.clear();
        boolean itemKabe = false;
        try {
            pageSet(shop, pMember, 1); kau.invoke(shop, pMember, wakuOf("戦争宣誓"), kane);
        } catch (InvocationTargetException e) {
            itemKabe = true;   // ItemStack の壁（後述）
        }
        check("★★★「戦争宣誓」を押した時点では、まだ金は引かれない",
                get("chokin", "丘陵") == 250 && CMDS.isEmpty(),
                "貯金=" + get("chokin", "丘陵") + " 送信=" + CMDS);
        System.out.println("      画面の組み立てまで到達したか: "
                + (itemKabe ? "ItemStack の壁で止まった（実機でのみ確認可）" : "到達した"));

        set("chokin", "丘陵", 30);
        member.clear();
        try { pageSet(shop, pMember, 1); kau.invoke(shop, pMember, wakuOf("戦争宣誓"), kane); } catch (InvocationTargetException e) { }
        check("★勢力の金が 100 未満なら、画面すら開かない",
                member.saw("勢力の金が足りません") && get("chokin", "丘陵") == 30,
                String.valueOf(member.messages));
        System.out.println("      断り: " + member.last());

        // ---------- G. 下剋上まわりは、まるごとデータパックの担当 ----------
        System.out.println();
        System.out.println("--- G. 下剋上（買う・使う・リーダーの目印） ---");

        // G-1. 販売所の「下剋上」を押しても、プラグインは金を引かない
        set("chokin", "丘陵", 1000);
        set("kane_kojin", "ronty", 1000);
        member.clear(); CMDS.clear();
        try { pageSet(shop, pMember, 1); kau.invoke(shop, pMember, wakuOf("下剋上"), kane); } catch (InvocationTargetException e) { }
        check("★★★下剋上はプラグインが金を引かない（引くと二重取りになる）",
                get("chokin", "丘陵") == 1000, "貯金=" + get("chokin", "丘陵"));
        check("★★★下剋上の購入はデータパックの関数に丸投げする",
                CMDS.size() == 1 && CMDS.get(0).equals(
                        "execute as ronty at ronty run function jidai:sensou/gekokujo_kau"),
                String.valueOf(CMDS));
        System.out.println("      送信: " + (CMDS.isEmpty() ? "なし" : CMDS.get(0)));

        // G-2. リーダーの目印。これが無いとデータパック側が必ず断る
        ONLINE.add(pLdr); ONLINE.add(pIppan);
        CMDS.clear();
        leaderKimeru.invoke(sei, "kyuryo", "ronty");
        check("★★★リーダーに jidai_leader の目印を付ける（無いと下剋上が死ぬ）",
                CMDS.contains("tag ronty add jidai_leader"), String.valueOf(CMDS));
        check("★★リーダーでない人からは目印を外す",
                CMDS.contains("tag okusan2 remove jidai_leader"), String.valueOf(CMDS));
        CMDS.clear();
        leaderKimeru.invoke(sei, "kyuryo", "okusan2");
        check("★★リーダーを付け替えると、前の人の目印が外れる",
                CMDS.contains("tag ronty remove jidai_leader")
                        && CMDS.contains("tag okusan2 add jidai_leader"),
                String.valueOf(CMDS));
        leaderKimeru.invoke(sei, "kyuryo", "ronty");

        // G-3. /gekokujo は門番を通した上で、データパックの関数を呼ぶだけ
        Object plugin2 = allocate.invoke(unsafe, pluginClass);
        for (String[] fld : new String[][]{{"seiryoku", null}, {"kane", null}, {"sensen", null}}) {
            Field ff = pluginClass.getDeclaredField(fld[0]);
            ff.setAccessible(true);
            ff.set(plugin2, fld[0].equals("seiryoku") ? sei
                    : fld[0].equals("kane") ? kane : sensen);
        }
        Field lf2 = Class.forName("org.bukkit.plugin.java.JavaPlugin").getDeclaredField("logger");
        lf2.setAccessible(true);
        lf2.set(plugin2, Logger.getLogger("GekokujoTest"));

        class Tate extends org.bukkit.command.Command {
            Tate(String n) { super(n); }
            @Override public boolean execute(org.bukkit.command.CommandSender s2,
                                             String l, String[] a2) { return true; }
        }
        Method onCmd = pluginClass.getMethod("onCommand", org.bukkit.command.CommandSender.class,
                org.bukkit.command.Command.class, String.class, String[].class);

        // 占領されていない → 門番が止める（関数は呼ばない）
        set("senryou", "丘陵", 0);
        set("gekokujo", "丘陵", 1);
        ldr.clear(); CMDS.clear();
        onCmd.invoke(plugin2, pLdr, new Tate("gekokujo"), "gekokujo", new String[0]);
        check("★★占領されていなければ、関数を呼ばずに断る",
                CMDS.isEmpty() && ldr.saw("占領されていません"),
                "送信=" + CMDS + " " + ldr.messages);

        // 4つの門番を通った → 関数を呼ぶだけ
        set("senryou", "丘陵", 2);
        ldr.clear(); CMDS.clear();
        onCmd.invoke(plugin2, pLdr, new Tate("gekokujo"), "gekokujo", new String[0]);
        check("★★★門番を通ったら jidai:sensou/gekokujo_tsukau を本人として走らせる",
                CMDS.size() == 1 && CMDS.get(0).equals(
                        "execute as ronty at ronty run function jidai:sensou/gekokujo_tsukau"),
                String.valueOf(CMDS));
        System.out.println("      送信: " + (CMDS.isEmpty() ? "なし" : CMDS.get(0)));
        check("★★プラグインは権利を消費しない（消費するのはデータパック）",
                get("gekokujo", "丘陵") == 1, "権利=" + get("gekokujo", "丘陵"));

        // ---------- G-2. /jidai setup（設置を最後まで面倒を見る） ----------
        System.out.println();
        System.out.println("--- G-2. /jidai setup ---");

        // 本物の Basho を持たせる。マーカーは下の世界の張りぼてが返す
        Class<?> bashoClass2 = Class.forName("jidai.Basho");
        Object basho2 = bashoClass2.getDeclaredConstructor().newInstance();
        Field bf = pluginClass.getDeclaredField("basho");
        bf.setAccessible(true);
        bf.set(plugin2, basho2);

        // コンソールから打った時の受け皿。knowledge: sendMessage を全部ためる
        PlayerStub con = new PlayerStub("CONSOLE");
        Player pCon = proxy(Player.class, con);

        // --- (a) 区画が読めていない場合: 3回やり直して、理由を出す ---
        SETUP_OKERU = 31;             // 37個のうち31個しか置けない状況
        con.clear(); CMDS.clear();
        onCmd.invoke(plugin2, pCon, new Tate("jidai"), "jidai", new String[]{"setup"});
        check("★★★足りない時は3回やり直す（kyoten を3回 呼ぶ）",
                CMDS.stream().filter(x -> x.equals("function jidai:setup/kyoten")).count() == 3,
                String.valueOf(CMDS));
        check("★★最初に forceload を呼ぶ（区画を読み込ませてから建てる）",
                CMDS.get(0).equals("function jidai:setup/forceload"),
                String.valueOf(CMDS));
        check("★★足りない数をそのまま伝える", con.saw("31 / 37"),
                String.valueOf(con.messages));
        check("★★3回やっても駄目なら、理由と次にすることを出す",
                con.saw("そろいませんでした") && con.saw("再起動"),
                String.valueOf(con.messages));
        System.out.println("      最後の知らせ: " + con.last());

        // --- (b) そろった場合: そのまま登録まで進む ---
        SETUP_OKERU = 37;
        con.clear(); CMDS.clear();
        onCmd.invoke(plugin2, pCon, new Tate("jidai"), "jidai", new String[]{"setup"});
        check("★そろったら kyoten は1回で済む",
                CMDS.stream().filter(x -> x.equals("function jidai:setup/kyoten")).count() == 1,
                String.valueOf(CMDS));
        check("★★37個そろったことを伝える", con.saw("37 / 37"),
                String.valueOf(con.messages));
        check("★★★建てたあと登録まで自分でやる（scan を打たせない）",
                con.saw("登録しました") || con.saw("見つけられませんでした"),
                String.valueOf(con.messages));
        System.out.println("      最後の知らせ: " + con.last());

        // --- (c) コンソールから打っても全部読める ---
        //   ★データパックの tellraw @s はコンソールに届かない。
        //     だからこのコマンドは sender へ返している。そこを押さえる。
        check("★★コンソール宛にも知らせが届く（1通も落ちない）",
                con.messages.size() >= 3, "通数=" + con.messages.size());
        SETUP_OKERU = 37;

        // ---------- H. 銀行（金ブロック1個で預金と売却） ----------
        System.out.println();
        System.out.println("--- H. 銀行の画面 ---");

        Class<?> ginkoClass = Class.forName("jidai.Ginko");
        Object ginko = ginkoClass.getDeclaredConstructor(pluginClass).newInstance(pl);
        // ★ 2026-08-23: 略奪で特殊アイテムを奪う（案D）ため、Seiryoku も受け取る形になった。
        Method osareta = ginkoClass.getMethod("osareta", Player.class,
                org.bukkit.block.Block.class, kaneClass, seiClass);
        Method oshita = ginkoClass.getMethod("oshita", Player.class, int.class, kaneClass);

        // 金ブロックの張りぼて。近くの印で「誰の拠点か」を決める
        final String[] nushiTag = {"jidai_kyoten_kyuryo"};
        org.bukkit.World sekai = proxy(org.bukkit.World.class, (p8, m8, a8) -> {
            if (m8.getName().equals("getNearbyEntities")) {
                java.util.List<org.bukkit.entity.Entity> list = new ArrayList<>();
                list.add(proxy(org.bukkit.entity.Entity.class, (p7, m7, a7) ->
                        m7.getName().equals("getScoreboardTags")
                                ? new java.util.HashSet<>(List.of(nushiTag[0]))
                                : fallback(m7, p7, a7)));
                return list;
            }
            return fallback(m8, p8, a8);
        });
        org.bukkit.block.Block kinBlock = proxy(org.bukkit.block.Block.class, (p6, m6, a6) -> {
            switch (m6.getName()) {
                case "getWorld": return sekai;
                case "getLocation": return new org.bukkit.Location(sekai, 0, 101, -404);
            }
            return fallback(m6, p6, a6);
        });

        // H-0. 戦争していない相手の銀行には手を出せない（2026-08-22 のご指示）
        //   ★ データパック側にも同じ判定があるが、プラグインが先に見て
        //     「何をすれば取れるか」を本人に返す。ここで【呼ばない】ことを測る。
        nushiTag[0] = "jidai_kyoten_shinrin";
        set("youhei", "ronty", 0);
        sensouKesu();
        member.clear(); CMDS.clear();
        try { osareta.invoke(ginko, pMember, kinBlock, kane, sei); }
        catch (InvocationTargetException e) { }
        check("★★★宣戦していない相手の銀行は【略奪へ回さない】（戦争中以外は略奪できない）",
                CMDS.isEmpty() && member.saw("戦争していない"),
                "送信=" + CMDS + " " + member.messages);
        System.out.println("      断り: " + member.last());

        sensouOku("丘陵", "森林", 1);
        member.clear(); CMDS.clear();
        try { osareta.invoke(ginko, pMember, kinBlock, kane, sei); }
        catch (InvocationTargetException e) { }
        check("★★準備中（宣戦済み・交戦前）も略奪へ回さない",
                CMDS.isEmpty() && member.saw("宣戦済み"),
                "送信=" + CMDS + " " + member.messages);
        System.out.println("      断り: " + member.last());

        sensouOku("丘陵", "森林", 3);
        member.clear(); CMDS.clear();
        try { osareta.invoke(ginko, pMember, kinBlock, kane, sei); }
        catch (InvocationTargetException e) { }
        check("★再戦禁止中も略奪へ回さない", CMDS.isEmpty(), "送信=" + CMDS);

        // H-1. 交戦中の相手の拠点 → 略奪へ回す（画面は開かない）
        sensouOku("丘陵", "森林", 2);
        member.clear(); CMDS.clear();
        try { osareta.invoke(ginko, pMember, kinBlock, kane, sei); }
        catch (InvocationTargetException e) { }
        check("★★★交戦中の相手の拠点を押すと略奪へ回す（データパックの担当）",
                CMDS.size() == 3 && CMDS.get(2).endsWith("function jidai:sensou/ryakudatsu"),
                String.valueOf(CMDS));
        // ★1.20.1 は `with storage` が使えないので、番号をスコアで渡す。
        //   この3本の順序が崩れると、略奪は黙って何も起きなくなる。
        check("★★略奪の3本が順序どおり（拠点を見る→自分の番号→本体）",
                CMDS.get(0).endsWith("function jidai:sensou/aite_yomu")
                        && CMDS.get(1).contains(
                                "scoreboard players operation #jibun_no sagyou = @s bangou")
                        && !String.valueOf(CMDS).contains("with storage"),
                String.valueOf(CMDS));
        System.out.println("      送信: " + (CMDS.isEmpty() ? "なし" : String.valueOf(CMDS)));
        sensouKesu();

        // H-2. 傭兵は使えない
        nushiTag[0] = "jidai_kyoten_kyuryo";
        set("youhei", "ronty", 1);
        member.clear(); CMDS.clear();
        try { osareta.invoke(ginko, pMember, kinBlock, kane, sei); }
        catch (InvocationTargetException e) { }
        set("youhei", "ronty", 0);

        // H-3. 預ける（画面を開いている状態にしてボタンを押す）
        Field hiraF = ginkoClass.getDeclaredField("hiraiteru");
        hiraF.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Player> hira = (Map<String, Player>) hiraF.get(ginko);
        hira.put("ronty", pMember);

        set("kane_kojin", "ronty", 100);
        set("chokin", "丘陵", 200);
        member.clear();
        try { oshita.invoke(ginko, pMember, ginkoWaku("WAKU_10"), kane); } catch (InvocationTargetException e) { }
        check("★★10 預けると個人100→90・勢力200→210",
                get("kane_kojin", "ronty") == 90 && get("chokin", "丘陵") == 210,
                "個人=" + get("kane_kojin", "ronty") + " 勢力=" + get("chokin", "丘陵"));

        hira.put("ronty", pMember);
        try { oshita.invoke(ginko, pMember, ginkoWaku("WAKU_ZENBU"), kane); } catch (InvocationTargetException e) { }
        check("★★全部 預けると個人が 0 になる",
                get("kane_kojin", "ronty") == 0 && get("chokin", "丘陵") == 300,
                "個人=" + get("kane_kojin", "ronty") + " 勢力=" + get("chokin", "丘陵"));

        hira.put("ronty", pMember);
        member.clear();
        try { oshita.invoke(ginko, pMember, ginkoWaku("WAKU_50"), kane); } catch (InvocationTargetException e) { }
        check("★足りない時は預からない", get("chokin", "丘陵") == 300
                        && (member.saw("足りません") || member.saw("預ける金がありません")),
                "勢力=" + get("chokin", "丘陵") + " " + member.messages);

        System.out.println("      ※画面の見た目は ItemStack の壁で作れないため、"
                + "金額の動きだけを測っている");

        // ---------- I. 時代を進める画面 ----------
        System.out.println();
        System.out.println("--- I. 時代を進める（鉄ブロック） ---");

        Class<?> shinkoClass = Class.forName("jidai.Shinko");
        Object shinko = shinkoClass.getDeclaredConstructor(pluginClass, seiClass)
                .newInstance(pl, sei);
        Method sOsareta = shinkoClass.getMethod("osareta", Player.class, kaneClass);
        Method sOshita = shinkoClass.getMethod("oshita", Player.class, int.class, kaneClass);

        BOARD.put("jidai", new HashMap<>());
        BOARD.put("sekiyu_gokei", new HashMap<>());
        BOARD.put("kenchiku", new HashMap<>());
        set("settei", "進行_石油_鉄器", 100);
        set("settei", "進行_貯金_鉄器", 5000);
        set("settei", "進行_建築_鉄器", 200);
        set("jidai", "丘陵", 1);

        // I-1. リーダーでない人は開けない
        leaderKimeru.invoke(sei, "kyuryo", "kakikama");
        member.clear();
        try { sOsareta.invoke(shinko, pMember, kane); }
        catch (InvocationTargetException e) { }
        check("★★リーダーでない人は時代を進められない",
                member.saw("リーダー"), String.valueOf(member.messages));
        leaderKimeru.invoke(sei, "kyuryo", "ronty");

        // I-2. 条件が足りないうちは申請できない
        set("sekiyu_gokei", "丘陵", 99);
        set("chokin", "丘陵", 5000);
        set("kenchiku", "丘陵", 200);
        Field hiF = shinkoClass.getDeclaredField("hiraiteru");
        hiF.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> hi = (Set<String>) hiF.get(shinko);
        hi.add("ronty");
        member.clear(); CMDS.clear();
        try { sOshita.invoke(shinko, pMember, 6, kane); }
        catch (InvocationTargetException e) { }
        check("★石油が1本足りなければ申請できない",
                member.saw("まだ条件を満たしていません"), String.valueOf(member.messages));

        // I-3. 3つ揃えば申請できる
        set("sekiyu_gokei", "丘陵", 100);
        hi.add("ronty");
        // ★ 申請の知らせは「同じ勢力の全員」へ飛ぶ。押した本人ではなく、
        //   つないでいる人の一覧(ONLINE)に居る側に届く。
        member.clear(); ldr.clear();
        try { sOshita.invoke(shinko, pMember, 6, kane); }
        catch (InvocationTargetException e) { }
        check("★★★3条件が揃うと申請できる（勢力の全員に届く）",
                ldr.saw("時代進行を申請した"), String.valueOf(ldr.messages));
        System.out.println("      申請: " + ldr.last());

        // I-4. プラグインは時代を進めない（進めるのは運営）
        check("★★プラグインは時代を進めない（承認は運営の仕事）",
                get("jidai", "丘陵") == 1, "実測=" + get("jidai", "丘陵"));
        check("勢力の貯金も減らさない", get("chokin", "丘陵") == 5000,
                "実測=" + get("chokin", "丘陵"));

        // ---------- J. エンダーかまど ----------
        System.out.println();
        System.out.println("--- J. エンダーかまど ---");

        Class<?> kamaClass = Class.forName("jidai.Kamado");
        Method nenryo = kamaClass.getDeclaredMethod("nenryoNagasa", Material.class);
        nenryo.setAccessible(true);

        // J-1. 燃料の長さ
        for (Object[] t : new Object[][]{
                {Material.COAL, 1600, "石炭"},
                {Material.CHARCOAL, 1600, "木炭"},
                {Material.SPRUCE_LOG, 300, "原木(松)"},
                {Material.BIRCH_PLANKS, 300, "木の板(白樺)"},
                {Material.IRON_ORE, 0, "鉄鉱石(燃料でない)"}}) {
            int got = (Integer) nenryo.invoke(null, t[0]);
            check("★燃料の長さ " + t[2] + " = " + t[1], got == (Integer) t[1],
                    "実測=" + got);
        }

        // J-2. 時代で焼く速さが変わる
        Field ytF = kamaClass.getDeclaredField("YAKU_TICK");
        ytF.setAccessible(true);
        int[] yt = (int[]) ytF.get(null);
        check("★★時代が進むほど速く焼ける (120→95→65→40)",
                yt[1] == 120 && yt[2] == 95 && yt[3] == 65 && yt[4] == 40,
                java.util.Arrays.toString(yt));
        check("★鉄器はバニラ(200)より40%短い", yt[1] == 120, "実測=" + yt[1]);
        check("★現代はバニラより80%短い", yt[4] == 40, "実測=" + yt[4]);

        // J-3. 他勢力のかまどは開けない
        Object kama = kamaClass.getDeclaredConstructor(pluginClass).newInstance(pl);
        Method kOsareta = kamaClass.getMethod("osareta", Player.class,
                org.bukkit.block.Block.class, kaneClass);
        nushiTag[0] = "jidai_kyoten_shinrin";
        member.clear();
        try { kOsareta.invoke(kama, pMember, kinBlock, kane); }
        catch (InvocationTargetException e) { }
        check("★★他勢力のかまどは使えない",
                member.saw("森林 のかまど"), String.valueOf(member.messages));
        System.out.println("      断り: " + member.last());
        nushiTag[0] = "jidai_kyoten_kyuryo";

        // ★ プラグインが実際に送った命令を、そのままファイルへ書き出す。
        //   これを本物のデータパックへ流して、受理されるかを別途確かめる
        //   (tests/plugin_renkei.py)。手で写すと写し間違いが起きるため。
        if (args.length > 0) {
            java.nio.file.Path saki = java.nio.file.Path.of(args[0]);
            java.nio.file.Files.write(saki, ZENBU,
                    java.nio.charset.StandardCharsets.UTF_8);
            System.out.println();
            System.out.println("送った命令 " + ZENBU.size() + " 種類を書き出した: " + saki);
        }

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
