// =============================================================
// RyakudatsuTest ── 略奪で「集める5種」を奪う（2026-08-23 のご指示・案D）
//
//   ★★ ここで測れること / 測れないこと ★★
//     測れる  … 奪ってよい名前かの見分け（Shouri.atsumeruNoNamae）
//               成立の合図が本当に合図になるか（データパックの並びを実物で読む）
//               配線（誰から奪うか・いつ呼ぶか）を字で
//     測れない… 紙が本当に持ち物を移るか。
//               この環境は Bukkit.getItemFactory() が null で、
//               ItemStack.getItemMeta() が動かない（Stage4bTest / TsuruhashiTest の実測）。
//               → docs/実機確認リスト.md に人の目で見る物として残してある。
// =============================================================

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RyakudatsuTest {

    static int pass = 0, fail = 0;

    static void check(String label, boolean cond, String detail) {
        if (cond) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + detail); }
    }

    static String src(String... michi) throws IOException {
        Path[] soko = {
                Paths.get("..", ".."),      // plugin/tests から
                Paths.get("."),             // 時代クラフト から
                Paths.get(".."),
        };
        for (Path ne : soko) {
            Path p = ne;
            for (String m : michi) p = p.resolve(m);
            if (Files.exists(p)) return new String(Files.readAllBytes(p), "UTF-8");
        }
        throw new FileNotFoundException(String.join("/", michi) + " が見つからない (cwd="
                + Paths.get("").toAbsolutePath() + ")");
    }

    static String java(String namae) throws IOException {
        return src("plugin", "src", "main", "java", "jidai", namae);
    }

    /**
     * moji の中で、saki が ato より前にあるか。
     *
     * ★★ indexOf をそのまま比べない（2026-08-23）★★
     *   見つからないと -1 が返り、-1 < 何か は必ず真になる。
     *   片方を消す壊し方に対して、順番の検査が黙って素通りする
     *   （実際に「受け取れるかを先に見る」を消して落ちなかった）。
     *   両方あることを先に見る。
     */
    static boolean saki(String moji, String saki, String ato) {
        int a = moji.indexOf(saki);
        int b = moji.indexOf(ato);
        return a >= 0 && b >= 0 && a < b;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 略奪で特殊アイテムを奪う（案D） ===");

        // =========================================================
        //  A. 奪ってよい名前かの見分け（実物を呼ぶ）
        // =========================================================
        System.out.println();
        System.out.println("-- A. 奪えるのは「集める5種」だけ --");

        Class<?> sc = Class.forName("jidai.Shouri");
        Method miwake = sc.getDeclaredMethod("atsumeruNoNamae", String.class);
        miwake.setAccessible(true);

        // 集める5種は Shouri が正本。ここに名前を並べ直さない。
        String[] atsumeru = (String[]) sc.getMethod("tokushuMei").invoke(null);
        check("集める5種を読めた（" + atsumeru.length + "種）", atsumeru.length > 0,
                "0種");
        for (String mei : atsumeru) {
            // ★ 実際に渡される紙は色の記号が頭に付く（§b）。その形で見分けられるか。
            check("★「" + mei + "」は奪える（色の記号つきでも）",
                    mei.equals(miwake.invoke(null, "§b" + mei)), "見分けられない");
        }

        // 遺物は奪えない。★ 名前は Shouri.ibutsuHyou() が正本。
        String[][] ibutsu = (String[][]) sc.getMethod("ibutsuHyou").invoke(null);
        check("遺物の表を読めた（" + ibutsu.length + "種）", ibutsu.length > 0, "0種");
        for (String[] r : ibutsu) {
            check("★★遺物「" + r[0] + "」は奪えない（効果は勢力に付いていて紙では移らない）",
                    miwake.invoke(null, "§b" + r[0]) == null, "奪えてしまう");
        }

        check("名前の無い紙は奪えない", miwake.invoke(null, (Object) null) == null, "奪えてしまう");
        check("関係のない名前の紙は奪えない",
                miwake.invoke(null, "§b ただの手紙") == null, "奪えてしまう");

        // =========================================================
        //  B. 成立の合図（データパックの実物を読む）
        // =========================================================
        System.out.println();
        System.out.println("-- B. 「奪えた」の合図が本当に合図になっているか --");

        String ryaku = src("datapacks", "jidai_craft", "data", "jidai", "functions",
                "sensou", "ryakudatsu.mcfunction");
        int kan = ryaku.indexOf("scoreboard players operation @s ryakudatsu_kan");
        check("★ryakudatsu.mcfunction が ryakudatsu_kan を置いている", kan >= 0, "置いていない");

        // ★★ ここが要 ★★
        //   プラグインは「押す前 0 → 押した後 1以上」で成立を見分ける。
        //   断る筋（準備中・クールダウン・上限）が、置く【前】に return 0 で
        //   引き返していないと、この見分けは成り立たない。
        int saigoNoReturn = ryaku.lastIndexOf("run return 0");
        check("★★断る筋はすべて ryakudatsu_kan を置く前に引き返している",
                saigoNoReturn >= 0 && kan > saigoNoReturn,
                "return 0 の位置=" + saigoNoReturn + " / 置く位置=" + kan);

        String kaneSrc = java("Kane.java");
        check("★Kane が読むのは同じ目的名 ryakudatsu_kan",
                kaneSrc.contains("RYAKUDATSU_KAN = \"ryakudatsu_kan\""), "違う名前を読んでいる");
        check("(字) ryakudatsuKan は yomu で読む（目的が無ければ 0）",
                kaneSrc.contains("return yomu(RYAKUDATSU_KAN, player.getName());"), "違う");

        // =========================================================
        //  C. 配線（字で見る）
        // =========================================================
        System.out.println();
        System.out.println("-- C. 誰から、いつ奪うか --");

        String gsrc = java("Ginko.java");
        check("★★(字) 押す前のクールダウンを控えている",
                gsrc.contains("int maeKan = kane.ryakudatsuKan(player);"), "控えていない");
        check("★★(字) 成立した時だけ奪う（前が0 かつ 後が1以上）",
                gsrc.contains("if (maeKan == 0 && kane.ryakudatsuKan(player) > 0) {"),
                "無条件に奪っている");
        check("★(字) 奪うのは略奪の本体を呼んだ後",
                saki(gsrc, "jidai:sensou/ryakudatsu\"", "Shouri.ryakudatsuDeUbau"),
                "呼ぶ前に奪っている（か、どちらかが無い）");

        String ssrc = java("Shouri.java");
        check("★★(字) 奪うのは相手勢力のリーダーから",
                ssrc.contains("seiryoku.leaderMei(aiteTeam)"), "違う人から奪っている");
        check("★(字) リーダーが居なければ何もしない",
                ssrc.contains("Player leader = Bukkit.getPlayerExact(leaderMei);")
                        && ssrc.contains("if (leader == null) {"), "見ていない");
        check("★★(字) 受け取れるかを、取り上げる前に見ている",
                saki(ssrc, "ubauHito.getInventory().firstEmpty() < 0",
                        "leader.getInventory().setItem(i, null)"),
                "取り上げてから見ている（紙が消える）／見ていない");
        check("★(字) 奪ったことを全員へ出す",
                ssrc.contains("Enshutsu.zeninTsuchi"), "出していない");
        check("★(字) 奪われた本人にも知らせる",
                ssrc.contains("leader.sendMessage("), "知らせていない");
        check("★(字) 見分けは atsumeruNoNamae を通す（遺物を含む tokushuNoNamaeKa ではない）",
                ssrc.contains("String mei = atsumeruNoNamae(m.getDisplayName());"), "違う");

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        System.out.println("★ 紙が本当に持ち物を移るかは、この環境では測れない");
        System.out.println("  （Bukkit.getItemFactory() が null で ItemMeta が作れない）。");
        System.out.println("  docs/実機確認リスト.md に人の目で見る物として残してある。");
        if (fail > 0) System.exit(1);
    }
}
