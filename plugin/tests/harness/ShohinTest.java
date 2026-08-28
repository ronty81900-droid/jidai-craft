// =============================================================
// ShohinTest ── 販売所（銃器専門店ではない方）の品揃えを数える
//
//   2026-08-20 に足した防具12点を、1つずつ突き合わせる。
//   ★ ピッケルの販売は同日に取りやめた。掘る道具は「時代のツルハシ」
//     1本きり（配る・壊れない・死んでも落とさない）。TsuruhashiTest が見る。
//   ★ここが狂うと「時代を進めても装備が良くならない」ことになり、
//     時代進行そのものの意味が消える。
//
//   銃と弾は別の店なので BukiTest が見る。
// =============================================================

import java.lang.reflect.*;
import java.util.*;

public class ShohinTest {

    static int pass = 0, fail = 0;

    static void check(String label, boolean cond, String detail) {
        if (cond) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + detail); }
    }

    /** 期待する1行。枠 / 名前に必ず入る字 / 値段 / 勢力の金か / 解禁の時代 */
    record Machi(int slot, String namae, int nedan, boolean seiryoku, int kaikin) { }

    /**
     * ★ ご指示どおりの割り当て。値段は提案。
     *
     * ★★ 2026-08-21: タブを入れた ★★
     *   1段目(0〜8) はタブ専用。商品は2段目から。
     *   タブ1「生活」= パン/石炭/戦争宣誓/下剋上
     *   タブ2「防具」= チェーン4・鉄4・ダイヤ4
     *   タブ3「武器」= 鉄の剣・弓・矢・ダイヤの剣・クロスボウ
     *   枠番号はタブごとに 9 から振り直される（重なってよい）。
     *
     * ★★ 2026-08-22: 剣を専用のタブ「武器」へ移した ★★
     */
    static final Machi[] MACHI = {
            // --- 生活と勢力の行為 ---
            //   ★ 2026-08-22: 6品 足した（ご指示）。並びは時代順。
            new Machi(9, "パン",         2,   false, 1),
            new Machi(10, "石炭",         5,   false, 1),
            new Machi(11, "オークの原木", 10,  false, 1),
            new Machi(12, "石レンガ",     12,  false, 1),
            new Machi(13, "松明",         5,   false, 1),
            new Machi(27, "焼肉",         30,  false, 2),
            new Machi(28, "ガラス",       25,  false, 2),
            new Machi(29, "戦争宣誓",     100, true,  2),
            new Machi(30, "金リンゴ",     100, false, 3),
            new Machi(31, "下剋上",       150, true,  3),

            // --- 武器（タブ3）★ 矢だけ個人の金 ---
            new Machi(9, "鉄の剣",       100, true,  1),
            new Machi(10, "弓",          150, true,  1),
            new Machi(11, "矢",           30, false, 1),
            new Machi(12, "ダイヤの剣",  500, true,  2),
            new Machi(13, "クロスボウ",  150, true,  2),

            // --- 防具: 時代ごとにランクアップ（勢力の金） ---
            new Machi(9, "チェーンの兜",     15, true, 1),
            new Machi(10, "チェーンの胸当て", 25, true, 1),
            new Machi(11, "チェーンの脚当て", 20, true, 1),
            new Machi(12, "チェーンの靴",     10, true, 1),
            new Machi(14, "鉄の兜",           25, true, 2),
            new Machi(15, "鉄の胸当て",       30, true, 2),
            new Machi(16, "鉄の脚当て",       30, true, 2),
            new Machi(17, "鉄の靴",           20, true, 2),
            // ★★ ダイヤは胸当てだけ近代、残りは現代 ★★
            //   ★ 2026-08-21: 帯を作るため 27〜30 → 36〜39 へ移した。
            new Machi(28, "ダイヤの胸当て",   500, true, 3),
            new Machi(27, "ダイヤの兜",       300, true, 4),
            new Machi(29, "ダイヤの脚当て",   400, true, 4),
            new Machi(30, "ダイヤの靴",       200, true, 4),
    };

    public static void main(String[] a) throws Exception {

        Class<?> shopCls = Class.forName("jidai.Shop");
        Field f = shopCls.getDeclaredField("IPPAN");
        f.setAccessible(true);
        Object[] shohin = (Object[]) f.get(null);
        System.out.println("販売所の品数: " + shohin.length);

        // ★ タブごとに枠番号が重なるので、(ページ, 枠) で引く
        Map<String, Object> waku = new HashMap<>();
        for (Object s : shohin) {
            waku.put(yobu(s, "page") + ":" + yobu(s, "slot"), s);
        }

        for (Machi m : MACHI) {
            Object s = waku.get(pageOf(m.namae()) + ":" + m.slot());
            if (s == null) {
                check("枠 " + m.slot() + " に " + m.namae(), false, "枠が空");
                continue;
            }
            check("枠" + m.slot() + " " + m.namae() + " の名前",
                    ((String) yobu(s, "namae")).contains(m.namae()),
                    "実際=" + yobu(s, "namae"));
            check("枠" + m.slot() + " " + m.namae() + " の値段 " + m.nedan(),
                    (Integer) yobu(s, "nedan") == m.nedan(), "実際=" + yobu(s, "nedan"));
            check("枠" + m.slot() + " " + m.namae() + " の財布 "
                            + (m.seiryoku() ? "勢力" : "個人"),
                    (Boolean) yobu(s, "seiryoku") == m.seiryoku(),
                    "実際=" + yobu(s, "seiryoku"));
            check("枠" + m.slot() + " " + m.namae() + " の解禁 " + m.kaikin(),
                    (Integer) yobu(s, "kaikin") == m.kaikin(), "実際=" + yobu(s, "kaikin"));
        }

        check("販売所の品数が " + MACHI.length, shohin.length == MACHI.length,
                "実際=" + shohin.length);

        // ---------- 全体の決まり ----------

        // ★ 販売所に MOD の品は無い。銃と弾は銃器専門店だけ。
        int mod = 0;
        for (Object s : shohin) {
            if (yobu(s, "modItem") != null) mod++;
        }
        check("★★販売所に銃と弾は無い（別の店にある）", mod == 0, "実際=" + mod);

        // ★★ ピッケルは1本も売っていない ★★
        //   掘る道具は配る「時代のツルハシ」だけ。売ると
        //   「金が無い＝掘れない＝金が稼げない」の詰み方が起きる。
        Set<Integer> aJidai = new TreeSet<>();
        List<String> pikkeru = new ArrayList<>();
        for (Object s : shohin) {
            String n = (String) yobu(s, "namae");
            if (n.contains("ピッケル")) pikkeru.add(n);
            if (n.contains("兜") || n.contains("胸当て") || n.contains("脚当て")
                    || n.contains("靴")) aJidai.add((Integer) yobu(s, "kaikin"));
        }
        check("★★ピッケルは1本も売っていない", pikkeru.isEmpty(), String.valueOf(pikkeru));
        check("★★全4時代に防具がある", aJidai.equals(Set.of(1, 2, 3, 4)),
                "実際=" + aJidai);

        // ★ 防具は勢力の金。取り違えると財布の設計が崩れる。
        List<String> chigau = new ArrayList<>();
        for (Object s : shohin) {
            String n = (String) yobu(s, "namae");
            boolean sei = (Boolean) yobu(s, "seiryoku");
            if ((n.contains("兜") || n.contains("胸当て") || n.contains("脚当て")
                    || n.contains("靴")) && !sei) chigau.add(n + "=個人");
        }
        check("★防具は勢力の金", chigau.isEmpty(), String.valueOf(chigau));

        check("★防具は時代が上がるほど高い（胸当てで比べる）",
              nedanOf(shohin, "チェーンの胸当て") < nedanOf(shohin, "鉄の胸当て")
                      && nedanOf(shohin, "鉄の胸当て") < nedanOf(shohin, "ダイヤの胸当て"),
              "鎖=" + nedanOf(shohin, "チェーンの胸当て")
                      + " 鉄=" + nedanOf(shohin, "鉄の胸当て")
                      + " ダ=" + nedanOf(shohin, "ダイヤの胸当て"));

        // ★★ ダイヤは胸当てだけ近代、残り3点は現代 ★★
        check("★★ダイヤの胸当てだけ近代(3)で解禁",
                kaikinOf(shohin, "ダイヤの胸当て") == 3, "実際=" + kaikinOf(shohin, "ダイヤの胸当て"));
        check("★★ダイヤの兜・脚当て・靴は現代(4)",
                kaikinOf(shohin, "ダイヤの兜") == 4
                        && kaikinOf(shohin, "ダイヤの脚当て") == 4
                        && kaikinOf(shohin, "ダイヤの靴") == 4,
                "兜=" + kaikinOf(shohin, "ダイヤの兜")
                        + " 脚=" + kaikinOf(shohin, "ダイヤの脚当て")
                        + " 靴=" + kaikinOf(shohin, "ダイヤの靴"));

        // ---------- タブの決まり ----------
        System.out.println();
        System.out.println("-- タブ --");
        Field tf = shopCls.getDeclaredField("TAB_IPPAN");
        tf.setAccessible(true);
        Object[] tabI = (Object[]) tf.get(null);
        Field tg = shopCls.getDeclaredField("TAB_JUKI");
        tg.setAccessible(true);
        Object[] tabJ = (Object[]) tg.get(null);

        check("★販売所のタブは3枚（生活・防具・武器）", tabI.length == 3,
                "実際=" + tabI.length);
        check("★銃器専門店のタブは2枚", tabJ.length == 2, "実際=" + tabJ.length);

        // ★★ 1段目(0〜8)はタブ専用。商品を置くと押せなくなる ★★
        List<String> kasanari = new ArrayList<>();
        for (Object t : tabI) {
            int ts = (Integer) yobu(t, "slot");
            if (ts < 0 || ts > 8) kasanari.add("タブが1段目の外: " + ts);
            for (Object x : shohin) {
                if ((Integer) yobu(x, "slot") == ts) {
                    kasanari.add("タブと商品が同じ枠: " + ts + " " + yobu(x, "namae"));
                }
            }
        }
        check("★★タブの枠に商品が重なっていない", kasanari.isEmpty(), String.valueOf(kasanari));

        List<String> ichidanme = new ArrayList<>();
        for (Object x : shohin) {
            if ((Integer) yobu(x, "slot") <= 8) ichidanme.add((String) yobu(x, "namae"));
        }
        check("★★1段目(0〜8)に商品を置いていない", ichidanme.isEmpty(),
                String.valueOf(ichidanme));

        // ★ 各ページに商品があること（空のタブを作らない）
        Set<Integer> pages = new TreeSet<>();
        for (Object x : shohin) pages.add((Integer) yobu(x, "page"));
        check("★どのタブにも商品がある", pages.equals(Set.of(1, 2, 3)),
                String.valueOf(pages));

        // ★ 同じページの中で枠が重なっていない
        Set<String> mita = new HashSet<>();
        List<String> daburi = new ArrayList<>();
        for (Object x : shohin) {
            String k = yobu(x, "page") + ":" + yobu(x, "slot");
            if (!mita.add(k)) daburi.add(k + " " + yobu(x, "namae"));
        }
        check("★★同じタブの中で枠が重なっていない", daburi.isEmpty(), String.valueOf(daburi));

        // ★ ページごとに見出しの絵が違う（どのタブを見ているか分かるように）
        String es = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Files.exists(java.nio.file.Paths.get(
                        "..", "src", "main", "java", "jidai", "Enshutsu.java"))
                        ? java.nio.file.Paths.get("..", "src", "main", "java", "jidai",
                                                  "Enshutsu.java")
                        : java.nio.file.Paths.get("plugin", "src", "main", "java", "jidai",
                                                  "Enshutsu.java")), "UTF-8");
        check("★ページ2用の見出しが2つある",
                es.contains("UI_MISE_2") && es.contains("UI_JUKI_2"), "無い");



        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }

    /**
     * その品がどのタブに出るか。名前で決まる。
     *
     * ★ 剣は「剣」で見る。「鉄」で見ると、タブ2の「鉄の兜」まで
     *   剣だと判定してしまう（防具にも鉄とダイヤがあるため）。
     */
    static int pageOf(String namae) {
        // ★ 武器タブ。「ボウ」で見ると銃器専門店の代用アイコンとは無関係だが、
        //   販売所の中には他に「弓」「矢」を含む品が無いので、これで一意に決まる。
        if (namae.contains("剣") || namae.contains("弓")
                || namae.contains("矢") || namae.contains("クロスボウ")) {
            return 3;
        }
        // ★ 防具は部位の名前で見る。生活の品は増えるので、こちらを列挙しない。
        if (namae.contains("兜") || namae.contains("胸当て")
                || namae.contains("脚当て") || namae.contains("靴")) {
            return 2;
        }
        return 1;
    }

    static int nedanOf(Object[] shohin, String namae) throws Exception {
        for (Object s : shohin) {
            if (yobu(s, "namae").equals(namae)) return (Integer) yobu(s, "nedan");
        }
        return -1;
    }

    static int kaikinOf(Object[] shohin, String namae) throws Exception {
        for (Object s : shohin) {
            if (yobu(s, "namae").equals(namae)) return (Integer) yobu(s, "kaikin");
        }
        return -1;
    }

    /** private record の値を1つ取り出す。 */
    static Object yobu(Object rec, String namae) throws Exception {
        Method m = rec.getClass().getDeclaredMethod(namae);
        m.setAccessible(true);
        return m.invoke(rec);
    }

    /** その名前のファイルを、候補の場所から探して読む。見つからなければ null。 */
    static String yomuMoshi(String... michi) {
        for (String m : michi) {
            java.nio.file.Path w = java.nio.file.Path.of(m);
            if (java.nio.file.Files.exists(w)) {
                try {
                    return java.nio.file.Files.readString(w);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

}
