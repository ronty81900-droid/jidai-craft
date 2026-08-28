// =============================================================
// KamadoTest ── エンダーかまどの「焼く処理」を測る
//
//   ★★ なぜ足したか（2026-08-22）★★
//     実機で「炎が点かない／燃料は消えるが燃えている判定が出ない」
//     という報告を受けた。ところが Kamado には検証が【1件も無かった】。
//     読むだけでは、焼く処理が壊れているのか、
//     画面へ状態を送る所が壊れているのかを切り分けられない。
//     ここでは【焼く処理だけ】をサーバー無しで回して、切り分ける。
//
//   ここで測れないもの:
//     ・画面に炎の絵が出るか（InventoryView.setProperty が効くか）
//       → 実サーバーとクライアントが要る。人の目でしか見られない。
// =============================================================

import java.lang.reflect.*;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class KamadoTest {

    static int pass = 0, fail = 0;

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
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> iface, InvocationHandler h) {
        return (T) Proxy.newProxyInstance(KamadoTest.class.getClassLoader(),
                new Class<?>[]{iface}, h);
    }

    /** 3枠の入れ物。中身を素の配列で持つだけ。 */
    static class Hako implements InvocationHandler {
        final ItemStack[] naka = new ItemStack[3];

        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "getItem": return naka[(Integer) a[0]];
                case "setItem": naka[(Integer) a[0]] = (ItemStack) a[1]; return null;
                case "getSize": return 3;
                default: return fallback(m, p, a);
            }
        }
    }

    /** 根からの相対パスを、どこから走らせても引く。 */
    static java.nio.file.Path sagasu2(String michi) {
        java.nio.file.Path[] ne = {java.nio.file.Paths.get("..", ".."),
                java.nio.file.Paths.get("."), java.nio.file.Paths.get("..")};
        for (java.nio.file.Path n : ne) {
            java.nio.file.Path p = n.resolve(michi);
            if (java.nio.file.Files.exists(p)) return p;
        }
        throw new IllegalStateException(michi + " が見つからない");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== エンダーかまど: 焼く処理の実測 ===");

        // --- Bukkit の土台。人は誰も居ないことにする ---
        //     ★ そうすると hyoujiKousin は早々に return するので、
        //       画面へ送る所を通らずに【焼く処理だけ】を測れる。
        Server sv = proxy(Server.class, (p, m, a) -> {
            if (m.getName().equals("getPlayerExact")) return null;
            if (m.getName().equals("getLogger")) return java.util.logging.Logger.getLogger("t");
            return fallback(m, p, a);
        });
        Field bf = Bukkit.class.getDeclaredField("server");
        bf.setAccessible(true);
        bf.set(null, sv);

        Class<?> kc = Class.forName("jidai.Kamado");
        Object kamado = unsafeNew(kc);

        // レシピは1つだけ入れる（丸石 → 石）。
        // ★ 本物のレシピ読み込みはサーバーが要るので、ここでは切り離す。
        Map<Material, ItemStack> reshipi = new HashMap<>();
        reshipi.put(Material.COBBLESTONE, new ItemStack(Material.STONE, 1));
        ire(kamado, "reshipi", reshipi);

        Class<?> hc = Class.forName("jidai.Kamado$Hitotsu");
        Constructor<?> hcon = hc.getDeclaredConstructor();
        hcon.setAccessible(true);

        Map<String, Object> hako = new HashMap<>();
        ire(kamado, "kamado", hako);

        Method susumu = kc.getMethod("susumu", Class.forName("jidai.Kane"));

        // ---------- A. 材料が無ければ燃料は減らない ----------
        Hako h1 = new Hako();
        Object t1 = tsukuru(hcon, hc, h1);
        hako.clear();
        hako.put("a", t1);
        h1.naka[1] = new ItemStack(Material.COAL, 3);
        susumu.invoke(kamado, (Object) null);
        check("材料が無ければ燃料は減らない",
                h1.naka[1] != null && h1.naka[1].getAmount() == 3,
                "燃料=" + (h1.naka[1] == null ? "無" : h1.naka[1].getAmount()));

        // ---------- B. 材料があれば燃料に火が点く ----------
        Hako h2 = new Hako();
        Object t2 = tsukuru(hcon, hc, h2);
        hako.clear();
        hako.put("b", t2);
        h2.naka[0] = new ItemStack(Material.COBBLESTONE, 5);
        h2.naka[1] = new ItemStack(Material.COAL, 3);
        susumu.invoke(kamado, (Object) null);
        int nokori = (Integer) toru(t2, "moeNokori");
        check("★材料があれば燃料が1つ減る",
                h2.naka[1] != null && h2.naka[1].getAmount() == 2,
                "燃料=" + (h2.naka[1] == null ? "無" : h2.naka[1].getAmount()));
        check("★火が点いている（残りtickが正の数）", nokori > 0, "残り=" + nokori);

        // ---------- C. 焼き上がる ----------
        //   鉄器は 120tick。1tick ぶんはBで進んでいるので、あと119回。
        for (int i = 0; i < 119; i++) {
            susumu.invoke(kamado, (Object) null);
        }
        check("★★120tick で焼き上がる（出来上がりに石が入る）",
                h2.naka[2] != null && h2.naka[2].getType() == Material.STONE,
                "出来上がり=" + (h2.naka[2] == null ? "無" : h2.naka[2].getType()));
        check("★焼けたら材料が1つ減る",
                h2.naka[0] != null && h2.naka[0].getAmount() == 4,
                "材料=" + (h2.naka[0] == null ? "無" : h2.naka[0].getAmount()));

        // ---------- D. 出来上がりが別の品なら焼かない ----------
        Hako h3 = new Hako();
        Object t3 = tsukuru(hcon, hc, h3);
        hako.clear();
        hako.put("c", t3);
        h3.naka[0] = new ItemStack(Material.COBBLESTONE, 1);
        h3.naka[1] = new ItemStack(Material.COAL, 1);
        h3.naka[2] = new ItemStack(Material.DIRT, 1);
        susumu.invoke(kamado, (Object) null);
        check("★出来上がりに別の品があれば火を点けない",
                h3.naka[1] != null && h3.naka[1].getAmount() == 1,
                "燃料=" + (h3.naka[1] == null ? "無" : h3.naka[1].getAmount()));

        // ---------- E. 燃料の見分け ----------
        Method nn = kc.getDeclaredMethod("nenryoNagasa", Material.class);
        nn.setAccessible(true);
        check("石炭は燃料", (Integer) nn.invoke(null, Material.COAL) > 0, "0");
        check("木炭は燃料", (Integer) nn.invoke(null, Material.CHARCOAL) > 0, "0");
        check("樫の原木は燃料", (Integer) nn.invoke(null, Material.OAK_LOG) > 0, "0");
        check("樫の板は燃料", (Integer) nn.invoke(null, Material.OAK_PLANKS) > 0, "0");
        check("木のハーフは燃料", (Integer) nn.invoke(null, Material.OAK_SLAB) > 0, "0");
        check("丸石は燃料ではない", (Integer) nn.invoke(null, Material.COBBLESTONE) == 0,
                "実際=" + nn.invoke(null, Material.COBBLESTONE));
        // ★★ ここが実測で見つかった間違い ★★
        //   名前が _SLAB で終わる物を全部 燃料にしていたので、
        //   バニラでは燃えない【石のハーフ】まで燃料になっていた。
        check("★★石のハーフは燃料ではない（バニラと同じ）",
                (Integer) nn.invoke(null, Material.STONE_SLAB) == 0,
                "実際=" + nn.invoke(null, Material.STONE_SLAB));
        check("★磨いた石のハーフも燃料ではない",
                (Integer) nn.invoke(null, Material.SMOOTH_STONE_SLAB) == 0,
                "実際=" + nn.invoke(null, Material.SMOOTH_STONE_SLAB));

        // ---------- F2. 「同じ画面か」の判定の仕方（ソース検査） ----------
        //   ★ Arclight は getTopInventory() のたびに新しい包みを返すことがある。
        //     == 比較に戻すと、炎が二度と出なくなる（実機で起きた）。
        String ksrc = null;
        for (String q : new String[]{"../src/main/java/jidai/Kamado.java",
                                     "plugin/src/main/java/jidai/Kamado.java",
                                     "src/main/java/jidai/Kamado.java"}) {
            java.nio.file.Path w = java.nio.file.Paths.get(q);
            if (java.nio.file.Files.exists(w)) {
                ksrc = java.nio.file.Files.readString(w);
                break;
            }
        }
        check("Kamado.java が読めた", ksrc != null, "見つからない");
        if (ksrc != null) {
            check("★★見ている人の一覧で判定している（getViewers）",
                    ksrc.contains("h.inv.getViewers().contains(p)"), "見つからない");
            check("★★包みの == 比較に戻っていない",
                    !ksrc.contains("view.getTopInventory() != h.inv"), "戻っている");
        }

        // ---------- F. 画面へ状態を送る所があるか ----------
        //   ★ 効くかどうかはここでは測れない（実サーバーが要る）。
        //     「送ろうとしている」ことだけを見る。
        check("★画面へ状態を送る処理がある（hyoujiKousin）",
                kc.getDeclaredMethods().length > 0
                        && aru(kc, "hyoujiKousin"), "見つからない");

        System.out.println();
        System.out.println("================================");
        // =========================================================
        //  ★★ 軽量化（2026-08-26 の監査）★★
        // =========================================================
        //   hyoujiKousin は【毎tick・全件】走り、しかも
        //     Bukkit.getPlayerExact / getViewers / setProperty×4
        //   を毎回 行っていた。空のかまどでも同じだけ走る。
        //   125人が一度ずつ開けば、閉じても表から減らないので 2,500回/秒。
        //   → 送る中身が前と同じなら送らない。**焼ける速さは変えない。**
        System.out.println();
        System.out.println("-- 軽量化: 変わっていなければ送らない --");
        String ksrc2 = new String(java.nio.file.Files.readAllBytes(
                sagasu2("plugin/src/main/java/jidai/Kamado.java")), "UTF-8");
        check("★★前と同じ値なら、何も引かず・何も送らない",
                ksrc2.contains("if (h.moeNokori == h.okuriMoe && h.moeGoukei == h.okuriGoukei"),
                "毎tick 送っている");
        check("★★見比べは getPlayerExact より【前】にある（引く手間も省く）",
                ksrc2.indexOf("h.moeNokori == h.okuriMoe")
                        < ksrc2.indexOf("Player p = Bukkit.getPlayerExact(namae);"),
                "後ろにある＝重い所を毎回 通る");
        check("★1回目は必ず送る（-1 から始める）",
                ksrc2.contains("int okuriMoe = -1;"), "0 始まりだと初回が送られない");
        check("★★送れた時だけ控える",
                ksrc2.contains("if (todoita) {"),
                "送れなくても控える＝一度 失敗すると永久に送らなくなる");
        check("★焼く処理そのものは間引いていない（速さを変えない）",
                ksrc2.contains("hitotsuSusumu(e.getKey(), e.getValue(), kane);")
                        && !ksrc2.contains("IDLE_MABIKI"),
                "焼き自体を間引いている＝火が点くのが遅れる");

        System.out.println();
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        System.out.println();
        System.out.println("★ 炎の絵が実際に出るかは、ここでは測れない。");
        System.out.println("  InventoryView.setProperty が効くかどうかは実機でしか分からない。");
        if (fail > 0) System.exit(1);
    }

    static boolean aru(Class<?> c, String mei) {
        for (Method m : c.getDeclaredMethods()) {
            if (m.getName().equals(mei)) return true;
        }
        return false;
    }

    static Object tsukuru(Constructor<?> con, Class<?> hc, Hako hako) throws Exception {
        Object t = con.newInstance();
        Field f = hc.getDeclaredField("inv");
        f.setAccessible(true);
        f.set(t, proxy(Inventory.class, hako));
        return t;
    }

    static Object toru(Object o, String mei) throws Exception {
        Field f = o.getClass().getDeclaredField(mei);
        f.setAccessible(true);
        return f.get(o);
    }

    static void ire(Object o, String mei, Object atai) throws Exception {
        Field f = o.getClass().getDeclaredField(mei);
        f.setAccessible(true);
        f.set(o, atai);
    }

    /** コンストラクタを通さずに作る。★ フィールドの初期化子も走らないので、後から入れる。 */
    static Object unsafeNew(Class<?> c) throws Exception {
        Class<?> u = Class.forName("sun.misc.Unsafe");
        Field f = u.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method m = u.getMethod("allocateInstance", Class.class);
        return m.invoke(unsafe, c);
    }
}
