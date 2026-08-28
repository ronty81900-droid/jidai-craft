import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class TabTest {

    static int pass = 0, fail = 0;
    static final Map<String, Map<String, Integer>> BOARD = new HashMap<>();
    static final Map<String, PlayerStub> ONLINE = new LinkedHashMap<>();

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
        if (r == double.class) return 0.0;
        if (r == float.class) return 0.0f;
        if (r == long.class) return 0L;
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> iface, InvocationHandler h) {
        return (T) Proxy.newProxyInstance(TabTest.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    static class PlayerStub implements InvocationHandler {
        final String name; Player self;
        PlayerStub(String name) { this.name = name; }
        public Object invoke(Object p, Method m, Object[] a) {
            return m.getName().equals("getName") ? name : fallback(m, p, a);
        }
    }

    static class TestCommand extends Command {
        TestCommand(String name) { super(name); }
        @Override public boolean execute(CommandSender s, String l, String[] a) { return true; }
    }

    static void join(String name, boolean youhei) {
        PlayerStub h = new PlayerStub(name);
        h.self = proxy(Player.class, h);
        ONLINE.put(name, h);
        BOARD.computeIfAbsent("youhei", k -> new HashMap<>()).put(name, youhei ? 1 : 0);
    }

    @SuppressWarnings("unchecked")
    static List<String> tab(Object plugin, Method mTab, String cmd, String... args) throws Exception {
        return (List<String>) mTab.invoke(plugin,
                proxy(CommandSender.class, (p, m, a) -> fallback(m, p, a)),
                new TestCommand(cmd), cmd, args);
    }

    public static void main(String[] args) throws Exception {
        Field tu = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        tu.setAccessible(true);
        Object unsafe = tu.get(null);
        Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);

        BOARD.put("sekiyu", new HashMap<>());
        BOARD.put("youhei", new HashMap<>());
        BOARD.put("settei", new HashMap<>());

        Scoreboard board = proxy(Scoreboard.class, (p, m, a) -> {
            if (m.getName().equals("getObjective")) {
                String n = (String) a[0];
                if (!BOARD.containsKey(n)) return null;
                return proxy(Objective.class, (p2, m2, a2) -> {
                    if (!m2.getName().equals("getScore")) return fallback(m2, p2, a2);
                    String entry = (a2[0] instanceof Player pl) ? pl.getName() : String.valueOf(a2[0]);
                    return proxy(Score.class, (p3, m3, a3) ->
                            m3.getName().equals("getScore")
                                    ? BOARD.get(n).getOrDefault(entry, 0)
                                    : fallback(m3, p3, a3));
                });
            }
            return fallback(m, p, a);
        });
        ScoreboardManager mgr = proxy(ScoreboardManager.class, (p, m, a) ->
                m.getName().equals("getMainScoreboard") ? board : fallback(m, p, a));
        Server server = proxy(Server.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getScoreboardManager": return mgr;
                case "getOnlinePlayers": {
                    List<Player> l = new ArrayList<>();
                    for (PlayerStub h : ONLINE.values()) l.add(h.self);
                    return l;
                }
                case "getLogger": return Logger.getLogger("TabTest");
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true); bs.set(null, server);

        join("Ronty8190", false);
        join("Kakikama", true);
        join("Kono", true);
        join("Okusan", false);

        Class<?> pluginClass = Class.forName("jidai.JidaiCraft");
        Class<?> kaneClass = Class.forName("jidai.Kane");
        Object plugin = allocate.invoke(unsafe, pluginClass);
        Field lf = JavaPlugin.class.getDeclaredField("logger");
        lf.setAccessible(true); lf.set(plugin, Logger.getLogger("TabTest"));
        // 本物のサーバーでは init() が入れてくれるフィールド
        Field sf = JavaPlugin.class.getDeclaredField("server");
        sf.setAccessible(true); sf.set(plugin, server);
        Field kf = pluginClass.getDeclaredField("kane");
        kf.setAccessible(true); kf.set(plugin, kaneClass.getDeclaredConstructor().newInstance());

        Method mTab = pluginClass.getMethod("onTabComplete", CommandSender.class,
                Command.class, String.class, String[].class);

        System.out.println("=== タブ補完の実測 ===");
        System.out.println();

        List<String> r = tab(plugin, mTab, "jidai", "");
        check("★/jidai <Tab> で小コマンドが出る",
                r.containsAll(List.of("add", "remove", "list", "leader", "beacon")),
                String.valueOf(r));
        check("★/jidai <Tab> にプレイヤー名が混ざらない",
                !r.contains("Ronty8190") && !r.contains("Kakikama"), String.valueOf(r));
        System.out.println("      出る候補: " + r);

        check("/jidai a<Tab> は add だけ",
                tab(plugin, mTab, "jidai", "a").equals(List.of("add")),
                String.valueOf(tab(plugin, mTab, "jidai", "a")));
        check("大文字でも拾える (/jidai A<Tab>)",
                tab(plugin, mTab, "jidai", "A").equals(List.of("add")),
                String.valueOf(tab(plugin, mTab, "jidai", "A")));

        check("★/jidai add <Tab> は何も出さない (人の名前が出ない)",
                tab(plugin, mTab, "jidai", "add", "").isEmpty(),
                String.valueOf(tab(plugin, mTab, "jidai", "add", "")));
        check("★/jidai list <Tab> は何も出さない",
                tab(plugin, mTab, "jidai", "list", "").isEmpty(),
                String.valueOf(tab(plugin, mTab, "jidai", "list", "")));


        List<String> kj = tab(plugin, mTab, "jidai", "kaijo", "");

        List<String> h = tab(plugin, mTab, "hire", "");
        System.out.println("      出る候補: " + h);


        check("★/yes <Tab> は何も出さない", tab(plugin, mTab, "yes", "").isEmpty(),
                String.valueOf(tab(plugin, mTab, "yes", "")));
        check("★/no <Tab> は何も出さない", tab(plugin, mTab, "no", "").isEmpty(),
                String.valueOf(tab(plugin, mTab, "no", "")));

        check("★どの場合も null を返さない (null だと Bukkit が人の名前に戻す)",
                tab(plugin, mTab, "jidai", "add", "") != null
                        && tab(plugin, mTab, "yes", "") != null, "null が返った");

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
