import java.io.InputStream;
import java.util.jar.JarFile;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * 組み上がった .jar の plugin.yml を、Bukkit 自身の読み取り機で読んで確かめる。
 *
 * ★★ 2026-08-21: 数えられていなかったのを直した ★★
 *   以前は "RESULT: OK" / "RESULT: FAIL" と出していたが、
 *   走らせ役(run_harness.py)は [PASS] / [FAIL] の数しか数えない。
 *   つまり plugin.yml が壊れていても、全体は緑のまま通り抜けていた。
 *   検査は「壊れている時に本当に落ちる」形で書かないと、無いのと同じ。
 */
public class YmlCheck {

    static int pass = 0, fail = 0;

    static void check(String label, boolean jouken, String shousai) {
        if (jouken) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + shousai); }
    }

    public static void main(String[] args) throws Exception {
        try (JarFile jar = new JarFile(args[0])) {

            var entry = jar.getJarEntry("plugin.yml");
            // ★ これが無いと、plugins に置いてもサーバーは完全に無視する
            //   (エラーすら出ないことがある)。最初に見る。
            check("plugin.yml が .jar に入っている", entry != null, args[0]);
            if (entry == null) { matome(); return; }

            PluginDescriptionFile d;
            try (InputStream in = jar.getInputStream(entry)) {
                d = new PluginDescriptionFile(in);
            }
            System.out.println("      name=" + d.getName() + " version=" + d.getVersion()
                    + " main=" + d.getMain() + " api-version=" + d.getAPIVersion());

            check("name がある", d.getName() != null && !d.getName().isEmpty(), "空");
            check("version がある", d.getVersion() != null && !d.getVersion().isEmpty(), "空");
            check("main がある", d.getMain() != null && !d.getMain().isEmpty(), "空");

            // ★ api-version が無いと「レガシープラグイン」扱いになり、
            //   素材名の解釈が変わる。黙って挙動が変わるので必ず入れる。
            check("api-version がある (無いとレガシー扱い)",
                    d.getAPIVersion() != null && !d.getAPIVersion().isEmpty(),
                    "未設定");

            // ★ main に書いたクラスが .jar に無いと、起動時に
            //   ClassNotFoundException で有効化されない。
            String path = d.getMain().replace('.', '/') + ".class";
            check("main のクラスが .jar に実在する (" + path + ")",
                    jar.getJarEntry(path) != null, "見つからない");
        }
        matome();
    }

    static void matome() {
        System.out.println();
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
