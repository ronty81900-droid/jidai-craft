import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;

/** Drives stage-3 behaviour without a running server. */
public class Stage3Test {

    static int pass = 0, fail = 0;
    static Object unsafe;
    static Method allocate;

    static void check(String label, boolean cond, String detail) {
        if (cond) { pass++; System.out.println("[PASS] " + label); }
        else { fail++; System.out.println("[FAIL] " + label + "  " + detail); }
    }

    /** Default answers so proxies never NPE on primitive returns. */
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

    static class PlayerStub implements InvocationHandler {
        final String name;
        final List<String> messages = new ArrayList<>();
        final List<Inventory> opened = new ArrayList<>();
        PlayerStub(String name) { this.name = name; }
        public Object invoke(Object proxy, Method m, Object[] args) {
            switch (m.getName()) {
                case "getName": return name;
                case "sendMessage":
                    if (args != null && args.length == 1 && args[0] instanceof String s) { messages.add(s); }
                    return null;
                case "openInventory":
                    if (args != null && args.length == 1 && args[0] instanceof Inventory inv) { opened.add(inv); }
                    return null;
            }
            return fallback(m, proxy, args);
        }
    }

    static class BlockStub implements InvocationHandler {
        final Material type;
        BlockStub(Material type) { this.type = type; }
        public Object invoke(Object proxy, Method m, Object[] args) {
            switch (m.getName()) {
                case "getType": return type;
                case "getX": case "getY": case "getZ": return 7;
            }
            return fallback(m, proxy, args);
        }
    }

    static class InventoryStub implements InvocationHandler {
        final InventoryHolder holder; final int size;
        InventoryStub(InventoryHolder holder, int size) { this.holder = holder; this.size = size; }
        public Object invoke(Object proxy, Method m, Object[] args) {
            switch (m.getName()) {
                case "getHolder": return holder;
                case "getSize": return size;
            }
            return fallback(m, proxy, args);
        }
    }

