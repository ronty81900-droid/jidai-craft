// =============================================================
// Ginko.java ── 銀行（金ブロック1個で、預金と売却の両方をやる画面）
//
//   拠点の金ブロックを押すと、この画面が開く。
//     ・個人の金を勢力の貯金へ預ける (10 / 50 / 全部)
//     ・手持ちの貴金属をまとめて売って個人の金にする
//
//   ★★ もとはデータパックが持っていた ★★
//     jidai:kane/azukeru と jidai:kane/uru が中身を持ち、
//     アドバンスメントで起動していた。だが実機で一度も発火せず、
//     「押しても何も起きない」状態だった。
//     そこで金額の計算ごとプラグインへ移した。
//     データパック側のあの2つの関数は、もう使っていない。
//
//   ★ 略奪だけはデータパックのまま。
//     他勢力の拠点の金ブロックを押した時は、この画面を開かずに
//     データパックの略奪へ回す。戦争の判定は向こうの持ち物。
//
//   ★ 金額はここ1箇所にまとまっている。変えるならこのファイルの上だけ。
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class Ginko implements InventoryHolder {

    /**
     * 画面の枠の数。18 = 2段。
     *
     * ★★ 2段目(9〜17)は「名前と説明の帯」（2026-08-21） ★★
     *   枠の間隔は 18px、アイテムは 16px なので、ボタンを詰めて並べると
     *   下に文字を描く隙間が 2px しか無い。
     *   1段空けると 18px まるごと使えるので、リソースパックの絵で
     *   「10 預ける」「石油を預ける」などの文字を焼き込める。
     *   ボタンの並びは固定なので、絵に焼いてもずれない。
     */
    private static final int WAKU_SUU = 18;

    /**
     * 押す場所。★ ここを変えると並びが変わる
     *
     * ★★ 2026-08-22: 1段6列 → 2段3列 へ（ご指示）★★
     *   1段目(3〜5)  預ける 10 / 50 / 全部
     *   2段目(12〜14) 貴金属を売る / 石油を預ける / 残高
     *   MOD の古地図の画面は、段ごとに真ん中へ寄せて描く。
     */
    private static final int WAKU_10 = 3;
    private static final int WAKU_50 = 4;
    private static final int WAKU_ZENBU = 5;
    private static final int WAKU_URU = 12;

    /**
     * 手持ちの石油を預ける枠。
     *
     * ★★ 2026-08-20 に、石油の扱いが変わった ★★
     *   拾ってもアイテムのままで、数には入らない。
     *   ここで預けて初めて「勢力の石油」になる。
     */
    private static final int WAKU_SEKIYU = 13;

    /** 残高の表示。押せない。1段目の右端。 */
    private static final int WAKU_ZANDAKA = 14;

    /**
     * 手持ちの石油を【売る】枠（2026-08-23 のご指示）。
     *
     * ★★ 預けるのと売るのは、まったく別のこと ★★
     *   預ける … 勢力の石油になる。時代を進める条件に効く
     *   売る   … 個人の金になる。勢力の石油は1本も増えない
     *   拾った石油を「時代へ回すか、金へ回すか」の選択がここで生まれる。
     *
     * ★ 売っても【勢力の石油スコアには触らない】。
     *   預けた分を後から取り崩せると、時代進行の条件が意味を失うため。
     */
    private static final int WAKU_SEKIYU_URU = 11;

    /** 石油1本の売値（個人の金）。勢力の人はこの値。 */
    static final int SEKIYU_NEDAN = 8;

    /** 石油を売れるようになる時代（2=中世）。★ その勢力の時代で見る。 */
    static final int SEKIYU_URU_JIDAI = 2;

    /**
     * 貴金属が2倍になる時代（3=近代）と、その倍率。
     *
     * ★★ なぜ入れたか（2026-08-23 のご指示「経済勝利への救済」）★★
     *   貯金 500,000 は掘りだけだと 11時間かかり、1回の催しでは届かなかった。
     *   中央が近代へ入ったら【世界が好景気になる】ことにして、稼ぎを倍にする。
     *
     * ★ 見るのは【中央の時代】。勢力ごとの時代で見ると、先に進んだ勢力だけが
     *   先に倍になり、救済のはずが独走を助けてしまう。
     */
    static final int BUBBLE_JIDAI = 3;
    static final int BUBBLE_BAI = 2;

    /**
     * 売れる物と、その値段。★ 値段を変えるならここだけ。
     *
     * ★★ データパックの kane/uru.mcfunction と必ず同じ値にすること ★★
     *   2026-08-20 に改定: 鉄1 / ラピス5 / 金10 / ダイヤ15 / ネザライト30
     *   (前は 鉄1 / ラピス2 / 金4 / ダイヤ8)
     *
     * ★ ネザライトインゴットは、石を掘った時に 0.1% で出る
     *   (JidaiCraft.onBlockBreak)。ネザーへは行かないので、
     *   この世界では石掘りだけが入手経路になる。
     */
    private static final Material[] URERU = {
            Material.IRON_INGOT, Material.LAPIS_LAZULI,
            Material.GOLD_INGOT, Material.DIAMOND,
            Material.NETHERITE_INGOT,
    };
    private static final int[] NEDAN = {1, 5, 10, 15, 30};

    /** 拠点の持ち主を表す印の頭。データパックが summon 時に付けている */
    private static final String KYOTEN = "jidai_kyoten_";

    private final JidaiCraft plugin;

    /** 名前 → その人が押した金ブロック（残高の表示を作り直すために持つ） */
    /**
     * 今この画面を開いている人 → その人が見ている【入れ物そのもの】。
     *
     * ★★ 名前だけでなく入れ物を覚える理由（2026-08-24 の実機の指摘）★★
     *   残高を出し直すために openInventory で開き直すと、
     *   Minecraft は先に【今の画面を閉じた】知らせを飛ばす。
     *   名前だけで覚えていると、その知らせで「もう閉じた」と記録してしまい、
     *   **次のクリックが全部 無視される**（10円 預けたら、その後 何も押せない）。
     *   十連で踏んだのと同じ罠。開き直した入れ物と食い違う時だけ忘れる。
     */
    private final Map<String, org.bukkit.inventory.Inventory> hiraiteru = new HashMap<>();

    public Ginko(JidaiCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, WAKU_SUU, Enshutsu.gamenMei(Enshutsu.UI_GINKO, "銀行"));
    }

    /**
     * 金ブロックが押された。
     *
     * ★ 順序が大事
     *   1. その拠点が他勢力のものなら、略奪へ回して終わり
     *   2. 勢力に入っていなければ断る
     *   3. 画面を開く
     */
    public void osareta(Player player, Block block, Kane kane, Seiryoku seiryoku) {
        String jibun = kane.teamMei(player);
        String nushi = kyotenNoNushi(block);

        // --- (1) 他勢力の拠点 → 略奪はデータパックの担当 ------------
        //
        // ★★ 2026-08-22: プラグイン側でも戦争の状態を見る（ご指示）★★
        //   「戦争中以外では銀行から略奪できない。宣戦布告をしてから、
        //    ようやく敵の銀行から取れる」
        //   データパックも同じ判定を持っているが、そちらは断る理由が
        //   1行出るだけで、何をすれば取れるのかが伝わらなかった。
        //   ここで先に見て、理由と次の手を本人に返す。
        //     0 なし / 3 再戦禁止 … 宣戦していない → 断る
        //     1 準備             … 宣戦済み。交戦が始まるまで待つ
        //     2 交戦             … データパックの略奪へ
        // ★★ 2026-09-09: 略奪の入口は【壊す】に変わった（ご指示）★★
        //   押しても奪えない。ここでは「壊せば奪える」ことだけ伝える。
        //   実際に奪うのは kowasareta（JidaiCraft.onBlockBreak から呼ばれる）。
        if (nushi != null && !nushi.equals(jibun)) {
            if (ryakudatsuJotai(player, jibun, nushi, kane) == 2) {
                player.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO,
                        Seiryoku.mei(nushi) + " と交戦中。この金ブロックを【壊す】と、"
                                + "相手の貯金と石油を 1% ずつ奪える"));
                Enshutsu.oto(player, Enshutsu.OTO_DAME);
            }
            return;
        }

        // --- (3) 勢力に入っていない人は預ける先が無い ---------------
        if (kane.kinkoMei(player) == null) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "勢力に入っていません。先に運営に入れてもらってください"));
            return;
        }

        org.bukkit.inventory.Inventory inv = tsukuru(player, kane);
        hiraiteru.put(player.getName(), inv);
        player.openInventory(inv);
    }

    /**
     * この金ブロックが「どの勢力の拠点か」を調べる。
     * 分からなければ null（＝誰の拠点でもない扱い）。
     *
     * ★ データパックが施設のマーカーに jidai_kyoten_<チーム名> を付けている。
     *   4ブロック以内を見るのは、データパック側の判定と同じ距離に揃えるため。
     */
    private String kyotenNoNushi(Block block) {
        for (Entity e : block.getWorld().getNearbyEntities(
                block.getLocation().add(0.5, 0.5, 0.5), 4, 4, 4)) {
            for (String tag : e.getScoreboardTags()) {
                if (tag.startsWith(KYOTEN)) {
                    return tag.substring(KYOTEN.length());
                }
            }
        }
        return null;
    }

    /**
     * 他の勢力の銀行に手を出せる状態かを見て、駄目なら理由を本人へ返す。
     * 戻り値は戦争の状態（2＝交戦中＝手を出せる）。それ以外は 0 を返す。
     *
     * ★★ なぜプラグイン側でも戦争の状態を見るか（2026-08-22 のご指示）★★
     *   「戦争中以外では銀行から略奪できない。宣戦布告をしてから、
     *    ようやく敵の銀行から取れる」
     *   データパックも同じ判定を持っているが、そちらは断る理由が1行 出るだけで、
     *   何をすれば取れるのかが伝わらなかった。ここで先に見て、次の手を返す。
     *
     * ★ 押した時（osareta）と壊した時（kowasareta）の両方から呼ぶ。
     *   同じ判定を2か所に書くと、片方だけ直した時に黙って食い違う。
     */
    private int ryakudatsuJotai(Player player, String jibun, String nushi, Kane kane) {
        if (jibun == null || !Seiryoku.seiryokuKa(jibun)) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "勢力に入っていないので、他の勢力の銀行には手を出せません"));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return 0;
        }
        String jibunMei = Seiryoku.mei(jibun);
        String nushiMei = Seiryoku.mei(nushi);
        int jotai = kane.sensou(jibunMei, nushiMei);
        if (jotai == 1) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO,
                    nushiMei + " には宣戦済み。交戦が始まるまで略奪できない (あと "
                            + kane.sensouByou(jibunMei, nushiMei) + " 秒)"));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return 0;
        }
        if (jotai != 2) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO,
                    nushiMei + " とは戦争していない。略奪するには、まず販売所の「戦争宣誓」で宣戦すること"));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return 0;
        }
        return 2;
    }

    /**
     * 他の勢力の銀行の金ブロックが【壊された】。奪えたら true。
     *
     * ★★ 2026-09-09 のご指示: 略奪の入口はここ1本 ★★
     *   1回 壊すごとに、相手の貯金と石油の 1% を奪う（量はデータパックが決める）。
     *   攻める側が 占領_必要回数(100) 回 壊すと、相手のビーコンが壊せるようになる。
     *
     * ★ ブロックは壊させない（呼び出し元がイベントを止める）。
     *   金ブロックが本当に消えると、その拠点は二度と略奪できなくなる。
     *   壊す動作そのものを1回と数え、演出だけ「壊れた」ように見せる。
     */
    public boolean kowasareta(Player player, Block block, Kane kane, Seiryoku seiryoku) {
        String jibun = kane.teamMei(player);
        String nushi = kyotenNoNushi(block);
        if (nushi == null || nushi.equals(jibun)) {
            return false;      // 自分の銀行・持ち主不明。呼び出し元がふつうに守る
        }
        if (ryakudatsuJotai(player, jibun, nushi, kane) != 2) {
            return false;
        }
        ryakudatsuHe(player, nushi, kane, seiryoku);
        return true;
    }

    /**
     * 略奪はデータパックへ丸ごと渡す。
     *
     * ★★ 1.20.1 移行で呼び方が変わった（2026-08-19） ★★
     *   1.21 では storage に勢力名を入れて `... with storage jidai:kari` と
     *   渡していた。これはマクロの形で、1.20.1 には無い（実測）。
     *   いまは【番号をスコアに置いてから呼ぶ】。
     *
     *     #aite_no  … その拠点を持つ勢力の番号（aite_yomu が入れる）
     *     #jibun_no … 押した人の勢力の番号（bangou をそのまま写す）
     *
     * ★ 戦争しているかどうかの判定は、いまも向こう持ち。
     */
    private void ryakudatsuHe(Player player, String aiteTeam, Kane kane, Seiryoku seiryoku) {
        String namae = player.getName();
        String asoko = "execute as " + namae + " at " + namae + " run ";

        // ★★ 押す前のクールダウンを控える（2026-08-23・案D）★★
        //   データパックは、断る時は何も置かずに引き返し、実際に奪えた時にだけ
        //   最後に ryakudatsu_kan を置く。前が 0 で 後が 1以上 なら成立している。
        //   ★ 返り値は dispatchCommand からは読めない。向こうが元から残す印を読む。
        int maeKan = kane.ryakudatsuKan(player);

        // (a) 足元の拠点が誰のものかを調べさせる（#aite_no が入る）
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                asoko + "function jidai:sensou/aite_yomu");
        // (b) 押した人の勢力の番号を置く
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "execute as " + namae
                + " run scoreboard players operation #jibun_no sagyou = @s bangou");
        // (c) 略奪の本体
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                asoko + "function jidai:sensou/ryakudatsu");

        // (d) 金と石油が動いた時だけ、特殊アイテムも1枚 奪う
        //   ★ 断られた時（クールダウン中・上限に達した）は何も奪わない。
        //     押しただけで紙が動くと、金を奪えない相手からでも紙だけ抜けてしまう。
        if (maeKan == 0 && kane.ryakudatsuKan(player) > 0) {
            Shouri.ryakudatsuDeUbau(player, aiteTeam, seiryoku);
        }
    }

    /** 画面を組み立てる。残高は開いた時点の値を焼き込む。 */
    private Inventory tsukuru(Player player, Kane kane) {
        Inventory inv = Bukkit.createInventory(this, WAKU_SUU, Enshutsu.gamenMei(Enshutsu.UI_GINKO, "銀行"));
        String kinko = kane.kinkoMei(player);
        int kojin = kane.kojinZandaka(player);
        int seiryoku = kane.seiryokuZandaka(kinko);

        inv.setItem(WAKU_10, botan(Material.GOLD_NUGGET, "10円",
                "個人の金 10 を勢力の貯金へ", kojin, 10));
        inv.setItem(WAKU_50, botan(Material.GOLD_INGOT, "50円",
                "個人の金 50 を勢力の貯金へ", kojin, 50));
        inv.setItem(WAKU_ZENBU, botan(Material.GOLD_BLOCK, "全額貯金",
                "持っている個人の金をすべて", kojin, 1));

        List<String> uru = new ArrayList<>();
        uru.add(moji("鉄1 / ラピス5 / 金10 / ダイヤ15 / ネザライト30"));
        uru.add(moji("手持ちをまとめて売って個人の金にします"));
        inv.setItem(WAKU_URU, kazari(Material.EMERALD, "貴金属を売る", uru));

        List<String> zan = new ArrayList<>();
        zan.add(moji("個人の金  " + kojin));
        zan.add(moji(kinko + " の貯金  " + seiryoku));
        List<String> abura = new ArrayList<>();
        abura.add(moji("手持ちの石油をまとめて預けます"));
        abura.add(moji("預けた石油だけが 時代進行の条件に入ります"));
        abura.add(moji("★手持ちのままだと死んだ時に落とします"));
        abura.add(moji("★勢力ごとに上限があります (時代を進めると上がる)"));
        inv.setItem(WAKU_SEKIYU, kazari(Material.BLACK_DYE, "石油を預ける", abura));

        // ★ 石油を売る（中世から）。まだ開いていない時は、販売所と同じ灰色の板ガラスで塞ぐ。
        //   MOD の画面は中身を見て「？？」と描くので、こちらで文言を持たなくてよい。
        int jidai = kane.jidaiOf(player);
        if (jidai >= SEKIYU_URU_JIDAI) {
            inv.setItem(WAKU_SEKIYU_URU, kazari(Material.BLACK_DYE, "石油を売る",
                    List.of("手持ちの石油を 1本 " + SEKIYU_NEDAN + " で売る",
                            "★ 預けた石油は減らない（別のもの）",
                            "売った金は個人の金へ")));
        } else {
            inv.setItem(WAKU_SEKIYU_URU, mikaikin("中世"));
        }

        // ★ 残高は2段目の右(枠14)。WAKU_SUU-1 と書くと並びが崩れるので実数で書く。
        inv.setItem(WAKU_ZANDAKA, kazari(Material.PAPER, "残高", zan));
        return inv;
    }

    /** 預ける用のボタン。足りない時は理由を出す。 */
    private ItemStack botan(Material material, String namae, String setsumei,
                            int kojin, int hitsuyou) {
        List<String> shita = new ArrayList<>();
        shita.add(moji(setsumei));
        if (kojin < hitsuyou) {
            shita.add(moji("個人の金が足りません (今 " + kojin + ")"));
        }
        return kazari(material, namae, shita);
    }

    /** 見た目を作るだけの小道具。 */
    private ItemStack kazari(Material material, String namae, List<String> shita) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(moji(namae));
        meta.setLore(shita);
        // ★★ 石油のボタンは、石油そのものと同じ絵にする（2026-09-20）★★
        //   石油のアイテムは黒い染料＋番号 8301 で樽の絵になる。
        //   ボタンだけ素の黒い染料のままだと、並べた時に食い違って見える。
        //   ★ MOD を入れていない人には、今までどおり黒い染料に見えるだけ。
        if (material == Material.BLACK_DYE) {
            meta.setCustomModelData(SEKIYU_MITAME);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** 押された枠に応じて処理する。 */
    public void oshita(Player player, int slot, Kane kane) {
        if (!hiraiteru.containsKey(player.getName())) {
            return;   // 画面を閉じた後の取りこぼし
        }
        if (slot == WAKU_SEKIYU_URU) {
            sekiyuUru(player, kane);
        } else if (slot == WAKU_SEKIYU) {
            sekiyuAzukeru(player);
        } else if (slot == WAKU_URU) {
            uru(player, kane);
        } else if (slot == WAKU_10) {
            azukeru(player, kane, 10);
        } else if (slot == WAKU_50) {
            azukeru(player, kane, 50);
        } else if (slot == WAKU_ZENBU) {
            azukeru(player, kane, kane.kojinZandaka(player));
        } else {
            return;   // 飾りの枠
        }
        // 残高が変わったので画面を作り直す。★ 次の tick に回すこと。
        //   クリックの処理中に画面を開き直してはいけない決まりのため。
        // ★★ 先に新しい入れ物を覚えてから開く ★★
        //   openInventory は【今の画面を閉じた】知らせを先に飛ばす。
        //   覚えるのが後だと、その知らせで自分を忘れてしまい、次から押せなくなる。
        Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.inventory.Inventory atarashii = tsukuru(player, kane);
            hiraiteru.put(player.getName(), atarashii);
            player.openInventory(atarashii);
        });
    }

    /**
     * まだ開いていない枠。販売所と同じ灰色の板ガラスにする。
     * ★ MOD の画面は「灰色の板ガラス＝未開放」で見分けるので、材質を変えないこと。
     */
    private ItemStack mikaikin(String jidaiMei) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        // ★★ 2026-08-26 の実機の指摘 ★★
        //   画面のカードは「？？」なのに、カーソルを乗せた吹き出しに
        //   本当の名前が出ていた（＝ネタバレ）。
        //   MOD はカードの字を差し替えるが、吹き出しは【アイテムの本当の名前】を出す。
        //   伏せたいなら、名前そのものを「？？」にするしかない。
        //   ★ 販売所の未開放（Shop.tsukuru）は元からこの形。そちらに合わせた。
        meta.setDisplayName(moji("？？"));
        meta.setLore(List.of(moji("§7" + jidaiMei + "になると使えます")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 手持ちの石油を売って、個人の金にする（2026-08-23 のご指示）。
     *
     * ★★ 預けた石油（勢力の石油）は1本も減らさない ★★
     *   売るのは【まだ預けていない、手に持っている石油】だけ。
     *   預けた分を取り崩せると、時代を進める条件が意味を失う。
     *
     * ★ 石油の見分け方は「黒色の染料で、名前に石油と入っている物」。
     *   データパックが summon する時に名前を付けている（sekiyu/waku.mcfunction）。
     *   金床と鍛冶台を塞いであるので、参加者は名前を付け替えられない。
     */
    private void sekiyuUru(Player player, Kane kane) {
        sekiyuUruHontai(player, kane);
    }

    /**
     * 石油を売る本体。★ 単価はここ1箇所。
     */
    static void sekiyuUruHontai(Player player, Kane kane) {
        if (kane.jidaiOf(player) < SEKIYU_URU_JIDAI) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "石油を売れるのは中世からです"));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }
        int kazu = 0;
        ItemStack[] naka = player.getInventory().getContents();
        for (int i = 0; i < naka.length; i++) {
            if (!sekiyuNoAitem(naka[i])) {
                continue;
            }
            kazu += naka[i].getAmount();
            player.getInventory().setItem(i, null);
        }
        if (kazu <= 0) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "手持ちに石油がありません（拾った石油だけ売れます）"));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }
        int gaku = kazu * SEKIYU_NEDAN;
        int mae = kane.kojinZandaka(player);
        kane.kojinKousin(player, mae + gaku);
        player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                "石油×" + kazu + " を 1本 " + SEKIYU_NEDAN + " で売って " + gaku
                        + " になった (個人の金 " + mae + " → " + (mae + gaku) + ")"));
        Enshutsu.oto(player, Enshutsu.OTO_CHU);
    }

    /**
     * 石油の見た目の番号（CustomModelData）。
     *
     * ★★ 正本はデータパック ★★
     *   `jidai:sekiyu/waku` と `jidai:sekiyu/dashi_give` が石油に付ける番号。
     *   ここは写しなので、`clientmod/tests/mod_kakunin.py` が
     *   **データパック・プラグイン・MOD の3つで同じか**を見張っている。
     */
    static final int SEKIYU_MITAME = 8301;

    /** 中央プラントが出した石油のアイテムか。 */
    static boolean sekiyuNoAitem(ItemStack item) {
        if (item == null || item.getType() != Material.BLACK_DYE) {
            return false;
        }
        ItemMeta m = item.getItemMeta();
        return m != null && m.hasDisplayName() && m.getDisplayName().contains("石油");
    }

    /**
     * 手持ちの石油を預ける。
     *
     * ★★ 中身はぜんぶデータパックがやる ★★
     *   数える・消す・先行ペナルティ・上限の判定は
     *   jidai:sekiyu/azukeru が持っている。プラグインは呼ぶだけ。
     *   ここで本数を数えて金を動かすと、二重に処理してしまう。
     *   （下剋上を jidai:sensou/gekokujo_kau に丸投げしているのと同じ考え方）
     *
     * ★ 断りの文も向こうが出す。持っていない時・上限の時も含めて。
     */
    private void sekiyuAzukeru(Player player) {
        String cmd = "execute as " + player.getName()
                + " run function jidai:sekiyu/azukeru";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    /**
     * 個人の金を勢力の貯金へ移す。
     *
     * ★ 全体に知らせるのは、データパックからそのまま引き継いだ狙い。
     *   「預けない人が見える」ことが、預けさせる圧力になる。
     */
    private void azukeru(Player player, Kane kane, int gaku) {
        String kinko = kane.kinkoMei(player);
        if (kinko == null) {
            return;
        }
        if (gaku <= 0) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU, "預ける金がありません"));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }
        int kojin = kane.kojinZandaka(player);
        if (kojin < gaku) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "個人の金が足りません (預ける額 " + gaku + " / 所持 " + kojin + ")"));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }

        int mae = kane.seiryokuZandaka(kinko);
        kane.kojinKousin(player, kojin - gaku);
        kane.seiryokuKousin(kinko, mae + gaku);

        Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.KOUNYU,
                player.getName() + " が 勢力へ " + gaku + " 預けた ("
                        + kinko + " の貯金 " + mae + " → " + (mae + gaku) + ")"));
        Enshutsu.oto(player, Enshutsu.OTO_SHO);
    }

    /** 手持ちの貴金属をまとめて売る。 */
    private void uru(Player player, Kane kane) {
        kikinzokuUru(player, kane);
    }

    /**
     * 手持ちの貴金属をまとめて売る本体。
     * ★ 単価はここ1箇所。
     */
    static void kikinzokuUru(Player player, Kane kane) {
        int gokei = 0;
        StringBuilder uchiwake = new StringBuilder();
        for (int i = 0; i < URERU.length; i++) {
            int kazu = kazoeru(player, URERU[i]);
            if (kazu <= 0) {
                continue;
            }
            gokei += kazu * NEDAN[i];
            player.getInventory().remove(URERU[i]);
            if (uchiwake.length() > 0) {
                uchiwake.append(" ");
            }
            uchiwake.append(mei(URERU[i])).append("×").append(kazu);
        }

        if (gokei <= 0) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "売れる貴金属を持っていません (鉄・ラピス・金・ダイヤ)"));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }

        // ★★ バブル（2026-08-23 のご指示）★★
        //   中央が近代へ入ったら、世界が好景気になって貴金属が2倍で売れる。
        //   経済勝利（貯金 500,000）が1回の催しで届かなかったことへの救済。
        //   ★ 見るのは中央の時代。勢力ごとに見ると、先に進んだ勢力だけ先に倍になる。
        String oma = "";
        if (kane.chuoJidai() >= BUBBLE_JIDAI) {
            gokei = gokei * BUBBLE_BAI;
            oma += " (バブル ×" + BUBBLE_BAI + ")";
        }

        // ★ 遺物「ロスチャイルドの金庫」を持つ勢力は さらに 1.2倍
        int bai = Ibutsu.uriBairitsu(kane.kinkoMei(player));
        if (bai != 100) {
            gokei = gokei * bai / 100;
            oma += " (金庫 ×" + (bai / 100) + "." + (bai % 100 / 10) + ")";
        }

        int mae = kane.kojinZandaka(player);
        kane.kojinKousin(player, mae + gokei);
        player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                uchiwake + " を売って " + gokei + " になった" + oma + " (個人の金 "
                        + mae + " → " + (mae + gokei) + ")"));
        Enshutsu.oto(player, Enshutsu.OTO_CHU);
    }

    /** 持ち物の中にその品がいくつあるか数える。 */
    private static int kazoeru(Player player, Material material) {
        int n = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                n += item.getAmount();
            }
        }
        return n;
    }

    /** 画面に出す日本語の名前。 */
    private static String mei(Material material) {
        if (material == Material.IRON_INGOT) {
            return "鉄";
        }
        if (material == Material.LAPIS_LAZULI) {
            return "ラピス";
        }
        if (material == Material.GOLD_INGOT) {
            return "金";
        }
        return "ダイヤ";
    }

    /** 画面を閉じた人を忘れる。 */
    public void tojita(String namae, org.bukkit.inventory.Inventory tojita) {
        // ★ 開き直した時の「閉じた」知らせで自分を忘れないよう、
        //   今 覚えている入れ物と同じ時だけ忘れる（2026-08-24）。
        if (hiraiteru.get(namae) == tojita) {
            hiraiteru.remove(namae);
        }
    }

    /** 文字列を画面に出せる形に変換する。 */
    private static String moji(String s) {
        return Enshutsu.MODOSU + s;
    }
}
