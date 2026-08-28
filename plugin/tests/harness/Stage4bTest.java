import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public class Stage4bTest {

    static int pass = 0, fail = 0;
    static Object unsafe; static Method allocate;

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
        return (T) Proxy.newProxyInstance(Stage4bTest.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    static World world; static Block ashimotoBlock;

    /** Command is abstract, so the test supplies a minimal concrete one. */
    static class TestCommand extends Command {
        TestCommand(String name) { super(name); }
        @Override
        public boolean execute(CommandSender sender, String label, String[] args) { return true; }
    }

    static Block block(Material type, int x, int y, int z) {
        return proxy(Block.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getType": return type;
                case "getX": return x;
                case "getY": return y;
                case "getZ": return z;
                case "getWorld": return world;
            }
            return fallback(m, p, a);
        });
    }

    static class PlayerStub implements InvocationHandler {
        final String name; final Location loc;
        final List<String> messages = new ArrayList<>();
        final List<Inventory> opened = new ArrayList<>();
        PlayerStub(String name, Location loc) { this.name = name; this.loc = loc; }
        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "getName": return name;
                case "getLocation": return loc.clone();
                case "sendMessage":
                    if (a != null && a.length == 1 && a[0] instanceof String s) messages.add(s);
                    return null;
                case "openInventory":
                    if (a != null && a.length == 1 && a[0] instanceof Inventory inv) opened.add(inv);
                    return null;
            }
            return fallback(m, p, a);
        }

        boolean saw(String f) {
            for (String s : messages) if (s.contains(f)) return true;
            return false;
        }
    }

    static Class<?> pluginClass;

    /** Builds a plugin instance without running JavaPlugin's constructor. */
    static Object newPlugin(File configFile) throws Exception {
        Object p = allocate.invoke(unsafe, pluginClass);
        Field lf = JavaPlugin.class.getDeclaredField("logger");
        lf.setAccessible(true); lf.set(p, Logger.getLogger("Stage4bTest"));
        Field cf = JavaPlugin.class.getDeclaredField("configFile");
        cf.setAccessible(true); cf.set(p, configFile);
        Field cl = JavaPlugin.class.getDeclaredField("classLoader");
        cl.setAccessible(true); cl.set(p, Stage4bTest.class.getClassLoader());

        // Shop's constructor needs a live server, so allocate it and inject an inventory.
        Class<?> shopClass = Class.forName("jidai.Shop");
        Object shop = allocate.invoke(unsafe, shopClass);
        tanaIreru(shopClass, shop, "IPPAN");
        Inventory inv = proxy(Inventory.class, (pp, mm, aa) ->
                mm.getName().equals("getSize") ? (Object) 9 : fallback(mm, pp, aa));
        Field invF = shopClass.getDeclaredField("inventory");
        invF.setAccessible(true); invF.set(shop, inv);
        Field shopF = pluginClass.getDeclaredField("shop");
        shopF.setAccessible(true); shopF.set(p, shop);
        // 段階9: ガチャは画面を1人ずつ作るので、コンストラクタで組む
        Class<?> gachaClass = Class.forName("jidai.Gacha");
        Object gacha = gachaClass.getDeclaredConstructor(pluginClass).newInstance(p);
        Field gachaF = pluginClass.getDeclaredField("gacha");
        gachaF.setAccessible(true); gachaF.set(p, gacha);
        // 段階8: 場所の管理が Basho に移った
        Class<?> bashoClass = Class.forName("jidai.Basho");
        Field bF = pluginClass.getDeclaredField("basho");
        bF.setAccessible(true);
        bF.set(p, bashoClass.getDeclaredConstructor().newInstance());
        // 段階6: 開く直前に中央の時代を読むので Kane が要る
        Class<?> kaneClass = Class.forName("jidai.Kane");
        Field kaneF = pluginClass.getDeclaredField("kane");
        kaneF.setAccessible(true);
        kaneF.set(p, kaneClass.getDeclaredConstructor().newInstance());
        return p;
    }

    /** Basho の中の、種別ごとの生の並びを取り出す。 */
    @SuppressWarnings("unchecked")
    static List<String> ichiran(Object plugin, String shurui) throws Exception {
        Field f = pluginClass.getDeclaredField("basho");
        f.setAccessible(true);
        Object basho = f.get(plugin);
        return (List<String>) basho.getClass()
                .getMethod("ichiran", String.class).invoke(basho, shurui);
    }

    static void setBasho(Object plugin, List<String> v) throws Exception {
        List<String> l = ichiran(plugin, "hanbaijo");
        l.clear(); l.addAll(v);
    }

    static List<String> getBasho(Object plugin) throws Exception {
        return ichiran(plugin, "hanbaijo");
    }

    public static void main(String[] args) throws Exception {
        Field tu = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        tu.setAccessible(true); unsafe = tu.get(null);
        allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);
        pluginClass = Class.forName("jidai.JidaiCraft");

        // 段階6: 開く時に中央の時代を読むので、最小限のサーバーを立てておく。
        // chuo の目的は用意しない → Kane は「時代1」と安全側に倒す。
        org.bukkit.scoreboard.Scoreboard sb = proxy(org.bukkit.scoreboard.Scoreboard.class,
                (p, m, a) -> fallback(m, p, a));
        org.bukkit.scoreboard.ScoreboardManager sm = proxy(org.bukkit.scoreboard.ScoreboardManager.class,
                (p, m, a) -> m.getName().equals("getMainScoreboard") ? sb : fallback(m, p, a));
        org.bukkit.Server srv = proxy(org.bukkit.Server.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getScoreboardManager": return sm;
                case "getLogger": return java.util.logging.Logger.getLogger("Stage4bTest");
            }
            return fallback(m, p, a);
        });
        java.lang.reflect.Field bsf = org.bukkit.Bukkit.class.getDeclaredField("server");
        bsf.setAccessible(true); bsf.set(null, srv);

        world = proxy(World.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getName": return "world";
                case "getBlockAt": return ashimotoBlock;
            }
            return fallback(m, p, a);
        });

        System.out.println("=== 段階4b: 販売所の登録とブロック保護の実測 ===");
        System.out.println();

        File dir = new File(System.getProperty("java.io.tmpdir"), "jidai_cfg_test");
        dir.mkdirs();
        File cfg = new File(dir, "config.yml");
        if (cfg.exists()) cfg.delete();

        Object plugin = newPlugin(cfg);
        setBasho(plugin, List.of());

        Method onRightClick = pluginClass.getMethod("onRightClick", PlayerInteractEvent.class);
        Method onBlockBreak = pluginClass.getMethod("onBlockBreak", BlockBreakEvent.class);
        Method onCommand = pluginClass.getMethod("onCommand", CommandSender.class,
                Command.class, String.class, String[].class);

        // ---------- A. 登録していない販売所は反応しない ----------
        Block emerald = block(Material.EMERALD_BLOCK, 10, 64, -20);
        Location loc = new Location(world, 10, 65, -20);

        PlayerStub a1 = new PlayerStub("ronty", loc);
        onRightClick.invoke(plugin, new PlayerInteractEvent(proxy(Player.class, a1),
                Action.RIGHT_CLICK_BLOCK, null, emerald, BlockFace.UP, EquipmentSlot.HAND));
        check("★未登録のエメラルドブロックは反応しない (画面が開かない)",
                a1.opened.isEmpty(), "開いた回数=" + a1.opened.size());

        // 登録してから
        setBasho(plugin, List.of("world,10,64,-20"));
        PlayerStub a2 = new PlayerStub("ronty", loc);
        PlayerInteractEvent ev2 = new PlayerInteractEvent(proxy(Player.class, a2),
                Action.RIGHT_CLICK_BLOCK, null, emerald, BlockFace.UP, EquipmentSlot.HAND);
        // 段階6以降、開く前に棚を並べ直すためアイテム生成に触れる。
        // この環境ではそこで止まるので、「そこまで進んだ」ことを開いた証拠とする。
        boolean tatoritsuita = false;
        try {
            onRightClick.invoke(plugin, ev2);
            tatoritsuita = a2.opened.size() == 1;
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            while (c.getCause() != null) c = c.getCause();
            // ★★ ItemStack の壁 ★★
            //   サーバー本体が無いと、商品のアイテムを作る所で必ず止まる。
            //   止まり方は版で違う（1.21.10=RegistryAccess /
            //   1.20.1=Bukkit.getItemFactory() が null。どちらも実測）。
            //   だから【文言】ではなく【どこで止まったか】で見る。
            //   jidai.Shop の中まで来ていれば、右クリックの振り分けは
            //   正しく販売所へ届いている。
            tatoritsuita = false;
            for (StackTraceElement f : c.getStackTrace()) {
                if (f.getClassName().equals("jidai.Shop")) {
                    tatoritsuita = true;
                    break;
                }
            }
            System.out.println("      止まった所: " + c);
        }
        check("登録済みなら販売所を開く処理に進む", tatoritsuita, "進まなかった");
        check("登録済みの右クリックは打ち切られる", ev2.isCancelled(), "cancelled=" + ev2.isCancelled());

        // 同じ座標でも世界が違えば別物
        Block otherWorld = proxy(Block.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getType": return Material.EMERALD_BLOCK;
                case "getX": return 10;
                case "getY": return 64;
                case "getZ": return -20;
                case "getWorld": return proxy(World.class, (pw, mw, aw) ->
                        mw.getName().equals("getName") ? "world_nether" : fallback(mw, pw, aw));
            }
            return fallback(m, p, a);
        });
        PlayerStub a3 = new PlayerStub("ronty", loc);
        onRightClick.invoke(plugin, new PlayerInteractEvent(proxy(Player.class, a3),
                Action.RIGHT_CLICK_BLOCK, null, otherWorld, BlockFace.UP, EquipmentSlot.HAND));
        check("★同じ座標でも別の世界なら反応しない", a3.opened.isEmpty(),
                "開いた回数=" + a3.opened.size());

        // ---------- B. ブロック保護 ----------
        PlayerStub b1 = new PlayerStub("ronty", loc);
        BlockBreakEvent br1 = new BlockBreakEvent(emerald, proxy(Player.class, b1));
        onBlockBreak.invoke(plugin, br1);
        check("★登録済みの販売所は壊せない", br1.isCancelled(), "cancelled=" + br1.isCancelled());
        check("壊せない理由が本人に伝わる",
                b1.messages.size() == 1 && b1.messages.get(0).contains("壊せません"),
                String.valueOf(b1.messages));

        Block emerald2 = block(Material.EMERALD_BLOCK, 99, 64, 99);
        PlayerStub b2 = new PlayerStub("ronty", loc);
        BlockBreakEvent br2 = new BlockBreakEvent(emerald2, proxy(Player.class, b2));
        onBlockBreak.invoke(plugin, br2);
        check("★未登録のエメラルドブロックは普通に壊せる", !br2.isCancelled(),
                "cancelled=" + br2.isCancelled());

        Block stone = block(Material.STONE, 1, 2, 3);
        PlayerStub b3 = new PlayerStub("ronty", loc);
        BlockBreakEvent br3 = new BlockBreakEvent(stone, proxy(Player.class, b3));
        onBlockBreak.invoke(plugin, br3);
        check("★関係ないブロックの採掘を邪魔しない", !br3.isCancelled() && b3.messages.isEmpty(),
                "cancelled=" + br3.isCancelled());

        // ---------- C. コマンド ----------
        setBasho(plugin, List.of());
        Command cmd = new TestCommand("jidai");

        // 足元が石
        ashimotoBlock = block(Material.STONE, 5, 64, 5);
        PlayerStub c1 = new PlayerStub("ronty", new Location(world, 5, 65, 5));
        onCommand.invoke(plugin, proxy(Player.class, c1), cmd, "jidai", new String[]{"add"});
        check("add: 足元がエメラルドでなければ登録しない",
                getBasho(plugin).isEmpty() && c1.messages.get(0).contains("エメラルドブロックではありません"),
                getBasho(plugin) + " " + c1.messages);

        // 足元がエメラルド
        ashimotoBlock = block(Material.EMERALD_BLOCK, 5, 64, 5);
        PlayerStub c2 = new PlayerStub("ronty", new Location(world, 5, 65, 5));
        onCommand.invoke(plugin, proxy(Player.class, c2), cmd, "jidai", new String[]{"add"});
        check("add: 足元のエメラルドブロックを登録する",
                getBasho(plugin).equals(List.of("world,5,64,5")), String.valueOf(getBasho(plugin)));
        System.out.println("      文面: " + c2.messages.get(0));

        // 二重登録
        PlayerStub c3 = new PlayerStub("ronty", new Location(world, 5, 65, 5));
        onCommand.invoke(plugin, proxy(Player.class, c3), cmd, "jidai", new String[]{"add"});
        check("add: 二重登録を断る",
                getBasho(plugin).size() == 1 && c3.messages.get(0).contains("既に登録"),
                getBasho(plugin) + " " + c3.messages);

        // list
        PlayerStub c4 = new PlayerStub("ronty", new Location(world, 5, 65, 5));
        onCommand.invoke(plugin, proxy(Player.class, c4), cmd, "jidai", new String[]{"list"});
        check("list: 件数と中身を出す",
                c4.saw("1 件") && c4.saw("world,5,64,5"), String.valueOf(c4.messages));

        // remove
        PlayerStub c5 = new PlayerStub("ronty", new Location(world, 5, 65, 5));
        onCommand.invoke(plugin, proxy(Player.class, c5), cmd, "jidai", new String[]{"remove"});
        check("remove: 登録を解除する", getBasho(plugin).isEmpty(), String.valueOf(getBasho(plugin)));

        PlayerStub c6 = new PlayerStub("ronty", new Location(world, 5, 65, 5));
        onCommand.invoke(plugin, proxy(Player.class, c6), cmd, "jidai", new String[]{"remove"});
        check("remove: 登録されていない所では断る",
                c6.messages.get(0).contains("登録されていません"), String.valueOf(c6.messages));

        // 引数なし
        Object r = onCommand.invoke(plugin, proxy(Player.class,
                new PlayerStub("ronty", new Location(world, 5, 65, 5))), cmd, "jidai", new String[]{});
        check("引数なしなら使い方を出させる (false を返す)", Boolean.FALSE.equals(r), "実測=" + r);

        // コンソールから add
        List<String> consoleMsgs = new ArrayList<>();
        CommandSender console = proxy(CommandSender.class, (p, m, a) -> {
            if (m.getName().equals("sendMessage") && a != null && a.length == 1
                    && a[0] instanceof String s) { consoleMsgs.add(s); return null; }
            return fallback(m, p, a);
        });
        onCommand.invoke(plugin, console, cmd, "jidai", new String[]{"add"});
        check("コンソールからの add は断る (足元が無いため)",
                consoleMsgs.size() == 1 && consoleMsgs.get(0).contains("ゲーム内から"),
                String.valueOf(consoleMsgs));

        // ---------- D. 再起動後も残ること ----------
        ashimotoBlock = block(Material.EMERALD_BLOCK, 7, 70, 7);
        PlayerStub d1 = new PlayerStub("ronty", new Location(world, 7, 71, 7));
        onCommand.invoke(plugin, proxy(Player.class, d1), cmd, "jidai", new String[]{"add"});
        check("登録すると config.yml が書き出される", cfg.exists(), "ファイルが無い");

        String body = new String(java.nio.file.Files.readAllBytes(cfg.toPath()), "UTF-8");
        check("config.yml に座標が入っている", body.contains("world,7,70,7"), body);
        System.out.println("      config.yml の中身: " + body.trim().replace("\n", " / "));

        // 別のインスタンスで読み戻す = サーバー再起動に相当
        Object plugin2 = newPlugin(cfg);
        Method getConfig = JavaPlugin.class.getMethod("getConfig");
        Object conf = getConfig.invoke(plugin2);
        Method getStringList = conf.getClass().getMethod("getStringList", String.class);
        @SuppressWarnings("unchecked")
        List<String> loaded = (List<String>) getStringList.invoke(conf, "hanbaijo");
        check("★再起動しても登録が残る (別インスタンスで読み戻せる)",
                loaded.contains("world,7,70,7"), String.valueOf(loaded));

        // ---------- E2. ガチャ(ダイヤブロック)の登録と保護 ----------
        setBasho(plugin, List.of());
        ichiran(plugin, "gacha").clear();

        // 足元がエメラルドなら /jidai gacha は断る
        ashimotoBlock = block(Material.EMERALD_BLOCK, 8, 64, 8);
        PlayerStub g1 = new PlayerStub("ronty", new Location(world, 8, 65, 8));
        onCommand.invoke(plugin, proxy(Player.class, g1), cmd, "jidai", new String[]{"gacha"});
        check("★/jidai gacha: 足元がダイヤブロックでなければ登録しない",
                ichiran(plugin, "gacha").isEmpty()
                        && g1.messages.get(0).contains("ダイヤブロックではありません"),
                String.valueOf(g1.messages));

        // ダイヤブロックなら登録できる
        ashimotoBlock = block(Material.DIAMOND_BLOCK, 8, 64, 8);
        PlayerStub g2 = new PlayerStub("ronty", new Location(world, 8, 65, 8));
        onCommand.invoke(plugin, proxy(Player.class, g2), cmd, "jidai", new String[]{"gacha"});
        check("/jidai gacha: 足元のダイヤブロックを登録する",
                ichiran(plugin, "gacha").equals(List.of("world,8,64,8")),
                String.valueOf(ichiran(plugin, "gacha")));

        // 未登録のダイヤブロックは反応しない
        Block yosoDia = block(Material.DIAMOND_BLOCK, 77, 64, 77);
        PlayerStub g3 = new PlayerStub("ronty", loc);
        PlayerInteractEvent ge = new PlayerInteractEvent(proxy(Player.class, g3),
                Action.RIGHT_CLICK_BLOCK, null, yosoDia, BlockFace.UP, EquipmentSlot.HAND);
        try { onRightClick.invoke(plugin, ge); } catch (InvocationTargetException ignored) { }
        check("★未登録のダイヤブロックは反応しない",
                g3.opened.isEmpty() && !ge.isCancelled(), "cancelled=" + ge.isCancelled());

        // 登録済みのダイヤブロックは壊せない
        Block dia = block(Material.DIAMOND_BLOCK, 8, 64, 8);
        PlayerStub g4 = new PlayerStub("ronty", loc);
        BlockBreakEvent gbr = new BlockBreakEvent(dia, proxy(Player.class, g4));
        onBlockBreak.invoke(plugin, gbr);
        check("★登録済みのガチャは壊せない", gbr.isCancelled(), "cancelled=" + gbr.isCancelled());

        // 未登録のダイヤブロックは普通に壊せる
        PlayerStub g5 = new PlayerStub("ronty", loc);
        BlockBreakEvent gbr2 = new BlockBreakEvent(yosoDia, proxy(Player.class, g5));
        onBlockBreak.invoke(plugin, gbr2);
        check("★未登録のダイヤブロックは普通に壊せる", !gbr2.isCancelled(),
                "cancelled=" + gbr2.isCancelled());

        System.out.println();
        System.out.println("================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }

    /**
     * ★ Unsafe.allocateInstance は【コンストラクタを通らない】。
     *   そのため 2026-08-20 に足した品揃え(shohin)と店名(namae)が
     *   null のままになり、買おうとすると落ちる。ここで入れてやる。
     *   hako は "IPPAN"(販売所) か "JUKIHIN"(銃器専門店)。
     */
    static void tanaIreru(Class<?> shopClass, Object shop, String hako) throws Exception {
        java.lang.reflect.Field tana = shopClass.getDeclaredField(hako);
        tana.setAccessible(true);
        java.lang.reflect.Field f = shopClass.getDeclaredField("shohin");
        f.setAccessible(true);
        f.set(shop, tana.get(null));
        java.lang.reflect.Field g = shopClass.getDeclaredField("namae");
        g.setAccessible(true);
        g.set(shop, hako.equals("JUKIHIN") ? "銃器専門店" : "販売所");
        java.lang.reflect.Field h = shopClass.getDeclaredField("shurui");
        h.setAccessible(true);
        h.set(shop, hako.equals("JUKIHIN") ? "juki" : "hanbaijo");
    }
}
