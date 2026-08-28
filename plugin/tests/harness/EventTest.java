import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Drives JidaiCraft.onRightClick(...) directly, without a running server.
 * Player and Block are java.lang.reflect.Proxy stand-ins.
 */
public class EventTest {

    static int pass = 0, fail = 0;

    static void check(String label, boolean cond, String detail) {
        if (cond) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + detail); }
    }

    /** Records every sendMessage(String) the plugin sends to this player. */
    static class PlayerStub implements InvocationHandler {
        final String name;
        final List<String> messages = new ArrayList<>();
        PlayerStub(String name) { this.name = name; }
        public Object invoke(Object proxy, Method m, Object[] args) {
            switch (m.getName()) {
                case "getName": return name;
                case "sendMessage":
                    if (args != null && args.length == 1 && args[0] instanceof String s) {
                        messages.add(s);
                        return null;
                    }
                    return null;
                case "toString": return "PlayerStub(" + name + ")";
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == args[0];
                default:
                    Class<?> r = m.getReturnType();
                    if (r == boolean.class) return false;
                    if (r == int.class) return 0;
                    if (r == double.class) return 0.0;
                    if (r == float.class) return 0.0f;
                    if (r == long.class) return 0L;
                    return null;
            }
        }
    }

    static class BlockStub implements InvocationHandler {
        final Material type; final int x, y, z;
        BlockStub(Material type, int x, int y, int z) { this.type = type; this.x = x; this.y = y; this.z = z; }

        /**
         * ★ 施設の鍵は "世界名,x,y,z" になった（ネザーと現世で同じ座標が
         *   あるため）。世界を返さないと Basho.kagi で落ちる。
         */
        static final org.bukkit.World SEKAI = (org.bukkit.World) java.lang.reflect.Proxy
                .newProxyInstance(EventTest.class.getClassLoader(),
                        new Class<?>[]{org.bukkit.World.class},
                        (p, m, a) -> {
                            switch (m.getName()) {
                                case "getName": return "world";
                                case "toString": return "WorldStub";
                                case "hashCode": return 1;
                                case "equals": return p == a[0];
                            }
                            Class<?> r = m.getReturnType();
                            if (r == boolean.class) return false;
                            if (r == int.class) return 0;
                            if (r == double.class) return 0.0;
                            if (r == float.class) return 0.0f;
                            if (r == long.class) return 0L;
                            return null;
                        });

        public Object invoke(Object proxy, Method m, Object[] args) {
            switch (m.getName()) {
                case "getWorld": return SEKAI;
                case "getType": return type;
                case "getX": return x;
                case "getY": return y;
                case "getZ": return z;
                case "toString": return "BlockStub(" + type + ")";
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == args[0];
                default:
                    Class<?> r = m.getReturnType();
                    if (r == boolean.class) return false;
                    if (r == int.class) return 0;
                    if (r == double.class) return 0.0;
                    if (r == float.class) return 0.0f;
                    if (r == long.class) return 0L;
                    return null;
            }
        }
    }

    static Player player(PlayerStub h) {
        return (Player) Proxy.newProxyInstance(EventTest.class.getClassLoader(),
                new Class<?>[]{Player.class}, h);
    }

    static Block block(Material t, int x, int y, int z) {
        return (Block) Proxy.newProxyInstance(EventTest.class.getClassLoader(),
                new Class<?>[]{Block.class}, new BlockStub(t, x, y, z));
    }

    public static void main(String[] args) throws Exception {
        Class<?> pluginClass = Class.forName("jidai.JidaiCraft");

        // JavaPlugin's constructor demands a real PluginClassLoader, so allocate
        // the object without running any constructor, then inject a logger.
        Field theUnsafe = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);
        Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);
        Object plugin = allocate.invoke(unsafe, pluginClass);

        Field loggerField = JavaPlugin.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(plugin, Logger.getLogger("EventTest"));

        Method handler = pluginClass.getMethod("onRightClick", PlayerInteractEvent.class);

        System.out.println("=== 段階2: 右クリック検知の実測 ===\n");

        // ★★ このハーネスは取り下げた（2026-08-19） ★★
        //   段階2の当時、エメラルドブロックの右クリックは「知らせを1通出す」
        //   だけだった。いまは施設として登録されているかを Basho に聞き、
        //   登録済みなら販売所の画面を開く（登録が無ければ何もしない）。
        //   期待している中身がもう違うので、直しても意味が無い。
        //
        //   いまの右クリックの振り分けは Stage4bTest（24項目）が
        //   実物の Basho ごと測っている。BeaconTest / ScanTest も同じ経路を通る。
        if (true) {
            System.out.println("  [取り下げ] 右クリック検知 ── "
                    + "経路が作り直され、期待が古い。"
                    + "いまの振り分けは Stage4bTest が測っている");
            System.out.println();
            System.out.println("================================");
            System.out.println("結果: この段階のハーネスは役目を終えた");
            return;
        }

        // --- 1) 本来の経路: 右手でエメラルドブロックを右クリック ---
        PlayerStub ronty = new PlayerStub("ronty");
        PlayerInteractEvent e1 = new PlayerInteractEvent(player(ronty), Action.RIGHT_CLICK_BLOCK,
                null, block(Material.EMERALD_BLOCK, 100, 64, -200), BlockFace.UP, EquipmentSlot.HAND);
        handler.invoke(plugin, e1);
        check("エメラルドブロック右クリックでメッセージが1通だけ届く",
                ronty.messages.size() == 1, "実測=" + ronty.messages.size() + "通");
        check("イベントがキャンセルされる (ブロック設置などが起きない)",
                e1.isCancelled(), "isCancelled=" + e1.isCancelled());
        String msg = ronty.messages.isEmpty() ? "" : ronty.messages.get(0);
        check("メッセージに本人の名前が入る", msg.contains("ronty"), "実測=" + msg);
        check("メッセージに座標 100 64 -200 が入る",
                msg.contains("100") && msg.contains("64") && msg.contains("-200"), "実測=" + msg);
        System.out.println("      実際の文面: " + msg + "\n");

        // --- 2) 左手ぶんのイベント: 無視されること (二重発火の罠) ---
        PlayerStub off = new PlayerStub("ronty");
        PlayerInteractEvent e2 = new PlayerInteractEvent(player(off), Action.RIGHT_CLICK_BLOCK,
                null, block(Material.EMERALD_BLOCK, 100, 64, -200), BlockFace.UP, EquipmentSlot.OFF_HAND);
        handler.invoke(plugin, e2);
        check("左手ぶんのイベントは無視される (1クリックで2通にならない)",
                off.messages.isEmpty(), "実測=" + off.messages.size() + "通");

        // --- 3) 左クリック ---
        PlayerStub left = new PlayerStub("ronty");
        PlayerInteractEvent e3 = new PlayerInteractEvent(player(left), Action.LEFT_CLICK_BLOCK,
                null, block(Material.EMERALD_BLOCK, 1, 2, 3), BlockFace.UP, EquipmentSlot.HAND);
        handler.invoke(plugin, e3);
        check("左クリックでは反応しない", left.messages.isEmpty() && !e3.isCancelled(),
                "msgs=" + left.messages.size() + " cancelled=" + e3.isCancelled());

        // --- 4) 別ブロック ---
        PlayerStub stone = new PlayerStub("ronty");
        PlayerInteractEvent e4 = new PlayerInteractEvent(player(stone), Action.RIGHT_CLICK_BLOCK,
                null, block(Material.STONE, 1, 2, 3), BlockFace.UP, EquipmentSlot.HAND);
        handler.invoke(plugin, e4);
        check("エメラルドブロック以外は反応しない (石)",
                stone.messages.isEmpty() && !e4.isCancelled(),
                "msgs=" + stone.messages.size() + " cancelled=" + e4.isCancelled());

        PlayerStub chest = new PlayerStub("ronty");
        PlayerInteractEvent e5 = new PlayerInteractEvent(player(chest), Action.RIGHT_CLICK_BLOCK,
                null, block(Material.CHEST, 1, 2, 3), BlockFace.UP, EquipmentSlot.HAND);
        handler.invoke(plugin, e5);
        check("エメラルドブロック以外は反応しない (チェスト)",
                chest.messages.isEmpty() && !e5.isCancelled(), "msgs=" + chest.messages.size());

        // --- 5) クリックしたブロックが無い場合 ---
        PlayerStub air = new PlayerStub("ronty");
        PlayerInteractEvent e6 = new PlayerInteractEvent(player(air), Action.RIGHT_CLICK_AIR,
                null, null, BlockFace.UP, EquipmentSlot.HAND);
        handler.invoke(plugin, e6);
        check("空振り(ブロック無し)でも落ちない", air.messages.isEmpty(), "msgs=" + air.messages.size());

        // --- 6) ★本題: 3人が同時に別々の販売所を押しても取り違えない ---
        PlayerStub a = new PlayerStub("ronty");
        PlayerStub b = new PlayerStub("okusama");
        PlayerStub c = new PlayerStub("kakikama");
        handler.invoke(plugin, new PlayerInteractEvent(player(a), Action.RIGHT_CLICK_BLOCK,
                null, block(Material.EMERALD_BLOCK, 10, 64, 10), BlockFace.UP, EquipmentSlot.HAND));
        handler.invoke(plugin, new PlayerInteractEvent(player(b), Action.RIGHT_CLICK_BLOCK,
                null, block(Material.EMERALD_BLOCK, 20, 64, 20), BlockFace.UP, EquipmentSlot.HAND));
        handler.invoke(plugin, new PlayerInteractEvent(player(c), Action.RIGHT_CLICK_BLOCK,
                null, block(Material.EMERALD_BLOCK, 30, 64, 30), BlockFace.UP, EquipmentSlot.HAND));
        check("3人同時: ronty には ronty 宛の1通だけ",
                a.messages.size() == 1 && a.messages.get(0).contains("ronty")
                        && !a.messages.get(0).contains("okusama"), String.valueOf(a.messages));
        check("3人同時: okusama には okusama 宛の1通だけ",
                b.messages.size() == 1 && b.messages.get(0).contains("okusama"), String.valueOf(b.messages));
        check("3人同時: kakikama には kakikama 宛の1通だけ",
                c.messages.size() == 1 && c.messages.get(0).contains("kakikama"), String.valueOf(c.messages));
        check("3人同時: 座標も取り違えない",
                a.messages.get(0).contains("10 64 10")
                        && b.messages.get(0).contains("20 64 20")
                        && c.messages.get(0).contains("30 64 30"),
                a.messages + " / " + b.messages + " / " + c.messages);

        System.out.println("\n================================");
        System.out.println("結果: PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
