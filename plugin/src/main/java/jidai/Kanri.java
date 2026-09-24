// =============================================================
// Kanri.java ── 運営（ゲームマスター）の操作
//
//   ★★ コマンドブロックから使う前提 ★★
//     ゲームマスターは開始位置（X=37 付近）のコマンドブロックで操作する
//     （2026-08-22 のご指示）。だから、ここの処理は
//     【送り手がプレイヤーでなくても動く】ように書いてある。
//     送り手が要るのは「here」（自分の足元を使う）だけ。
//
//   できること（すべて /jidai game …）
//     start / stop      … ゲームの開始・停止（石油の栓を開け閉めし、全員に知らせる）
//     reset [zenbu]     … 時代の進行状況を最初に戻す（zenbu なら金・石油・戦争も）
//     tp kyoten         … 勢力に所属する全員を、その勢力の拠点へ
//     risu              … 勢力の全員のリスポーンを、その勢力の拠点にする
//     spawn [here|x y z]… 初期リスポーン（世界のスポーン）を決める
//
//   ★ 拠点の位置は Basho が【施設マーカーから】求める。ここに座標は書かない。
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class Kanri {

    /** 2語目の言葉。補完に使う。 */
    public static final String[] KOTOBA = {"start", "stop", "reset", "tp", "risu", "spawn", "kyoten", "beacon"};

    // ── 設定ファイルの鍵 ──
    static final String KEY_SPAWN = "game.hajime_no_risu";
    static final String KEY_UGOKI = "game.ugoiteru";
    static final String KEY_RISU = "game.risu_kyoten";
    /**
     * 初期リスポーン。★ 2026-08-22 のご指示（実機の画面の座標 40.98 / 176 / -926.68）。
     * 設定ファイルに無い時だけ、この値を書き込んで使う。
     */
    static final String HAJIME_NO_RISU = "40 176 -927";

    private final JidaiCraft plugin;
    private final Kane kane;
    private final Basho basho;

    private final Random ran = new Random();

    /** ガチャ（全部リセットで特殊アイテムの記録を消すために持つ）。 */
    private Gacha gacha;

    public void gachaWatasu(Gacha g) {
        this.gacha = g;
    }

    public Kanri(JidaiCraft plugin, Kane kane, Basho basho) {
        this.plugin = plugin;
        this.kane = kane;
        this.basho = basho;
    }

    // =========================================================
    //  コマンドの振り分け
    // =========================================================

    /** /jidai game … の中身。戻り値は常に true（使い方は自前で出す）。 */
    public boolean command(CommandSender sender, String[] args) {
        if (args.length < 2) {
            joutai(sender);
            return true;
        }
        switch (args[1]) {
            case "start":
                return kirikae(sender, true);
            case "stop":
                return kirikae(sender, false);
            case "reset":
                return reset(sender, args.length >= 3 && args[2].equals("zenbu"));
            case "tokushu":
                return tokushu(sender, args);
            case "ibutsu":
                return ibutsu(sender, args);
            case "tp":
                if (args.length >= 3 && args[2].equals("kyoten")) {
                    return tpKyoten(sender);
                }
                sender.sendMessage("[運営] 使い方: jidai game tp kyoten");
                return true;
            case "risu":
                return risu(sender);
            case "spawn":
                return spawn(sender, args);
            case "kyoten":
                return kyoten(sender, args);
            case "beacon":
                return beacon(sender);
            default:
                tsukaikata(sender);
                return true;
        }
    }

    private void tsukaikata(CommandSender sender) {
        sender.sendMessage("[運営] 使い方: jidai game <start|stop|reset [zenbu]|tp kyoten|risu|spawn [here|x y z]|kyoten <勢力>|beacon|tokushu [名前]|ibutsu <名前>>");
    }

    /**
     * 拠点の立ち位置を、自分の足元で手で決める（走査の自動値より優先）。
     * ★ ゲーム内からだけ。コマンドブロックには足元が無い。
     */
    private boolean kyoten(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("[運営] kyoten はゲーム内から、その場所に立って打ってください");
            return true;
        }
        if (args.length < 3 || !Seiryoku.seiryokuKa(args[2])) {
            sender.sendMessage("[運営] 使い方: jidai game kyoten <勢力>  勢力: "
                    + String.join(" / ", Seiryoku.zenTeam()));
            return true;
        }
        basho.kyotenTeKimeru(args[2], p.getLocation());
        basho.hozon(plugin);
        sender.sendMessage("[運営] " + Seiryoku.mei(args[2]) + " の拠点の立ち位置を、今の足元にしました");
        return true;
    }

    /** 今の状態をまとめて出す。 */
    private void joutai(CommandSender sender) {
        boolean ugoki = plugin.getConfig().getBoolean(KEY_UGOKI, false);
        sender.sendMessage("[運営] ゲーム: " + (ugoki ? "開始中" : "停止中")
                + " / 石油: " + (kane.sekiyuSenJunbiOK()
                        ? (kane.sekiyuTomatteruKa() ? "停止" : "稼働 " + kane.sekiyuBairitsu() + "%")
                        : "(データパック未読込)"));
        sender.sendMessage("  初期リスポーン: " + plugin.getConfig().getString(KEY_SPAWN, "(未設定)")
                + " / 勢力のリスポーン=拠点: "
                + (plugin.getConfig().getBoolean(KEY_RISU, false) ? "有効" : "無効"));
        Location plant = basho.plantIchi();
        sender.sendMessage("  石油プラント: " + (plant == null ? "未登録（/jidai scan）"
                : plant.getBlockX() + "," + plant.getBlockY() + "," + plant.getBlockZ()));
        List<String> k = basho.kyotenIchiran();
        sender.sendMessage("  拠点の立ち位置: " + (k.isEmpty() ? "未登録（/jidai scan）" : ""));
        for (String s : k) {
            sender.sendMessage("    " + s);
        }
        tsukaikata(sender);
    }

    // =========================================================
    //  開始と停止
    // =========================================================

    /**
     * ゲームを始める／止める。
     *
     * ★ 中身は【石油の栓】＋全員への知らせ。
     *   データパックの時計（戦争・腐敗・時代判定）は止めない。
     *   時計を止めると、サイドバーも所持金の帯も止まり、
     *   「止めた」のか「壊れた」のか参加者に見分けがつかなくなる。
     *   止める前に石油を抑えたい時は jidai sekiyu osaeru を使う。
     */
    private boolean kirikae(CommandSender sender, boolean ugokasu) {
        if (!kane.sekiyuSenJunbiOK()) {
            sender.sendMessage("[運営] データパックが読み込まれていません");
            return true;
        }
        kane.sekiyuTomeru(!ugokasu);
        plugin.getConfig().set(KEY_UGOKI, ugokasu);
        plugin.saveConfig();

        String dai = ugokasu ? "ゲーム開始" : "ゲーム停止";
        String shita = ugokasu ? "中央プラントが石油を出し始めた" : "中央プラントが止まった";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§6§l" + dai, "§7" + shita, 10, 70, 20);
        }
        Enshutsu.otoZenin(ugokasu ? Enshutsu.OTO_JIDAI : Enshutsu.OTO_TEISHI);
        Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.JIDAI, dai + " ── " + shita));
        sender.sendMessage("[運営] " + dai + "。石油は" + (ugokasu ? "稼働" : "停止") + "しています");
        plugin.getLogger().info("運営: " + dai);
        return true;
    }

    // =========================================================
    //  リセット
    // =========================================================

    /**
     * 時代の進行状況を最初に戻す。
     *
     *   reset        … 中央の時代=1 / 各勢力の時代=1 / 湧いた本数=0 /
     *                  銃器専門店の解放を取り消す / 中央ビーコンとプラントの見た目を鉄器へ
     *   reset zenbu  … 上に加えて 貯金・個人の金・石油・占領・植民地・下剋上・勝利・戦争 を消し、
     *                  参加者の手元の特殊アイテム（紙）も取り上げる
     *
     * ★ 特殊アイテムの「勢力ごとに各1個」の記録は、zenbu でなくても戻す
     *   （2026-08-23 のご指示）。時代だけ最初に戻しても、記録が残っていると
     *   二度目の催しで特殊アイテムが1個も出ない。
     *
     * ★ 徴収の世代（choshu）は触らない。世界の値だけ戻すと、
     *   全員が「まだ徴収されていない人」と見なされて、石油を4割 取られる。
     */
    private boolean reset(CommandSender sender, boolean zenbu) {
        if (!kane.junbiOK()) {
            sender.sendMessage("[運営] データパックが読み込まれていません");
            return true;
        }
        int kaita = scoreRisetto(zenbu);

        Shop.jukiRisetto(plugin);
        basho.chuoIro(1);
        dispatch("function jidai:shinko/plant");
        // ★ 特殊アイテムの「勢力ごとに各1個」の記録は、どちらのリセットでも戻す
        //   （2026-08-23 のご指示）。遺物の効果も一緒に切れる（Gacha.tokushuRisetto）。
        if (gacha != null) {
            gacha.tokushuRisetto();
        }
        // ★★ 現物も、どちらのリセットでも取り上げる（2026-08-24 のご指示）★★
        //   記録だけ消して紙が手元に残ると、
        //     ・リーダーが5種そろえたまま＝即 勝利になる
        //     ・遺物の紙は残るのに効果は切れていて、見た目と中身が食い違う
        //   「リセットしたら効果も消える」を言い切れる形にする。
        int kaishu = Shouri.kaishu();
        if (zenbu) {
            // 戦争はマーカー1体＝1件。消してから要約を作り直させる
            dispatch("kill @e[type=marker,tag=jidai_sensou]");
            dispatch("function jidai:sensou/youyaku");
            // ★ 現物も取り上げる。記録だけ消して紙が手元に残っていると、
            //   リセット直後にリーダーが5種そろえたまま＝即 勝利になる。
            // まっさらにしたら「停止中」。start を打つまで石油は出ない
            kane.sekiyuTomeru(true);
            plugin.getConfig().set(KEY_UGOKI, false);
            plugin.saveConfig();
        }

        String nani = zenbu ? "時代・金・石油・戦争をすべて" : "時代の進行状況を";
        Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.JIDAI, "運営が" + nani + "最初に戻した"));
        sender.sendMessage("[運営] " + nani + "最初に戻しました (書いたスコア " + kaita
                + " 件 / 特殊アイテムの記録も初期化"
                + (zenbu ? " / 取り上げた特殊アイテム " + kaishu + " 個"
                        : "") + ")");
        plugin.getLogger().info("運営: リセット" + (zenbu ? "（全部）" : "（時代）") + " " + kaita + " 件");
        return true;
    }

    /**
     * スコアを書き戻す部分だけ。★ 検証から直接 呼べるように分けてある。
     *
     * @return 書いたスコアの数
     */
    int scoreRisetto(boolean zenbu) {
        int n = 0;
        n += kaku("chuo", "世界", 1);
        n += kaku("wakidashi", "世界", 0);
        for (String team : Seiryoku.zenTeam()) {
            String mei = Seiryoku.mei(team);
            n += kaku("jidai", mei, 1);
            if (zenbu) {
                n += kaku("chokin", mei, 0);
                n += kaku("senryou", mei, 0);
                n += kaku("gekokujo", mei, 0);
                n += kaku("shokuminchi", mei, 0);
                n += kaku("senkou", mei, 0);
            }
        }
        if (zenbu) {
            n += kaku("shouri", "世界", 0);
            // 個人の金と石油。★ 持っている人だけ。勢力名や設定項目には作らない
            for (String na : kane.zenHojisha()) {
                if (kane.scoreAru("kane_kojin", na)) {
                    n += kaku("kane_kojin", na, 0);
                }
                if (kane.scoreAru("sekiyu", na)) {
                    n += kaku("sekiyu", na, 0);
                }
            }
        }
        return n;
    }

    /**
     * `jidai game tokushu [名前]` ── 特殊アイテムの紙を出す（絵を確かめる用）。
     *
     * ★★ なぜ要るか（2026-08-24 のご指示）★★
     *   「暗い5つが並んだ時に見分けられるか」を確かめたいのに、
     *   ガチャは 0.5% なので、狙って出せない。
     *   ★ 出すのは【紙だけ】。遺物の効果は付かない（付けるのは jidai game ibutsu）。
     *   ★ ガチャの記録（勢力ごとに各1個）にも触らない。確認用なので。
     */
    private boolean tokushu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("[運営] ゲーム内から実行してください");
            return true;
        }
        List<String> mei = new ArrayList<>();
        if (args.length >= 3) {
            mei.add(String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)));
        } else {
            mei.addAll(java.util.Arrays.asList(Shouri.tokushuMei()));
            for (String[] r : Shouri.ibutsuHyou()) {
                mei.add(r[0]);
            }
        }
        int dashita = 0;
        int tsuita = 0;
        for (String na : mei) {
            ItemStack item = Shouri.tsukuruKami(na);
            if (item == null) {
                sender.sendMessage("[運営] 知らない名前: " + na);
                continue;
            }
            for (ItemStack nokori : p.getInventory().addItem(item).values()) {
                p.getWorld().dropItem(p.getLocation(), nokori);
            }
            dashita++;
            // ★★ 遺物なら効果も付ける（2026-08-24 の実機の指摘）★★
            //   前は紙だけ出していたので、「持っているのに何も起きない」になった。
            //   確かめるための道具なので、紙と効果は揃っている方がよい。
            //   ★ 勢力に入っていない人には付かない（効果は勢力のものなので）。
            if (Shouri.ibutsuKouka(na) != null) {
                String t = kane.teamMei(p);
                if (t != null && Seiryoku.seiryokuKa(t)) {
                    Ibutsu.kiroku(Seiryoku.mei(t), na);
                    tsuita++;
                }
            }
        }
        sender.sendMessage("[運営] 特殊アイテムを " + dashita + " 枚 出しました"
                + (args.length >= 3 ? "" : "（集める5種 + 遺物11種）")
                + " / 遺物の効果を " + tsuita + " 件 付けました");
        if (tsuita == 0 && dashita > 0) {
            sender.sendMessage("[運営] ★ 勢力に入っていないので、遺物の効果は付いていません"
                    + "（「" + Ibutsu.JOHO_MEI + "」の /joho だけは紙で使えます）");
        }
        return true;
    }

    /**
     * `jidai game ibutsu <名前>` ── 遺物の【効果】を自分の勢力に付ける（確かめる用）。
     *
     * ★ 効果は勢力に付くので、勢力に入っていないと確かめられない。
     * ★ 紙は出さない（欲しければ jidai game tokushu <名前>）。
     * ★ reset で消える。ガチャの記録と同じ入れ物を使っているため。
     */
    private boolean ibutsu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("[運営] ゲーム内から実行してください");
            return true;
        }
        // ★★ 一覧は【勢力チェックより先】に出す（2026-08-24 の実機の指摘）★★
        //   前は勢力チェックが先にあったので、勢力に入っていない人が打つと
        //   「先に勢力へ入ってください」で終わり、**一覧に永久に届かなかった**。
        if (args.length < 3) {
            sender.sendMessage("[運営] 使い方: jidai game ibutsu <遺物の名前>");
            for (String[] r : Shouri.ibutsuHyou()) {
                sender.sendMessage("  §f" + r[0] + " §7… " + r[5]);
            }
            sender.sendMessage("[運営] ★ 効果は勢力に付きます。紙だけ欲しい時は jidai game tokushu");
            return true;
        }
        String team = kane.teamMei(p);
        if (team == null || !Seiryoku.seiryokuKa(team)) {
            sender.sendMessage("[運営] 遺物の効果は勢力に付きます。先に勢力へ入ってください");
            return true;
        }
        String na = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        if (Shouri.ibutsuKouka(na) == null) {
            sender.sendMessage("[運営] 遺物ではありません: " + na);
            return true;
        }
        Ibutsu.kiroku(Seiryoku.mei(team), na);
        sender.sendMessage("[運営] " + Seiryoku.mei(team) + " に「" + na + "」の効果を付けました");
        return true;
    }

    private int kaku(String mokuteki, String mochinushi, int atai) {
        return kane.kaku(mokuteki, mochinushi, atai) ? 1 : 0;
    }

    private void dispatch(String cmd) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("運営の命令に失敗: " + cmd + " / " + e);
        }
    }

    // =========================================================
    //  テレポート
    // =========================================================

    /** 勢力に所属する全員を、その勢力の拠点へ。 */
    private boolean tpKyoten(CommandSender sender) {
        Map<String, Integer> kazu = new TreeMap<>();
        List<String> nai = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            String team = kane.teamMei(p);
            if (team == null || !Seiryoku.seiryokuKa(team)) {
                continue;
            }
            Location l = basho.kyotenIchi(team);
            if (l == null) {
                if (!nai.contains(Seiryoku.mei(team))) {
                    nai.add(Seiryoku.mei(team));
                }
                continue;
            }
            p.teleport(l);
            p.sendMessage(Enshutsu.kazaru(Enshutsu.JIDAI, Seiryoku.mei(team) + " の拠点へ移動した"));
            kazu.merge(Seiryoku.mei(team), 1, Integer::sum);
        }
        StringBuilder b = new StringBuilder("[運営] 拠点へ送りました:");
        for (Map.Entry<String, Integer> e : kazu.entrySet()) {
            b.append(' ').append(e.getKey()).append(' ').append(e.getValue()).append("人");
        }
        if (kazu.isEmpty()) {
            b.append(" (勢力に居る人が1人も居ません)");
        }
        sender.sendMessage(b.toString());
        if (!nai.isEmpty()) {
            sender.sendMessage("  ★ 拠点の位置が未登録: " + String.join(" / ", nai)
                    + " → /jidai scan を打つか、jidai game kyoten <勢力> で手で決めてください");
        }
        return true;
    }

    // =========================================================
    //  拠点のビーコン（2026-08-22 のご指示）
    // =========================================================
    //
    // ★★ 拠点の【後方】に置く ★★
    //   施設の列は拠点の中心から +z 側（南）にあり、南を向いている。
    //   立ち位置（kyotenIchi）はその2ブロック手前 = 中心の +8。
    //   「後方」はその反対側なので、中心の -8 = 立ち位置の -16。
    //   ★ 拠点の向きを変えたら、この 16 を見直すこと。
    //
    // ★ 形は Basho.beaconNoIchibu が守る範囲と同じ:
    //     本体 (x, y, z) / 上のガラス (x, y+1, z) / 下の鉄ブロック 3x3 (y-1)
    //   ガラスは勢力の色。遠くからどこの拠点か分かる。

    /** 立ち位置からビーコンまでの距離（-z 方向）。 */
    static final int BEACON_USHIRO = 16;

    private boolean beacon(CommandSender sender) {
        int oita = 0;
        List<String> nai = new ArrayList<>();
        for (String team : Seiryoku.zenTeam()) {
            Location k = basho.kyotenIchi(team);
            if (k == null) {
                nai.add(Seiryoku.mei(team));
                continue;
            }
            World w = k.getWorld();
            int x = k.getBlockX();
            int y = k.getBlockY();
            int z = k.getBlockZ() - BEACON_USHIRO;

            // 土台 3x3（足場の面に埋める）
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    w.getBlockAt(x + dx, y - 1, z + dz).setType(Material.IRON_BLOCK);
                }
            }
            w.getBlockAt(x, y, z).setType(Material.BEACON);
            w.getBlockAt(x, y + 1, z).setType(garasu(team));
            // ビームの邪魔をしない（上に何か残っていれば空にする）
            for (int dy = 2; dy <= 6; dy++) {
                Block ue = w.getBlockAt(x, y + dy, z);
                if (ue.getType() != Material.AIR) {
                    ue.setType(Material.AIR);
                }
            }

            String kagi = Basho.kagi(w.getName(), x, y, z);
            basho.tsuika(Basho.BEACON, kagi);        // 既に登録済みなら false で素通り
            basho.beaconKuniTouroku(kagi, team);
            oita++;
            sender.sendMessage("  " + Seiryoku.mei(team) + " のビーコン: " + x + "," + y + "," + z);
        }
        basho.hozon(plugin);
        // ★ 2026-09-09: 壊せる条件が「相手の貯金が100以下」から
        //   「相手の銀行を 占領_必要回数 回 壊した」へ変わった（ご指示）。
        //   回数はデータパックの settei が正本。ここでは読んだ値をそのまま出す。
        int hitsuyou = kane.settei("占領_必要回数");
        if (hitsuyou <= 0) {
            hitsuyou = JidaiCraft.BEACON_KAISU_YOBI;
        }
        sender.sendMessage("[運営] ビーコンを " + oita + " 拠点に置いて登録しました"
                + "（交戦中の相手の銀行を " + hitsuyou + " 回 壊すと壊せる）");
        if (!nai.isEmpty()) {
            sender.sendMessage("  ★ 拠点の位置が未登録: " + String.join(" / ", nai) + " → /jidai scan");
        }
        return true;
    }

    /** 勢力の色のガラス（load.mcfunction の team color と同じ並び）。 */
    static Material garasu(String team) {
        switch (team) {
            case "kyuryo":  return Material.WHITE_STAINED_GLASS;
            case "shinrin": return Material.GREEN_STAINED_GLASS;
            case "kawa":    return Material.LIGHT_BLUE_STAINED_GLASS;
            case "naikai":  return Material.BLUE_STAINED_GLASS;
            case "iwaba":   return Material.YELLOW_STAINED_GLASS;
            default:        return Material.GLASS;
        }
    }

    // =========================================================
    //  リスポーン
    // =========================================================

    /** 勢力の全員（今居る人）のリスポーンを、その勢力の拠点にする。以後も拠点に戻る。 */
    private boolean risu(CommandSender sender) {
        int n = 0;
        List<String> nai = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            String team = kane.teamMei(p);
            if (team == null || !Seiryoku.seiryokuKa(team)) {
                continue;
            }
            Location l = basho.kyotenIchi(team);
            if (l == null) {
                if (!nai.contains(Seiryoku.mei(team))) {
                    nai.add(Seiryoku.mei(team));
                }
                continue;
            }
            // ★ force=true: ベッドが無くてもそこに戻る
            p.setBedSpawnLocation(l, true);
            n++;
        }
        plugin.getConfig().set(KEY_RISU, true);
        plugin.saveConfig();
        sender.sendMessage("[運営] " + n + " 人のリスポーンを拠点にしました。"
                + "後から入った人も、死ねば拠点に戻ります");
        if (!nai.isEmpty()) {
            sender.sendMessage("  ★ 拠点の位置が未登録: " + String.join(" / ", nai));
        }
        return true;
    }

    /**
     * 生き返る時。risu を一度でも打ってあれば、勢力の人は拠点に戻す。
     * ★ ベッドやリスポーンアンカーがある人は、そちらを尊重する。
     */
    public void ikikaeru(PlayerRespawnEvent e) {
        if (!plugin.getConfig().getBoolean(KEY_RISU, false)) {
            return;
        }
        if (e.isBedSpawn() || e.isAnchorSpawn()) {
            return;
        }
        String team = kane.teamMei(e.getPlayer());
        if (team == null || !Seiryoku.seiryokuKa(team)) {
            return;
        }
        Location l = basho.kyotenIchi(team);
        if (l != null) {
            e.setRespawnLocation(l);
        }
    }

    // =========================================================
    //  初期リスポーン（世界のスポーン）
    // =========================================================

    private boolean spawn(CommandSender sender, String[] args) {
        if (args.length == 2) {
            sender.sendMessage("[運営] 初期リスポーン: " + plugin.getConfig().getString(KEY_SPAWN, "(未設定)"));
            sender.sendMessage("       変えるには jidai game spawn here（足元） / jidai game spawn <x> <y> <z>");
            return true;
        }
        String atai;
        if (args[2].equals("here")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("[運営] here はゲーム内から。コマンドブロックからは座標で指定してください");
                return true;
            }
            atai = p.getLocation().getBlockX() + " " + p.getLocation().getBlockY()
                    + " " + p.getLocation().getBlockZ();
        } else if (args.length >= 5) {
            atai = args[2] + " " + args[3] + " " + args[4];
        } else {
            sender.sendMessage("[運営] 使い方: jidai game spawn here | <x> <y> <z>");
            return true;
        }
        if (tenkai(atai) == null) {
            sender.sendMessage("[運営] 座標が読めません: " + atai);
            return true;
        }
        plugin.getConfig().set(KEY_SPAWN, atai);
        plugin.saveConfig();
        spawnTekiyou();
        sender.sendMessage("[運営] 初期リスポーンを " + atai + " にしました");
        return true;
    }

    /**
     * 設定の初期リスポーンを、世界に書き込む。★ 起動時（onEnable）にも呼ぶ。
     * 設定に無ければ、ご指示の座標（HAJIME_NO_RISU）を書き込んで使う。
     */
    public void spawnTekiyou() {
        String atai = plugin.getConfig().getString(KEY_SPAWN, null);
        if (atai == null) {
            atai = HAJIME_NO_RISU;
            plugin.getConfig().set(KEY_SPAWN, atai);
            plugin.saveConfig();
        }
        int[] xyz = tenkai(atai);
        if (xyz == null) {
            plugin.getLogger().warning("初期リスポーンの座標が読めません: " + atai);
            return;
        }
        List<World> sekai = Bukkit.getWorlds();
        if (sekai == null || sekai.isEmpty()) {
            plugin.getLogger().warning("世界がまだ無いので、初期リスポーンを書けませんでした");
            return;
        }
        World w = sekai.get(0);
        w.setSpawnLocation(xyz[0], xyz[1], xyz[2]);
        plugin.getLogger().info("初期リスポーンを " + atai + " にした (" + w.getName() + ")");
    }

    /** "x y z"（空白か読点区切り）→ 3つの整数。読めなければ null。 */
    static int[] tenkai(String s) {
        if (s == null) {
            return null;
        }
        String[] bu = s.trim().split("[\\s,]+");
        if (bu.length != 3) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(bu[0]), Integer.parseInt(bu[1]), Integer.parseInt(bu[2])};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
