import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

/** Verifies /jidai scan: marker discovery, categorisation, and the never-wipe rule. */
public class ScanTest {

    static int pass = 0, fail = 0;
    static final List<String> CMDS = new ArrayList<>();   // データパックへ送った命令
    static final List<Object[]> MARKERS = new ArrayList<>();   // {tags, x, y, z}
    static final Map<String, Material> BLOCKS = new HashMap<>();
    static int forceChunks = 4;
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
        return (T) Proxy.newProxyInstance(ScanTest.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    static class PlayerStub implements InvocationHandler {
        final String name; final Location loc;
        final List<String> messages = new ArrayList<>();
        final List<Inventory> opened = new ArrayList<>();
        PlayerStub(String name, Location loc) { this.name = name; this.loc = loc; }
        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "getName": return name;
                case "getLocation": return loc == null ? null : loc.clone();
                case "sendMessage":
                    // ★ 文字列だけでなく Component も来る（演出つきの通知）。
                    //   片方しか拾わないと「出したのに記録されない」ことになる。
                    if (a != null && a.length == 1) {
                        if (a[0] instanceof String s) {
                            messages.add(s);
                        } else if (a[0] instanceof net.kyori.adventure.text.Component cc) {
                            messages.add(net.kyori.adventure.text.serializer.plain
                                    .PlainTextComponentSerializer.plainText().serialize(cc));
                        }
                    }
                    return null;
                case "openInventory":
                    if (a != null && a.length == 1 && a[0] instanceof Inventory inv) opened.add(inv);
                    return null;
            }
            return fallback(m, p, a);
        }
        String last() { return messages.isEmpty() ? "" : messages.get(messages.size() - 1); }
        boolean saw(String f) { for (String s : messages) if (s.contains(f)) return true; return false; }
        void clear() { messages.clear(); opened.clear(); }
    }

    static class TestCommand extends Command {
        TestCommand(String n) { super(n); }
        @Override public boolean execute(CommandSender s, String l, String[] a) { return true; }
    }

    static void marker(int x, int y, int z, String... tags) {
        MARKERS.add(new Object[]{new LinkedHashSet<>(Arrays.asList(tags)), x, y, z});
    }

    static Block block(Material t, int x, int y, int z) {
        return proxy(Block.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getType": return t;
                case "getX": return x;
                case "getY": return y;
                case "getZ": return z;
                case "getWorld": return world;
            }
            return fallback(m, p, a);
        });
    }

    @SuppressWarnings("unchecked")
    static List<String> ichiran(Object basho, String shu) throws Exception {
        return (List<String>) basho.getClass().getMethod("ichiran", String.class).invoke(basho, shu);
    }

    public static void main(String[] args) throws Exception {
        Field tu = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        tu.setAccessible(true);
        Object unsafe = tu.get(null);
        Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);

        world = proxy(World.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getName": return "world";
                case "getForceLoadedChunks": {
                    List<Chunk> cs = new ArrayList<>();
                    for (int i = 0; i < forceChunks; i++) {
                        cs.add(proxy(Chunk.class, (pc, mc, ac) -> fallback(mc, pc, ac)));
                    }
                    return cs;
                }
                case "getEntitiesByClass": {
                    List<Marker> out = new ArrayList<>();
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
                    // getBlockAt には (int,int,int) と (Location) の2つがある
                    int x, y, z;
                    if (a.length == 1 && a[0] instanceof Location lo) {
                        x = lo.getBlockX(); y = lo.getBlockY(); z = lo.getBlockZ();
                    } else {
                        x = (Integer) a[0]; y = (Integer) a[1]; z = (Integer) a[2];
                    }
                    Material t = BLOCKS.getOrDefault(x + "," + y + "," + z, Material.AIR);
                    return block(t, x, y, z);
                }
            }
            return fallback(m, p, a);
        });

        Server server = proxy(Server.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getWorlds": return new ArrayList<>(List.of(world));
                case "getWorld": return world;
                case "dispatchCommand": CMDS.add(String.valueOf(a[1])); return true;
                case "getConsoleSender":
                    return proxy(org.bukkit.command.ConsoleCommandSender.class,
                            (p9, m9, a9) -> fallback(m9, p9, a9));
                case "getLogger": return Logger.getLogger("ScanTest");
                case "getScoreboardManager": return null;
            }
            return fallback(m, p, a);
        });
        Field bs = Bukkit.class.getDeclaredField("server");
        bs.setAccessible(true); bs.set(null, server);

        System.out.println("=== 第1部: 拠点登録の自動化（マーカー走査）の実測 ===");
        System.out.println();

        // データパックが実際に置くマーカーを、そのまま再現する
        marker(0, 97, 6, "jidai_shisetsu", "jidai_uru_chuo");           // 中央=販売所
        marker(0, 97, 0, "jidai_shisetsu", "jidai_plant");              // 石油プラント(施設でない)
        marker(-6, 101, 6, "jidai_shisetsu", "jidai_ginko", "jidai_ginko_10");
        marker(-3, 101, 6, "jidai_shisetsu", "jidai_ginko", "jidai_ginko_50");
        marker(0, 101, 6, "jidai_shisetsu", "jidai_ginko", "jidai_ginko_zenbu");
        marker(4, 101, 6, "jidai_shisetsu", "jidai_uru");               // 拠点=売却所
        marker(7, 101, 6, "jidai_shisetsu", "jidai_gacha");             // 拠点=ガチャ
        // ★ 2026-08-18 に増えた3種類。ここを足さないと、
        //   「未知の種類が販売所に落ちる」事故を見逃す(実機で1度やった)。
        marker(9, 101, 6, "jidai_shisetsu", "jidai_mise");              // 拠点=販売所
        marker(-4, 101, 6, "jidai_shisetsu", "jidai_shinko");           // 拠点=時代を進める
        marker(-8, 101, 6, "jidai_shisetsu", "jidai_kamado");           // 拠点=エンダーかまど
        marker(-10, 101, 6, "jidai_shisetsu", "jidai_kamado");

        Class<?> bashoClass = Class.forName("jidai.Basho");
        Object basho = bashoClass.getDeclaredConstructor().newInstance();
        Method mScan = bashoClass.getMethod("scan");

        Object k = mScan.invoke(basho);
        Class<?> kk = k.getClass();
        int mise = (Integer) kk.getMethod("mise").invoke(k);
        int uru = (Integer) kk.getMethod("uru").invoke(k);
        int ginko = (Integer) kk.getMethod("ginko").invoke(k);
        int gacha = (Integer) kk.getMethod("gacha").invoke(k);
        int mk = (Integer) kk.getMethod("marker").invoke(k);
        boolean kara = (Boolean) kk.getMethod("karappo").invoke(k);

        check("★マーカーを全件読み取る (11件)", mk == 11, "実測=" + mk);
        int shinko = (Integer) kk.getMethod("shinko").invoke(k);
        int kamado = (Integer) kk.getMethod("kamado").invoke(k);
        check("★★★時代を進める と エンダーかまど が販売所に混ざらない",
                shinko == 1 && kamado == 2 && mise == 2,
                "進行=" + shinko + " かまど=" + kamado + " 販売所=" + mise);
        System.out.println("      ※混ざると浮遊テキストが全部「販売所」になる(実機で発生)");
        check("★種別が正しく振り分けられる (販売所2/売却所1/銀行3/ガチャ1)",
                mise == 2 && uru == 1 && ginko == 3 && gacha == 1,
                "販売所=" + mise + " 売却所=" + uru + " 銀行=" + ginko + " ガチャ=" + gacha);
        check("★施設でないマーカー(石油プラント)は登録しない",
                mise + uru + ginko + gacha + shinko + kamado == 10,
                "合計=" + (mise + uru + ginko + gacha + shinko + kamado));
        check("走査できた時は karappo が false", !kara, "true");
        System.out.println("      販売所=" + ichiran(basho, "hanbaijo"));
        System.out.println("      売却所=" + ichiran(basho, "urikaijo"));
        System.out.println("      銀行  =" + ichiran(basho, "ginko"));
        System.out.println("      ガチャ=" + ichiran(basho, "gacha"));

        // 再スキャンで重複しない
        mScan.invoke(basho);
        mScan.invoke(basho);
        check("★何度スキャンしても重複登録されない",
                ichiran(basho, "ginko").size() == 3 && ichiran(basho, "hanbaijo").size() == 2,
                "銀行=" + ichiran(basho, "ginko").size());

        // 中央のマーカーは jidai_uru_chuo。売却所と取り違えない
        // ★ 販売所は中央(0,97,6)と拠点(9,101,6)の2つ。売却所とは別枠。
        check("★中央のエメラルドは「販売所」であって売却所ではない",
                ichiran(basho, "hanbaijo").contains("world,0,97,6")
                        && !ichiran(basho, "urikaijo").contains("world,0,97,6"),
                String.valueOf(ichiran(basho, "hanbaijo")));
        check("★拠点のエメラルドは「売却所」であって販売所ではない",
                ichiran(basho, "urikaijo").equals(List.of("world,4,101,6")),
                String.valueOf(ichiran(basho, "urikaijo")));

        // ---------- ★最重要: 0件で一覧を壊さない ----------
        MARKERS.clear();
        forceChunks = 0;
        Object k2 = mScan.invoke(basho);
        boolean kara2 = (Boolean) k2.getClass().getMethod("karappo").invoke(k2);
        check("★★マーカーが0件でも、今の登録を消さない",
                ichiran(basho, "ginko").size() == 3
                        && ichiran(basho, "hanbaijo").size() == 2
                        && ichiran(basho, "gacha").size() == 1
                        && ichiran(basho, "kamado").size() == 2,
                "銀行=" + ichiran(basho, "ginko").size()
                        + " 販売所=" + ichiran(basho, "hanbaijo").size());
        check("0件の時は karappo が true になり、呼び出し側が気づける", kara2, "false");
        System.out.println("      (本番でここが上書きされると、全店が黙って死ぬ)");

        // ---------- 保存と再起動 ----------
        File dir = new File(System.getProperty("java.io.tmpdir"), "jidai_scan_test");
        dir.mkdirs();
        File cfg = new File(dir, "config.yml");
        if (cfg.exists()) cfg.delete();

        Class<?> pluginClass = Class.forName("jidai.JidaiCraft");
        Object plugin = allocate.invoke(unsafe, pluginClass);
        Field lf = JavaPlugin.class.getDeclaredField("logger");
        lf.setAccessible(true); lf.set(plugin, Logger.getLogger("ScanTest"));
        Field cf = JavaPlugin.class.getDeclaredField("configFile");
        cf.setAccessible(true); cf.set(plugin, cfg);
        Field clf = JavaPlugin.class.getDeclaredField("classLoader");
        clf.setAccessible(true); clf.set(plugin, ScanTest.class.getClassLoader());
        Field srf = JavaPlugin.class.getDeclaredField("server");
        srf.setAccessible(true); srf.set(plugin, server);

        bashoClass.getMethod("hozon", pluginClass).invoke(basho, plugin);
        check("走査した結果が config.yml に書き出される", cfg.exists(), "ファイルが無い");

        Object basho2 = bashoClass.getDeclaredConstructor().newInstance();
        Object plugin2 = allocate.invoke(unsafe, pluginClass);
        lf.set(plugin2, Logger.getLogger("ScanTest"));
        cf.set(plugin2, cfg);
        clf.set(plugin2, ScanTest.class.getClassLoader());
        srf.set(plugin2, server);
        bashoClass.getMethod("yomikomi", pluginClass).invoke(basho2, plugin2);
        check("★再起動しても登録が残る",
                ichiran(basho2, "ginko").size() == 3
                        && ichiran(basho2, "hanbaijo").size() == 2
                        && ichiran(basho2, "kamado").size() == 2,
                "銀行=" + ichiran(basho2, "ginko") + " 販売所=" + ichiran(basho2, "hanbaijo"));

        // ---------- 右クリックの振り分け ----------
        Field bf = pluginClass.getDeclaredField("basho");
        bf.setAccessible(true); bf.set(plugin, basho);
        Class<?> kaneClass = Class.forName("jidai.Kane");
        Field kf = pluginClass.getDeclaredField("kane");
        kf.setAccessible(true); kf.set(plugin, kaneClass.getDeclaredConstructor().newInstance());
        Field sf2 = pluginClass.getDeclaredField("shop");
        sf2.setAccessible(true);
        sf2.set(plugin, allocate.invoke(unsafe, Class.forName("jidai.Shop")));
        Field gf = pluginClass.getDeclaredField("gacha");
        gf.setAccessible(true);
        gf.set(plugin, allocate.invoke(unsafe, Class.forName("jidai.Gacha")));

        Method onRight = pluginClass.getMethod("onRightClick", PlayerInteractEvent.class);
        Method onBreak = pluginClass.getMethod("onBlockBreak", BlockBreakEvent.class);

        PlayerStub u = new PlayerStub("ronty", null);
        PlayerInteractEvent eu = new PlayerInteractEvent(proxy(Player.class, u),
                Action.RIGHT_CLICK_BLOCK, null, block(Material.EMERALD_BLOCK, 4, 101, 6),
                org.bukkit.block.BlockFace.UP, EquipmentSlot.HAND);
        try { onRight.invoke(plugin, eu); } catch (InvocationTargetException ignored) { }
        // ★ 売却は銀行(金ブロック)の画面へ統合した(2026-08-18)。
        //   古い拠点に残っているエメラルドを押した人が迷わないよう案内を出す。
        check("★★旧・売却所を押すと「銀行へ移った」と案内する",
                eu.isCancelled() && u.saw("銀行"), 
                "cancelled=" + eu.isCancelled() + " " + u.messages);
        System.out.println("      案内: " + u.last());
        CMDS.clear();

        PlayerStub g = new PlayerStub("ronty", null);
        PlayerInteractEvent eg = new PlayerInteractEvent(proxy(Player.class, g),
                Action.RIGHT_CLICK_BLOCK, null, block(Material.GOLD_BLOCK, -6, 101, 6),
                org.bukkit.block.BlockFace.UP, EquipmentSlot.HAND);
        try { onRight.invoke(plugin, eg); } catch (InvocationTargetException ignored) { }
        // ★ 銀行は Ginko の画面へ回す。中身(預金・売却・略奪の振り分け)は
        //   SeiryokuTest の H節で測っている。ここでは
        //   「クリックを確かに捕まえていること」だけを見る。
        check("★★銀行の右クリックを捕まえて打ち切る (プラグインが拾う)",
                eg.isCancelled(), "cancelled=" + eg.isCancelled());
        System.out.println("      ※アドバンスメントは実機で一度も発火しなかったので、"
                + "クリックはプラグインが拾う形に変えた");
        CMDS.clear();

        PlayerStub c = new PlayerStub("ronty", null);
        PlayerInteractEvent ec = new PlayerInteractEvent(proxy(Player.class, c),
                Action.RIGHT_CLICK_BLOCK, null, block(Material.EMERALD_BLOCK, 0, 97, 6),
                org.bukkit.block.BlockFace.UP, EquipmentSlot.HAND);
        try { onRight.invoke(plugin, ec); } catch (InvocationTargetException ignored) { }
        check("★中央のエメラルドは打ち切って販売所を開く", ec.isCancelled(),
                "cancelled=" + ec.isCancelled());

        PlayerStub n = new PlayerStub("ronty", null);
        PlayerInteractEvent en = new PlayerInteractEvent(proxy(Player.class, n),
                Action.RIGHT_CLICK_BLOCK, null, block(Material.EMERALD_BLOCK, 999, 60, 999),
                org.bukkit.block.BlockFace.UP, EquipmentSlot.HAND);
        try { onRight.invoke(plugin, en); } catch (InvocationTargetException ignored) { }
        check("★マーカーが無い座標は反応しない", !en.isCancelled() && n.opened.isEmpty(),
                "cancelled=" + en.isCancelled());

        // ---------- 4種すべて壊せない ----------
        int mamotta = 0;
        Object[][] shisetsu = {
                {Material.EMERALD_BLOCK, 0, 97, 6}, {Material.EMERALD_BLOCK, 4, 101, 6},
                {Material.GOLD_BLOCK, -6, 101, 6}, {Material.DIAMOND_BLOCK, 7, 101, 6}};
        for (Object[] sh : shisetsu) {
            PlayerStub b = new PlayerStub("ronty", null);
            BlockBreakEvent be = new BlockBreakEvent(
                    block((Material) sh[0], (Integer) sh[1], (Integer) sh[2], (Integer) sh[3]),
                    proxy(Player.class, b));
            onBreak.invoke(plugin, be);
            if (be.isCancelled() && b.saw("壊せません")) mamotta++;
        }
        check("★4種の施設すべてが壊せない (販売所・売却所・銀行・ガチャ)",
                mamotta == 4, "守れた数=" + mamotta);

        PlayerStub b2 = new PlayerStub("ronty", null);
        BlockBreakEvent be2 = new BlockBreakEvent(
                block(Material.EMERALD_BLOCK, 999, 60, 999), proxy(Player.class, b2));
        onBreak.invoke(plugin, be2);
        check("未登録のブロックは普通に壊せる", !be2.isCancelled(), "cancelled=" + be2.isCancelled());

        // ---------- 手動の add が従来どおり動く ----------
        Method onCmd = pluginClass.getMethod("onCommand", CommandSender.class,
                Command.class, String.class, String[].class);
        BLOCKS.put("50,64,50", Material.DIAMOND_BLOCK);
        PlayerStub adder = new PlayerStub("ronty", new Location(world, 50, 65, 50));
        onCmd.invoke(plugin, proxy(Player.class, adder), new TestCommand("jidai"),
                "jidai", new String[]{"gacha"});
        check("★手動の /jidai gacha が従来どおり動く",
                ichiran(basho, "gacha").contains("world,50,64,50"),
                String.valueOf(ichiran(basho, "gacha")));

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
