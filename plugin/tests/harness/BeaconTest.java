import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BeaconTest {

    static int pass = 0, fail = 0;
    static final List<Object[]> MARKERS = new ArrayList<>();
    static final Map<String, Material> BLOCKS = new HashMap<>();
    static final Map<String, Boolean> KOUKA = new HashMap<>();   // 座標 → 効果が入っているか
    static World world;

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
        return (T) Proxy.newProxyInstance(BeaconTest.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    static class PlayerStub implements InvocationHandler {
        final List<String> messages = new ArrayList<>();
        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "getName": return "ronty";
                case "sendMessage":
                    if (a != null && a.length == 1 && a[0] instanceof String s) messages.add(s);
                    return null;
            }
            return fallback(m, p, a);
        }
        boolean saw(String f) { for (String s : messages) if (s.contains(f)) return true; return false; }
    }

    static String key(int x, int y, int z) { return x + "," + y + "," + z; }

    /** 「効果が入っている」ことだけを表す、中身の無い印。 */
    static PotionEffect KARA_KOUKA;

    static Block block(int x, int y, int z) {
        String k = key(x, y, z);
        return proxy(Block.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getType": return BLOCKS.getOrDefault(k, Material.AIR);
                case "setType": BLOCKS.put(k, (Material) a[0]); return null;
                case "getX": return x;
                case "getY": return y;
                case "getZ": return z;
                case "getWorld": return world;
                case "getState": {
                    if (BLOCKS.getOrDefault(k, Material.AIR) != Material.BEACON) return null;
                    return proxy(Beacon.class, (pb, mb, ab) -> {
                        switch (mb.getName()) {
                            case "getPrimaryEffect":
                                // 中身は見られないので、空の PotionEffect を作って渡す
                                // (PotionEffectType はアイテム登録簿を要求するため使えない)
                                return KOUKA.getOrDefault(k, false) ? KARA_KOUKA : null;
                            case "getSecondaryEffect": return null;
                            case "setPrimaryEffect": KOUKA.put(k, false); return null;
                            case "setSecondaryEffect": return null;
                            case "update": return true;
                        }
                        return fallback(mb, pb, ab);
                    });
                }
            }
            return fallback(m, p, a);
        });
    }

    static void marker(int x, int y, int z, String... tags) {
        MARKERS.add(new Object[]{new LinkedHashSet<>(Arrays.asList(tags)), x, y, z});
    }

    public static void main(String[] args) throws Exception {
        Field tu = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        tu.setAccessible(true);
        Object unsafe = tu.get(null);
        Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);
        KARA_KOUKA = (PotionEffect) allocate.invoke(unsafe, PotionEffect.class);

        world = proxy(World.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getName": return "world";
                case "getForceLoadedChunks": return new ArrayList<org.bukkit.Chunk>();
                case "getEntitiesByClass": {
                    List<Marker> out = new ArrayList<>();
                    if (!a[0].equals(Marker.class)) return out;
                    for (Object[] mk : MARKERS) {
                        @SuppressWarnings("unchecked")
                        Set<String> tags = (Set<String>) mk[0];
                        int x = (Integer) mk[1], y = (Integer) mk[2], z = (Integer) mk[3];
                        out.add(proxy(Marker.class, (pm, mm, am) -> {
                            switch (mm.getName()) {
                                case "getScoreboardTags": return tags;
                                case "getLocation": return new Location(world, x, y, z);
                            }
                            return fallback(mm, pm, am);
                        }));
                    }
                    return out;
                }
                case "getBlockAt": {
                    int x, y, z;
                    if (a.length == 1 && a[0] instanceof Location lo) {
                        x = lo.getBlockX(); y = lo.getBlockY(); z = lo.getBlockZ();
                    } else { x = (Integer) a[0]; y = (Integer) a[1]; z = (Integer) a[2]; }
                    return block(x, y, z);
                }
            }
            return fallback(m, p, a);
        });

        Server server = proxy(Server.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getWorlds": return new ArrayList<>(List.of(world));
                case "getWorld": return world;
                case "getLogger": return Logger.getLogger("BeaconTest");
                case "getScoreboardManager": {
                    // 中央の時代を読むために要る。chuo の目的は用意しないので
                    // Kane は「時代1」と安全側に倒す
                    org.bukkit.scoreboard.Scoreboard sb = proxy(
                            org.bukkit.scoreboard.Scoreboard.class,
                            (p2, m2, a2) -> fallback(m2, p2, a2));
                    return proxy(org.bukkit.scoreboard.ScoreboardManager.class, (p2, m2, a2) ->
                            m2.getName().equals("getMainScoreboard") ? sb : fallback(m2, p2, a2));
                }
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true); bs.set(null, server);

        System.out.println("=== ビーコンによる可視化（プラグイン側）の実測 ===");
        System.out.println();

        // データパックが置く想定のマーカーとブロックを再現する
        // 拠点: (100,70,100) / 中央: (0,97,0)
        marker(100, 70, 100, "jidai_shisetsu", "jidai_beacon");
        marker(0, 97, 0, "jidai_shisetsu", "jidai_beacon_chuo");
        BLOCKS.put(key(100, 70, 100), Material.BEACON);
        BLOCKS.put(key(100, 71, 100), Material.GREEN_STAINED_GLASS);
        BLOCKS.put(key(0, 97, 0), Material.BEACON);
        BLOCKS.put(key(0, 98, 0), Material.WHITE_STAINED_GLASS);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BLOCKS.put(key(100 + dx, 69, 100 + dz), Material.IRON_BLOCK);
                BLOCKS.put(key(dx, 96, dz), Material.IRON_BLOCK);
            }
        }

        Class<?> bashoClass = Class.forName("jidai.Basho");
        Object basho = bashoClass.getDeclaredConstructor().newInstance();
        Object k = bashoClass.getMethod("scan").invoke(basho);
        int beacons = (Integer) k.getClass().getMethod("beacon").invoke(k);
        check("★マーカーからビーコンを登録する (拠点1 + 中央1)", beacons == 2, "実測=" + beacons);

        // ---------- 効果を消す ----------
        KOUKA.put(key(100, 70, 100), true);      // 採掘速度上昇が入っている状態
        KOUKA.put(key(0, 97, 0), true);
        int keshita = (Integer) bashoClass.getMethod("koukaKesu").invoke(basho);
        check("★★ビーコンに効果が入っていたら消す", keshita == 2, "消した数=" + keshita);
        check("消したあとは効果が残っていない",
                !KOUKA.get(key(100, 70, 100)) && !KOUKA.get(key(0, 97, 0)), "残っている");
        int futatabi = (Integer) bashoClass.getMethod("koukaKesu").invoke(basho);
        check("効果が無ければ何もしない (無駄に書き換えない)", futatabi == 0, "実測=" + futatabi);

        // ---------- 中央の色が時代で変わる ----------
        Method chuoIro = bashoClass.getMethod("chuoIro", int.class);
        Material[] machigai = new Material[5];
        chuoIro.invoke(basho, 1); machigai[1] = BLOCKS.get(key(0, 98, 0));
        chuoIro.invoke(basho, 2); machigai[2] = BLOCKS.get(key(0, 98, 0));
        chuoIro.invoke(basho, 3); machigai[3] = BLOCKS.get(key(0, 98, 0));
        chuoIro.invoke(basho, 4); machigai[4] = BLOCKS.get(key(0, 98, 0));
        check("★中央のガラスが時代で変わる (白→黄→橙→赤)",
                machigai[1] == Material.WHITE_STAINED_GLASS
                        && machigai[2] == Material.YELLOW_STAINED_GLASS
                        && machigai[3] == Material.ORANGE_STAINED_GLASS
                        && machigai[4] == Material.RED_STAINED_GLASS,
                Arrays.toString(machigai));
        check("★拠点のガラスは時代で変わらない (勢力色のまま)",
                BLOCKS.get(key(100, 71, 100)) == Material.GREEN_STAINED_GLASS,
                String.valueOf(BLOCKS.get(key(100, 71, 100))));
        System.out.println("      中央: " + machigai[1] + " → " + machigai[2]
                + " → " + machigai[3] + " → " + machigai[4]);

        // ---------- プラグイン本体を用意 ----------
        Class<?> pluginClass = Class.forName("jidai.JidaiCraft");
        Object plugin = allocate.invoke(unsafe, pluginClass);
        Field lf = JavaPlugin.class.getDeclaredField("logger");
        lf.setAccessible(true); lf.set(plugin, Logger.getLogger("BeaconTest"));
        Field srf = JavaPlugin.class.getDeclaredField("server");
        srf.setAccessible(true); srf.set(plugin, server);
        Field bf = pluginClass.getDeclaredField("basho");
        bf.setAccessible(true); bf.set(plugin, basho);
        // 手で登録すると設定ファイルへ書き出すので、置き場所を用意する
        java.io.File dir = new java.io.File(System.getProperty("java.io.tmpdir"), "jidai_beacon_test");
        dir.mkdirs();
        java.io.File cfg = new java.io.File(dir, "config.yml");
        if (cfg.exists()) cfg.delete();
        Field cff = JavaPlugin.class.getDeclaredField("configFile");
        cff.setAccessible(true); cff.set(plugin, cfg);
        Field clf = JavaPlugin.class.getDeclaredField("classLoader");
        clf.setAccessible(true); clf.set(plugin, BeaconTest.class.getClassLoader());

        Method onRight = pluginClass.getMethod("onRightClick", PlayerInteractEvent.class);
        Method onBreak = pluginClass.getMethod("onBlockBreak", BlockBreakEvent.class);

        // ---------- 右クリックの打ち切り ----------
        PlayerStub p1 = new PlayerStub();
        PlayerInteractEvent e1 = new PlayerInteractEvent(proxy(Player.class, p1),
                Action.RIGHT_CLICK_BLOCK, null, block(100, 70, 100), BlockFace.UP, EquipmentSlot.HAND);
        onRight.invoke(plugin, e1);
        check("★★ビーコンの右クリックを打ち切る (効果を選ばせない)", e1.isCancelled(),
                "cancelled=" + e1.isCancelled());

        // 登録していないビーコンも塞ぐ（誰が置いても経済が壊れるため）
        BLOCKS.put(key(500, 64, 500), Material.BEACON);
        PlayerStub p2 = new PlayerStub();
        PlayerInteractEvent e2 = new PlayerInteractEvent(proxy(Player.class, p2),
                Action.RIGHT_CLICK_BLOCK, null, block(500, 64, 500), BlockFace.UP, EquipmentSlot.HAND);
        onRight.invoke(plugin, e2);
        check("★★登録していないビーコンも打ち切る (参加者が置いた分も)", e2.isCancelled(),
                "cancelled=" + e2.isCancelled());

        // ---------- 壊せない範囲 ----------
        Object[][] mamoru = {
                {100, 70, 100, "ビーコン本体"},
                {100, 71, 100, "上のガラス"},
                {99, 69, 99, "土台の角"},
                {100, 69, 100, "土台の中央"},
                {101, 69, 101, "土台の反対の角"},
                {0, 97, 0, "中央のビーコン"},
                {0, 98, 0, "中央のガラス"},
        };
        int mamotta = 0;
        List<String> more = new ArrayList<>();
        for (Object[] b : mamoru) {
            PlayerStub ps = new PlayerStub();
            BlockBreakEvent be = new BlockBreakEvent(
                    block((Integer) b[0], (Integer) b[1], (Integer) b[2]), proxy(Player.class, ps));
            onBreak.invoke(plugin, be);
            if (be.isCancelled()) mamotta++;
            else more.add((String) b[3]);
        }
        check("★★ビーコン・ガラス・土台3×3 がすべて壊せない (7か所)",
                mamotta == mamoru.length, "守れなかった: " + more);

        // 範囲の外は普通に壊せる
        Object[][] kowaseru = {
                {102, 69, 100, "土台の1つ外"},
                {100, 72, 100, "ガラスの1つ上"},
                {100, 68, 100, "土台の1つ下"},
                {500, 64, 500, "登録していないビーコン"},
        };
        int kowaseta = 0;
        List<String> dame = new ArrayList<>();
        for (Object[] b : kowaseru) {
            PlayerStub ps = new PlayerStub();
            BlockBreakEvent be = new BlockBreakEvent(
                    block((Integer) b[0], (Integer) b[1], (Integer) b[2]), proxy(Player.class, ps));
            onBreak.invoke(plugin, be);
            if (!be.isCancelled()) kowaseta++;
            else dame.add((String) b[3]);
        }
        check("★守る範囲の外は普通に壊せる (行き過ぎた保護をしない)",
                kowaseta == kowaseru.length, "壊せなかった: " + dame);

        PlayerStub ps2 = new PlayerStub();
        BlockBreakEvent be2 = new BlockBreakEvent(block(100, 70, 100), proxy(Player.class, ps2));
        onBreak.invoke(plugin, be2);
        check("壊せない理由が本人に伝わる", ps2.saw("壊せません"), String.valueOf(ps2.messages));

        // ---------- 手で登録する道（データパックがまだ置いていない時） ----------
        Method onCmd = pluginClass.getMethod("onCommand", org.bukkit.command.CommandSender.class,
                org.bukkit.command.Command.class, String.class, String[].class);
        Class<?> kaneClass = Class.forName("jidai.Kane");
        Field kf = pluginClass.getDeclaredField("kane");
        kf.setAccessible(true); kf.set(plugin, kaneClass.getDeclaredConstructor().newInstance());

        // 足元(200,80,200)にビーコン、その上は空気
        BLOCKS.put(key(200, 80, 200), Material.BEACON);

        class Tate extends org.bukkit.command.Command {
            Tate(String n) { super(n); }
            @Override public boolean execute(org.bukkit.command.CommandSender s2,
                                             String l, String[] a2) { return true; }
        }
        PlayerStub pc = new PlayerStub() {
            @Override public Object invoke(Object p, Method m, Object[] a) {
                if (m.getName().equals("getLocation")) return new Location(world, 200, 81, 200);
                return super.invoke(p, m, a);
            }
        };
        onCmd.invoke(plugin, proxy(Player.class, pc), new Tate("jidai"), "jidai",
                new String[]{"chuo"});
        check("★/jidai chuo で中央の目印を手で登録できる",
                pc.saw("登録しました"), String.valueOf(pc.messages));
        check("★★登録した瞬間に、今の時代の色のガラスが乗る",
                BLOCKS.get(key(200, 81, 200)) == Material.WHITE_STAINED_GLASS,
                "実測=" + BLOCKS.get(key(200, 81, 200)));

        // 関係ないブロックがあった時は潰さない
        BLOCKS.put(key(200, 81, 200), Material.IRON_BLOCK);
        bashoClass.getMethod("chuoIro", int.class).invoke(basho, 4);
        check("★ガラス以外のブロックは塗り潰さない",
                BLOCKS.get(key(200, 81, 200)) == Material.IRON_BLOCK,
                "実測=" + BLOCKS.get(key(200, 81, 200)));

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
