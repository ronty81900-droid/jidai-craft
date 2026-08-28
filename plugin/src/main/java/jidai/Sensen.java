// =============================================================
// Sensen.java ── 戦争宣誓の「相手を選ぶ画面」
//
//   販売所で「戦争宣誓」を押すと、この画面が開く。
//   勢力5つが並び、選べない相手は灰色になって理由が出る。
//
//   ★★ 戦争そのものはデータパックが持つ ★★
//     プラグインがやるのは「誰に宣戦するかを選ばせて、代金を引いて、
//     データパックの関数を呼ぶ」ところまで。
//     開戦・略奪・占領・終了・全体通知はすべてデータパック側。
//
//   ★ 確認は挟まない（指示書の方針）。選んだ時点で購入確定。
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class Sensen implements InventoryHolder {

    /**
     * 画面の枠の数。18 = 2段。
     *
     * ★ 1段目に相手の勢力が並ぶ。
     *   2段目(9〜17)は「宣戦すると戻れない」という注意を
     *   絵で描くための帯（2026-08-21）。
     */
    private static final int WAKU_SUU = 18;

    /** 5勢力を真ん中に並べる。左から 2,3,4,5,6 の枠 */
    private static final int HAJIME = 2;

    /**
     * 勢力ごとのアイコン。★ 見た目を変えるならここだけ。
     * 並びは Seiryoku.zenTeam() と同じ順にしておくこと。
     */
    private static final Material[] ICON = {
            Material.WHITE_BANNER,       // 丘陵
            Material.GREEN_BANNER,       // 森林
            Material.LIGHT_BLUE_BANNER,  // 川
            Material.BLUE_BANNER,        // 内海
            Material.GRAY_BANNER,        // 岩場
    };

    /** 選べない枠に置くもの */
    private static final Material ERABENAI = Material.GRAY_STAINED_GLASS_PANE;

    private final JidaiCraft plugin;
    private final Seiryoku seiryoku;

    /**
     * 名前 → その人が払う額（販売所から渡される）。
     *
     * ★ この表に載っている＝「宣戦の画面を開いていて、まだ買っていない」印。
     *   選んだ瞬間に消すので、同じtickに2回押されても2回買えない。
     */
    private final Map<String, Integer> nedan = new HashMap<>();

    public Sensen(JidaiCraft plugin, Seiryoku seiryoku) {
        this.plugin = plugin;
        this.seiryoku = seiryoku;
    }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, WAKU_SUU, Enshutsu.gamenMei(Enshutsu.UI_SENSEN, "宣戦する相手を選ぶ"));
    }

    /**
     * 相手を選ぶ画面を開く。
     * 自勢力から見て選べるか選べないかを、その場で組み立てる。
     */
    public void hiraku(Player player, Kane kane, int ikura) {
        String jibun = kane.teamMei(player);
        Inventory inv = Bukkit.createInventory(this, WAKU_SUU, Enshutsu.gamenMei(Enshutsu.UI_SENSEN, "宣戦する相手を選ぶ"));

        String[] team = Seiryoku.zenTeam();
        for (int i = 0; i < team.length && i < ICON.length; i++) {
            String riyuu = seiryoku.senseniDekiruka(jibun, team[i], kane);
            inv.setItem(HAJIME + i, tsukuru(team[i], ICON[i], riyuu, ikura));
        }

        nedan.put(player.getName(), ikura);

        // ★★ 次の tick まで待ってから開く ★★
        //   この画面は「販売所のクリック」から開かれる。
        //   Bukkit の決まりで、クリックの処理中に openInventory を呼んではいけない。
        //   (1.21.10 の公式説明に、名指しで「呼ぶな」と書いてある。実物を読んで確認済み)
        //   守らないと、開いた直後に閉じたり、見た目がずれたりする。
        //   runTask は「次の tick に、この処理をやってくれ」という予約。
        //   () -> ... は「あとでやる処理」をその場で書く書き方 (ラムダ式)。
        Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
    }

    /** 1つぶんのアイコン。選べない時は灰色にして理由を出す。 */
    private ItemStack tsukuru(String team, Material icon, String riyuu, int ikura) {
        boolean erabu = riyuu == null;
        ItemStack item = new ItemStack(erabu ? icon : ERABENAI, 1);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(moji(Seiryoku.mei(team)));

        List<String> setsumei = new ArrayList<>();
        if (erabu) {
            setsumei.add(moji("クリックで宣戦 (勢力の金 " + ikura + ")"));
            setsumei.add(moji("★ 確認は出ません。押した時点で確定します"));
        } else {
            // ★ 押せないだけだと理由が分からない。必ず書く
            setsumei.add(moji("選べません: " + riyuu));
        }
        meta.setLore(setsumei);
        item.setItemMeta(meta);
        return item;
    }

    /** クリックされた枠から勢力を割り出す。枠が違えば null。 */
    public String teamKara(int slot) {
        int i = slot - HAJIME;
        String[] team = Seiryoku.zenTeam();
        return (i >= 0 && i < team.length) ? team[i] : null;
    }

    /**
     * 相手を選んだ。ここで購入が確定する。
     *
     * ★ 順序を崩さないこと
     *   1. もう一度、選べる相手か確かめる（画面を開いたまま状況が変わった時のため）
     *   2. 代金を引く
     *   3. データパックの関数を呼ぶ
     *
     *   3 が失敗したら代金を戻す。金だけ引かれる事故を防ぐため。
     */
    public void erabu(Player player, int slot, Kane kane) {
        String aite = teamKara(slot);
        if (aite == null) {
            return;   // 飾りの枠
        }

        String namae = player.getName();
        // 表に無い＝すでに買ったか、画面を閉じた後。二度押しはここで止まる。
        if (!nedan.containsKey(namae)) {
            return;
        }

        // --- (1) もう一度、選べる相手か確かめる ------------------
        //   画面を開いている間に戦争が始まることがあるため。
        String jibun = kane.teamMei(player);
        String riyuu = seiryoku.senseniDekiruka(jibun, aite, kane);
        if (riyuu != null) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO, "選べません: " + riyuu));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;   // 表は消さない。別の相手を選び直せる
        }

        String kinko = kane.kinkoMei(player);
        if (kinko == null) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO, "勢力に入っていません"));
            return;
        }

        int ikura = nedan.get(namae);
        int mae = kane.seiryokuZandaka(kinko);
        if (mae < ikura) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO,
                    "勢力の金が足りません (必要 " + ikura + " / 金庫 " + mae + ")"));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }

        // ★ ここから確定。先に表から消して、二度押しを断ち切る。
        nedan.remove(namae);

        // --- (2) 代金を引く --------------------------------------
        kane.seiryokuKousin(kinko, mae - ikura);

        // ★ 閉じるのも次の tick。クリックの処理中に closeInventory を
        //   呼んではいけないという決まりは open と同じ。
        //   ★ ここを player::closeInventory と短く書くとコンパイルが通らない。
        //     runTask には Runnable 版と Consumer 版の2種類があり、
        //     「::」の書き方だとどちらか決められないため (実測でエラーを確認)。
        //     () -> ... と書けば引数ゼロなので Runnable 版に決まる。
        Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());

        // --- (3) データパックへ渡す ------------------------------
        //
        // ★★ 「送れたか」ではなく「本当に始まったか」で見る ★★
        //   データパックの sensen は、条件に合わなければ `return 0` で
        //   静かに何もしない（範囲外の番号・自分自身・再戦禁止中）。
        //   命令そのものは通っているので、送信の成否だけを見ていると
        //   「100 を取られたのに戦争が始まっていない」事故になる。
        //   そこで、呼んだあとに状態を読み直して、確かに戦争が
        //   立っていることを確かめる。立っていなければ代金を戻す。
        //
        // ★ 読み直す前に【要約を作り直させる】こと。
        //   戦争そのものはマーカーが持っており、勢力名の保持者へ写されるのは
        //   データパックの時計が回った時。1秒待てないので、ここで呼ぶ。
        boolean okutta = sensenSuru(jibun, aite, false, kane);
        kane.sensouYouyaku();
        boolean hajimatta = okutta
                && kane.sensou(Seiryoku.mei(jibun), Seiryoku.mei(aite)) >= 1;
        if (!hajimatta) {
            kane.seiryokuKousin(kinko, mae);   // 始まらなかったので代金を戻す
            player.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO,
                    "宣戦できませんでした。代金は戻しました（管理者に連絡してください）"));
            plugin.getLogger().warning("宣戦が成立しなかった: " + Seiryoku.mei(jibun)
                    + " → " + Seiryoku.mei(aite) + " (送信=" + okutta + ")");
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }

        plugin.getLogger().info("宣戦: " + Seiryoku.mei(jibun) + " → " + Seiryoku.mei(aite)
                + " (" + namae + " / 勢力の金 " + ikura + ")");
        // ★ 全体通知と開戦の演出はデータパックが出す。ここでは出さない
    }

    /**
     * データパックの宣戦の関数を呼ぶ。
     *
     *   scoreboard players set #w_kuni sagyou 1    ← 宣戦する側の番号
     *   scoreboard players set #w_aite sagyou 2    ← される側の番号
     *   scoreboard players set #w_soku sagyou 0    ← 0=ふつう / 1=即交戦
     *   function jidai:sensou/sensen
     *
     * ★★ 1.20.1 に関数の引数（マクロ）は無い（実測） ★★
     *   1.21 では `function ... {kuni:"丘陵",...}` と書いていたが、
     *   1.20.1 では "Incorrect argument for command" で落ちる。
     *   そこで【先にスコアへ置いてから呼ぶ】形に変えた。
     *
     * ★ 渡すのは勢力名ではなく【番号】。番号を配るのはデータパック
     *   （bangou 目的）なので、プラグインは覚えずに毎回聞く。
     *
     * ★ 3つとも必ず置くこと。前回の値が残っていると別の戦争が始まる。
     *
     * @param kane 勢力の番号を引くために使う（番号はデータパックが配る）
     * @return 4つの命令すべてが通れば true
     */
    public boolean sensenSuru(String jibunTeam, String aiteTeam, boolean soku,
                              Kane kane) {
        int jibunNo = kane.bangou(Seiryoku.mei(jibunTeam));
        int aiteNo = kane.bangou(Seiryoku.mei(aiteTeam));
        if (jibunNo == 0 || aiteNo == 0) {
            plugin.getLogger().warning("勢力の番号が引けませんでした: "
                    + Seiryoku.mei(jibunTeam) + "=" + jibunNo + " / "
                    + Seiryoku.mei(aiteTeam) + "=" + aiteNo);
            return false;
        }
        String[] cmds = {
            "scoreboard players set #w_kuni sagyou " + jibunNo,
            "scoreboard players set #w_aite sagyou " + aiteNo,
            "scoreboard players set #w_soku sagyou " + (soku ? 1 : 0),
            "function jidai:sensou/sensen",
        };
        try {
            boolean zenbu = true;
            for (String cmd : cmds) {
                // & ではなく &= を使うと、途中で失敗しても残りを送ってしまう。
                // ここは「全部送って、1つでも失敗したら false」でよい。
                zenbu = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd) && zenbu;
            }
            return zenbu;
        } catch (RuntimeException e) {
            plugin.getLogger().warning("宣戦の関数を呼べませんでした: " + e);
            return false;
        }
    }

    /** 画面を閉じた人を忘れる。 */
    public void tojita(String namae) {
        nedan.remove(namae);
    }

    /** 文字列を画面に出せる形に変換する。 */
    private static String moji(String s) {
        return Enshutsu.MODOSU + s;
    }
}
