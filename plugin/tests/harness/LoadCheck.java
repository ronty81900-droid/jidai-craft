import java.io.InputStream;
import java.util.jar.JarFile;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoadOrder;

/**
 * plugin.yml の load（いつ有効化されるか）を確かめる。
 *
 * ★★ 2026-08-21: 見せるだけだったのを「検査」にした ★★
 *
 * ★ なぜ POSTWORLD でなければならないか
 *   このプラグインは onEnable でワールドとスコアボードを触る。
 *   STARTUP だとワールドが読み込まれる前に走り、null で落ちる。
 *   既定は POSTWORLD なので、ふつうは書かなくてよい。
 *   「速くしよう」と STARTUP を書き足した時に、ここで止める。
 */
public class LoadCheck {

    static int pass = 0, fail = 0;

    static void check(String label, boolean jouken, String shousai) {
        if (jouken) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + shousai); }
    }

    public static void main(String[] a) throws Exception {
        try (JarFile jar = new JarFile(a[0]);
             InputStream in = jar.getInputStream(jar.getJarEntry("plugin.yml"))) {

            PluginDescriptionFile d = new PluginDescriptionFile(in);
            System.out.println("      version=" + d.getVersion() + " / load=" + d.getLoad());

            check("load が POSTWORLD (ワールド読み込み後に有効化)",
                    d.getLoad() == PluginLoadOrder.POSTWORLD,
                    "実際=" + d.getLoad() + " ← onEnable でワールドを触ると落ちる");

            // ★ depend に書いた物が無いとサーバーは起動を止める。
            //   このプラグインは MOD にもプラグインにも依存していない。
            //   （MOD のアイテムは give コマンド経由で、API を呼んでいないため）
            check("depend が空 (MOD の有無で起動が止まらない)",
                    d.getDepend() == null || d.getDepend().isEmpty(),
                    "実際=" + d.getDepend());
        }
        System.out.println();
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
