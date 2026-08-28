// =============================================================
// GachaTest ── ガチャの「表そのもの」と、十連を測る
//
//   ★★ なぜ足したか（2026-08-22）★★
//     「当たりが少ない」「かなり渋い」というご指摘を受けて
//     景品表を大きく入れ替えた。ところがガチャの検証は
//     Stage7Test の【まわして落ちないか】しか見ておらず、
//     **確率の設計どおりになっているか**は誰も測っていなかった。
//     人が電卓で足すと、必ずどこかで1桁ずれる。ここで機械に足させる。
//
//   ここで測るもの:
//     A. 表の性質（重みの合計・割り当て・金の期待値）
//     B. 十連（10個・天井・時代を守る）
//     C. まわしたあとボタンが戻るか（＝続けてまわせるか）
//     D. MOD 側と枠・見出しが一致しているか
//
//   ここで測れないもの:
//     ・実機で「気持ちよく感じるか」。これは人が回すしかない。
// =============================================================

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;

public class GachaTest {

    static int pass = 0, fail = 0;

    static void check(String label, boolean cond, String detail) {
        if (cond) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + detail); }
    }

    // ── 表を読む道具 ───────────────────────────────
    static Class<?> gc;
    static Object[] hyou;

    static int kazu(Object rec, String mei) throws Exception {
        Method m = rec.getClass().getMethod(mei);
        m.setAccessible(true);
        return (Integer) m.invoke(rec);
    }

    static String moji(Object rec, String mei) throws Exception {
        Method m = rec.getClass().getMethod(mei);
        m.setAccessible(true);
        return (String) m.invoke(rec);
    }

    /** ソースを読む。呼ばれ方によって作業場所が変わるので、候補を順に見る。 */
    static String src(String... michi) throws Exception {
        for (String q : michi) {
            Path w = Paths.get(q);
            if (Files.exists(w)) {
                return Files.readString(w);
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== ガチャ: 表と十連の実測 ===");
        System.out.println();

        gc = Class.forName("jidai.Gacha");
        Field kf = gc.getDeclaredField("KEIHIN");
        kf.setAccessible(true);
        hyou = (Object[]) kf.get(null);

        Method mNedan = gc.getDeclaredMethod("nedan", int.class);
        mNedan.setAccessible(true);

        check("景品表が読めた", hyou != null && hyou.length > 0,
                "件数=" + (hyou == null ? "null" : hyou.length));

        // =========================================================
        //  A. 表の性質
        // =========================================================
        System.out.println();
        System.out.println("-- A. 表の性質 --");

        for (int jidai = 1; jidai <= 4; jidai++) {
            int nedan = (Integer) mNedan.invoke(null, jidai);

            int goukei = 0, hazure = 0, kaneOmomi = 0, tokushu = 0, dai = 0;
            double kaneKitai = 0.0;
            Set<String> mihon = new TreeSet<>();

            for (Object k : hyou) {
                if (kazu(k, "jidai") != jidai) {
                    continue;
                }
                int om = kazu(k, "omomi");
                goukei += om;
                mihon.add(moji(k, "namae") + "×" + kazu(k, "kazu"));

                if (moji(k, "toukyuu").equals("ハズレ")) {
                    hazure += om;
                }
                if (moji(k, "toukyuu").equals("大")) {
                    dai += om;
                }
                if (kazu(k, "kane") > 0) {
                    kaneOmomi += om;
                    kaneKitai += om / 10000.0 * kazu(k, "kane");
                } else if (tokushuKa(moji(k, "namae"))) {
                    tokushu += om;
                }
            }

            String mei = "時代 " + jidai + " (" + nedan + "円)";

            // ★★ 重みは1万分率。合計が 10000 でないと、
            //    書いてある % と実際の出方がずれる。
            check(mei + ": 重みの合計が ちょうど 10000", goukei == 10000,
                    "実際=" + goukei);

            check(mei + ": はずれは 25% 以下（渋くしすぎない）", hazure <= 2500,
                    "実際=" + (hazure / 100.0) + "%");

            int ooatari = kaneOmomi + tokushu;
            check(mei + ": 大当たりの見出しが 20〜35%（およそ3〜5回に1回）",
                    ooatari >= 2000 && ooatari <= 3500,
                    "実際=" + (ooatari / 100.0) + "%");

            check(mei + ": 特殊アイテムが1種以上ある", tokushu > 0, "0");
            check(mei + ": 特賞(大)が1種以上ある", dai > 0, "0");

            // ★★ ここが一番大事 ★★
            //   金の期待値が値段を超えると、まわすほど金が増える。
            //   売って稼ぐ・時代を進める、という筋がまるごと壊れる。
            check(mei + ": ★★金の期待値が値段を【超えない】（増殖しない）",
                    kaneKitai < nedan,
                    String.format("期待値=%.2f 値段=%d", kaneKitai, nedan));

            double wari = kaneKitai / nedan * 100.0;
            check(mei + ": 金の戻りが 40〜75%（渋すぎず、増えすぎず）",
                    wari >= 40.0 && wari <= 75.0,
                    String.format("実際=%.1f%%", wari));

            // ★ 検証の見分け方（名前×個数）が成り立つ前提
            int kensu = 0;
            for (Object k : hyou) {
                if (kazu(k, "jidai") == jidai) {
                    kensu++;
                }
            }
            check(mei + ": 景品が（名前×個数）で見分けられる", kensu == mihon.size(),
                    "表 " + kensu + " 件 / 名前は " + mihon.size() + " 通り");

            System.out.println(String.format(
                    "      %s  はずれ%.1f%% 金%.1f%%(戻り%.1f%%) 特殊%.2f%% 特賞%.2f%%",
                    mei, hazure / 100.0, kaneOmomi / 100.0, wari,
                    tokushu / 100.0, dai / 100.0));

            // ★ 2026-08-22 の実機のご指摘（松明が多い / パンが多い / 丸石を +5%）
            //   品名ごとに足して、決めた範囲に収まっているかを見る。
            int taimatsu = 0, pan = 0, maruishi = 0;
            for (Object k : hyou) {
                if (kazu(k, "jidai") != jidai) {
                    continue;
                }
                String na = moji(k, "namae");
                if (na.equals("松明")) {
                    taimatsu += kazu(k, "omomi");
                } else if (na.equals("パン")) {
                    pan += kazu(k, "omomi");
                } else if (na.equals("丸石")) {
                    maruishi += kazu(k, "omomi");
                }
            }
            check(mei + ": 松明は 15% 以下", taimatsu <= 1500, "実際=" + (taimatsu / 100.0) + "%");
            check(mei + ": パンは 22% 以下", pan <= 2200, "実際=" + (pan / 100.0) + "%");
            check(mei + ": 丸石(はずれ)は 10% 以上", maruishi >= 1000, "実際=" + (maruishi / 100.0) + "%");
        }

        // =========================================================
        //  B. 十連
        // =========================================================
        System.out.println();
        System.out.println("-- B. 十連 --");

        Object plugin = null;                 // juren は plugin を使わない
        Object gacha = unsafeNew(gc);
        Field ranF = gc.getDeclaredField("ran");
        ranF.setAccessible(true);
        ranF.set(gacha, new Random(2468));

        Method mJuren = gc.getDeclaredMethod("juren", int.class);
        mJuren.setAccessible(true);
        Method mErabu = gc.getDeclaredMethod("erabu", int.class);
        mErabu.setAccessible(true);

        final int TAMESHI = 2000;
        for (int jidai = 1; jidai <= 4; jidai++) {
            Set<String> sonoJidai = new TreeSet<>();
            for (Object k : hyou) {
                if (kazu(k, "jidai") == jidai) {
                    sonoJidai.add(moji(k, "namae") + "×" + kazu(k, "kazu"));
                }
            }

            int kazuNG = 0, tenjouNG = 0;
            List<String> yosomono = new ArrayList<>();
            for (int i = 0; i < TAMESHI; i++) {
                List<?> deta = (List<?>) mJuren.invoke(gacha, jidai);
                if (deta == null || deta.size() != 10) {
                    kazuNG++;
                    continue;
                }
                boolean atari = false;
                for (Object k : deta) {
                    String t = moji(k, "toukyuu");
                    if (t.equals("中") || t.equals("大")) {
                        atari = true;
                    }
                    String key = moji(k, "namae") + "×" + kazu(k, "kazu");
                    if (!sonoJidai.contains(key) && yosomono.size() < 5) {
                        yosomono.add(key);
                    }
                }
                if (!atari) {
                    tenjouNG++;
                }
            }
            check("時代 " + jidai + ": 十連は必ず10個 返る", kazuNG == 0,
                    "おかしい回=" + kazuNG);
            check("時代 " + jidai + ": 十連に他の時代の景品が混ざらない",
                    yosomono.isEmpty(), "混ざった: " + yosomono);
            check("時代 " + jidai + ": ★★天井が効く（必ず「中」以上が1つ以上）",
                    tenjouNG == 0, TAMESHI + "回のうち " + tenjouNG + " 回 外れた");

            // ★ 天井が無かったら どれくらい空振りするか。
            //   ここが 0 に近いと「天井が要らない」ことになり、この仕掛け自体が無意味。
            int nashi = 0;
            for (int i = 0; i < TAMESHI; i++) {
                boolean atari = false;
                for (int j = 0; j < 10; j++) {
                    String t = moji(mErabu.invoke(gacha, jidai), "toukyuu");
                    if (t.equals("中") || t.equals("大")) {
                        atari = true;
                    }
                }
                if (!atari) {
                    nashi++;
                }
            }
            double sugao = nashi * 100.0 / TAMESHI;
            check("時代 " + jidai + ": 天井が仕事をしている（素なら5%以上 空振りする）",
                    sugao >= 5.0, String.format("素の空振り=%.1f%%", sugao));
            System.out.println(String.format(
                    "      時代 %d: 天井が無ければ %.1f%% の十連が「小」以下だけになる",
                    jidai, sugao));
        }

        // =========================================================
        //  B2. 特殊アイテムは勢力ごとに各1個まで（2026-08-22 のご指示）
        // =========================================================
        System.out.println();
        System.out.println("-- B2. 特殊アイテムは勢力ごとに各1個まで --");
        {
            // 偽の Bukkit（チームだけ分かればよい）
            org.bukkit.scoreboard.Team kyuryo = proxy(org.bukkit.scoreboard.Team.class,
                    (p, m, a) -> m.getName().equals("getName") ? "kyuryo" : fallback(m, p, a));
            org.bukkit.scoreboard.Scoreboard board = proxy(org.bukkit.scoreboard.Scoreboard.class,
                    (p, m, a) -> {
                        if (m.getName().equals("getEntryTeam")) {
                            return "ronty".equals(a[0]) ? kyuryo : null;
                        }
                        return fallback(m, p, a);
                    });
            org.bukkit.scoreboard.ScoreboardManager mgr = proxy(
                    org.bukkit.scoreboard.ScoreboardManager.class,
                    (p, m, a) -> m.getName().equals("getMainScoreboard") ? board : fallback(m, p, a));
            org.bukkit.Server server = proxy(org.bukkit.Server.class, (p, m, a) -> {
                if (m.getName().equals("getScoreboardManager")) return mgr;
                if (m.getName().equals("getLogger")) return java.util.logging.Logger.getLogger("GachaTest");
                return fallback(m, p, a);
            });
            Field bs = org.bukkit.Bukkit.class.getDeclaredField("server");
            bs.setAccessible(true);
            bs.set(null, server);
            org.bukkit.entity.Player ronty = proxy(org.bukkit.entity.Player.class,
                    (p, m, a) -> m.getName().equals("getName") ? "ronty" : fallback(m, p, a));
            org.bukkit.entity.Player hei = proxy(org.bukkit.entity.Player.class,
                    (p, m, a) -> m.getName().equals("getName") ? "kakikama" : fallback(m, p, a));
            Object kane = Class.forName("jidai.Kane").getDeclaredConstructor().newInstance();

            Object gacha2 = unsafeNew(gc);
            ire(gacha2, "ran", new Random(99));
            ire(gacha2, "tokushuDeta", new HashMap<String, Set<String>>());
            Method mJuren3 = gc.getDeclaredMethod("juren", int.class,
                    org.bukkit.entity.Player.class, Class.forName("jidai.Kane"));
            mJuren3.setAccessible(true);
            Method mDeta = gc.getDeclaredMethod("tokushuDetaKa", String.class, String.class);
            mDeta.setAccessible(true);

            // 丘陵の人が近代で 3000 回（= 十連 300 回）まわす
            Map<String, Integer> kazu = new TreeMap<>();
            int tokushuGokei = 0;
            for (int i = 0; i < 300; i++) {
                List<?> deta = (List<?>) mJuren3.invoke(gacha2, 3, ronty, kane);
                for (Object k : deta) {
                    String na = moji(k, "namae");
                    if (tokushuKa(na)) {
                        kazu.merge(na, 1, Integer::sum);
                        tokushuGokei++;
                    }
                }
            }
            // ★★ 期待する顔ぶれは【景品表から数え直す】★★
            //   ここは「近代で5種そろう」と 5 と 月の石 を焼き込んでいた。
            //   2026-08-23 の案C（歯車と月の石を近代から外す）で 2件 落ちた。
            //   落ちたこと自体は正しいが、直すたびに数字を書き換えるのでは、
            //   表と検査が別々に育って いつか食い違う。表から引く形にする。
            Set<String> hazu = new TreeSet<>();
            for (Object k : hyou) {
                if (kazu(k, "jidai") == 3 && tokushuKa(moji(k, "namae"))) {
                    hazu.add(moji(k, "namae"));
                }
            }
            check("★★特殊は 近代の表にある " + hazu.size() + "種 それぞれ【1個まで】しか出ない"
                            + "（3000回で合計 " + tokushuGokei + "）",
                    tokushuGokei == hazu.size() && kazu.keySet().equals(hazu),
                    "出た数=" + kazu + " / 表=" + hazu);

            // 出た物すべてが記録に残っているか（1つでも漏れたら落ちる）
            String nokoranai = null;
            for (String na : kazu.keySet()) {
                if (!(Boolean) mDeta.invoke(gacha2, "丘陵", na)) {
                    nokoranai = na;
                }
            }
            check("出た特殊はすべて勢力の記録に残る（丘陵: " + kazu.keySet() + "）",
                    !kazu.isEmpty() && nokoranai == null, "残っていない=" + nokoranai);

            // ★★ 案C そのものの検査（2026-08-23）★★
            //   近代に「蒸気機関の歯車」「月の石」を戻すと、超特殊勝利が
            //   また 57〜67分になり、本命の現代到達を1時間 追い越す。
            //   表を戻したら、ここで落ちる。
            Set<String> gendai = new TreeSet<>();
            for (Object k : hyou) {
                if (kazu(k, "jidai") == 4 && tokushuKa(moji(k, "namae"))) {
                    gendai.add(moji(k, "namae"));
                }
            }
            check("★★案C: 近代のガチャでは 5種そろわない（" + hazu.size() + "種まで）",
                    hazu.size() < gendai.size(), "近代=" + hazu);
            check("★案C: 近代に「蒸気機関の歯車」が出ない", !hazu.contains("蒸気機関の歯車"), "出る");
            check("★案C: 近代に「月の石」が出ない", !hazu.contains("月の石"), "出る");
            check("★案C: 現代のガチャなら 5種そろう",
                    gendai.size() == jidai.Shouri.tokushuKazu(), "現代=" + gendai);
            System.out.println("      3000回: " + kazu);

            // 勢力に入っていない人には 1つも出ない
            int heiTokushu = 0;
            for (int i = 0; i < 300; i++) {
                List<?> deta = (List<?>) mJuren3.invoke(gacha2, 3, hei, kane);
                for (Object k : deta) {
                    if (tokushuKa(moji(k, "namae"))) {
                        heiTokushu++;
                    }
                }
            }
            check("★勢力に居ない人には特殊が出ない（3000回）", heiTokushu == 0,
                    "出た=" + heiTokushu);

            // 引き直しても 10個 返り、期待値の筋は崩れない（金の景品は残る）
            List<?> d = (List<?>) mJuren3.invoke(gacha2, 1, ronty, kane);
            check("引き直しても十連は 10個", d.size() == 10, "実際=" + d.size());
        }

        // =========================================================
        //  C. まわしたあと、ボタンが戻るか
        // =========================================================
        System.out.println();
        System.out.println("-- C. 続けてまわせるか --");

        String gsrc = src("plugin/src/main/java/jidai/Gacha.java",
                          "../src/main/java/jidai/Gacha.java",
                          "src/main/java/jidai/Gacha.java");
        check("Gacha.java が読めた", gsrc != null, "見つからない");

        if (gsrc != null) {
            // ★★ 実機で起きた不具合そのもの ★★
            //   止まった景品が「まわす」ボタンの枠に居座り、
            //   ボタンが消えたので、まわせるように見えなかった。
            check("★★(字) 止まったあと、まわすボタンを戻している",
                    gsrc.contains("private void modosu(")
                            && gsrc.contains("inv.setItem(BUTTON, button(jidai))"),
                    "modosu が無い / ボタンを戻していない");
            check("★(字) 戻す処理を、演出が止まってから予約している",
                    gsrc.contains("() -> modosu(player, atari, kane), MODOSU_MADE"),
                    "予約していない");
            check("★(字) まわしている最中なら戻さない（次の演出を壊さないため）",
                    gsrc.contains("if (mawashichu.contains(player.getName())) {\n            return;                 // もう次がまわっている"),
                    "その番人が無い");
            check("★(字) 戻す時に十連ボタンも置き直している",
                    gsrc.contains("inv.setItem(BUTTON_10, button10(jidai))"),
                    "置き直していない");
            check("★(字) 流れ終わった帯を片づけている",
                    gsrc.contains("inv.setItem(i, null)"), "片づけていない");

            // 十連の門番
            check("★(字) 十連は【抽選のあと】に空き枠を数える（金だけの当たりは枠が要らない）",
                    gsrc.indexOf("List<Keihin> deta = juren(jidai, player, kane)")
                            < gsrc.indexOf("if (akiWaku(player) < iru)"),
                    "順番が逆");
            //   ★ 見たいのは【断るのが金を引くより前か】だけ。
            //     以前は「金を引いた行の次が machi.put」という書き方を見ていたので、
            //     並べ替えただけで落ちた（2026-08-23）。mawasu10 の中だけで比べる。
            String m10 = kiridashi(gsrc, "public void mawasu10(", "private void mekuru(");
            check("★★(字) 空き枠が足りない時、金を引く前に断っている",
                    m10.indexOf("if (akiWaku(player) < iru)") >= 0
                            && m10.indexOf("if (akiWaku(player) < iru)")
                                    < m10.indexOf("kane.kojinKousin("),
                    "金を引いた後に断っている＝金だけ取られる");

            // ★★★ 2026-08-23 のご指摘「もう一度 十連 を押すと、めくられず停止する」★★★
            //   画面を開き直す時の【閉じた】知らせを tojita が
            //   「途中で閉じた」と取り違え、まだ1枚もめくっていない結果を
            //   その場で渡していた。渡すと machi から消えるので、直後の
            //   mekuru が「もう片づいている」と見なして止まっていた。
            check("★★★(字) 十連の控えが【自分の画面】を覚えている（Juren に inv）",
                    gsrc.contains("final Inventory inv;")
                            && gsrc.contains("Juren(List<Keihin> deta, int jidai, int mae, Kane kane, Inventory inv)"),
                    "覚えていない＝どの画面を閉じたのか見分けられない");
            check("★★★(字) 閉じた時は【その結果の画面】の時だけ渡す（もう一度 十連 で横取りしない）",
                    gsrc.contains("!j.watashita && j.inv == inv"),
                    "画面を見ずに渡している＝2回目の十連がめくられず止まる");
            check("★★(字) 十連は画面を【作ってから】控える（Juren に画面を渡すため）",
                    m10.indexOf("Bukkit.createInventory(this, JUREN_SUU") >= 0
                            && m10.indexOf("Bukkit.createInventory(this, JUREN_SUU")
                                    < m10.indexOf("machi.put("),
                    "控えてから作っている＝画面を渡せない");
            check("★★(字) もう一度 十連 は、クリックの処理中に開かない（1tick 待つ）",
                    kiridashi(gsrc, "if (slot == JUREN_MOUICHIDO) {", "} else if (slot == JUREN_TOJIRU)")
                            .contains("runTaskLater"),
                    "クリックの中で開き直している＝新しい画面が届かないことがある");

            // ★★ 2026-08-22 のご指摘「結果が出る前にアイテムが付与される」 ★★
            check("★★★(字) 十連は、まわした時点では何も渡さない（mawasu10 に addItem が無い）",
                    !kiridashi(gsrc, "public void mawasu10(", "private void mekuru(")
                            .contains("addItem("),
                    "まわした瞬間に渡している");
            check("★★★(字) 渡すのは めくり終えた後（mekuru の最後で watasu10）",
                    kiridashi(gsrc, "private void mekuru(", "private void watasu10(")
                            .contains("watasu10(player, j);"),
                    "mekuru が渡していない");
            check("★(字) 途中で閉じた人にも渡す（tojita が watasu10 を呼ぶ）",
                    kiridashi(gsrc, "public void tojita(", "public void sanka(")
                            .contains("watasu10(p, j);"),
                    "閉じた時の出口が無い＝金だけ取られる");
            check("★(字) 切断した人にも、次に入った時に渡す（sanka）",
                    kiridashi(gsrc, "public void sanka(", "private void watasu(Player player, Keihin atari")
                            .contains("watasu10(player, j);"),
                    "切断の出口が無い");
            check("★(字) 二重に渡さない（watashita の見張り）",
                    gsrc.contains("if (j.watashita) {\n            return;\n        }\n        j.watashita = true;"),
                    "見張りが無い");
            check("★(字) 金は【今の残高】に足す（めくっている間に動いた金を消さない）",
                    gsrc.contains("int zangaku = j.kane.kojinZandaka(player);"),
                    "控えた時の残高に足している");
            check("★(字) 入り切らない品は足元へ落とす",
                    gsrc.contains("player.getWorld().dropItem(player.getLocation(), nokori);"),
                    "あふれた品が消える");

            // 単発の演出中に十連ボタンが居座る（ご指摘）
            String ens = kiridashi(gsrc, "private void enshutsu(", "private void susumu(");
            check("★(字) 単発をまわしている間は十連ボタンを引っ込める",
                    ens.contains("inv.setItem(BUTTON_10, null);"), "引っ込めていない");
            check("★(字) 結果画面のアイテムにも見た目の番号が付く（kekkaItem10 が keihinItem を土台にする）",
                    kiridashi(gsrc, "private ItemStack kekkaItem10(", "private static ItemStack fuseta(")
                            .contains("ItemStack item = keihinItem(k);"),
                    "結果画面では紙のまま");
        }

        // めくる演出の長さ（ご指示: 5秒程度）
        int nagasa = (Integer) gc.getMethod("jurenNagasa").invoke(null);
        check("★十連の演出が 4〜6 秒（80〜120 tick）", nagasa >= 80 && nagasa <= 120,
                "実際=" + nagasa + " tick");
        System.out.println("      十連の演出: " + nagasa + " tick = " + (nagasa / 20.0) + " 秒");

        // =========================================================
        //  D. MOD 側との突き合わせ
        // =========================================================
        System.out.println();
        System.out.println("-- D. MOD 側と食い違っていないか --");

        Method mWaku = gc.getMethod("jurenWaku");
        Method mMidashi = gc.getMethod("jurenMidashi");
        int[] waku = (int[]) mWaku.invoke(null);
        String midashi = (String) mMidashi.invoke(null);

        check("十連の枠が 10 個", waku.length == 10, "実際=" + waku.length);

        String jsrc = src("clientmod/src/main/java/jidai/ui/JurenGamen.java",
                          "../../clientmod/src/main/java/jidai/ui/JurenGamen.java");
        check("MOD の JurenGamen.java が読めた", jsrc != null, "見つからない");

        if (jsrc != null) {
            StringBuilder b = new StringBuilder("{");
            for (int i = 0; i < waku.length; i++) {
                if (i > 0) {
                    b.append(", ");
                }
                b.append(waku[i]);
            }
            b.append("}");
            // ★★ ここが食い違うと、MOD 側だけ空の画面になる ★★
            //   しかもプラグインは正しく動くので、実機で見るまで気づけない。
            check("★★(字) MOD の枠の並びが プラグインと同じ",
                    jsrc.contains("WAKU = " + b), "MOD 側に " + b + " が無い");
            check("★(字) MOD の見出しが プラグインと同じ",
                    jsrc.contains("\"" + midashi + "\""),
                    "MOD 側に " + midashi + " が無い");

            // 等級の印も、両方で同じ文字でないと色が付かない
            Field sd = gc.getDeclaredField("SHIRUSHI_DAI");
            sd.setAccessible(true);
            Field sc = gc.getDeclaredField("SHIRUSHI_CHU");
            sc.setAccessible(true);
            check("★(字) 等級の印（大）が MOD 側と同じ",
                    jsrc.contains("SHIRUSHI_DAI = \"" + sd.get(null) + "\""),
                    "MOD 側の印が違う");
            check("★(字) 等級の印（中）が MOD 側と同じ",
                    jsrc.contains("SHIRUSHI_CHU = \"" + sc.get(null) + "\""),
                    "MOD 側の印が違う");
        }

        String tsrc = src("clientmod/src/main/java/jidai/ui/Tsunagu.java",
                          "../../clientmod/src/main/java/jidai/ui/Tsunagu.java");
        if (tsrc != null) {
            check("★(字) MOD が十連の画面を差し替える口を持っている",
                    tsrc.contains("JUREN = \"" + midashi + "\"")
                            && tsrc.contains("new JurenGamen("),
                    "振り分けが無い");
        }

        // =========================================================
        //  E. 特殊アイテムが、本当に手元へ届くか
        // =========================================================
        //
        // ★★★ 2026-08-22 に見つかった、催しを壊す不具合 ★★★
        //   勝利判定 Shouri.motteru は【紙の表示名】で見分けているのに、
        //   ガチャが渡していたのは `new ItemStack(材質, 個数)` ＝ **名前の無い紙**
        //   だった。つまり **5種そろえても永久に勝てない**状態だった。
        //   ここまで誰も測っていなかったので、実機でも気づけなかった。
        System.out.println();
        System.out.println("-- E. 特殊アイテムが手元へ届くか --");

        Class<?> sc = Class.forName("jidai.Shouri");
        String[] tokushuMei = (String[]) sc.getMethod("tokushuMei").invoke(null);
        String[][] tokushuHyou = (String[][]) sc.getMethod("tokushuHyou").invoke(null);
        Method mModel = sc.getMethod("tokushuModelData", String.class);

        check("特殊アイテムは 5 種", tokushuMei.length == 5, "実際=" + tokushuMei.length);

        // ★ 表に無い特殊アイテムがあると、その時代では絶対に手に入らない
        for (String mei : tokushuMei) {
            boolean aru = false;
            for (Object k : hyou) {
                if (moji(k, "namae").equals(mei)) {
                    aru = true;
                }
            }
            check("「" + mei + "」がガチャの景品表にある", aru, "表に無い＝一生 手に入らない");
        }

        // ★ 番号は1つずつ違うこと。重なると別の絵が出る
        Set<Integer> ban = new TreeSet<>();
        for (String[] r : tokushuHyou) {
            ban.add(Integer.parseInt(r[2]));
            check("「" + r[0] + "」に絵と番号がある",
                    !r[1].isEmpty() && Integer.parseInt(r[2]) > 0, Arrays.toString(r));
        }
        check("見た目の番号が重なっていない", ban.size() == tokushuHyou.length,
                "番号=" + ban);

        // ★ ふつうの景品に番号を付けてはいけない（名前が付いて重ならなくなる）
        List<String> yokei = new ArrayList<>();
        for (Object k : hyou) {
            String na = moji(k, "namae");
            boolean toku = false;
            for (String mei : tokushuMei) {
                if (mei.equals(na)) {
                    toku = true;
                }
            }
            boolean ibutsu = (Boolean) sc.getMethod("ibutsuKa", String.class).invoke(null, na);
            if (!toku && !ibutsu && (Integer) mModel.invoke(null, na) != 0) {
                yokei.add(na);
            }
        }
        check("ふつうの景品には見た目の番号が付かない", yokei.isEmpty(), String.valueOf(yokei));

        // ★★★ 2026-08-23 のご指摘「遺物の説明が『5種そろえると勝ち』になっている」★★★
        //   遺物は勝利条件に入らない。集める5種と同じ説明を付けると、
        //   持ち主は集めれば勝てると思い込み、いつまでも勝てない＝説明が嘘になる。
        Method mSetsumei = Class.forName("jidai.Gacha")
                .getDeclaredMethod("setsumeiGyou", String.class);
        mSetsumei.setAccessible(true);
        Method mIbSetsu = sc.getMethod("ibutsuSetsumei", String.class);
        Method mIbJidai = sc.getMethod("ibutsuJidai", String.class);
        Method mJidaiMei = Class.forName("jidai.Kane").getMethod("jidaiMei", int.class);

        for (String mei : tokushuMei) {
            @SuppressWarnings("unchecked")
            List<String> gyou = (List<String>) mSetsumei.invoke(null, mei);
            check("集める5種「" + mei + "」の説明は「5種そろえると勝ち」",
                    String.join(" ", gyou).contains("5種そろえると勝ち"), String.valueOf(gyou));
        }
        String[][] ibHyou = (String[][]) sc.getMethod("ibutsuHyou").invoke(null);
        for (String[] r : ibHyou) {
            @SuppressWarnings("unchecked")
            List<String> gyou = (List<String>) mSetsumei.invoke(null, r[0]);
            String zen = String.join(" ", gyou);
            check("★★★遺物「" + r[0] + "」の説明に「5種そろえると勝ち」が【無い】",
                    !zen.contains("5種そろえると勝ち"), zen);
            check("遺物「" + r[0] + "」の説明に効果が書いてある",
                    zen.contains((String) mIbSetsu.invoke(null, r[0])), zen);
            check("遺物「" + r[0] + "」の説明に時代と「遺物」が書いてある",
                    zen.contains((String) mJidaiMei.invoke(null,
                            (Integer) mIbJidai.invoke(null, r[0])) + "の遺物"), zen);
            check("遺物「" + r[0] + "」の説明に「勢力」に効果が付くと書いてある",
                    zen.contains("勢力"), zen);
        }

        // ★ アイコン（見た目の番号）が、渡す物・帯・十連の結果の【3か所とも】付くか
        //   2026-08-23 のご指摘「ガチャにアイコンが出ていない」。
        //   絵そのものは MOD 側（tests/mod_kakunin.py）で見る。ここは番号の付け忘れ。
        if (gsrc != null) {
            for (String[] r : ibHyou) {
                check("遺物「" + r[0] + "」に見た目の番号がある (" + r[2] + ")",
                        (Integer) mModel.invoke(null, r[0]) == Integer.parseInt(r[2]),
                        "番号が引けない＝MOD が絵を差し替えられない");
            }
            check("★★(字) 帯・結果に出す物にも見た目の番号を付けている（keihinItem）",
                    kiridashi(gsrc, "private ItemStack keihinItem(", "/**")
                            .contains("meta.setCustomModelData(moderu)"),
                    "ガチャの画面だけ紙のままになる");
            check("★★(字) 十連の結果の1枚は keihinItem を土台にしている（番号を落とさない）",
                    kiridashi(gsrc, "private ItemStack kekkaItem10(", "/** めくる前の伏せ札。")
                            .contains("ItemStack item = keihinItem(k);"),
                    "結果画面だけ紙のままになる");
        }

        if (gsrc != null) {
            check("★★★(字) 渡す物を作る所がある（watasuMono）",
                    gsrc.contains("private static ItemStack watasuMono("),
                    "無い");
            check("★★★(字) 単発で渡す時、素の ItemStack を使っていない",
                    !gsrc.contains("addItem(new ItemStack(atari.material()")
                            && gsrc.contains("addItem(watasuMono(atari))"),
                    "名前の無い紙を渡している＝5種そろえても勝てない");
            check("★★★(字) 十連で渡す時も、素の ItemStack を使っていない",
                    !gsrc.contains("addItem(new ItemStack(k.material()")
                            && gsrc.contains("addItem(watasuMono(k))"),
                    "同上");
            check("★(字) 渡す物に、その景品の名前を付けている",
                    gsrc.contains("meta.setDisplayName(moji(\"§b\" + k.namae()))"),
                    "名前を付けていない");
            check("★(字) 特殊でなければ素のまま渡す（パンが重ならなくなるのを防ぐ）",
                    gsrc.contains("if (moderu == 0) {\n            return item;"),
                    "全部に名前を付けている疑い");
            check("★(字) 渡す物に見た目の番号を付けている",
                    gsrc.contains("meta.setCustomModelData(moderu)"), "付けていない");
        }

        // ★ 勝利判定が「表示名で探す」ままであること。
        //   ここを変えるなら、渡す側も同時に変えないと また同じ穴が開く。
        String ssrc = src("plugin/src/main/java/jidai/Shouri.java",
                          "../src/main/java/jidai/Shouri.java",
                          "src/main/java/jidai/Shouri.java");
        if (ssrc != null) {
            check("★(字) 勝利判定は【表示名】で特殊アイテムを探している",
                    ssrc.contains("m.hasDisplayName()")
                            && ssrc.contains("m.getDisplayName().contains(mei)"),
                    "探し方が変わった。渡す側も見直すこと");
            check("★(字) 名前の一覧が二重に書かれていない（TOKUSHU_HYOU が正本）",
                    ssrc.contains("private static final String[] TOKUSHU = mei();"),
                    "名前を別に持っている");
        }

        // ── MOD 側に絵と模型が入っているか ──
        java.nio.file.Path modJar = null;
        for (String q : new String[]{"clientmod/JidaiUI-0.1.0.jar",
                                     "../../clientmod/JidaiUI-0.1.0.jar"}) {
            if (java.nio.file.Files.exists(java.nio.file.Paths.get(q))) {
                modJar = java.nio.file.Paths.get(q);
            }
        }
        if (modJar == null) {
            System.out.println("      （MOD の jar が見つからないので、絵の点検はとばす）");
        } else {
            try (JarFile jar = new JarFile(modJar.toFile())) {
                for (String[] r : tokushuHyou) {
                    check("MOD に " + r[0] + " の絵がある",
                            jar.getJarEntry("assets/jidaiui/textures/item/" + r[1] + ".png") != null,
                            r[1] + ".png");
                    check("MOD に " + r[0] + " の模型がある",
                            jar.getJarEntry("assets/jidaiui/models/item/" + r[1] + ".json") != null,
                            r[1] + ".json");
                }
                var e = jar.getJarEntry("assets/minecraft/models/item/paper.json");
                check("MOD が紙の対応表を持っている", e != null, "paper.json が無い");
                if (e != null) {
                    String kami;
                    try (var in = jar.getInputStream(e)) {
                        kami = new String(in.readAllBytes(), "UTF-8");
                    }
                    // ★★ 番号がずれると【別の絵】が出る。数字で突き合わせる
                    for (String[] r : tokushuHyou) {
                        check("(字) 紙の対応表に " + r[0] + " の番号 " + r[2] + " がある",
                                kami.contains("\"custom_model_data\": " + r[2])
                                        && kami.contains("jidaiui:item/" + r[1]),
                                "番号か絵の名前が食い違っている");
                    }
                    check("(字) 紙の対応表が、ふつうの紙の見た目を壊していない",
                            kami.contains("\"layer0\": \"minecraft:item/paper\""),
                            "ふつうの紙まで別の絵になる");
                }
            }
        }

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> iface, java.lang.reflect.InvocationHandler h) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(GachaTest.class.getClassLoader(),
                new Class<?>[]{iface}, h);
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

    static void ire(Object o, String mei, Object atai) throws Exception {
        Field f = o.getClass().getDeclaredField(mei);
        f.setAccessible(true);
        f.set(o, atai);
    }

    /** ソースの中から、目印と目印の間を切り出す。無ければ空文字。 */
    static String kiridashi(String src, String kara, String made) {
        int i = src.indexOf(kara);
        int j = src.indexOf(made, i < 0 ? 0 : i);
        if (i < 0 || j < 0 || j < i) {
            return "";
        }
        return src.substring(i, j);
    }

    /**
     * 特殊アイテムか。★ 名前の正本は Shouri。ここで並べ直すと食い違う。
     */
    static boolean tokushuKa(String namae) throws Exception {
        Class<?> sc = Class.forName("jidai.Shouri");
        Method m = sc.getMethod("tokushuMei");
        m.setAccessible(true);
        for (String mei : (String[]) m.invoke(null)) {
            if (mei.equals(namae)) {
                return true;
            }
        }
        return false;
    }

    /** コンストラクタを通さずに作る。 */
    static Object unsafeNew(Class<?> c) throws Exception {
        Class<?> u = Class.forName("sun.misc.Unsafe");
        Field f = u.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method m = u.getMethod("allocateInstance", Class.class);
        return m.invoke(unsafe, c);
    }
}
