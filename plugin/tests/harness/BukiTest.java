// =============================================================
// BukiTest ── 販売所に並ぶ銃9丁と弾6種を、1つずつ数える
//
//   2026-08-20 に指示された割り当てを、そのまま表にして突き合わせる。
//   ★ここが狂うと、時代の意味（何が撃てるようになるか）が壊れる。
//
//   実物の JUKIHIN 配列をリフレクションで読む。ソースの文字を見るのでは
//   なく、コンパイル後の値を見る。コメントだけ直して実装を直し忘れた、
//   という事故をここで捕まえる。
// =============================================================

import java.lang.reflect.*;
import java.util.*;

public class BukiTest {

    static int pass = 0, fail = 0;

    static void check(String label, boolean cond, String detail) {
        if (cond) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + detail); }
    }

    /**
     * 期待する1行ぶん。
     *   枠 / 名前に必ず入る字 / 値段 / 勢力の金か / 解禁の時代 / MODのID
     */
    record Machi(int slot, String namae, int nedan, boolean seiryoku,
                 int kaikin, String modItem, int page) { }

    /** ★ 指示どおりの割り当て。ここが正本。 */
    static final Machi[] MACHI = {
            // --- 銃 (タブ1枚目・2段目の 9〜17) ---
            //   ★ 2026-08-21: 1段目(0〜8)はタブ専用になった。
            new Machi(9,  "M1851",   800,   true,  2, "hamster:coltm1851", 1),
            new Machi(10,  "M1873",   1000,  true,  3, "hamster:colt1873", 1),
            new Machi(11,  "SKS",     3000,  true,  3, "hamster:sks", 1),
            new Machi(12,  "ガーランド", 5000,  true,  3, "hamster:m1garand", 1),
            new Machi(13,  "マドセン",   7000,  true,  3, "hamster:madsen", 1),
            new Machi(14,  "グロック",   1200,  true,  4, "tacz:glock_17", 1),
            new Machi(15,  "UZI",     4000,  true,  4, "tacz:uzi", 1),   // ★2026-08-20 に個人→勢力
            new Machi(16,  "M4A1",    8000,  true,  4, "tacz:m4a1", 1),
            new Machi(17,  "M107",    12000, true,  4, "tacz:m107", 1),
            // --- 弾 (値段は提案。財布は個人) ---
            //   ★ タブ2枚目・2段目の 9〜14。3段目(18〜26)は名前と価格の帯。
            new Machi(9, "小口径弾", 15, false, 2, "hamster:compact_ammo", 2),
            new Machi(10, "中口径弾", 20, false, 3, "hamster:medium_ammo", 2),
            new Machi(11, "大口径弾", 30, false, 3, "hamster:long_ammo", 2),
            new Machi(12, "9mm弾",   25, false, 4, "tacz:9mm", 2),
            new Machi(13, "5.56mm弾", 40, false, 4, "tacz:556x45", 2),
            new Machi(14, "50BMG弾", 60, false, 4, "tacz:50bmg", 2),
    };

    /**
     * 銃器専門店の解放条件を測る。★ 2026-08-22 に足した。
     *
     * ★★ 門は【2か所】に要る ★★
     *   画面を作る所だけだと、開いたまま放置した古い並びから押せてしまう。
     *   買う手続きだけだと、買えない品が買えるように見えてしまう。
     */
    static void kaihouWoMiru(Class<?> shopCls) throws Exception {
        System.out.println();
        System.out.println("-- 銃器専門店の解放（勢力の貯金 10,000 超え）--");

        Field kf = shopCls.getDeclaredField("JUKI_KAIHOU");
        kf.setAccessible(true);
        int shikii = (Integer) kf.get(null);
        check("★解放に要る貯金が 10,000", shikii == 10000, "実際=" + shikii);

        Method miru = shopCls.getMethod("jukiKaihouMiru",
                Class.forName("jidai.JidaiCraft"), String.class, int.class);
        Method aru = shopCls.getMethod("jukiKaihou", String.class);

        check("はじめは どの勢力も開いていない",
                !(Boolean) aru.invoke(null, "kyuryo"), "開いている");
        check("★ちょうど 10,000 では開かない（「超えた時」なので）",
                !(Boolean) miru.invoke(null, null, "kyuryo", 10000), "開いてしまった");
        check("★10,001 で開く",
                (Boolean) miru.invoke(null, null, "kyuryo", 10001), "開かない");
        check("開いたあとは jukiKaihou が true",
                (Boolean) aru.invoke(null, "kyuryo"), "false");
        check("★★一度開いたら、貯金が 0 になっても閉じない",
                (Boolean) aru.invoke(null, "kyuryo")
                        && !(Boolean) miru.invoke(null, null, "kyuryo", 0),
                "閉じた");
        check("★ほかの勢力は巻き添えで開かない",
                !(Boolean) aru.invoke(null, "shinrin"), "開いた");
        check("勢力が null なら開かない",
                !(Boolean) miru.invoke(null, null, null, 999999), "開いた");

        // ★ 門が両方にあるか。片方だけだと素通りできる。
        String src = null;
        for (String q : new String[]{"../src/main/java/jidai/Shop.java",
                                     "plugin/src/main/java/jidai/Shop.java",
                                     "src/main/java/jidai/Shop.java"}) {
            java.nio.file.Path w = java.nio.file.Paths.get(q);
            if (java.nio.file.Files.exists(w)) {
                src = java.nio.file.Files.readString(w);
                break;
            }
        }
        check("Shop.java が読めた", src != null, "見つからない");
        if (src != null) {
            check("★★買う手続きに門がある",
                    src.contains("if (!jukiAiteru(kane, player))"), "見つからない");
            check("★★画面を作る所にも門がある",
                    src.contains("tsukuru(s, jidai, jukiAiteru("), "見つからない");
        }
    }

    public static void main(String[] a) throws Exception {

        Class<?> shopCls = Class.forName("jidai.Shop");
        kaihouWoMiru(shopCls);
        Field f = shopCls.getDeclaredField("JUKIHIN");
        f.setAccessible(true);
        Object[] shohin = (Object[]) f.get(null);

        System.out.println("銃器専門店の品数: " + shohin.length);

        // 枠の番号で引けるようにしておく
        // ★ タブごとに枠番号が重なる（銃も弾も 9 から）。
        //   (ページ, 枠) で引かないと、別の品を取ってしまう。
        Map<String, Object> waku = new HashMap<>();
        for (Object s : shohin) {
            waku.put(yobu(s, "page") + ":" + yobu(s, "slot"), s);
        }

        for (Machi m : MACHI) {
            Object s = waku.get(m.page() + ":" + m.slot());
            if (s == null) {
                check("枠 " + m.slot() + " に " + m.namae() + " がある", false, "枠が空");
                continue;
            }
            String namae = (String) yobu(s, "namae");
            String mod   = (String) yobu(s, "modItem");
            int nedan    = (Integer) yobu(s, "nedan");
            boolean sei  = (Boolean) yobu(s, "seiryoku");
            int kaikin   = (Integer) yobu(s, "kaikin");

            check("枠" + m.slot() + " " + m.namae() + " の名前",
                    namae.contains(m.namae()), "実際=" + namae);
            check("枠" + m.slot() + " " + m.namae() + " の値段 " + m.nedan(),
                    nedan == m.nedan(), "実際=" + nedan);
            check("枠" + m.slot() + " " + m.namae() + " の財布 "
                            + (m.seiryoku() ? "勢力" : "個人"),
                    sei == m.seiryoku(), "実際=" + (sei ? "勢力" : "個人"));
            check("枠" + m.slot() + " " + m.namae() + " の解禁 " + m.kaikin(),
                    kaikin == m.kaikin(), "実際=" + kaikin);
            check("枠" + m.slot() + " " + m.namae() + " のMOD ID",
                    mod != null && mod.contains(m.modItem()), "実際=" + mod);
        }

        // ---------- 全体の決まり ----------

        // 銃は9丁ちょうど。増えても減っても時代の設計が変わる
        int juu = 0, tama = 0, vanilla = 0;
        for (Object s : shohin) {
            String mod = (String) yobu(s, "modItem");
            if (mod == null) { vanilla++; continue; }
            if (mod.contains("modern_kinetic_gun")) juu++;
            if (mod.contains("tacz:ammo")) tama++;
        }
        check("★銃はちょうど9丁", juu == 9, "実際=" + juu);
        check("★弾はちょうど6種", tama == 6, "実際=" + tama);
        // ★ 銃器専門店にバニラの品は1つも無い。全部 MOD のアイテム。
        check("★銃器専門店はすべて MOD の品 (バニラの品が混ざっていない)",
                vanilla == 0, "実際=" + vanilla);

        // ★ 鉄器(1)に銃は無い。素手と鉄装備の時代を守る
        boolean tekkiNiJuu = false;
        for (Object s : shohin) {
            String mod = (String) yobu(s, "modItem");
            if (mod != null && mod.contains("modern_kinetic_gun")
                    && (Integer) yobu(s, "kaikin") <= 1) {
                tekkiNiJuu = true;
            }
        }
        check("★★鉄器の時代には銃が1丁も無い", !tekkiNiJuu, "鉄器で買える銃がある");

        // ★ 弾は、それを使う銃と同じかそれより前に解禁される。
        //   銃だけ買えて弾が買えない時代があると、置物になる。
        // ★ 弾の一覧は【実装の JUKIHIN】から作る。
        //   期待表(MACHI)から作ると、自分の書いた表を自分で確かめるだけになる。
        //   最初そう書いて、tacz:9mm に "ammo" の字が無いことに気付かず
        //   4件の誤検出を出した。判定に使う字は実物から取ること。
        Map<String, Integer> tamaKaikin = new HashMap<>();
        for (Object s2 : shohin) {
            String mod = (String) yobu(s2, "modItem");
            if (mod == null || !mod.startsWith("tacz:ammo{")) continue;
            int a1 = mod.indexOf('"') + 1;
            int a2 = mod.indexOf('"', a1);
            tamaKaikin.put(mod.substring(a1, a2), (Integer) yobu(s2, "kaikin"));
        }
        check("弾6種の解禁時代を実装から読めた", tamaKaikin.size() == 6,
                "読めた=" + tamaKaikin);
        // 銃 → 使う弾（実ファイルから読んだ対応）
        Map<String, String> tsukau = Map.of(
                "hamster:coltm1851", "hamster:compact_ammo",
                "hamster:colt1873",  "hamster:medium_ammo",
                "hamster:sks",       "hamster:medium_ammo",
                "hamster:m1garand",  "hamster:long_ammo",
                "hamster:madsen",    "hamster:long_ammo",
                "tacz:glock_17",     "tacz:9mm",
                "tacz:uzi",          "tacz:9mm",
                "tacz:m4a1",         "tacz:556x45",
                "tacz:m107",         "tacz:50bmg");
        List<String> okure = new ArrayList<>();
        for (Machi m : MACHI) {
            String t = tsukau.get(m.modItem());
            if (t == null) continue;
            Integer tk = tamaKaikin.get(t);
            if (tk == null || tk > m.kaikin()) {
                okure.add(m.namae() + "→" + t);
            }
        }
        check("★★銃より弾が遅れて解禁される組み合わせが無い",
                okure.isEmpty(), "遅れ=" + okure);

        // ★ MODの品は give で渡す。持ち物へ直接入れていないこと
        //   （Material が MOD のアイテムを表せないため、入れても空になる）
        check("MODの品を渡す仕組みがある",
                hasMethod(shopCls, "watasu"), "watasu が無い");

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }

    /** private record の値を1つ取り出す。 */
    static Object yobu(Object rec, String namae) throws Exception {
        Method m = rec.getClass().getDeclaredMethod(namae);
        m.setAccessible(true);
        return m.invoke(rec);
    }

    static boolean hasMethod(Class<?> c, String namae) {
        for (Method m : c.getDeclaredMethods()) {
            if (m.getName().equals(namae)) return true;
        }
        return false;
    }
}
