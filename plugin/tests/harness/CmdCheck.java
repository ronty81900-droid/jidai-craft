import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * plugin.yml の commands を、Bukkit 自身の読み取り機で読んで確かめる。
 *
 * ★★ 2026-08-21: 見せるだけだったのを「検査」にした ★★
 *   以前は鍵の一覧を並べるだけで、1件も数えていなかった。
 *   commands の書き方が壊れていても全体は緑のまま通り抜ける。
 *
 * ★ ここで見るのは「Bukkit がどう読んだか」であって、
 *   コードがそのコマンドを処理するかどうかではない。後者は onCommand 側の話。
 *   ただし plugin.yml に無いコマンドは【そもそも呼ばれない】ので、
 *   ここが通らないとコードをいくら直しても動かない。
 */
public class CmdCheck {

    static int pass = 0, fail = 0;

    static void check(String label, boolean jouken, String shousai) {
        if (jouken) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + shousai); }
    }

    public static void main(String[] a) throws Exception {
        try (JarFile jar = new JarFile(a[0]);
             InputStream in = jar.getInputStream(jar.getJarEntry("plugin.yml"))) {

            PluginDescriptionFile d = new PluginDescriptionFile(in);
            Map<String, Map<String, Object>> cmds = d.getCommands();
            System.out.println("      version=" + d.getVersion()
                    + " / commands=" + cmds.keySet());

            check("commands を1つ以上 宣言している", !cmds.isEmpty(), "空");

            for (String k : cmds.keySet()) {
                // ★ 大文字や空白が混じると、打った文字と一致せず反応しない。
                //   Bukkit は名前をそのまま鍵にするので、ここで弾く。
                check("コマンド名が小文字で空白なし: [" + k + "]",
                        k.equals(k.toLowerCase()) && !k.contains(" "), k);
                // ★ description が無いと /help に出ず、運営が使い方を追えない。
                check("説明がある: [" + k + "]",
                        cmds.get(k).get("description") != null, "description 無し");
            }

            // ★ 別名(aliases)が本名とぶつかると、後から読んだ方が勝って
            //   別のコマンドが動く。ぶつかっていないかを見る。
            for (String k : cmds.keySet()) {
                Object al = cmds.get(k).get("aliases");
                if (al instanceof List<?> l) {
                    for (Object x : l) {
                        check("別名が他のコマンド名とぶつからない: " + k + " -> " + x,
                                !cmds.containsKey(String.valueOf(x)), String.valueOf(x));
                    }
                }
            }
        }
        System.out.println();
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