    static class ViewStub implements InvocationHandler {
        final Inventory top, bottom; final Player player;
        ViewStub(Inventory top, Inventory bottom, Player player) {
            this.top = top; this.bottom = bottom; this.player = player;
        }
        public Object invoke(Object proxy, Method m, Object[] args) {
            switch (m.getName()) {
                case "getTopInventory": return top;
                case "getBottomInventory": return bottom;
                case "getPlayer": return player;
                case "getType": return InventoryType.CHEST;
                case "countSlots": return 9 + 36;
            }
            return fallback(m, proxy, args);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> iface, InvocationHandler h) {
        return (T) Proxy.newProxyInstance(Stage3Test.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    public static void main(String[] args) throws Exception {
        Field tu = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        tu.setAccessible(true);
        unsafe = tu.get(null);
        allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);

        Class<?> shopClass = Class.forName("jidai.Shop");
        Class<?> pluginClass = Class.forName("jidai.JidaiCraft");

        // Shop's constructor needs a live server, so allocate it without one.
        Object shop = allocate.invoke(unsafe, shopClass);
        tanaIreru(shopClass, shop, "IPPAN");

        System.out.println("=== 段階3: 販売所の画面の実測 ===\n");

        // ---------- A. 商品データ ----------
        // ★★ この節は取り下げた（2026-08-19） ★★
        //   段階3の当時は Shop.setsumei(int) が説明文を返し、商品は
        //   「パン / 鉄インゴット」だった。その後、販売所は作り直され
        //   （パン・石炭・鉄の胸当て・銃・戦争宣誓・下剋上）、
        //   setsumei という関数そのものが無くなっている。
        //   いまの商品の中身は Stage4Test と Stage5Test が測っている。
        //   ここに古い期待を残すと、動かないまま落ち続ける。
        if (true) {
            System.out.println("  [取り下げ] A. 商品データ ── "
                    + "販売所の作り直しで Shop.setsumei が無くなった。"
                    + "いまの中身は Stage4Test / Stage5Test が測っている");
            System.out.println();
            System.out.println("================================");
            System.out.println("結果: この段階のハーネスは役目を終えた");
            return;
        }
        Method setsumei = shopClass.getMethod("setsumei", int.class);
        String s0 = (String) setsumei.invoke(shop, 0);
        String s1 = (String) setsumei.invoke(shop, 1);
        String s5 = (String) setsumei.invoke(shop, 5);
        check("枠0 = パン ×3 / 個人の金", s0.contains("パン") && s0.contains("×3")
                && s0.contains("個人の金") && s0.contains("2"), "実測=" + s0);
        check("枠1 = 鉄インゴット ×8 / 勢力の金", s1.contains("鉄インゴット") && s1.contains("×8")
                && s1.contains("勢力の金") && s1.contains("20"), "実測=" + s1);
        check("商品の無い枠は「商品なし」", s5.equals("商品なし"), "実測=" + s5);
        System.out.println("      枠0: " + s0);
        System.out.println("      枠1: " + s1 + "\n");

        // ---------- プラグイン本体を用意 ----------
        Object plugin = allocate.invoke(unsafe, pluginClass);
        Field lf = JavaPlugin.class.getDeclaredField("logger");
        lf.setAccessible(true);
        lf.set(plugin, Logger.getLogger("Stage3Test"));
        Field sf = pluginClass.getDeclaredField("shop");
        sf.setAccessible(true);
        sf.set(plugin, shop);

        // Give the allocated Shop an inventory so getInventory() returns it.
        Inventory shopInv = proxy(Inventory.class, new InventoryStub((InventoryHolder) shop, 9));
        Field invField = shopClass.getDeclaredField("inventory");
        invField.setAccessible(true);
        invField.set(shop, shopInv);

        Method onRightClick = pluginClass.getMethod("onRightClick", PlayerInteractEvent.class);
        Method onShopClick = pluginClass.getMethod("onShopClick", InventoryClickEvent.class);

        // ---------- B. 右クリックで販売所が開く ----------
        PlayerStub ps = new PlayerStub("ronty");
        Player p = proxy(Player.class, ps);
        PlayerInteractEvent ev = new PlayerInteractEvent(p, Action.RIGHT_CLICK_BLOCK, null,
                proxy(Block.class, new BlockStub(Material.EMERALD_BLOCK)), BlockFace.UP, EquipmentSlot.HAND);
        onRightClick.invoke(plugin, ev);
        check("エメラルドブロック右クリックで画面が1回だけ開く", ps.opened.size() == 1,
                "実測=" + ps.opened.size() + "回");
        check("開いたのは販売所の画面そのもの",
                ps.opened.size() == 1 && ps.opened.get(0) == shopInv, "別の画面が開いた");
        check("右クリックのイベントは打ち切られる", ev.isCancelled(), "isCancelled=" + ev.isCancelled());

        // 左手ぶんでは開かない (二重に開かない)
        PlayerStub ps2 = new PlayerStub("ronty");
        onRightClick.invoke(plugin, new PlayerInteractEvent(proxy(Player.class, ps2),
                Action.RIGHT_CLICK_BLOCK, null, proxy(Block.class, new BlockStub(Material.EMERALD_BLOCK)),
                BlockFace.UP, EquipmentSlot.OFF_HAND));
        check("左手ぶんでは画面が開かない (二重に開かない)", ps2.opened.isEmpty(),
                "実測=" + ps2.opened.size() + "回");

        // 石では開かない
        PlayerStub ps3 = new PlayerStub("ronty");
        onRightClick.invoke(plugin, new PlayerInteractEvent(proxy(Player.class, ps3),
                Action.RIGHT_CLICK_BLOCK, null, proxy(Block.class, new BlockStub(Material.STONE)),
                BlockFace.UP, EquipmentSlot.HAND));
        check("石を右クリックしても画面は開かない", ps3.opened.isEmpty(), "実測=" + ps3.opened.size() + "回");

        // ---------- C. 販売所の中のクリック ----------
        Inventory playerInv = proxy(Inventory.class, new InventoryStub(null, 36));
        PlayerStub cs = new PlayerStub("ronty");
        Player cp = proxy(Player.class, cs);
        InventoryView shopView = proxy(InventoryView.class, new ViewStub(shopInv, playerInv, cp));

        InventoryClickEvent c0 = new InventoryClickEvent(shopView, InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        onShopClick.invoke(plugin, c0);
        check("販売所の枠0クリックは打ち切られる (取り出せない)", c0.isCancelled(),
                "isCancelled=" + c0.isCancelled());
        check("枠0クリックで商品名がチャットに出る",
                cs.messages.size() == 1 && cs.messages.get(0).contains("パン"),
                String.valueOf(cs.messages));
        System.out.println("      実際の文面: " + (cs.messages.isEmpty() ? "" : cs.messages.get(0)));

        // シフトクリック (持ち出しの典型) も塞がっているか
        PlayerStub sh = new PlayerStub("ronty");
        InventoryView shV = proxy(InventoryView.class, new ViewStub(shopInv, playerInv, proxy(Player.class, sh)));
        InventoryClickEvent cShift = new InventoryClickEvent(shV, InventoryType.SlotType.CONTAINER,
                1, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        onShopClick.invoke(plugin, cShift);
        check("シフトクリックでも取り出せない", cShift.isCancelled(), "isCancelled=" + cShift.isCancelled());

        // 数字キー / Qキー
        PlayerStub nk = new PlayerStub("ronty");
        InventoryView nkV = proxy(InventoryView.class, new ViewStub(shopInv, playerInv, proxy(Player.class, nk)));
        InventoryClickEvent cNum = new InventoryClickEvent(nkV, InventoryType.SlotType.CONTAINER,
                0, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP, 3);
        onShopClick.invoke(plugin, cNum);
        check("数字キーでも取り出せない", cNum.isCancelled(), "isCancelled=" + cNum.isCancelled());

        PlayerStub dq = new PlayerStub("ronty");
        InventoryView dqV = proxy(InventoryView.class, new ViewStub(shopInv, playerInv, proxy(Player.class, dq)));
        InventoryClickEvent cDrop = new InventoryClickEvent(dqV, InventoryType.SlotType.CONTAINER,
                0, ClickType.DROP, InventoryAction.DROP_ONE_SLOT);
        onShopClick.invoke(plugin, cDrop);
        check("Qキー(捨てる)でも取り出せない", cDrop.isCancelled(), "isCancelled=" + cDrop.isCancelled());

        // ---------- D. 自分の持ち物側のクリック ----------
        PlayerStub own = new PlayerStub("ronty");
        InventoryView ownV = proxy(InventoryView.class, new ViewStub(shopInv, playerInv, proxy(Player.class, own)));
        InventoryClickEvent cOwn = new InventoryClickEvent(ownV, InventoryType.SlotType.CONTAINER,
                20, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        onShopClick.invoke(plugin, cOwn);
        check("自分の持ち物側と分かる文面が返る",
                own.messages.size() == 1 && own.messages.get(0).contains("自分の持ち物"),
                String.valueOf(own.messages));

        // ---------- E. ★最重要: 販売所でない画面を壊さない ----------
        PlayerStub chest = new PlayerStub("ronty");
        Inventory normalChest = proxy(Inventory.class, new InventoryStub(null, 27));
        InventoryView chestView = proxy(InventoryView.class,
                new ViewStub(normalChest, playerInv, proxy(Player.class, chest)));
        InventoryClickEvent cChest = new InventoryClickEvent(chestView, InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        onShopClick.invoke(plugin, cChest);
        check("★普通のチェストのクリックは打ち切らない", !cChest.isCancelled(),
                "isCancelled=" + cChest.isCancelled());
        check("★普通のチェストでは何も喋らない", chest.messages.isEmpty(),
                String.valueOf(chest.messages));

        // 持ち主が別のプラグインの画面だった場合
        PlayerStub other = new PlayerStub("ronty");
        InventoryHolder otherHolder = proxy(InventoryHolder.class, (pr, m, a) -> fallback(m, pr, a));
        Inventory otherInv = proxy(Inventory.class, new InventoryStub(otherHolder, 54));
        InventoryView otherView = proxy(InventoryView.class,
                new ViewStub(otherInv, playerInv, proxy(Player.class, other)));
        InventoryClickEvent cOther = new InventoryClickEvent(otherView, InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        onShopClick.invoke(plugin, cOther);
        check("★他プラグインの画面のクリックも打ち切らない", !cOther.isCancelled(),
                "isCancelled=" + cOther.isCancelled());

        System.out.println("\n================================");
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
