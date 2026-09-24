// =============================================================
// UneiTest ── 運営が手元で切り替える2つを見る
//
//   (1) クラフト禁止  … 既定は禁止。塞ぐ台が抜けていないか
//   (2) 石油の栓      … 止める／抑える。名前と切り詰めが正しいか
//
//   ★ 見方を2つ使い分けている。理由も書いておく。
//     ・リフレクション … static な値（塞ぐ台の一覧）は実物を読める
//     ・ソースの字     … インスタンスの初期値やイベントの中身は、
//                        サーバー無しでは動かせないので字で見る
//     どちらで見たかを検査名に出す。「動かして確かめた」ように
//     見せないため。
// =============================================================

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

import org.bukkit.Material;

public class UneiTest {

    static int pass = 0, fail = 0;

    static void check(String label, boolean cond, String detail) {
        if (cond) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + detail); }
    }

    /** ソースを読む。ハーネスは plugin/tests から動くので、そこからの道順。 */
    static String src(String namae) throws IOException {
        Path[] soko = {
                Paths.get("..", "src", "main", "java", "jidai", namae),
                Paths.get("plugin", "src", "main", "java", "jidai", namae),
                Paths.get("src", "main", "java", "jidai", namae),
        };
        for (Path p : soko) {
            if (Files.exists(p)) return new String(Files.readAllBytes(p), "UTF-8");
        }
        throw new FileNotFoundException(namae + " が見つからない (cwd="
                + Paths.get("").toAbsolutePath() + ")");
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {

        // =====================================================
        //  (1) クラフト禁止
        // =====================================================
        System.out.println("-- クラフト禁止 --");

        Class<?> jc = Class.forName("jidai.JidaiCraft");
        Field dai = jc.getDeclaredField("TSUKURU_DAI");
        dai.setAccessible(true);
        List<Material> ichiran = (List<Material>) dai.get(null);

        // ★ 実物の一覧を読んでいる。ソースの字ではない。
        // ★ 作業台は 2026-08-20 に開放した（建材のハーフと柵に要るため）。
        Material[] iru = {
                Material.STONECUTTER, Material.SMITHING_TABLE,
                Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
                Material.LOOM, Material.CARTOGRAPHY_TABLE, Material.GRINDSTONE};
        for (Material m : iru) {
            check("(実物) 塞ぐ台に " + m + " が入っている", ichiran.contains(m), "抜けている");
        }
        check("(実物) 塞ぐ台はちょうど8種", ichiran.size() == 8, "実際=" + ichiran.size());

        // ★★ 作業台は塞がない ★★
        //   ハーフブロックは 1x3、柵は 2x3 で作る（実物のレシピで確認）。
        //   持ち物の 2x2 では足りず、作業台が無いと1つも作れない。
        check("★★作業台は塞いでいない（建材を作るのに要る）",
                !ichiran.contains(Material.CRAFTING_TABLE), "塞いでいる");

        // ★★ かまどは塞いではいけない ★★
        //   エンダーかまどはこの企画の仕組みそのもの。ここに入れると
        //   「精錬できない世界」になり、石炭を売る意味も消える。
        check("★★かまどは塞いでいない（エンダーかまどが死ぬため）",
                !ichiran.contains(Material.FURNACE)
                        && !ichiran.contains(Material.BLAST_FURNACE)
                        && !ichiran.contains(Material.SMOKER), String.valueOf(ichiran));

        String jcs = src("JidaiCraft.java");
        check("(字) 既定は禁止（craftKinshi = true）",
                jcs.contains("private boolean craftKinshi = true;"), "既定が true でない");

        // 3つの入口すべてが、切り替えを見ていること。
        // 1つでも見ていないと「許可したのに作れない」「禁止なのに作れる」が起きる。
        for (String hairi : new String[]{"onCraftJunbi", "onCraft", "onInvOpen"}) {
            int i = jcs.indexOf("public void " + hairi + "(");
            check("(字) " + hairi + " が禁止の切り替えを見ている",
                    i > 0 && jcs.indexOf("if (!craftKinshi)", i) > 0
                            && jcs.indexOf("if (!craftKinshi)", i) - i < 400, "見ていない");
        }
        check("(字) 台の右クリックも切り替えを見ている",
                jcs.contains("if (craftKinshi && TSUKURU_DAI.contains("), "見ていない");

        // ★ 切り替えは保存しない。再起動したら必ず禁止に戻るのが安全側。
        check("★(字) 許可の状態を保存していない（再起動で禁止に戻る）",
                !jcs.contains("craftKinshi\", ") && !jcs.contains("set(\"craftKinshi"),
                "保存している");

        // ---------- 作ってよい物の判定を、実際に呼んで確かめる ----------
        //
        // ★★ ここが一番大事 ★★
        //   作業台を開放したので、この判定が漏れると
        //   【銃も防具も作れる世界】になる。1つずつ呼んで確かめる。
        System.out.println();
        System.out.println("-- 作ってよい物の判定（実際に呼ぶ）--");
        Method yoi = jc.getDeclaredMethod("tsukutteYoi", Material.class);
        yoi.setAccessible(true);

        // ご指示の建材。作れなければならない
        Material[] tsukureru = {
                Material.OAK_SLAB, Material.STONE_SLAB, Material.SMOOTH_STONE_SLAB,
                Material.STONE_BRICK_SLAB, Material.BIRCH_SLAB,
                Material.STONE_BRICKS, Material.CHISELED_STONE_BRICKS,
                Material.POLISHED_ANDESITE, Material.POLISHED_DIORITE,
                Material.POLISHED_GRANITE, Material.POLISHED_DEEPSLATE,
                Material.POLISHED_BLACKSTONE,
                Material.OAK_FENCE, Material.SPRUCE_FENCE, Material.NETHER_BRICK_FENCE,
                Material.OAK_FENCE_GATE,
                // ★ 2026-08-22: 原木→板材が通らず作業台が作れなかった（ご指摘）
                Material.OAK_PLANKS, Material.BIRCH_PLANKS, Material.SPRUCE_PLANKS,
                Material.CRAFTING_TABLE};
        for (Material m : tsukureru) {
            check("作れる: " + m, (boolean) yoi.invoke(null, m), "作れない判定になっている");
        }

        // ★★ 作れてはいけない物 ★★
        //   装備・道具・武器・弾・仕掛け。1つでも通ると企画が壊れる。
        Material[] dame = {
                Material.IRON_SWORD, Material.DIAMOND_SWORD,
                Material.IRON_PICKAXE, Material.WOODEN_PICKAXE, Material.DIAMOND_PICKAXE,
                Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE,
                Material.CHAINMAIL_HELMET, Material.SHIELD,
                Material.BOW, Material.CROSSBOW, Material.ARROW,
                Material.TNT, Material.FLINT_AND_STEEL,
                Material.FURNACE, Material.ANVIL,
                Material.STONECUTTER, Material.SMITHING_TABLE,
                Material.BREAD, Material.TORCH,
                Material.STONE_STAIRS, Material.COBBLESTONE_WALL,
                Material.AIR};
        // ★ 2026-08-24 のご指示でチェストを解禁した。物を仕舞う場所が無いと拠点が組めない。
        check("★作れる: CHEST（2026-08-24 のご指示）",
                (boolean) yoi.invoke(null, Material.CHEST), "作れない");
        for (Material m : dame) {
            check("★作れない: " + m, !(boolean) yoi.invoke(null, m), "作れてしまう");
        }
        check("★★null でも落ちず、作れない側になる",
                !(boolean) yoi.invoke(null, (Object) null), "落ちるか、作れてしまう");

        // ★ クラフトの入口2つが、この判定を通していること
        check("★(字) 出来上がりの下ごしらえで判定している",
                jcs.contains("public void onCraftJunbi")
                        && jcs.substring(jcs.indexOf("public void onCraftJunbi"))
                              .contains("tsukutteYoi(dekiagari.getType())"), "見ていない");
        check("★(字) 実際に取り出す時も判定している",
                jcs.contains("public void onCraft(CraftItemEvent")
                        && jcs.substring(jcs.indexOf("public void onCraft(CraftItemEvent"))
                              .contains("tsukutteYoi(dekiagari.getType())"), "見ていない");

        // =====================================================
        //  (2) 石油の栓
        // =====================================================
        System.out.println();
        System.out.println("-- 石油の栓 --");

        Class<?> kc = Class.forName("jidai.Kane");
        for (String m : new String[]{"sekiyuSenJunbiOK", "sekiyuTomatteruKa",
                                     "sekiyuBairitsu", "sekiyuTomeru", "sekiyuBairitsuSet"}) {
            boolean aru = false;
            for (Method x : kc.getDeclaredMethods()) {
                if (x.getName().equals(m)) aru = true;
            }
            check("(実物) Kane に " + m + " がある", aru, "無い");
        }

        String ks = src("Kane.java");
        check("★(字) 倍率は 1〜100 に切り詰めている（0除算を防ぐ）",
                ks.contains("Math.max(1, Math.min(100, bairitsu))"), "切り詰めていない");
        check("(字) 読み出しでも 0 を 100 に読み替えている",
                ks.contains("n < 1 ? 100 : n"), "読み替えていない");
        check("★(字) 石油の栓は Kane にだけある（他がスコアボードに触らない）",
                !src("Shop.java").contains("石油_停止")
                        && !jcs.contains("石油_停止"), "他のファイルが触っている");

        // コマンドの受け口
        check("(字) jidai sekiyu の受け口がある",
                jcs.contains("args[0].equals(\"sekiyu\")"), "無い");
        check("(字) jidai craft の受け口がある",
                jcs.contains("args[0].equals(\"craft\")"), "無い");
        check("(字) tomeru / hajimeru / osaeru の3つを見ている",
                jcs.contains("\"tomeru\"") && jcs.contains("\"hajimeru\"")
                        && jcs.contains("\"osaeru\""), "足りない");
        check("★(字) 数字でない指定で落ちない（NumberFormatException を受けている）",
                jcs.contains("catch (NumberFormatException"), "受けていない");
        check("(字) タブ補完に sekiyu と craft を出している",
                jcs.contains("\"leader\", \"sekiyu\", \"craft\""), "出していない");

        // plugin.yml の usage
        String yml = new String(Files.readAllBytes(
                Files.exists(Paths.get("..", "src", "main", "resources", "plugin.yml"))
                        ? Paths.get("..", "src", "main", "resources", "plugin.yml")
                        : Paths.get("plugin", "src", "main", "resources", "plugin.yml")),
                "UTF-8");
        check("(実物) plugin.yml の usage に sekiyu がある", yml.contains("sekiyu"), "無い");
        check("(実物) plugin.yml の usage に craft がある", yml.contains("craft"), "無い");

        // ---------- 銀行の「石油を預ける」 ----------
        //
        // ★★ 2026-08-20 に、石油はアイテムのままになった ★★
        //   拾っても数に入らない。ここで預けて初めて勢力の石油になる。
        System.out.println();
        System.out.println("-- 銀行の石油預け --");
        String gs = src("Ginko.java");
        check("★(字) 石油を預ける枠がある", gs.contains("WAKU_SEKIYU"), "無い");

        // ---- 2026-08-23: 経済勝利の救済（バブル）と 石油の換金 ----
        Class<?> ginkoCls = Class.forName("jidai.Ginko");
        int sekiyuNedan = (Integer) teisuu(ginkoCls, "SEKIYU_NEDAN");
        int sekiyuJidai = (Integer) teisuu(ginkoCls, "SEKIYU_URU_JIDAI");
        int bubbleJidai = (Integer) teisuu(ginkoCls, "BUBBLE_JIDAI");
        int bubbleBai = (Integer) teisuu(ginkoCls, "BUBBLE_BAI");
        check("★石油の売値が 1本 8（ご指示）", sekiyuNedan == 8, "実際=" + sekiyuNedan);
        check("★石油を売れるのは中世(2)から（ご指示）", sekiyuJidai == 2, "実際=" + sekiyuJidai);
        check("★バブルは近代(3)から（ご指示）", bubbleJidai == 3, "実際=" + bubbleJidai);
        check("★バブルの倍率が 2（ご指示）", bubbleBai == 2, "実際=" + bubbleBai);

        check("★(字) 石油を売る枠がある", gs.contains("WAKU_SEKIYU_URU"), "無い");
        check("★(字) 石油を売る枠が押した時に振り分けられている",
                gs.contains("if (slot == WAKU_SEKIYU_URU)"), "配線が無い");
        check("★★(字) バブルは【中央の時代】で見る（勢力ごとに見ると先頭だけ先に倍になる）",
                gs.contains("kane.chuoJidai() >= BUBBLE_JIDAI"), "勢力の時代で見ている疑い");
        check("★(字) バブルが掛かったことを画面に出す",
                gs.contains("バブル ×"), "黙って倍にしている");

        // ★★ 売っても勢力の石油スコアに触らないこと ★★
        //   触ると、預けた石油を後から取り崩せてしまい、時代進行の条件が意味を失う。
        String uruBu = kiridashi(gs, "private void sekiyuUru(", "/** 中央プラントが出した石油のアイテムか。 */");
        check("★★★(字) 石油を売る処理が【勢力の石油】に触らない",
                !uruBu.contains("sekiyu") || !uruBu.contains("kaku("), "勢力の石油を書いている疑い");
        check("★★(字) 石油を売る処理が、手持ちのアイテムだけを取る",
                uruBu.contains("player.getInventory().setItem(i, null)"), "アイテムを取っていない");
        check("★(字) 中世より前は断る",
                uruBu.contains("< SEKIYU_URU_JIDAI"), "門番が無い");
        check("★★(字) 中身はデータパックへ丸投げしている（二重処理を避ける）",
                gs.contains("run function jidai:sekiyu/azukeru"), "自前で処理している");
        check("★(字) 押した枠の振り分けに入っている",
                gs.contains("if (slot == WAKU_SEKIYU)"), "入っていない");
        check("(字) プラグインは石油の本数を自分で数えていない",
                !gs.contains("BLACK_DYE, 1") || !gs.contains("sekiyuKousin"),
                "自分で数えている");

        // ---------- 売却レートとネザライト ----------
        System.out.println();
        System.out.println("-- 売却レート --");
        Class<?> gc = Class.forName("jidai.Ginko");
        Field nf = gc.getDeclaredField("NEDAN"); nf.setAccessible(true);
        int[] nedan = (int[]) nf.get(null);
        Field uf = gc.getDeclaredField("URERU"); uf.setAccessible(true);
        Material[] ureru = (Material[]) uf.get(null);

        check("(実物) 売れる物とその値段の数がそろっている",
                ureru.length == nedan.length, ureru.length + " vs " + nedan.length);
        int[] machi = {1, 5, 10, 15, 30};
        Material[] machiM = {Material.IRON_INGOT, Material.LAPIS_LAZULI,
                             Material.GOLD_INGOT, Material.DIAMOND,
                             Material.NETHERITE_INGOT};
        check("★(実物) 売れるのは5種", ureru.length == 5, "実際=" + ureru.length);
        for (int i = 0; i < machiM.length && i < ureru.length; i++) {
            check("(実物) " + machiM[i] + " が " + machi[i],
                    ureru[i] == machiM[i] && nedan[i] == machi[i],
                    "実際=" + ureru[i] + "/" + nedan[i]);
        }

        // ★★ データパックと同じ値か ★★
        //   ずれると、売った額が画面の説明と食い違う。
        String usrc = new String(Files.readAllBytes(
                Files.exists(Paths.get("..", "..", "datapacks"))
                        ? Paths.get("..", "..", "datapacks", "jidai_craft", "data", "jidai",
                                    "functions", "kane", "uru.mcfunction")
                        : Paths.get("datapacks", "jidai_craft", "data", "jidai",
                                    "functions", "kane", "uru.mcfunction")), "UTF-8");
        for (int i = 1; i < machi.length; i++) {
            check("★★データパックの倍率にも " + machi[i] + " がある",
                    usrc.contains("scoreboard players set #bai sagyou " + machi[i]),
                    "無い");
        }
        check("★★データパックもネザライトを数えている",
                usrc.contains("minecraft:netherite_ingot"), "数えていない");

        // ---------- 石を掘るとネザライト ----------
        System.out.println();
        System.out.println("-- 石からのネザライト --");
        check("★(字) 確率は 1/1000 = 0.1%",
                jcs.contains("NEZA_KAKURITSU = 1000"), "違う");
        check("(字) 対象は5種の石（石・花崗岩・閃緑岩・安山岩・深層岩。丸石は含めない）",
                jcs.contains("Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE,")
                        && jcs.contains("Material.DEEPSLATE);")
                        && !jcs.contains("Material.COBBLESTONE,"), "違う");
        check("★(字) 深層岩は 1/500（2026-08-22 のご指示「深層岩は出やすく」）",
                jcs.contains("NEZA_KAKURITSU_SHINSO = 500"), "違う");
        check("★(字) くじは後から作る（Unsafe で落ちないため）",
                jcs.contains("private Random ishiRan;")
                        && jcs.contains("ishiRan().nextInt"), "作りっぱなし");
        check("(字) 持ち物ではなく地面へ落とす（いっぱいでも消えない）",
                jcs.contains("dropItemNaturally"), "持ち物へ入れている");

        // ---------- 銀行のまわり3マス（2026-09-18 のご指示）----------
        // ★ 守りの入口は「施設でない（shu == null）」かつ「運営でない」時だけ。
        //   条件が増えても壊れないよう、呼び出しの字そのもので位置を取る。
        int mamoru = jcs.indexOf("basho.ginkoNoMawari(event.getBlock(), GINKO_MAMORU)");
        int neza = jcs.indexOf("NETHERITE_INGOT");
        check("★★(字) 銀行のまわりの守りが【ネザライトの抽選より前】にある",
                mamoru >= 0 && neza >= 0 && mamoru < neza,
                "守り=" + mamoru + " ネザライト=" + neza
                        + "（後ろだと、壊せないのにドロップだけ出て無限に湧く）");
        check("★★(字) 施設そのもの（銀行を含む）はこの守りで弾かない",
                jcs.contains("shu == null && !event.getPlayer().isOp()"),
                "銀行を壊す＝略奪、という戦争の入口を塞いでしまう");
        check("(字) 運営（op）は守りを素通りできる（詰んだ時の逃げ道）",
                jcs.contains("!event.getPlayer().isOp()"), "逃げ道が無い");
        check("★★(字) 壊した銀行は落とし物を出さず、あとで戻す",
                jcs.contains("event.setDropItems(false)")
                        && jcs.contains("ginkoModosuYoyaku(event.getBlock())"),
                "金ブロックが増える／戻らない");
        check("★★(字) 止める時に、戻し残した銀行を戻す",
                jcs.contains("壊れていた銀行を"), "落ちたまま消えると運営でも置き直せない");
        check("(字) 置く方も同じ範囲で止める",
                jcs.contains("public void onBlockPlace(BlockPlaceEvent event)")
                        && jcs.contains("basho.ginkoNoMawari(event.getBlock(), GINKO_MAMORU)"),
                "壊す方だけだと箱で囲める");

        // ---------- 戦争勝利（ビーコンを壊す条件） ----------
        System.out.println();
        System.out.println("-- 戦争勝利 --");
        check("★(字) ビーコンを壊してよいかの判定がある",
                jcs.contains("private boolean beaconKowaseruKa("), "無い");
        int bk = jcs.indexOf("private boolean beaconKowaseruKa(");
        String bkBody = jcs.substring(bk, Math.min(jcs.length(), bk + 3000));
        check("★★(字) 勢力に入っていない人を弾く",
                bkBody.contains("Seiryoku.seiryokuKa(jibun)"), "弾いていない");
        check("★★(字) 自分の勢力の拠点は壊せない",
                bkBody.contains("jibun.equals(mochinushi)"), "壊せてしまう");
        check("★★(字) 交戦中(2)でなければ壊せない",
                bkBody.contains("sensouJotai(jibunMei) != 2"), "見ていない");
        check("★★(字) 銀行を壊した回数が足りなければ壊せない（2026-09-09: 貯金→回数）",
                bkBody.contains("function jidai:sensou/kai_yomu")
                        && bkBody.contains("kane.sagyou(\"#q_kai\")")
                        && bkBody.contains("kaisu < hitsuyou"), "見ていない");
        check("★★(字) 必要回数はデータパックの settei から読む（数字を持たない）",
                bkBody.contains("kane.settei(\"占領_必要回数\")"), "プラグインが数字を持っている");
        check("★(字) 壊れた後の処理はデータパックへ渡す",
                bkBody.contains("function jidai:sensou/shokuminchi"), "自前でやっている");
        check("(実物) Basho がビーコンの持ち主を覚えている",
                Class.forName("jidai.Basho").getDeclaredMethod(
                        "beaconNoKuni", org.bukkit.block.Block.class) != null, "無い");

        // ---------- 勝利条件（超特殊）----------
        //   ★ 人望は傭兵ごと止めた（2026-08-26）。見るのは超特殊だけ。
        System.out.println();
        System.out.println("-- 超特殊勝利 --");
        Class<?> sc = Class.forName("jidai.Shouri");


        // ===== 金庫の対応（プラグイン ⇄ データパック）=====
        //
        // ★★ 2026-08-22 に実機で見つかった事故の再発防止 ★★
        //   プラグイン側の対応表が2勢力ぶんしか無く、
        //   川・内海・岩場の人は勢力の金がまるごと使えなかった。
        //   コンパイルも当時の検査も通っていたので、人が試すまで分からなかった。
        System.out.println();
        System.out.println("-- 金庫の対応（貯金の持ち主名）--");
        Class<?> kaneCls = Class.forName("jidai.Kane");
        java.lang.reflect.Method kmt = kaneCls.getMethod("kinkoMeiTeam", String.class);

        String[][] kinkoMachi = {
                {"kyuryo", "丘陵"}, {"shinrin", "森林"}, {"kawa", "川"},
                {"naikai", "内海"}, {"iwaba", "岩場"},
        };
        for (String[] g : kinkoMachi) {
            Object de = kmt.invoke(null, g[0]);
            check("★★" + g[0] + " の金庫は " + g[1], g[1].equals(de), "実際=" + de);
        }
        check("勢力でないチームは金庫を持たない", kmt.invoke(null, "kanri") == null,
                "実際=" + kmt.invoke(null, "kanri"));

        // ★ データパック側の対応と1文字でも違うと、金が別の所へ入る
        String dp = null;
        for (String q : new String[]{
                "../../datapacks/jidai_craft/data/jidai/functions/kane/kinko_yomu.mcfunction",
                "datapacks/jidai_craft/data/jidai/functions/kane/kinko_yomu.mcfunction"}) {
            java.nio.file.Path w = java.nio.file.Paths.get(q);
            if (java.nio.file.Files.exists(w)) {
                dp = java.nio.file.Files.readString(w);
                break;
            }
        }
        check("データパックの kinko_yomu が読めた", dp != null, "見つからない");
        if (dp != null) {
            for (String[] g : kinkoMachi) {
                check("★★データパックも " + g[0] + " → " + g[1],
                        dp.contains("team=" + g[0] + "]") && dp.contains(g[1] + " chokin"),
                        "対応が見つからない");
            }
        }

        Field tkF = sc.getDeclaredField("TOKUSHU"); tkF.setAccessible(true);
        String[] tokushu = (String[]) tkF.get(null);
        check("★★(実物) 特殊アイテムは5種", tokushu.length == 5,
                "実際=" + tokushu.length);
        // ★ 名前を手で並べない。改名した時に、中身は正しいのに
        //   ここだけ古くなって赤くなる（2026-08-22 に実際そうなった）。
        for (String m : (String[]) Class.forName("jidai.Shouri")
                .getMethod("tokushuMei").invoke(null)) {
            boolean aru = false;
            for (String x : tokushu) if (x.equals(m)) aru = true;
            check("(実物) 特殊アイテムに " + m + " がある", aru, "無い");
        }

        // ★★ ガチャの景品表とそろっているか ★★
        //   片方だけ名前を変えると、永久に集まらない勝利条件になる。
        String gsrc2 = src("Gacha.java");
        for (String m : tokushu) {
            check("★★ガチャの景品にも " + m + " がある", gsrc2.contains(m), "無い");
        }

        String ssrc = src("Shouri.java");
        check("★★(字) 超特殊はリーダー1人の持ち物だけを見る",
                ssrc.contains("seiryoku.leaderMei(team)")
                        && ssrc.contains("zensyu(leader)"), "違う");
        check("★(字) 決着済みなら何もしない（二重に祝わない）",
                ssrc.contains("kane.shouriSumi()"), "見ていない");
        check("★(字) 勝ちの宣言はデータパックへ渡す",
                ssrc.contains("function jidai:sensou/shouri_kakutei"), "自前でやっている");

        check("(字) 毎秒の見回りから呼ばれている",
                jcs.contains("shouri.byoumai(kane, seiryoku)"), "呼ばれていない");

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }

    /** クラスの定数（private static final）を、名前で読む。 */
    static Object teisuu(Class<?> c, String mei) throws Exception {
        java.lang.reflect.Field f = c.getDeclaredField(mei);
        f.setAccessible(true);
        return f.get(null);
    }

    /** ソースの一部を切り出す（kara から made の手前まで）。 */
    static String kiridashi(String src, String kara, String made) {
        int i = src.indexOf(kara);
        int j = src.indexOf(made, i < 0 ? 0 : i);
        if (i < 0 || j < 0 || j < i) {
            return "";
        }
        return src.substring(i, j);
    }

}
