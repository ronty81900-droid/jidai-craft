// =============================================================
// Tsuruhashi.java ── 「時代のツルハシ」1本きり
//
//   ★★ この企画では、掘る道具はこの1本だけ ★★
//     ・全員に1本ずつ配る（最初に入った時に自動で渡る）
//     ・壊れない（耐久がまったく減らない）
//     ・死んでも落とさない（その場に残らず、生き返った時に手元へ戻る）
//     ・店では売らない
//
//   なぜそうするか:
//     クラフトが禁止なので、ツルハシが壊れると二度と掘れなくなる。
//     売って買い直す形にすると「金が無い＝掘れない＝金が稼げない」
//     という抜け出せない詰み方が起きる。1本を壊れないものにして、
//     掘ること自体は誰にでも常にできる状態にしておく。
//
//   3ファイルの分担:
//     JidaiCraft.java  … 入口。死んだ時・生き返った時・入った時の受け取り
//     Tsuruhashi.java  … このツルハシ自体（作る・見分ける・配る）
//     Kane.java        … 金の出し入れ（ここでは使わない）
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * 時代のツルハシを作り、見分け、配る係。
 */
public final class Tsuruhashi {

    /** 画面に出す名前。 */
    public static final String MEI = "時代のツルハシ";

    /**
     * 「これは時代のツルハシだ」という目印を、アイテムそのものに書き込む鍵。
     *
     * ★★ 名前や見た目で見分けてはいけない ★★
     *   金床が使えないので改名はできないが、それでも名前で判定すると
     *   「たまたま同じ名前のアイテム」を取り違える余地が残る。
     *   PersistentDataContainer はアイテムに直接ぶら下がる覚え書きで、
     *   持ち替えても、チェストに入れても、再起動しても消えない。
     */
    private NamespacedKey kagi;

    /** 設定ファイルの見出し。もう配った人の名前を並べる。 */
    private static final String HAITTA = "tsuruhashi_watashita";

    private final JidaiCraft plugin;

    /** もう配った人。★ 二重に配らないための記録。まだ読んでいなければ null。 */
    private List<String> watashita;

    /**
     * ★★ ここで鍵も設定も作らない ★★
     *   このクラスは onEnable の中で作られる。その時点で
     *     new NamespacedKey(plugin, ...) は plugin.getName() を呼び、
     *     getConfig() はプラグインのファイルを読みに行く。
     *   どちらもサーバーが完全に立ち上がる前には使えないことがある
     *   （検証ハーネスで NullPointerException になることを実測した）。
     *   実際に使う時に1回だけ用意する。
     */
    public Tsuruhashi(JidaiCraft plugin) {
        this.plugin = plugin;
    }

    /** 目印の鍵。初めて必要になった時に1回だけ作る。 */
    private NamespacedKey kagi() {
        if (kagi == null) {
            kagi = new NamespacedKey(plugin, "tsuruhashi");
        }
        return kagi;
    }

    /** 配った人の記録。初めて必要になった時に1回だけ読む。 */
    private List<String> watashita() {
        if (watashita == null) {
            watashita = new ArrayList<>(plugin.getConfig().getStringList(HAITTA));
        }
        return watashita;
    }

    /**
     * 時代のツルハシを1本 作る。
     *
     * ★ setUnbreakable(true) が「耐久が減らない」の本体。
     *   エンチャントの耐久力とは別物で、減り方が遅くなるのではなく
     *   まったく減らない。1.20.1 の Bukkit にある機能で、
     *   ライブラリを足してはいない。
     */
    public ItemStack tsukuru() {
        ItemStack it = new ItemStack(Material.IRON_PICKAXE, 1);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(Enshutsu.MODOSU + MEI);
        m.setLore(List.of(
                Enshutsu.MODOSU + "壊れない",
                Enshutsu.MODOSU + "死んでも落とさない",
                Enshutsu.MODOSU + "1人1本。店では売っていない"));
        m.setUnbreakable(true);
        m.getPersistentDataContainer().set(kagi(), PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    /** そのアイテムが時代のツルハシか。 */
    public boolean sore(ItemStack it) {
        if (it == null || it.getType() != Material.IRON_PICKAXE) {
            return false;
        }
        ItemMeta m = it.getItemMeta();
        return m != null
                && m.getPersistentDataContainer().has(kagi(), PersistentDataType.BYTE);
    }

    /** その人が今、時代のツルハシを持っているか。 */
    public boolean motteru(Player player) {
        for (ItemStack it : player.getInventory().getContents()) {
            if (sore(it)) {
                return true;
            }
        }
        return false;
    }

    /**
     * まだ配っていない人に1本 配る。配ったら true。
     *
     * ★★ 一度配った人には二度と配らない ★★
     *   「持っていなければ配る」にすると、チェストへ預けて入り直すだけで
     *   何本でも増える。増えたツルハシはエンダーかまどで鉄塊に焼けるので、
     *   物を無から作る道になってしまう。
     *   （鉄塊そのものは銀行が買い取らないので金にはならないが、
     *     「増やせる」状態を残すこと自体を避ける。）
     *   無くした人には運営が jidai tsuruhashi <名前> で渡す。
     */
    public boolean hajimete(Player player) {
        if (watashita().contains(player.getName())) {
            return false;
        }
        watasu(player);
        watashita().add(player.getName());
        hozon();
        return true;
    }

    /**
     * 1本 渡す。持ち物がいっぱいなら足元に落とす。
     * ★ 落とす方は消える可能性があるが、渡らないよりはよい。
     */
    public void watasu(Player player) {
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), tsukuru());
            player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    MEI + " を足元に置きました（持ち物がいっぱいです）"));
            return;
        }
        player.getInventory().addItem(tsukuru());
        player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                MEI + " を受け取った（壊れない / 死んでも落とさない）"));
    }

    /**
     * 運営が手で渡す時に使う。記録も付ける。
     * ★ 既に持っている人には渡さない。押し間違いで2本目が出ないように。
     */
    public boolean uneiWatasu(Player player) {
        if (motteru(player)) {
            return false;
        }
        watasu(player);
        if (!watashita().contains(player.getName())) {
            watashita().add(player.getName());
            hozon();
        }
        return true;
    }

    /**
     * 死んだ時に、落ちる物の中から時代のツルハシを取り除く。
     * 取り除いた本数を返す。
     *
     * ★ ここで抜いておかないと、その場に落ちて他人に拾われる。
     *   拾われると「掘れない人」が生まれ、企画が止まる。
     */
    public int shinda(List<ItemStack> ochirumono) {
        int nuita = 0;
        for (int i = ochirumono.size() - 1; i >= 0; i--) {
            if (sore(ochirumono.get(i))) {
                ochirumono.remove(i);
                nuita++;
            }
        }
        return nuita;
    }

    /**
     * 生き返った時に手元へ戻す。
     *
     * ★ 既に持っていれば何もしない。
     *   keepInventory が on のサーバーでは死んでも持ち物が残るので、
     *   確かめずに渡すと1本増える。
     */
    public void ikikaetta(Player player) {
        if (motteru(player)) {
            return;
        }
        watasu(player);
    }

    /** 配った記録を設定ファイルへ書く。 */
    private void hozon() {
        plugin.getConfig().set(HAITTA, watashita());
        plugin.saveConfig();
    }

    /** もう配った人数。/jidai list などで様子を見るために使う。 */
    public int watashitaKazu() {
        return watashita().size();
    }
}
