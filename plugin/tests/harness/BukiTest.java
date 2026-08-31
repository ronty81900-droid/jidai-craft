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
            // --- 銃5丁 (タブ1枚目・2段目の 9〜13) ---
            //   ★★ 2026-08-30: 9丁6種 → 5丁1種に整理 ★★
            //     「どれが強いか分からない」「銃と弾の組み合わせを覚えられない」
            //     の2つを消すため。呼び名を役割にして、実銃名は説明の行へ回した。
            //   ★ 並びは【毎秒の威力】を実物のパックから測って決めた。
            //     18 → 45 → 60 → 79 → 108 と必ず上がる。
            new Machi(9,  "拳銃",     800,   true,  2, "hamster:nagantm1895", 1),
            new Machi(10, "小銃",     3000,  true,  3, "hamster:sks", 1),
            new Machi(11, "連射銃",   5000,  true,  3, "hamster:mp18", 1),
            new Machi(12, "自動小銃", 8000,  true,  4, "tacz:m4a1", 1),
            new Machi(13, "狙撃銃",   12000, true,  4, "tacz:ai_awp", 1),
            // --- 弾1種 (財布は個人) ---
            //   ★ 弾を1種類にしたので、どの銃にも同じ物が使える。
            //     組み合わせを間違えて撃てない、が起きない。
            new Machi(9, "弾", 20, false, 2, "tacz:556x45", 2),
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
        // ★ 2026-08-30: 9丁6種 → 5丁1種。MACHI の数から見るようにした。
        //   数を2か所に書くと、片方だけ直した時に黙って食い違う。
        int machiJuu = 0, machiTama = 0;
        for (Machi m : MACHI) {
            if (m.page() == 1) machiJuu++; else machiTama++;
        }
        check("★銃は正本(MACHI)と同じ丁数", juu == machiJuu,
                "実装=" + juu + " / 正本=" + machiJuu);
        check("★弾は正本(MACHI)と同じ種類数", tama == machiTama,
                "実装=" + tama + " / 正本=" + machiTama);
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
        check("弾の解禁時代を実装から読めた", tamaKaikin.size() == machiTama,
                "読めた=" + tamaKaikin);
        // 銃 → 使う弾（実ファイルから読んだ対応）
        // ★★ 2026-08-30: 弾を1種類に統一した ★★
        //   tools/jyu_chousei.py がパック側の "ammo" を書き換えている。
        //   ここは(字)の控え。**実物と突き合わせるのは
        //   tests/buki_kakunin.py の仕事**（あちらはパックを直接 読む）。
        Map<String, String> tsukau = Map.of(
                "hamster:nagantm1895", "tacz:556x45",
                "hamster:sks",         "tacz:556x45",
                "hamster:mp18",        "tacz:556x45",
                "tacz:m4a1",           "tacz:556x45",
                "tacz:ai_awp",         "tacz:556x45");
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
