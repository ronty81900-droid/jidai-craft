// =============================================================
// Shinko.java ── 時代を進めるブロック（拠点の鉄ブロック）
//
//   押すとチェスト画面が開き、3条件の達成状況が数字で見える。
//     ・石油   … 勢力の合計本数
//     ・貯金   … 勢力の貯金
//     ・建築   … 拠点に置かれた 丸石・原木・木の板・石レンガ・かまど の合計
//   3つ揃っていれば「運営へ申請」が押せる。
//
//   ★★ 進めるのは運営 ★★
//     このブロックは申請までしかしない。実際に時代が上がるのは
//     運営が jidai:shinko/shounin_<勢力> を打った時。
//     「数えるのはシステム、開けるのは人間」という決まりを守っている。
//
//   ★ 押せるのはリーダーと代行だけ。勢力の貯金を大きく使う決断なので、
//     誰か一人が勝手に押して使い果たす事故を防ぐ。
//
//   ★ 条件の数字そのものはデータパックが持っている。ここは読むだけ。
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class Shinko implements InventoryHolder {

    /**
     * 画面の枠の数。18 = 2段。
     *
     * ★ 1段目に3条件と申請ボタン。
     *   2段目(9〜17)は「石油 / 貯金 / 建築」の見出しを
     *   絵で描くための帯（2026-08-21）。
     */
    private static final int WAKU_SUU = 18;

    /** 押す場所。★ 並びを変えるならここ */
    private static final int WAKU_SEKIYU = 1;
    private static final int WAKU_CHOKIN = 2;
    private static final int WAKU_KENCHIKU = 3;
    private static final int WAKU_SHINSEI = 6;

    /** チーム名 → その勢力の時代の呼び名を作るために使う */
    private final JidaiCraft plugin;
    private final Seiryoku seiryoku;

    /** 画面を開いている人。二度押しの取りこぼしを防ぐ */
    private final Set<String> hiraiteru = new HashSet<>();

    public Shinko(JidaiCraft plugin, Seiryoku seiryoku) {
        this.plugin = plugin;
        this.seiryoku = seiryoku;
    }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, WAKU_SUU, Enshutsu.gamenMei(Enshutsu.UI_SHINKO, "時代を進める"));
    }

    /**
     * 鉄ブロックが押された。
     *
     * ★ 門番は2つ。勢力に入っていること、リーダー（か代行）であること。
     */
    public void osareta(Player player, Kane kane) {
        String team = kane.teamMei(player);
        if (team == null || !Seiryoku.seiryokuKa(team)) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.JIDAI, "勢力に入っていません"));
            return;
        }
        if (!seiryoku.leaderKa(team, player.getName())) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.JIDAI,
                    "時代を進められるのはリーダー（と代行）だけです"));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }
        hiraiteru.add(player.getName());
        player.openInventory(tsukuru(team, kane));
    }

    /** 画面を組み立てる。数字は開いた時点のもの。 */
    private Inventory tsukuru(String team, Kane kane) {
        Inventory inv = Bukkit.createInventory(this, WAKU_SUU, Enshutsu.gamenMei(Enshutsu.UI_SHINKO, "時代を進める"));
        String mei = Seiryoku.mei(team);
        int ima = kane.seiryokuJidai(mei);

        inv.setItem(WAKU_SEKIYU, jouken(Material.COAL, "石油",
                kane.sekiyuGokei(mei), kane.hitsuyoSekiyu(ima)));
        inv.setItem(WAKU_CHOKIN, jouken(Material.GOLD_INGOT, "勢力の貯金",
                kane.seiryokuZandaka(mei), kane.hitsuyoChokin(ima)));
        inv.setItem(WAKU_KENCHIKU, jouken(Material.BRICKS, "建築",
                kane.kenchiku(mei), kane.hitsuyoKenchiku(ima)));

        inv.setItem(WAKU_SHINSEI, shinseiBotan(team, kane, ima));
        return inv;
    }

    /** 条件1つぶんの見た目。足りているかで名前の頭を変える。 */
    private ItemStack jouken(Material material, String namae, int ima, int hitsuyo) {
        boolean ok = hitsuyo > 0 && ima >= hitsuyo;
        ItemStack item = new ItemStack(ok ? material : Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(moji((ok ? "○ " : "× ") + namae));
        List<String> shita = new ArrayList<>();
        if (hitsuyo <= 0) {
            shita.add(moji("この時代では要りません"));
        } else {
            shita.add(moji("今 " + ima + " / 必要 " + hitsuyo));
        }
        meta.setLore(shita);
        item.setItemMeta(meta);
        return item;
    }

    /** 申請のボタン。3つ揃っていない時は押せない見た目にする。 */
    private ItemStack shinseiBotan(String team, Kane kane, int ima) {
        String mei = Seiryoku.mei(team);
        boolean zenbu = sorotta(kane, mei, ima);
        boolean jouken = ima < Kane.JIDAI_MIRAI;

        ItemStack item = new ItemStack(
                (zenbu && jouken) ? Material.BEACON : Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        List<String> shita = new ArrayList<>();
        if (!jouken) {
            meta.setDisplayName(moji("未来へ到達しています"));
            shita.add(moji("この勢力は勝ちました"));
        } else if (!zenbu) {
            meta.setDisplayName(moji("まだ申請できません"));
            shita.add(moji("3つの条件をすべて満たしてください"));
        } else {
            meta.setDisplayName(moji("運営へ申請する"));
            shita.add(moji(Kane.jidaiMei(ima) + " → " + Kane.jidaiMei(ima + 1)));
            shita.add(moji("★ 進めるのは運営です。承認されると貯金を使います"));
            // ★ 現代 → 未来 は、進むこと自体が勝ち（2026-08-23 のご指示）。
            //   ここで言っておかないと、押す人が「ただの時代進行」だと思う。
            if (ima + 1 == Kane.JIDAI_MIRAI) {
                shita.add(moji("§b★★ 承認されたら、この勢力の勝ちです ★★"));
            }
        }
        meta.setLore(shita);
        item.setItemMeta(meta);
        return item;
    }

    /** 3条件がすべて揃っているか。 */
    private boolean sorotta(Kane kane, String mei, int ima) {
        return kane.sekiyuGokei(mei) >= kane.hitsuyoSekiyu(ima)
                && kane.seiryokuZandaka(mei) >= kane.hitsuyoChokin(ima)
                && kane.kenchiku(mei) >= kane.hitsuyoKenchiku(ima);
    }

    /** 枠が押された。 */
    public void oshita(Player player, int slot, Kane kane) {
        if (slot != WAKU_SHINSEI || !hiraiteru.contains(player.getName())) {
            return;   // 条件の表示は押しても何も起きない
        }
        String team = kane.teamMei(player);
        if (team == null || !seiryoku.leaderKa(team, player.getName())) {
            return;
        }
        String mei = Seiryoku.mei(team);
        int ima = kane.seiryokuJidai(mei);
        if (ima >= Kane.JIDAI_MIRAI || !sorotta(kane, mei, ima)) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.JIDAI, "まだ条件を満たしていません"));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }

        // ★ 二度押しを断ち切ってから知らせる
        hiraiteru.remove(player.getName());
        Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());

        // --- 勢力の中と、運営へ知らせる --------------------------
        var shirase = Enshutsu.kazaru(Enshutsu.JIDAI,
                player.getName() + " が " + mei + " の時代進行を申請した ("
                        + Kane.jidaiMei(ima) + " → " + Kane.jidaiMei(ima + 1) + ")");
        for (Player nakama : kane.onajiSeiryoku(player)) {
            nakama.sendMessage(shirase);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp()) {
                p.sendMessage(shirase);
                p.sendMessage(Enshutsu.kazaru(Enshutsu.JIDAI,
                        "  承認するなら /function jidai:shinko/shounin_" + team));
            }
        }
        plugin.getLogger().info("時代進行の申請: " + mei + " (" + player.getName() + ") "
                + Kane.jidaiMei(ima) + " → " + Kane.jidaiMei(ima + 1));
        Enshutsu.oto(player, Enshutsu.OTO_CHU);
    }

    /** 画面を閉じた人を忘れる。 */
    public void tojita(String namae) {
        hiraiteru.remove(namae);
    }

    /** 文字列を画面に出せる形に変換する。 */
    private static String moji(String s) {
        return Enshutsu.MODOSU + s;
    }
}
