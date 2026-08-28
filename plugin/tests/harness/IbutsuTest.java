// =============================================================
// IbutsuTest ── 遺物（効果のある特殊アイテム）を測る
//
//   ★★ なぜ足したか（2026-08-22）★★
//     特殊アイテムが「集める5種」と「遺物11種」の2系統になった。
//     ここで測るもの:
//       A. 表そのもの（11種・時代の割り当て・番号が16個すべて違う・勝利条件は5種のまま）
//       B. 効果の対応（ポーション / 倍率 / 敷居 / 戻し / 盾 / 回復）── 純粋な計算
//       C. 鉄王冠のおまけ（10万回 回して期待値）と、表がデータパックの正本と同じこと
//       D. ガチャの景品表に遺物が全部あり、時代が表と同じ
//       E. 配線（毎秒の処理・売り・解放・戻し・イベント・/joho）── ソースを読むだけ (字)
// =============================================================

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class IbutsuTest {

    static int pass = 0, fail = 0;

    static void check(String label, boolean cond, String detail) {
        if (cond) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + detail); }
    }

    static String src(String... michi) throws Exception {
        for (String q : michi) {
            Path w = Paths.get(q);
            if (Files.exists(w)) {
                return Files.readString(w);
            }
        }
        return null;
    }

    static Object yobu(Object rec, String mei) throws Exception {
        Method m = rec.getClass().getMethod(mei);
        m.setAccessible(true);
        return m.invoke(rec);
    }

    static Object teisuu(Class<?> c, String mei) throws Exception {
        Field f = c.getDeclaredField(mei);
        f.setAccessible(true);
        return f.get(null);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 遺物: 実測 ===");
        System.out.println();

        Class<?> sc = Class.forName("jidai.Shouri");
        Class<?> ic = Class.forName("jidai.Ibutsu");
        String[][] ib = (String[][]) sc.getMethod("ibutsuHyou").invoke(null);
        String[][] tk = (String[][]) sc.getMethod("tokushuHyou").invoke(null);
        String[] katsu = (String[]) sc.getMethod("tokushuMei").invoke(null);

        // ---------- A. 表 ----------
        System.out.println("-- A. 表 --");
        check("遺物は 11 種", ib.length == 11, "実際=" + ib.length);
        check("★勝利条件の「集める」は 5 種のまま（遺物は入らない）", katsu.length == 5, "実際=" + katsu.length);
        int[] jidaiKazu = new int[5];
        Set<Integer> ban = new TreeSet<>();
        Set<String> na = new HashSet<>();
        Set<String> fai = new HashSet<>();
        List<String> warui = new ArrayList<>();
        for (String[] r : ib) {
            jidaiKazu[Integer.parseInt(r[3])]++;
            if (!ban.add(Integer.parseInt(r[2]))) warui.add("番号の重なり: " + r[0]);
            if (!na.add(r[0])) warui.add("名前の重なり: " + r[0]);
            if (!fai.add(r[1])) warui.add("絵のファイル名の重なり: " + r[0]);
            if (r[5] == null || r[5].isEmpty()) warui.add("説明が無い: " + r[0]);
        }
        for (String[] r : tk) {
            if (!ban.add(Integer.parseInt(r[2]))) warui.add("集める物と番号が重なる: " + r[0]);
            if (!na.add(r[0])) warui.add("集める物と名前が重なる: " + r[0]);
            if (!fai.add(r[1])) warui.add("集める物と絵のファイル名が重なる: " + r[0]);
        }
        check("★16 種の名前・絵のファイル名・見た目の番号が全部 違う", warui.isEmpty(), String.valueOf(warui));
        check("時代ごとの遺物の数: 鉄器2 / 中世3 / 近代3 / 現代3",
                jidaiKazu[1] == 2 && jidaiKazu[2] == 3 && jidaiKazu[3] == 3 && jidaiKazu[4] == 3,
                Arrays.toString(jidaiKazu));
        Method mModel = sc.getMethod("tokushuModelData", String.class);
        boolean zenbu = true;
        for (String[] r : ib) {
            zenbu &= (Integer) mModel.invoke(null, r[0]) == Integer.parseInt(r[2]);
        }
        check("tokushuModelData が遺物の番号も返す（絵の差し替え・勢力ごと1個 に乗る）", zenbu, "返さない物がある");
        check("遺物でない名前は 遺物ではない", sc.getMethod("ibutsuKouka", String.class).invoke(null, "パン") == null, "パンが遺物");
        // 効果の種類は Ibutsu の定数に必ずある（字を打ち間違えると効果が黙って消える）
        Set<String> shurui = new HashSet<>();
        for (String k : new String[]{"SPEED", "HORI", "STRENGTH", "RESISTANCE", "JUKI", "HASTE", "URI", "NEBIKI", "REGEN", "NIGHT", "JOHO"}) {
            shurui.add((String) teisuu(ic, k));
        }
        List<String> shiranai = new ArrayList<>();
        for (String[] r : ib) {
            if (!shurui.contains(r[4])) shiranai.add(r[0] + "=" + r[4]);
        }
        check("★表の効果の種類は、すべて Ibutsu の定数にある", shiranai.isEmpty(), String.valueOf(shiranai));

        // ---------- B. 効果の対応 ----------
        System.out.println();
        System.out.println("-- B. 効果 --");
        Method mPotion = ic.getDeclaredMethod("potion", String.class);
        mPotion.setAccessible(true);
        int potionAri = 0;
        for (String[] r : ib) {
            if (mPotion.invoke(null, r[4]) != null) potionAri++;
        }
        check("ポーションで出す効果は 3 種（速度・採掘・暗視）", potionAri == 3, "実際=" + potionAri);
        check("知らない効果は null", mPotion.invoke(null, "NANIKA") == null, "null でない");
        check("★攻撃力は +10%（属性の修正子）", Math.abs((Double) teisuu(ic, "KOUGEKI_WARI") - 0.10) < 1e-9, String.valueOf(teisuu(ic, "KOUGEKI_WARI")));
        check("★盾は −15%（×0.85）", Math.abs((Double) teisuu(ic, "BOUGYO_WARI") - 0.85) < 1e-9, String.valueOf(teisuu(ic, "BOUGYO_WARI")));
        check("★回復は +20%（×1.2）", Math.abs((Double) teisuu(ic, "KAIFUKU_BAI") - 1.2) < 1e-9, String.valueOf(teisuu(ic, "KAIFUKU_BAI")));

        Method mKiroku = ic.getDeclaredMethod("kiroku", String.class, String.class);
        mKiroku.setAccessible(true);
        Method mRisetto = ic.getDeclaredMethod("risetto");
        mRisetto.setAccessible(true);
        Method mUri = ic.getMethod("uriBairitsu", String.class);
        Method mJuki = ic.getMethod("jukiShikii", String.class);
        Method mNebiki = ic.getMethod("nebikiModori", String.class, int.class);
        Method mJoho = ic.getMethod("johoMieru", String.class);
        Method mMotteru = ic.getMethod("motteru", String.class, String.class);

        mRisetto.invoke(null);
        check("何も持っていない勢力: 売値 100% / 敷居 10000 / 戻し 0 / 情報は見えない",
                (Integer) mUri.invoke(null, "丘陵") == 100 && (Integer) mJuki.invoke(null, "丘陵") == 10000
                        && (Integer) mNebiki.invoke(null, "丘陵", 800) == 0 && !(Boolean) mJoho.invoke(null, "丘陵"),
                "違う");
        check("勢力が無い人の売値は 100%", (Integer) mUri.invoke(null, new Object[]{null}) == 100, "違う");

        mKiroku.invoke(null, "丘陵", "ロスチャイルドの金庫");
        mKiroku.invoke(null, "森林", "武器商人の手形");
        mKiroku.invoke(null, "川", "死の商人の手引書");
        mKiroku.invoke(null, "内海", "スマートフォン");
        mKiroku.invoke(null, "森林", "パン");      // 遺物でない物は無視される
        check("★金庫: 売値 120%（持たない勢力は 100%）",
                (Integer) mUri.invoke(null, "丘陵") == 120 && (Integer) mUri.invoke(null, "森林") == 100, "違う");
        check("★手形: 敷居 5500", (Integer) mJuki.invoke(null, "森林") == 5500, String.valueOf(mJuki.invoke(null, "森林")));
        check("★手引書: 800 の銃で 80 戻る（持たない勢力は 0）",
                (Integer) mNebiki.invoke(null, "川", 800) == 80 && (Integer) mNebiki.invoke(null, "丘陵", 800) == 0, "違う");
        check("★スマートフォン: 情報が見える（持たない勢力は見えない）",
                (Boolean) mJoho.invoke(null, "内海") && !(Boolean) mJoho.invoke(null, "丘陵"), "違う");
        check("遺物でない名前は記録されない", !(Boolean) mMotteru.invoke(null, "森林", "パン"), "記録された");
        mRisetto.invoke(null);
        check("リセットで消える", (Integer) mUri.invoke(null, "丘陵") == 100, "残っている");

        // ---------- C. 鉄王冠のおまけ ----------
        System.out.println();
        System.out.println("-- C. 鉄王冠（10万回）--");
        Method mOmake = ic.getDeclaredMethod("horiOmake", String.class, boolean.class, Random.class);
        mOmake.setAccessible(true);
        Random r = new Random(11);
        int nashi = 0;
        for (int i = 0; i < 10000; i++) {
            nashi += ((List<?>) mOmake.invoke(null, "丘陵", false, r)).size();
        }
        check("★王冠が無ければ おまけは 0", nashi == 0, "出た=" + nashi);
        mKiroku.invoke(null, "丘陵", "ロンバルディアの鉄王冠");
        int tetsu = 0, dia = 0, tetsuShinso = 0;
        final int N = 100000;
        for (int i = 0; i < N; i++) {
            for (Object m : (List<?>) mOmake.invoke(null, "丘陵", false, r)) {
                if (m.toString().equals("IRON_INGOT")) tetsu++;
                if (m.toString().equals("DIAMOND")) dia++;
            }
            for (Object m : (List<?>) mOmake.invoke(null, "丘陵", true, r)) {
                if (m.toString().equals("IRON_INGOT")) tetsuShinso++;
            }
        }
        double tetsuWari = tetsu * 100.0 / N;         // 期待 0.2 × 15% = 3.0%
        double diaWari = dia * 100.0 / N;             // 期待 0.2 × 1% = 0.2%
        double shinsoWari = tetsuShinso * 100.0 / N;  // 期待 0.2 × 22.5% = 4.5%
        check("★王冠の おまけで鉄が 約3%（=本来の15%の2割。合わせて1.2倍）", tetsuWari > 2.6 && tetsuWari < 3.4,
                String.format("%.2f%%", tetsuWari));
        check("★ダイヤは 約0.2%", diaWari > 0.12 && diaWari < 0.3, String.format("%.3f%%", diaWari));
        check("★深層岩は 1.5倍（鉄 約4.5%）", shinsoWari > 4.0 && shinsoWari < 5.0, String.format("%.2f%%", shinsoWari));
        System.out.println(String.format("      鉄 %.2f%% / ダイヤ %.3f%% / 深層岩の鉄 %.2f%%", tetsuWari, diaWari, shinsoWari));
        mRisetto.invoke(null);

        // ★★ 表がデータパックの正本（tests/horu_tsukuru.py）と同じか ★★
        String py = src("tests/horu_tsukuru.py", "../../tests/horu_tsukuru.py", "../tests/horu_tsukuru.py");
        check("tests/horu_tsukuru.py が読めた", py != null, "見つからない");
        if (py != null) {
            Object[][] hyou = (Object[][]) teisuu(ic, "HORI_HYOU");
            List<String> zure = new ArrayList<>();
            for (Object[] g : hyou) {
                String mono = "minecraft:" + g[0].toString().toLowerCase();
                Matcher mt = Pattern.compile("\\('" + mono + "',\\s*([0-9.]+)\\)").matcher(py);
                if (!mt.find()) {
                    zure.add(mono + " が正本に無い");
                } else if (Math.abs(Double.parseDouble(mt.group(1)) * 100 - (Integer) g[1]) > 1e-6) {
                    zure.add(mono + " 正本=" + mt.group(1) + " 王冠=" + g[1] + "%");
                }
            }
            Matcher mb = Pattern.compile("SHINSO_BAI = ([0-9.]+)").matcher(py);
            if (mb.find() && Math.abs(Double.parseDouble(mb.group(1)) - (Double) teisuu(ic, "HORI_SHINSO_BAI")) > 1e-9) {
                zure.add("深層岩の倍率が違う");
            }
            check("★★王冠の表が、データパックの正本（horu_tsukuru.py）と同じ", zure.isEmpty(), String.valueOf(zure));
        }

        // ---------- D. ガチャの景品表 ----------
        System.out.println();
        System.out.println("-- D. ガチャの景品表 --");
        Class<?> gc = Class.forName("jidai.Gacha");
        Field kf = gc.getDeclaredField("KEIHIN");
        kf.setAccessible(true);
        Object[] hyou = (Object[]) kf.get(null);
        List<String> nai = new ArrayList<>();
        for (String[] rr : ib) {
            boolean aru = false;
            for (Object k : hyou) {
                if (yobu(k, "namae").equals(rr[0]) && yobu(k, "jidai").equals(Integer.parseInt(rr[3]))
                        && (Integer) yobu(k, "kane") == 0) {
                    aru = true;
                }
            }
            if (!aru) nai.add(rr[0] + "(時代" + rr[3] + ")");
        }
        check("★遺物11種がガチャの景品表に、表と同じ時代で入っている", nai.isEmpty(), "無い: " + nai);
        for (int j = 1; j <= 4; j++) {
            int g = 0;
            for (Object k : hyou) {
                if ((Integer) yobu(k, "jidai") == j) g += (Integer) yobu(k, "omomi");
            }
            check("時代 " + j + " の重みの合計は 10000 のまま", g == 10000, "実際=" + g);
        }
        // ご指示の確率: 戦車 0.8% / 王冠 0.7% / 他 0.5%
        Map<String, Integer> omomi = new HashMap<>();
        for (Object k : hyou) {
            omomi.put((String) yobu(k, "namae") + "@" + yobu(k, "jidai"), (Integer) yobu(k, "omomi"));
        }
        check("★確率: 戦車 0.8% / 鉄王冠 0.7% / 他の9種 0.5%",
                omomi.getOrDefault("ヒッタイトの戦車@1", 0) == 80 && omomi.getOrDefault("ロンバルディアの鉄王冠@1", 0) == 70
                        && omomi.getOrDefault("聖剣エクスカリバー@2", 0) == 50 && omomi.getOrDefault("スマートフォン@4", 0) == 50
                        && omomi.getOrDefault("死の商人の手引書@3", 0) == 50,
                String.valueOf(omomi));

        // ---------- E. 配線 (字) ----------
        System.out.println();
        System.out.println("-- E. 配線 --");
        String j = src("plugin/src/main/java/jidai/JidaiCraft.java", "../src/main/java/jidai/JidaiCraft.java", "src/main/java/jidai/JidaiCraft.java");
        String g = src("plugin/src/main/java/jidai/Ginko.java", "../src/main/java/jidai/Ginko.java", "src/main/java/jidai/Ginko.java");
        String s = src("plugin/src/main/java/jidai/Shop.java", "../src/main/java/jidai/Shop.java", "src/main/java/jidai/Shop.java");
        String ga = src("plugin/src/main/java/jidai/Gacha.java", "../src/main/java/jidai/Gacha.java", "src/main/java/jidai/Gacha.java");
        String yml = src("plugin/src/main/resources/plugin.yml", "../src/main/resources/plugin.yml", "src/main/resources/plugin.yml");
        check("(字) 毎秒の処理で効果をかけ直している", j != null && j.contains("Ibutsu.byoumai(kane);"), "無い");
        check("(字) 盾と回復のイベントがつながっている",
                j != null && j.contains("Ibutsu.damageUketa(event, kane);") && j.contains("Ibutsu.kaifuku(event, kane);"), "無い");
        check("(字) 鉄王冠のおまけが石を掘った時に落ちる", j != null && j.contains("Ibutsu.horiOmake(kane.kinkoMei(event.getPlayer())"), "無い");
        check("(字) /joho が Ibutsu.joho へ", j != null && j.contains("Ibutsu.joho(sender, p6, kane);") && yml != null && yml.contains("  joho:"), "無い");
        check("(字) 銀行の売りが金庫の倍率を見る", g != null && g.contains("Ibutsu.uriBairitsu(kane.kinkoMei(player))"), "無い");
        check("(字) 銃器専門店の解放が手形の敷居を見る", s != null && s.contains("Ibutsu.jukiShikii(kuni)"), "無い");
        check("(字) 手引書の戻しは【銃器専門店だけ】で、個人と勢力の両方の財布に戻す",
                s != null && s.contains("if (!Basho.JUKI.equals(shurui)) {\n            return;\n        }\n        String kinko = kane.kinkoMei(player);\n        int modori = Ibutsu.nebikiModori(kinko, s.nedan());")
                        && s.contains("tebikiModosu(player, kane, s, false);") && s.contains("tebikiModosu(player, kane, s, true);"),
                "配線が足りない");
        check("(字) ガチャが記録した瞬間に遺物の台帳へ写す", ga != null && ga.contains("Ibutsu.kiroku(kuni, k.namae());"), "無い");
        check("(字) 知らせは【渡す時】（抽選の時ではない）",
                ga != null && ga.contains("Ibutsu.teniireta(player, kane, atari.namae());")
                        && ga.contains("Ibutsu.teniireta(player, j.kane, k.namae());")
                        && !ga.substring(ga.indexOf("private Keihin kisei("), ga.indexOf("private Keihin kiseiMiru(")).contains("teniireta"),
                "抽選の時に知らせている");

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
