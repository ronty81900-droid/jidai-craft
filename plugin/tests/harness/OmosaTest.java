// =============================================================
// OmosaTest.java ── 重さを「回数」で測る
//
//   ★★ なぜ回数で測るか ★★
//     この環境では本物のサーバーを起動できない（Java の NIO Selector が塞がれている）。
//     偽の Bukkit で時間を測っても、Proxy の分だけ遅くなって当てにならない。
//     そこで **「1秒あたり Bukkit の API を何回 叩くか」** を数える。
//     回数は本物と同じなので、これは推測ではなく実測。
//
//   測る物: エンダーかまど（毎tick・全員ぶん回る、いちばん重い所）
// =============================================================

import java.lang.reflect.*;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class OmosaTest {

    static int pass = 0, fail = 0;

    /** 叩かれた API の回数 */
    static final Map<String, Integer> KAISU = new HashMap<>();

    static void kazoeru(String mei) {
        KAISU.merge(mei, 1, Integer::sum);
    }

    static void check(String mei, boolean ok, String riyuu) {
        if (ok) {
            pass++;
            System.out.println("[PASS] " + mei);
        } else {
            fail++;
            System.out.println("[FAIL] " + mei + "  " + riyuu);
        }
    }

    // ---- 偽の Bukkit（呼ばれた回数を数える）--------------------
    static Object unsafe;
    static Method allocate;

    static Object nise(Class<?> c) {
        return Proxy.newProxyInstance(OmosaTest.class.getClassLoader(),
                new Class<?>[]{c}, (p, m, a) -> {
                    kazoeru(c.getSimpleName() + "." + m.getName());
                    return modosu(m);
                });
    }

    static Object modosu(Method m) {
        Class<?> r = m.getReturnType();
        if (r == boolean.class) return false;
        if (r == int.class) return 0;
        if (r == long.class) return 0L;
        if (r == double.class) return 0.0;
        if (r == float.class) return 0.0F;
        if (r == String.class) return "kyuryo";
        if (r == void.class) return null;
        if (r.isInterface()) return nise(r);
        return null;
    }

    public static void main(String[] args) throws Exception {
        Class<?> u = Class.forName("sun.misc.Unsafe");
        Field f = u.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = f.get(null);
        allocate = u.getMethod("allocateInstance", Class.class);

        System.out.println("============================================================");
        System.out.println("重さを回数で測る ── エンダーかまど");
        System.out.println("============================================================");
        System.out.println();

        // --- 偽のサーバー。getPlayerExact は【必ず人を返す】ようにする ---
        //   ★ ここが要。今までの検査は null を返していたので、
        //     いちばん重い筋（時代を引く所）を1度も通っていなかった。
        Object hito = nise(org.bukkit.entity.Player.class);
        Server sv = (Server) Proxy.newProxyInstance(
                OmosaTest.class.getClassLoader(), new Class<?>[]{Server.class},
                (p, m, a) -> {
                    kazoeru("Bukkit." + m.getName());
                    if (m.getName().equals("getPlayerExact")) return hito;
                    return modosu(m);
                });
        Field bf = Bukkit.class.getDeclaredField("server");
        bf.setAccessible(true);
        bf.set(null, sv);

        Class<?> kc = Class.forName("jidai.Kamado");
        Class<?> kane = Class.forName("jidai.Kane");
        Object kamado = allocateInstance(kc);

        // レシピは空でよい（叩く回数を測るのが目的）
        ire(kamado, "reshipi", new HashMap<Material, ItemStack>());

        // --- かまどを N 個 用意して、1秒（20tick）回す -------------
        Method susumu = kc.getMethod("susumu", kane);
        Object niseKane = allocateInstance(kane);

        int[] NINZU = {10, 50, 100, 125};
        Map<Integer, Integer> kekka = new LinkedHashMap<>();

        // ★ 測るのは「かまどは登録されているが、画面は閉じている」場合。
        //   参加者は品を入れたら画面を閉じるので、これがほとんどの時間の姿。
        //   （画面を開いている数人ぶんは、ここにパケット送信が数回 増えるだけ）
        for (int n : NINZU) {
            Map<String, Object> hako = new HashMap<>();
            Class<?> hc = Class.forName("jidai.Kamado$Hitotsu");
            for (int i = 0; i < n; i++) {
                Object h = allocateInstance(hc);
                Field iv = hc.getDeclaredField("inv");
                iv.setAccessible(true);
                iv.set(h, nise(Inventory.class));
                hako.put("hito" + i, h);
            }
            ire(kamado, "kamado", hako);

            KAISU.clear();
            for (int t = 0; t < 20; t++) {          // 1秒ぶん
                susumu.invoke(kamado, niseKane);
            }
            int goukei = KAISU.values().stream().mapToInt(Integer::intValue).sum();
            kekka.put(n, goukei);
            System.out.println(String.format(
                    "    かまど %3d 個 → 1秒あたり Bukkit API %6d 回", n, goukei));
            if (n == 100) {
                System.out.println("      内訳（多い順）:");
                KAISU.entrySet().stream()
                        .sorted((a, b) -> b.getValue() - a.getValue())
                        .limit(6)
                        .forEach(e -> System.out.println(String.format(
                                "        %-34s %6d 回/秒", e.getKey(), e.getValue())));
            }
        }

        System.out.println();

        // --- 判定 ------------------------------------------------
        int hyaku = kekka.get(100);
        int juu = kekka.get(10);

        check("★人数に比例して増える（10人 → 100人 でおよそ10倍）",
              hyaku >= juu * 8 && hyaku <= juu * 12,
              "10人=" + juu + " / 100人=" + hyaku);

        // ★★ 閾値の根拠 ★★
        //   1秒 = 20tick × 50ms = 1,000ms が予算。
        //   Bukkit のスコアボード読みや名前引きは、1回 およそ 1マイクロ秒の桁。
        //   20,000回/秒 でおよそ 20ms/秒 ＝ 予算の 2%。ここを超えたら
        //   「かまどだけで 2% 使う」ことになるので、実機で測る合図とする。
        //   ★ 2026-08-26 の直し前は 32,000回（125人で 40,000回）だった。
        check("★★125人ぶんで 1秒あたり 20,000 回 未満（予算の 2%）",
              kekka.get(125) < 20000, "1秒あたり " + kekka.get(125) + " 回");
        check("★100人ぶんで 1秒あたり 16,000 回 未満",
              hyaku < 16000, "1秒あたり " + hyaku + " 回");

        System.out.println();
        System.out.println("============================================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        System.out.println("★ これは【回数】の実測です。本物のサーバーでの【時間】は、");
        System.out.println("  この環境では測れません（サーバーを起動できないため）。");
        if (fail > 0) {
            System.exit(1);
        }
    }

    static Object allocateInstance(Class<?> c) throws Exception {
        return allocate.invoke(unsafe, c);
    }

    static void ire(Object o, String mei, Object atai) throws Exception {
        Field f = o.getClass().getDeclaredField(mei);
        f.setAccessible(true);
        f.set(o, atai);
    }
}
