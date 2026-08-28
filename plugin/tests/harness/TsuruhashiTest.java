// =============================================================
// TsuruhashiTest ── 「時代のツルハシ」1本きりの決まりを見る
//
//   ★★ 測れること / 測れないこと をはっきり分ける ★★
//     測れる … 落ちる物から取り除く処理そのもの（動かして確かめる）
//              クラスの形（メソッドが在るか）
//              ソースに書いてある決まり（壊れない指定・記録の付け方）
//     測れない … 実際にアイテムを1本作ること。
//              ItemStack の生成にはサーバーのアイテム登録簿が要り、
//              この検証環境には無い（1.20.1 は getItemFactory() が null）。
//              「本当に壊れないか」「本当に落ちないか」は実機で見るしかない。
// =============================================================

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

import org.bukkit.inventory.ItemStack;

public class TsuruhashiTest {

    static int pass = 0, fail = 0;

    static void check(String label, boolean cond, String detail) {
        if (cond) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + detail); }
    }

    /** ソースを読む。ハーネスは plugin/tests から動く。 */
    static String src(String... michi) throws Exception {
        for (String[] atama : new String[][]{
                {"..", "src", "main", "java", "jidai"},
                {"plugin", "src", "main", "java", "jidai"}}) {
            Path p = Paths.get(atama[0], Arrays.copyOfRange(atama, 1, atama.length));
            for (String x : michi) p = p.resolve(x);
            if (Files.exists(p)) return new String(Files.readAllBytes(p), "UTF-8");
        }
        throw new NoSuchFileException(String.join("/", michi)
                + " (cwd=" + Paths.get("").toAbsolutePath() + ")");
    }

    /** データパックのソースを読む。 */
    static String dp(String michi) throws Exception {
        for (String atama : new String[]{"../..", "."}) {
            Path p = Paths.get(atama, "datapacks", "jidai_craft", "data", "jidai",
                    "functions", michi);
            if (Files.exists(p)) return new String(Files.readAllBytes(p), "UTF-8");
        }
        throw new NoSuchFileException(michi);
    }

    public static void main(String[] a) throws Exception {

        Class<?> cls = Class.forName("jidai.Tsuruhashi");

        // ---------- 1) クラスの形 ----------
        System.out.println("-- クラスの形（実物を読む） --");
        for (String m : new String[]{"tsukuru", "sore", "motteru", "hajimete",
                                     "watasu", "uneiWatasu", "shinda", "ikikaetta",
                                     "watashitaKazu"}) {
            boolean aru = false;
            for (Method x : cls.getDeclaredMethods()) if (x.getName().equals(m)) aru = true;
            check("(実物) Tsuruhashi に " + m + " がある", aru, "無い");
        }

        // ---------- 2) 落ちる物から取り除く処理を動かす ----------
        System.out.println();
        System.out.println("-- 死んだ時の処理（動かして確かめる） --");

        // ★ コンストラクタを通さずに作る。sore(null) は鍵に触る前に false を返すので、
        //   サーバーが無くてもここまでは動く。
        Field tu = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        tu.setAccessible(true);
        Object unsafe = tu.get(null);
        Method allocate = Class.forName("sun.misc.Unsafe")
                .getMethod("allocateInstance", Class.class);
        Object t = allocate.invoke(unsafe, cls);

        Method shinda = cls.getMethod("shinda", List.class);

        List<ItemStack> karappo = new ArrayList<>();
        check("落ちる物が空でも落ちない", (int) shinda.invoke(t, karappo) == 0, "例外");

        // ★ null は「ツルハシではない何か」の代わり。
        //   本物のアイテムはここでは作れないため、
        //   「ツルハシ以外を取り除かない」ことだけを確かめる。
        List<ItemStack> futsuu = new ArrayList<>(Arrays.asList(null, null, null, null));
        int nuita = (int) shinda.invoke(t, futsuu);
        check("★★ツルハシ以外は1つも取り除かない",
                nuita == 0 && futsuu.size() == 4,
                "取り除いた=" + nuita + " 残り=" + futsuu.size());

        // ---------- 3) ソースに書いてある決まり ----------
        System.out.println();
        System.out.println("-- 決まり（ソースの字で見る） --");
        String ts = src("Tsuruhashi.java");

        check("★★(字) 壊れない指定が入っている（setUnbreakable(true)）",
                ts.contains("m.setUnbreakable(true);"), "入っていない");
        check("★(字) 目印はアイテム自身に書く（PersistentDataContainer）",
                ts.contains("getPersistentDataContainer().set("), "書いていない");
        check("★(字) 見分けは名前ではなく目印で行う",
                !ts.contains("getDisplayName().equals"), "名前で見分けている");
        check("(字) 素材は鉄のツルハシ",
                ts.contains("Material.IRON_PICKAXE"), "違う");

        // ★★ 二重に配らない ★★
        check("★★(字) 一度配った人を記録している（増殖を防ぐ）",
                ts.contains("watashita().contains(player.getName())")
                        && ts.contains("watashita().add(player.getName())"), "記録していない");
        check("★(字) 生き返った時は、持っていなければ渡す（持っていれば何もしない）",
                ts.contains("public void ikikaetta")
                        && ts.substring(ts.indexOf("public void ikikaetta"))
                             .contains("if (motteru(player)) {"), "確かめずに渡している");

        // ★ 取り除く時は後ろから回す。前から回して remove すると1つ飛ばす。
        check("★★(字) 取り除く時は後ろから回している（飛ばさないため）",
                ts.contains("for (int i = ochirumono.size() - 1; i >= 0; i--)"),
                "前から回している");

        // ★ onEnable の時点で鍵も設定も触らない（実測で NullPointerException になった）
        check("★★(字) 鍵と記録は後から用意する（onEnable で落ちないため）",
                ts.contains("private NamespacedKey kagi()")
                        && ts.contains("private List<String> watashita()"), "作りっぱなし");

        // ---------- 4) 入口に配線されているか ----------
        System.out.println();
        System.out.println("-- 入口（JidaiCraft）--");
        String js = src("JidaiCraft.java");
        check("★(字) 死んだ時の受け口がある（PlayerDeathEvent）",
                js.contains("public void onDeath(PlayerDeathEvent"), "無い");
        check("★(字) 生き返った時の受け口がある（PlayerRespawnEvent）",
                js.contains("public void onRespawn(PlayerRespawnEvent"), "無い");
        check("★(字) 死んだ時に落ちる物からツルハシを抜いている",
                js.contains("tsuruhashi.shinda(event.getDrops())"), "抜いていない");
        check("★(字) 入った人に自動で配る",
                js.contains("tsuruhashi.hajimete(event.getPlayer())"), "配っていない");
        check("(字) 運営が渡し直せる（jidai tsuruhashi）",
                js.contains("args[0].equals(\"tsuruhashi\")"), "無い");
        check("(字) タブ補完に tsuruhashi がある",
                js.contains("\"juki\", \"tsuruhashi\""), "無い");

        // ---------- 5) 店とデータパック ----------
        System.out.println();
        System.out.println("-- 店とデータパック --");
        String sh = src("Shop.java");
        check("★★(字) 店でピッケルを売っていない",
                !sh.contains("WOODEN_PICKAXE") && !sh.contains("STONE_PICKAXE")
                        && !sh.contains("IRON_PICKAXE") && !sh.contains("DIAMOND_PICKAXE"),
                "まだ売っている");

        String tetsu = dp("setup/tetsu.mcfunction");
        check("★★(実物) 配布から素の鉄のツルハシが外れている",
                !tetsu.contains("give @s minecraft:iron_pickaxe"), "まだ配っている");
        check("(実物) 鉄装備の配布そのものは残っている",
                tetsu.contains("minecraft:iron_chestplate")
                        && tetsu.contains("minecraft:iron_sword"), "消えている");

        // ---------- 6) 測れないことを、そのまま書き出す ----------
        System.out.println();
        System.out.println("-- ここでは測れないこと（実機で見るしかない） --");
        System.out.println("   ・実際に1本 作れること（ItemStack の生成にサーバーが要る）");
        System.out.println("   ・本当に耐久が減らないこと");
        System.out.println("   ・死んだ時、本当に地面に落ちないこと");
        System.out.println("   ・生き返った時、本当に手元へ戻ること");

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
