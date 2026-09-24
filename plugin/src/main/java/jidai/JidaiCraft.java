// =============================================================
// JidaiCraft.java ── 時代クラフト 運営用プラグインの入口
//
//   段階4 の中身:
//     登録された座標のエメラルドブロックを右クリックすると販売所が開く。
//     商品をクリックすると、データパックの金で実際に買える。
//     勢力の金で買った場合は全体に通知する。
//     登録済みのブロックは壊せない。
//
//   このファイルの担当は「サーバーとのやりとり」だけ。
//     何をいくらで売るか → Shop.java
//     金の出し入れ       → Kane.java
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * プラグインの入口。サーバーはこのクラスを1個だけ作って動かす。
 *
 * extends JavaPlugin
 *   「JavaPlugin を土台にして、その一部を自分用に差し替える」宣言 (継承)。
 *   ★ プラグインは必ず JavaPlugin を継承する。これがサーバーとの約束。
 *
 * implements Listener
 *   「このクラスはイベントを受け取る係でもある」という宣言。
 *   extends は1つしか書けないが、implements は何個でも書ける。
 */
public final class JidaiCraft extends JavaPlugin implements Listener {

    /** 販売所。起動時に1軒だけ作り、全員で共有する。 */
    private Shop shop;

    /**
     * 銃器専門店。販売所と同じ作りで、品揃えだけが違う。
     * ★ どちらもエメラルドブロック。押した場所の【登録】で開く店が決まる。
     */
    private Shop juki;

    /** 金の出し入れ係（データパックのスコアボードに触る唯一の場所）。 */
    private Kane kane;

    /** ガチャ。画面は開くたびに1人ずつ作る（演出が混ざらないように）。 */
    private Gacha gacha;

    /** 運営（ゲーム開始・停止・リセット・テレポ・リスポーン）。コマンドブロックから使う。 */
    private Kanri kanri;

    /** 見せ方（音・通知の書式・時代進行の演出）。 */
    private Enshutsu enshutsu;

    /** 勢力の状態（リーダー・代行・下剋上の権利）。 */
    private Seiryoku seiryoku;

    /** 戦争宣誓の相手を選ぶ画面。 */
    private Sensen sensen;

    /** 銀行の画面（預金と売却）。 */
    private Ginko ginko;

    /** 時代を進める画面。 */
    private Shinko shinko;

    /** エンダーかまど（1人1枚の精錬枠）。 */
    private Kamado kamado;

    /** 時代のツルハシ（1人1本・壊れない・死んでも落とさない）。 */
    private Tsuruhashi tsuruhashi;

    /**
     * 勝利条件のうち、プラグインが見る物（**超特殊のみ**）。
     * ★ 人望は傭兵ごと止めた（2026-08-26）。ここは1つだけを見ている。
     */
    private Shouri shouri;

    /** 同士討ちの禁止と、殺した時の分捕り（2026-08-24 のご指示）。 */
    private Tatakai tatakai;

    /**
     * 石を掘った時のくじ引きに使う。
     * ★ ガチャとは別に持つ。同じ種を使い回すと、
     *   片方の引きがもう片方の出目に影響してしまう。
     *
     * ★★ final にせず、初めて使う時に作る ★★
     *   検証ハーネスは Unsafe でコンストラクタを通さずにこのクラスを作る。
     *   その時 = new Random() は走らないので、null のまま石を掘ると落ちる
     *   （実際に落ちた。ツルハシの鍵と同じ罠）。
     */
    private Random ishiRan;

    /** くじ引き。初めて必要になった時に1回だけ作る。 */
    private Random ishiRan() {
        if (ishiRan == null) {
            ishiRan = new Random();
        }
        return ishiRan;
    }

    /**
     * 石を掘った時に、これだけの確率でネザライトインゴットが出る。
     * 1000 なら 1/1000 = 0.1%。★ 確率を変えるならここだけ。
     */
    private static final int NEZA_KAKURITSU = 1000;

    /**
     * ネザライトが出る石。
     * ★ ネザーへは行かない企画なので、ここが唯一の入手経路になる。
     *   丸石や深層岩の丸石ではなく、地中の素の石だけを対象にする。
     *   (積み直した丸石を掘り直して稼ぐ、という手を塞ぐため)
     */
    private static final List<Material> NEZA_ISHI = List.of(
            Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE,
            Material.DEEPSLATE);

    /** 深層岩は出やすい（2026-08-22 のご指示）。500 なら 1/500 = 0.2%。 */
    private static final int NEZA_KAKURITSU_SHINSO = 500;

    /**
     * ★★ 作ってよい物 ★★
     *   ご指示の「建材アイテムのみ」を、名前の形で表している。
     *     各ハーフブロック   … 名前が _SLAB で終わる物すべて
     *     石レンガ           … STONE_BRICKS と、_STONE_BRICKS で終わる物
     *     磨かれた系         … 名前が POLISHED_ で始まる物すべて
     *     各柵               … _FENCE と _FENCE_GATE で終わる物
     *     作業台             … 上の物を作るのに要るので、これだけ足した
     *
     *   ★ 一覧ではなく名前の形で見ているのは、木の種類が増えるたびに
     *     書き足す必要をなくすため（樫・白樺・トウヒ…で20行を超える）。
     *
     *   ★★ 装備・道具・銃は1つも通らない ★★
     *     剣も防具もツルハシも、上のどの形にも当てはまらない。
     *     載っていない物は【すべて作れない】側なので、
     *     新しい MOD が入っても勝手に作れるようにはならない。
     *
     *   ★ 階段(_STAIRS)と塀(_WALL)はご指示に無いので入れていない。
     *     要るようなら、下に1行足すだけで足せる。
     */
    private static boolean tsukutteYoi(Material m) {
        if (m == null || m == Material.AIR) {
            return false;
        }
        String n = m.name();
        // ★★ 板材（_PLANKS）を足した（2026-08-22 のご指摘）★★
        //   原木→板材が通らないと、作業台そのものが作れない
        //   （作業台は板材4枚）。建材の入口なので、ここが閉じていると
        //   上の一覧は全部 飾りになる。
        return n.endsWith("_SLAB")
                || n.endsWith("_PLANKS")
                || n.equals("STONE_BRICKS") || n.endsWith("_STONE_BRICKS")
                || n.startsWith("POLISHED_")
                || n.endsWith("_FENCE") || n.endsWith("_FENCE_GATE")
                || m == Material.CRAFTING_TABLE
                // ★ 2026-08-24 のご指示。物を仕舞う場所が無いと拠点が組めない。
                || m == Material.CHEST;
    }

    /**
     * クラフトを禁止しているか。★ 既定は true（禁止）。
     *   jidai craft kyoka で一時的に外せる。保存はしない ――
     *   再起動したら必ず禁止に戻る方が、外しっぱなしの事故より安全。
     */
    private boolean craftKinshi = true;

    /**
     * 物を作れてしまう台のうち、【使わせない】もの。
     *
     * ★★ 作業台はここから外した (2026-08-20) ★★
     *   建材のクラフトを許したが、ハーフブロックは 1x3、柵は 2x3 の
     *   並びで作る（実物のレシピで確認）。持ち物の 2x2 では足りず、
     *   作業台が無いと1つも作れない。そのため作業台だけ開放した。
     *   ★ 作れる物は TSUKURERU_KENZAI に載っているものだけなので、
     *     作業台を開けても装備も銃も作れない。
     *
     * ★ かまど（エンダーかまど）はこの企画の仕組みなので入れない。
     */
    private static final List<Material> TSUKURU_DAI = List.of(
            Material.STONECUTTER,
            Material.SMITHING_TABLE,
            Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
            Material.LOOM,
            Material.CARTOGRAPHY_TABLE,
            Material.GRINDSTONE);

    /**
     * 施設の場所を覚えている係。
     * 販売所・売却所・銀行・ガチャの4種類を種別ごとに持つ。
     */
    private Basho basho;

    /**
     * サーバー起動時、プラグインが有効になった直後に呼ばれる。
     * 呼ぶのはサーバー側。こちらから呼ぶことはない。
     */
    @Override
    public void onEnable() {
        kane = new Kane();

        // --- 先に、サーバーの状態に頼らない部分をすべて済ませる -------
        // 画面の組み立て(new Shop)より前に置いているのは、
        // ここで失敗しても原因が分かりやすいようにするため。

        // 設定ファイルから、前回までに登録した販売所を読み戻す。
        //
        // ★ saveDefaultConfig() を呼んではいけない。
        //   あれは「.jar に同梱した config.yml を、初回だけ書き出す」処理で、
        //   同梱していないと IllegalArgumentException で落ちる。
        //   しかも落ちるのは config.yml がまだ無い初回起動のときだけなので、
        //   2回目以降は動いてしまい、気づきにくい。(実機で発生)
        //
        //   同梱ファイルは要らない。getConfig() はファイルが無ければ
        //   空の設定を返し、saveConfig() が書き出す時に作ってくれる。
        basho = new Basho();
        basho.yomikomi(this);
        seiryoku = new Seiryoku(this);
        seiryoku.yomikomi();

        // --- ここから先はサーバーが起動しきっている必要がある ---------
        sensen = new Sensen(this, seiryoku);
        ginko = new Ginko(this);
        shinko = new Shinko(this, seiryoku);
        kamado = new Kamado(this);
        kamado.reshipiYomikomi();
        kamado.yomikomi();
        Shop.jukiYomikomi(this);      // 銃器専門店の解放（勢力ごと）を読み戻す
        tsuruhashi = new Tsuruhashi(this);

        // 運営の係。初期リスポーン地点はここで世界に書き込む
        kanri = new Kanri(this, kane, basho);
        kanri.spawnTekiyou();
        shouri = new Shouri(this);
        tatakai = new Tatakai(this);
        shop = new Shop(Basho.MISE);
        shop.seiryokuWatasu(this, seiryoku, sensen);
        juki = new Shop(Basho.JUKI);
        juki.seiryokuWatasu(this, seiryoku, sensen);
        gacha = new Gacha(this);
        gacha.yomikomi();             // 特殊アイテムの「勢力ごとに各1個」の記録
        kanri.gachaWatasu(gacha);     // 全部リセットでその記録も消せるように（kanri は上で作成済み）
        enshutsu = new Enshutsu(this, kane);

        // 1秒(20tick)に1回、見張りを回す。
        //
        // () -> ... は「この処理を後で実行してほしい」の渡し方。
        // 今すぐ呼ぶのではなく、処理そのものをサーバーに預けている。
        getServer().getScheduler().runTaskTimer(this, () -> {
            enshutsu.byoumai();     // 中央の時代が進んだかを見る
            // ★ 勝ちが出ていないかを見る（超特殊勝利）。
            //   戦争・経済・未来到達はデータパック側で見ている。
            shouri.byoumai(kane, seiryoku);
            Ibutsu.byoumai(kane);   // 遺物の効果（スピード等）を勢力の人にかけ直す
            // ★ 保険。コマンド等で効果が入れられていたら消す
            basho.kamadoNoTsubu();  // エンダーかまどの粒（見た目だけ）

            // ★ 銃器専門店の解放を見張る（貯金が一度でも 10,000 を超えたら開く）
            for (String kuni : Seiryoku.zenTeam()) {
                // ★ 貯金の持ち主は日本語名。チーム名のままでは必ず 0 になる
                String kinko = Kane.kinkoMeiTeam(kuni);
                if (kinko != null
                        && Shop.jukiKaihouMiru(this, kuni, kane.seiryokuZandaka(kinko))) {
                    for (Player p : getServer().getOnlinePlayers()) {
                        if (kuni.equals(kane.teamMei(p))) {
                            p.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                                    "銃器専門店が開きました（勢力の貯金が "
                                    + Shop.JUKI_KAIHOU + " を超えました）"));
                            Enshutsu.oto(p, Enshutsu.OTO_CHU);
                        }
                    }
                    getLogger().info("銃器専門店が開いた: " + Seiryoku.mei(kuni));
                }
            }
            int keshita = basho.koukaKesu();
            if (keshita > 0) {
                getLogger().warning("ビーコンの効果を " + keshita + " 件消しました"
                        + "（採掘速度上昇が付くと経済が崩れるため）");
            }
        }, 20L, 20L);

        // --- エンダーかまどを毎tick進める --------------------------
        // ★ 1秒に1回では焼き上がりが粗くなるので、こちらは毎tick。
        //   ★ 画面を閉じていても焼き続ける必要があるため、
        //     開いている人だけでなく全員ぶんを回す。
        //     1人あたりは品物3つを見るだけなので、50人でも軽い。
        getServer().getScheduler().runTaskTimer(this,
                () -> kamado.susumu(kane), 20L, 1L);

        // イベントを受け取るには「登録」が要る。名乗るだけでは届かない。
        //   1個目の this = イベントを受け取る係 (Listener を名乗っているこのクラス)
        //   2個目の this = どのプラグインのものか (このプラグイン自身)
        // ★ この1行を書き忘れると、エラーも出ずに「何も起きない」だけになる。
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("読み込み完了 / 登録済みの施設 " + basho.kensuu() + " 件");

        // --- 起動から5秒後に、ワールドのマーカーを走査して登録し直す -------
        //
        // ★ すぐに走査しない理由
        //   マーカーは「読み込まれている区画」にある分しか取れない。
        //   データパックが拠点の区画を forceload するのは jidai:load の中で、
        //   プラグインの起動と前後する可能性がある。少し待ってから見る。
        //
        // ★ 見つからなくても一覧は壊さない（Basho.scan の中で守っている）。
        getServer().getScheduler().runTaskLater(this, () -> jidoScan(), 100L);

        // ★★ データパックの確認は「ここ」ではやらない ★★
        //   ここ(onEnable)は、まだ1tickも進んでいない時点です。
        //   データパックの jidai:load は #minecraft:load という仕組みで
        //   【最初の tick】に走るので、この時点ではまだ目的が1つも無い。
        //   ここで調べると、正しく入っていても「読み込まれていません」と
        //   出てしまいます（実機の Arclight で実際に起きた。2026-08-20）。
        //
        //   ★ Paper では出ないことがあります。前に一度動かした世界だと
        //     目的が scoreboard.dat に残っているためで、まっさらな世界なら
        //     Paper でも同じ誤警告が出ます。
        //
        //   そこで、5秒後の jidoScan() の中でまとめて調べます。
    }

    /**
     * データパックがちゃんと動いたかを調べて、駄目なら理由を出す。
     *
     * ★ 起動から5秒後に呼ぶこと。onEnable では早すぎる（上の説明を参照）。
     */
    private void datapackKakunin() {
        if (kane.junbiOK()) {
            getLogger().info("データパック jidai_craft を確認しました");
            return;
        }
        getLogger().warning("★ データパック jidai_craft が動いていません");
        if (!kane.junbiOK()) {
            getLogger().warning("  kane_kojin / chokin が見つかりません（購入ができません）");
        }
        getLogger().warning("  確かめること:");
        getLogger().warning("   1. <ワールド名>/datapacks/jidai_craft/pack.mcmeta があるか");
        getLogger().warning("   2. コンソールで  datapack list enabled  に jidai_craft が出るか");
        getLogger().warning("   3. 出ていなければ  datapack enable \"file/jidai_craft\"");
        getLogger().warning("   4. それでも駄目なら  function jidai:load  を手で打つ");
    }

    /**
     * 中央のビーコンのガラスを、その時代の色に塗り替える。塗った数を返す。
     * 時代が進んだ時に Enshutsu から呼ばれる。
     */
    public int chuoIroNuru(int jidai) {
        return basho.chuoIro(jidai);
    }

    /** サーバー停止時に呼ばれる。イベントの登録はサーバーが自動で外す。 */
    @Override
    public void onDisable() {
        // ★ 最初にエンダーかまどの中身を書き出す。
        //   これが無いと、止めた瞬間に全員の材料と焼き上がりが消える。
        if (kamado != null) {
            kamado.hozon();
        }
        // ★ 3秒で戻す約束の途中で止まると、銀行が消えたままになる。
        //   まわりの守りのせいで運営でも置き直せないので、ここで戻す。
        if (ginkoModosu != null) {
            for (java.util.Map.Entry<org.bukkit.Location, Material> e : ginkoModosu.entrySet()) {
                e.getKey().getBlock().setType(e.getValue());
            }
            if (!ginkoModosu.isEmpty()) {
                getLogger().info("壊れていた銀行を " + ginkoModosu.size() + " 個 戻しました");
            }
            ginkoModosu.clear();
        }
        getLogger().info("停止しました");
    }

    // =========================================================
    //  販売所の場所の管理
    // =========================================================

    /** 設置をやり直す上限。これを超えたら諦めて理由を出す */
    private static final int SETUP_JOUGEN = 3;

    /** そろっているべき施設の数。5拠点×6 + 中央2（プラント・販売所） */
    private static final int SETUP_KAZU = 37;

    /**
     * /jidai setup ── 拠点の設置を、最後まで面倒を見る。
     *
     * ★★ なぜコマンドにしたのか ★★
     *   手で打つと、3つのことを間違えます。実機で3つとも起きました。
     *     1. サーバーの黒い画面ではスラッシュを付けない
     *        （付けると "Unknown command" で何も起きない）
     *     2. 区画が読み込まれる前に打つと、【黙って途中までしか建たない】
     *        （32個のうち26個。エラーは出ない）
     *     3. 建てたあと /jidai scan を打ち忘れると、押しても何も起きない
     *   このコマンドが全部やります。打つのは1回だけでよい。
     *
     * やること:
     *   forceload → 5秒待つ → 建てる → 1秒待つ → 数を数える
     *   足りなければ建て直す（最大3回）→ そろったら登録 → 結果を出す
     *
     * ★ 知らせは sender へ返すので、コンソールからでも全部読めます。
     *   （データパックの tellraw @s はコンソールには届かない）
     *
     * @param sender 結果を知らせる相手。コンソールでもプレイヤーでもよい
     * @param kaime  何回目の試行か（1から始まる）
     */
    private void setupSusumeru(CommandSender sender, int kaime) {
        if (kaime == 1) {
            sender.sendMessage("[設置] 始めます。区画を読み込むので 5秒ほどかかります");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "function jidai:setup/forceload");
        } else {
            sender.sendMessage("[設置] " + kaime + "回目をやり直します");
        }

        // ★ forceload は「次の tick から読み込む」ので、待たないと間に合わない。
        //   100 tick = 5秒。runTaskLater は「N tick 後に1回だけ走らせる」。
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "function jidai:setup/kyoten");

            // 建てた結果を数えるのは、その次の tick で
            Bukkit.getScheduler().runTaskLater(this, () -> {
                int n = kane.sagyou("#shisetsu");
                if (n < SETUP_KAZU) {
                    sender.sendMessage("[設置] まだ " + n + " / " + SETUP_KAZU + " 個です");
                    if (kaime < SETUP_JOUGEN) {
                        setupSusumeru(sender, kaime + 1);
                    } else {
                        sender.sendMessage("[設置] " + SETUP_JOUGEN + "回やっても"
                                + " そろいませんでした (" + n + " / " + SETUP_KAZU + ")");
                        sender.sendMessage("  拠点の区画が読み込めていません。"
                                + "サーバーを再起動してから もう一度お試しください");
                        getLogger().warning("設置が " + n + " / " + SETUP_KAZU
                                + " 個で止まりました");
                    }
                    return;
                }

                sender.sendMessage("[設置] 施設が " + n + " / " + SETUP_KAZU + " 個 建ちました");

                // そろったので、プラグインに覚えさせる（これが無いと押しても無反応）
                Basho.Kekka k = basho.scan();
                if (k.karappo()) {
                    sender.sendMessage("[設置] ただし、プラグインは1件も見つけられませんでした");
                    sender.sendMessage("  読み込み済みマーカー " + k.marker()
                            + " 件 / forceload 区画 " + k.forceChunk() + " 件");
                    sender.sendMessage("  登録は消していません (" + basho.kensuu() + " 件)");
                    return;
                }
                basho.hozon(this);
                basho.chuoIro(kane.chuoJidai());
                basho.fuyuText();
                sender.sendMessage("[設置] 登録しました: 販売所 " + k.mise()
                        + " / 銀行 " + k.ginko() + " / ガチャ " + k.gacha()
                        + " / 進行 " + k.shinko() + " / かまど " + k.kamado()
                        + " / 目印 " + k.beacon());
                sender.sendMessage("[設置] 完了です。次は勢力へ入れてください"
                        + " (team join kyuryo <名前>)");
                getLogger().info("setup 完了: 施設 " + n + " 個 / 登録 "
                        + basho.kensuu() + " 件");
            }, 20L);
        }, 100L);
    }

    /**
     * 起動から少し経ってから走る自動の走査。
     * 見つかれば登録し直し、見つからなければ今の一覧をそのまま残す。
     */
    private void jidoScan() {
        // ★ データパックの確認もここで行う。onEnable では早すぎるため。
        datapackKakunin();

        Basho.Kekka k = basho.scan();
        if (k.karappo()) {
            getLogger().warning("自動走査: マーカーが1件も見つかりませんでした"
                    + " (読み込み済みマーカー " + k.marker()
                    + " / forceload 区画 " + k.forceChunk() + ")");
            getLogger().warning("  登録は消していません（" + basho.kensuu()
                    + " 件のまま）。拠点が読み込まれてから /jidai scan を打ってください");
            return;
        }
        basho.hozon(this);
        basho.chuoIro(kane.chuoJidai());
        int fuyu = basho.fuyuText();
        getLogger().info("浮遊テキストを " + fuyu + " 個置きました");
        getLogger().info("自動走査: 販売所 " + k.mise() + " / 売却所 " + k.uru()
                + " / 銀行 " + k.ginko() + " / ガチャ " + k.gacha()
                + " / 進行 " + k.shinko() + " / かまど " + k.kamado() + " / 目印 " + k.beacon()
                + " (マーカー " + k.marker() + " 件 / forceload 区画 " + k.forceChunk() + ")");
    }

    /**
     * /jidai add | remove | list を受け取る。
     *
     * ★ このメソッドがあるだけでは動かない。plugin.yml の commands 欄に
     *   jidai を書いて初めて、サーバーが /jidai を認識する。
     *   OP だけが使えるようにする指定も plugin.yml 側にある。
     *
     * 引数の意味:
     *   sender … コマンドを打った人（プレイヤーとは限らない。コンソールもある）
     *   args   … 空白で区切った後ろの言葉。"/jidai add" なら args[0] が "add"
     *
     * 戻り値 true = 正常に処理した / false = 使い方(usage)を表示させる
     */
    /**
     * コマンドの途中で Tab を押した時に、候補を出す。
     *
     * ★ これを書かないと、Bukkit が既定で「オンラインのプレイヤー名」を出す。
     *   /jidai add と打ちたいのに人の名前ばかり出るのはそのため。
     *
     * ★ 候補を出したくない場所では、**空の並びを返す**こと。
     *   null を返すと Bukkit が既定のプレイヤー名に戻してしまう。(実測で確認)
     *
     * 引数 args は「今まで打った言葉」。
     *   "/jidai a" なら args = ["a"]（打ちかけの言葉も入る）
     *   "/jidai beacon ky" なら args = ["beacon", "ky"]
     * つまり args.length が「今どこを打っているか」を表す。
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {

        String namae = command.getName();

        if (namae.equals("jidai")) {
            // 1語目 … 運営用の小コマンド
            if (args.length == 1) {
                return erabu(args[0], List.of("setup", "scan", "list", "add", "gacha",
                        "beacon", "chuo", "remove",
                        "leader", "sekiyu", "craft", "juki", "tsuruhashi", "ui", "game"));
            }
            if (args.length == 2 && args[0].equals("game")) {
                return erabu(args[1], List.of(Kanri.KOTOBA));
            }
            if (args.length == 3 && args[0].equals("game") && args[1].equals("tp")) {
                return erabu(args[2], List.of("kyoten"));
            }
            // 2語目 … 名前を取るものだけ候補を出す
            if (args.length == 2 && args[0].equals("beacon")) {
                return erabu(args[1], List.of(Seiryoku.zenTeam()));
            }
            if (args.length == 2 && args[0].equals("tsuruhashi")) {
                return erabu(args[1], zenin());
            }
            if (args.length == 2 && args[0].equals("sekiyu")) {
                return erabu(args[1], List.of("tomeru", "hajimeru", "osaeru"));
            }
            if (args.length == 2 && args[0].equals("ui")) {
                return erabu(args[1], List.of("on", "off"));
            }
            if (args.length == 2 && args[0].equals("craft")) {
                return erabu(args[1], List.of("kinshi", "kyoka"));
            }
            if (args.length == 3 && args[0].equals("sekiyu")
                    && args[1].equals("osaeru")) {
                // よく使う刻みだけ出す。手で数字を打っても構わない
                return erabu(args[2], List.of("100", "50", "25", "10"));
            }
            return List.of();
        }

        return List.of();
    }

    /**
     * 打ちかけの言葉で候補を絞る。
     * 大文字小文字は区別しない（Ronty でも ronty でも拾える）。
     */
    private List<String> erabu(String uchikake, List<String> kouho) {
        String sagasu = uchikake.toLowerCase(Locale.ROOT);
        List<String> kotae = new ArrayList<>();
        for (String k : kouho) {
            if (k.toLowerCase(Locale.ROOT).startsWith(sagasu)) {
                kotae.add(k);
            }
        }
        return kotae;
    }

    /** 今ログインしている全員の名前。 */
    private List<String> zenin() {
        List<String> namae = new ArrayList<>();
        for (Player hito : getServer().getOnlinePlayers()) {
            namae.add(hito.getName());
        }
        return namae;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // どのコマンドが打たれたかは command.getName() で分かる。
        String namae = command.getName();

        // 下剋上の宣言。リーダーだけが通る（判定は Seiryoku 側）
        if (namae.equals("gekokujo")) {
            if (!(sender instanceof Player p2)) {
                sender.sendMessage("[下剋上] ゲーム内から実行してください");
                return true;
            }
            String dame = seiryoku.gekokujoDekiruka(p2, kane);
            if (dame != null) {
                p2.sendMessage(Enshutsu.kazaru(Enshutsu.GEKOKUJO, "宣言できません: " + dame));
                Enshutsu.oto(p2, Enshutsu.OTO_DAME);
                return true;
            }
            // ★ ここから先はデータパックの担当。
            //   占領の解除・即時開戦・略奪上限の2倍・権利の消費・全体通知は
            //   すべて jidai:sensou/gekokujo_tsukau が行う。
            //   プラグインは「本人として」その関数を走らせるだけ。
            //
            // ★ 本人として走らせないと駄目。向こうは @s（＝実行者）を見て
            //   チーム・リーダーの目印を判定している。
            //   参加者に /function の権限は無いので、コンソールから
            //   execute as <名前> で本人にすり替えて走らせる。
            // 効いたかを後で確かめるため、呼ぶ前の状態を控えておく
            String jibun = kane.teamMei(p2);
            String aite = seiryoku.senryoShiteiru(jibun, kane);

            String cmd = "execute as " + p2.getName() + " at " + p2.getName()
                    + " run function jidai:sensou/gekokujo_tsukau";
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)) {
                p2.sendMessage(Enshutsu.kazaru(Enshutsu.GEKOKUJO,
                        "下剋上の処理を呼べませんでした（データパックが入っていますか）"));
                return true;
            }
            // ★ 呼べたことと、効いたことは別。データパックは条件に合わなければ
            //   静かに断る（例: jidai_leader の目印が付いていない）。
            //   占領が解けたかどうかで、効いたかを見分ける。
            //   ここで金は動かないので返すものは無いが、ログが嘘をつかないようにする。
            if (seiryoku.senryoShiteiru(jibun, kane) != null) {
                getLogger().warning("下剋上を呼んだが占領が解けていない: " + p2.getName()
                        + " (" + Seiryoku.mei(jibun) + ") ── データパック側で断られた可能性");
            } else {
                getLogger().info("下剋上: " + p2.getName() + " が行使 ("
                        + Seiryoku.mei(jibun) + " → " + Seiryoku.mei(aite) + ")");
            }
            // ★ 演出と全体通知はデータパックが出す
            return true;
        }

        // 代行の指名。リーダーだけが使える
        if (namae.equals("daikou")) {
            if (!(sender instanceof Player p3)) {
                sender.sendMessage("[勢力] ゲーム内から実行してください");
                return true;
            }
            String team = kane.teamMei(p3);
            if (team == null || !Seiryoku.seiryokuKa(team)) {
                p3.sendMessage("[勢力] 勢力に入っていません");
                return true;
            }
            if (!p3.getName().equals(seiryoku.leaderMei(team))) {
                p3.sendMessage("[勢力] リーダーだけが代行を指名できます");
                return true;
            }
            if (args.length < 1) {
                p3.sendMessage("[勢力] 使い方: /daikou <プレイヤー名>");
                return true;
            }
            seiryoku.daikouKimeru(team, args[0]);
            p3.sendMessage("[勢力] " + Seiryoku.mei(team) + " の代行を " + args[0] + " にしました");
            return true;
        }

        // 他勢力の情報。遺物「スマートフォン」を持つ勢力だけ中身が見える。全員が打てる
        if (namae.equals("joho")) {
            if (!(sender instanceof Player p6)) {
                sender.sendMessage("[情報] ゲーム内から実行してください");
                return true;
            }
            Ibutsu.joho(sender, p6, kane);
            return true;
        }

        return jidaiCommand(sender, args);
    }

    /**
     * /jidai ... の中身。運営用のコマンドをまとめてある。
     *
     * 戻り値 true = 正常に処理した / false = 使い方(usage)を表示させる
     */
    private boolean jidaiCommand(CommandSender sender, String[] args) {

        // 言葉が足りなければ、plugin.yml に書いた使い方を出させる
        if (args.length == 0) {
            return false;
        }

        // 設置を最後まで面倒を見る。コンソールからでも使える
        if (args[0].equals("setup")) {
            setupSusumeru(sender, 1);
            return true;
        }

        // list はコンソールからでも使える（足元が要らないため）
        if (args[0].equals("list")) {
            sender.sendMessage("[登録] 合計 " + basho.kensuu() + " 件");
            for (String shu : Basho.zenShurui()) {
                for (String k : basho.ichiran(shu)) {
                    sender.sendMessage("  " + Basho.shuruiMei(shu) + " " + k);
                }
            }
            int zure = basho.zureNoKazu();
            if (zure > 0) {
                sender.sendMessage("  ※ ブロックが見当たらない登録が " + zure + " 件あります");
            }
            return true;
        }

        // 無くした人にツルハシを渡し直す
        //   jidai tsuruhashi <名前>
        if (args[0].equals("tsuruhashi")) {
            if (args.length < 2) {
                sender.sendMessage("[ツルハシ] 使い方: jidai tsuruhashi <名前>");
                sender.sendMessage("       配った人数: " + tsuruhashi.watashitaKazu() + " 人");
                return true;
            }
            Player aite = Bukkit.getPlayerExact(args[1]);
            if (aite == null) {
                sender.sendMessage("[ツルハシ] " + args[1] + " は今サーバーに居ません");
                return true;
            }
            if (tsuruhashi.uneiWatasu(aite)) {
                sender.sendMessage("[ツルハシ] " + aite.getName() + " に渡しました");
            } else {
                sender.sendMessage("[ツルハシ] " + aite.getName() + " は既に持っています");
            }
            return true;
        }

        // リソースパックの絵をタイトルに使うかの切り替え
        //   jidai ui        … 今の状態
        //   jidai ui on     … 絵を使う（パックを配り終えてから）
        //   jidai ui off    … ふつうの文字に戻す
        if (args[0].equals("ui")) {
            return uiCommand(sender, args);
        }

        // 石油の栓。コンソールからでも使える
        //   jidai sekiyu            … 今の状態
        //   jidai sekiyu tomeru     … 止める
        //   jidai sekiyu hajimeru   … 再開する
        //   jidai sekiyu osaeru 50  … 半分の速さにする (百分率・1〜100)
        if (args[0].equals("sekiyu")) {
            return sekiyuCommand(sender, args);
        }

        // クラフト禁止の入り切り。コンソールからでも使える
        //   jidai craft         … 今の状態
        //   jidai craft kinshi  … 禁止する (既定)
        //   jidai craft kyoka   … 許可する
        if (args[0].equals("craft")) {
            return craftCommand(sender, args);
        }

        // ワールドのマーカーを走査して登録し直す。コンソールからでも使える
        if (args[0].equals("scan")) {
            Basho.Kekka k = basho.scan();
            if (k.karappo()) {
                sender.sendMessage("[登録] マーカーが1件も見つかりませんでした");
                sender.sendMessage("  読み込み済みマーカー " + k.marker()
                        + " 件 / forceload 区画 " + k.forceChunk() + " 件");
                sender.sendMessage("  今の登録は消していません (" + basho.kensuu() + " 件)");
                sender.sendMessage("  拠点が未設置なら /function jidai:setup/kyoten を先に");
                return true;
            }
            basho.hozon(this);
            basho.chuoIro(kane.chuoJidai());
            int fuyu = basho.fuyuText();
            sender.sendMessage("[登録] 浮遊テキストを " + fuyu + " 個置きました");
            sender.sendMessage("[登録] 走査しました: 販売所 " + k.mise()
                    + " / 売却所 " + k.uru() + " / 銀行 " + k.ginko()
                    + " / ガチャ " + k.gacha() + " / 進行 " + k.shinko() + " / かまど " + k.kamado() + " / 目印 " + k.beacon());
            sender.sendMessage("  読み取ったマーカー " + k.marker()
                    + " 件 / forceload 区画 " + k.forceChunk() + " 件");
            return true;
        }

        // リーダーの登録。運営が使う。コンソールからでも可
        //   /jidai leader set <勢力> <プレイヤー名>
        //   /jidai leader          … 今のリーダーの一覧
        if (args[0].equals("leader")) {
            if (args.length == 1) {
                for (String t : Seiryoku.zenTeam()) {
                    String l = seiryoku.leaderMei(t);
                    String d = seiryoku.daikouMei(t);
                    sender.sendMessage("  " + Seiryoku.mei(t) + " リーダー="
                            + (l == null ? "未設定" : l)
                            + " 代行=" + (d == null ? "未設定" : d)
                            + " 下剋上の権利=" + (seiryoku.kenriAru(t, kane) ? "あり" : "なし"));
                }
                return true;
            }
            if (args.length < 4 || !args[1].equals("set")) {
                sender.sendMessage("[勢力] 使い方: /jidai leader set <勢力> <プレイヤー名>");
                sender.sendMessage("  勢力: " + String.join(" / ", Seiryoku.zenTeam()));
                return true;
            }
            if (!Seiryoku.seiryokuKa(args[2])) {
                sender.sendMessage("[勢力] そんな勢力はありません: " + args[2]);
                sender.sendMessage("  勢力: " + String.join(" / ", Seiryoku.zenTeam()));
                return true;
            }
            seiryoku.leaderKimeru(args[2], args[3]);
            sender.sendMessage("[勢力] " + Seiryoku.mei(args[2])
                    + " のリーダーを " + args[3] + " にしました");
            return true;
        }

        // 運営のゲーム操作。★ コマンドブロックからも使える（送り手を問わない）
        //   jidai game start | stop | reset [zenbu] | tp kyoten | risu | spawn ...
        if (args[0].equals("game")) {
            return kanri.command(sender, args);
        }

        // add と remove は「足元のブロック」を使うので、プレイヤー限定。
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[販売所] add と remove はゲーム内から実行してください");
            return true;
        }

        // 足元のブロック = 立っている位置から1つ下。
        // subtract(0, 1, 0) で「Y を1つ下げた場所」を指している。
        Block ashimoto = player.getLocation().subtract(0, 1, 0).getBlock();

        // 手で1件足す。走査で拾えない例外的な場所のため残してある
        if (args[0].equals("add")) {
            return tesaki(player, ashimoto, Basho.MISE, Material.EMERALD_BLOCK);
        }
        // ★ 足元のエメラルドを「銃器専門店」として登録する。
        //   /jidai add は販売所として登録するので、店を取り違えた時の直し方は
        //     /jidai remove  →  /jidai juki
        if (args[0].equals("juki")) {
            return tesaki(player, ashimoto, Basho.JUKI, Material.EMERALD_BLOCK);
        }

        if (args[0].equals("gacha")) {
            return tesaki(player, ashimoto, Basho.GACHA, Material.DIAMOND_BLOCK);
        }

        // 拠点の目印。足元がビーコンであること
        // 足元のビーコンを、ある勢力の拠点の目印として登録する。
        //   jidai beacon kyuryo
        // ★★ 勢力名が要る ★★
        //   勝利条件その1（戦争勝利）で「誰の拠点か」を見るため。
        //   勢力を書かないと、壊せる条件を判定できない。
        if (args[0].equals("beacon")) {
            if (args.length < 2) {
                player.sendMessage("[目印] 使い方: jidai beacon <チーム名>");
                player.sendMessage("       kyuryo / shinrin / kawa / naikai / iwaba");
                return true;
            }
            if (!Seiryoku.seiryokuKa(args[1])) {
                player.sendMessage("[目印] 知らない勢力です: " + args[1]);
                return true;
            }
            boolean ok = tesaki(player, ashimoto, Basho.BEACON, Material.BEACON);
            basho.beaconKuniTouroku(Basho.kagi(ashimoto), args[1]);
            basho.hozon(this);
            player.sendMessage("[目印] " + Seiryoku.mei(args[1]) + " の拠点として覚えました");
            return ok;
        }

        // 中央の目印。登録したらその場で今の時代の色に塗る
        if (args[0].equals("chuo")) {
            boolean kekka = tesaki(player, ashimoto, Basho.BEACON_CHUO, Material.BEACON);
            int nutta = basho.chuoIro(kane.chuoJidai());
            if (nutta > 0) {
                player.sendMessage("[中央の目印] ガラスを "
                        + Kane.jidaiMei(kane.chuoJidai()) + " の色にしました");
            }
            return kekka;
        }

        // remove はどの種別でも外せる
        if (args[0].equals("remove")) {
            String k = Basho.kagi(ashimoto);
            if (!basho.sakujo(k)) {
                player.sendMessage("[登録] 足元は登録されていません");
                return true;
            }
            basho.hozon(this);
            player.sendMessage("[登録] 解除しました (残り " + basho.kensuu() + " 件)");
            return true;
        }

        return false;
    }

    /**
     * リソースパックの絵をタイトルに使うかを切り替える。
     *
     * ★★ 参加者がパックを入れ終わってから on にすること ★★
     *   パックが無い人の画面では、絵の文字が「□□」に化けます。
     *   逆にパックだけ入れて off のままなら、ふつうの文字が出るだけで
     *   困りません。だから既定は off にしてあります。
     *
     * ★ 保存はしません。再起動すると off に戻ります。
     *   本番で常に使うようになったら、Enshutsu.uiPack の
     *   既定値そのものを true に変えてください。
     */
    private boolean uiCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage("[UI] 今は "
                    + (Enshutsu.uiPackKa() ? "【絵】" : "【ふつうの文字】") + " です");
            sender.sendMessage("     使い方: jidai ui [on|off]");
            return true;
        }
        boolean tsukau = args[1].equals("on");
        if (!tsukau && !args[1].equals("off")) {
            sender.sendMessage("[UI] 使い方: jidai ui [on|off]");
            return true;
        }
        Enshutsu.uiPackSet(tsukau);
        // ★ 販売所と銃器専門店は画面を使い回しているので、作り直す。
        shop.namaeKousin();
        juki.namaeKousin();
        sender.sendMessage("[UI] " + (tsukau ? "絵に切り替えました"
                : "ふつうの文字に戻しました"));
        if (tsukau) {
            sender.sendMessage("     ★ リソースパックを入れていない人には「□」に見えます");
        }
        return true;
    }

    /**
     * 石油の湧きを、運営が手元で止めたり抑えたりする。
     *
     * ★★ なぜ要るか ★★
     *   説明の時間・トラブル対応・仕切り直しの間、中央に石油が溜まり続けると、
     *   再開した瞬間に拾える量が偏り、先に着いた勢力が独り勝ちする。
     *   「止める」で溜まるのを完全に止め、「抑える」で流量だけ絞る。
     *
     * ★ 実体はデータパックの settei に置いた2つの値。
     *   プラグインは値を書くだけで、湧かせる処理そのものは触らない。
     */
    private boolean sekiyuCommand(CommandSender sender, String[] args) {
        if (!kane.sekiyuSenJunbiOK()) {
            sender.sendMessage("[石油] データパックが読み込まれていません");
            return true;
        }

        // 引数なし … 今どうなっているかを出すだけ
        if (args.length == 1) {
            sekiyuJoutai(sender);
            return true;
        }

        if (args[1].equals("tomeru")) {
            kane.sekiyuTomeru(true);
            sender.sendMessage("[石油] 止めました。中央プラントは1本も出しません");
            Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "中央プラントが停止しました"));
            return true;
        }

        if (args[1].equals("hajimeru")) {
            kane.sekiyuTomeru(false);
            sender.sendMessage("[石油] 再開しました");
            Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "中央プラントが再開しました"));
            return true;
        }

        if (args[1].equals("osaeru")) {
            if (args.length < 3) {
                sender.sendMessage("[石油] 使い方: jidai sekiyu osaeru <1〜100>");
                sender.sendMessage("       100 が通常の速さ。50 で半分、10 で十分の一");
                return true;
            }
            int n;
            try {
                n = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                // ★ 数字でない物を入れられた時に落ちないようにする。
                sender.sendMessage("[石油] 数字で指定してください (1〜100)");
                return true;
            }
            kane.sekiyuBairitsuSet(n);
            sekiyuJoutai(sender);
            return true;
        }

        sender.sendMessage("[石油] 使い方: jidai sekiyu [tomeru|hajimeru|osaeru <1〜100>]");
        return true;
    }

    /** 石油の栓の今の状態を1行で出す。 */
    private void sekiyuJoutai(CommandSender sender) {
        if (kane.sekiyuTomatteruKa()) {
            sender.sendMessage("[石油] 今は【停止中】です (jidai sekiyu hajimeru で再開)");
            return;
        }
        int b = kane.sekiyuBairitsu();
        sender.sendMessage("[石油] 稼働中 / 速さ " + b + "%"
                + (b == 100 ? " (通常)" : " (通常の " + b + "%。間隔が " + (100 / b) + "倍)"));
    }

    /**
     * クラフト禁止の入り切り。
     *
     * ★ 既定は【禁止】。企画の前後で運営が物を組みたい時だけ外す想定で、
     *   遊んでいる間に外すことは想定していない。
     *   外している間はサーバー全員に見えるよう、あえて全体へ流す。
     */
    private boolean craftCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage("[クラフト] 今は "
                    + (craftKinshi ? "【建材だけ】" : "【何でも作れる】") + " です");
            return true;
        }
        if (args[1].equals("kinshi")) {
            craftKinshi = true;
            sender.sendMessage("[クラフト] 禁止しました");
            Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "クラフトは建材だけに戻りました"));
            return true;
        }
        if (args[1].equals("kyoka")) {
            craftKinshi = false;
            sender.sendMessage("[クラフト] 許可しました。★企画中は禁止に戻してください");
            Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "★運営がクラフトを一時的に許可しました"));
            return true;
        }
        sender.sendMessage("[クラフト] 使い方: jidai craft [kinshi|kyoka]");
        return true;
    }

    /**
     * 足元のブロックを手で1件登録する。/jidai add と /jidai gacha の共通部分。
     */
    private boolean tesaki(Player player, Block ashimoto, String shurui, Material hitsuyou) {
        String mei = Basho.shuruiMei(shurui);
        if (ashimoto.getType() != hitsuyou) {
            player.sendMessage("[" + mei + "] 足元が" + Basho.blockMei(shurui)
                    + "ではありません (今は " + ashimoto.getType() + ")");
            return true;
        }
        if (!basho.tsuika(shurui, Basho.kagi(ashimoto))) {
            player.sendMessage("[" + mei + "] そこは既に登録されています");
            return true;
        }
        basho.hozon(this);
        player.sendMessage("[" + mei + "] 登録しました: " + Basho.kagi(ashimoto)
                + " (合計 " + basho.kensuu() + " 件)");
        return true;
    }

    // =========================================================
    //  イベントの受け口
    // =========================================================

    /**
     * プレイヤーが何かを右クリック/左クリックするたびに、サーバーが呼ぶ。
     *
     * @EventHandler
     *   「この処理をイベントの受け口として使え」という印。
     *   メソッド名は自由。サーバーが見ているのは印と引数の型だけ。
     *
     * ★ ここが購買システムをプラグインにした理由そのもの。
     *   event.getPlayer() は「このクリックを起こした本人」を直接返す。
     *   データパックのように「近くのプレイヤー」から推定していないので、
     *   50人が同時にクリックしても取り違えが原理的に起きない。
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {

        // --- 門番を並べて、関係ない場合は早めに帰る ------------------

        // (1) 右クリックでブロックを叩いた時だけ扱う。
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // (2) このイベントは、右手と左手で**2回**飛んでくる。右手の分だけ扱う。
        //     ★ これが無いと画面が二重に開き、購入も2回走る。
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // (3) 叩いたブロックを取り出す。null = 「中身が無い」という特別な値。
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        // ★ 石切台・鍛冶台などは開かせない。作業台だけは使える。
        //   村の中などに元から置いてある物にも効く。
        if (craftKinshi && TSUKURU_DAI.contains(block.getType())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "これは使えません（作れるのは建材だけ。作業台を使ってください）"));
            return;
        }

        // (4) 扱うのは2種類だけ。
        //       エメラルドブロック … 販売所
        //       ダイヤブロック     … ガチャ
        //     ★ ブロックの種類と登録の一覧は必ず対にする。
        //       「登録さえされていれば何でも反応する」にはしない。
        // ★★ ビーコンは、登録の有無にかかわらず必ず打ち切る ★★
        //   ビーコンの画面が開くと効果を選べてしまう。レベル1でも
        //   範囲20の採掘速度上昇が付き、「1分に20個」の経済が崩れる。
        //   目印のビーコンだけでなく、**誰が置いたビーコンでも**塞ぐ。
        //   （効果が無くてもビーム自体は出るので、目印としては困らない）
        if (block.getType() == Material.BEACON) {
            event.setCancelled(true);
            return;
        }

        String k = Basho.kagi(block);
        String shu = basho.shurui(k);
        if (shu == null) {
            return;   // 登録されていない場所。何もしない
        }
        Player player = event.getPlayer();

        // ★ エンダーかまど。1人1枚の専用の画面を開く。
        //   ★ 打ち切ること。打ち切らないと本物のかまど（共有）も開いてしまう。
        if (shu.equals(Basho.KAMADO)) {
            event.setCancelled(true);
            kamado.osareta(player, block, kane);
            return;
        }

        // ★ 時代を進める鉄ブロック。押せるのはリーダーと代行だけ。
        if (shu.equals(Basho.SHINKO)) {
            event.setCancelled(true);
            shinko.osareta(player, kane);
            return;
        }

        // ★★ 銀行(金ブロック) ★★
        //   預金も売却も、1個の金ブロックの画面で完結する。
        //   中身はプラグインが持っている(Ginko.java)。
        //
        // ★ もとはデータパックのアドバンスメントが拾う作りだったが、
        //   実機で一度も発火しなかった(登録はされているのに、素手で
        //   普通のブロックを押しても default_block_use が起きない)。
        //   ガチャ・販売所と同じく、クリックはプラグインが拾う。
        //
        // ★ 他勢力の拠点を押した時の【略奪】だけはデータパックのまま。
        //   その振り分けは Ginko の中でやっている。
        if (shu.equals(Basho.GINKO)) {
            event.setCancelled(true);
            ginko.osareta(player, block, kane, seiryoku);
            return;
        }

        // ★ 拠点のエメラルド(旧・売却所)は、銀行の画面へ統合した。
        //   古い拠点に残っているブロックを押しても迷わないよう、
        //   案内だけ出す。データパックが新しく置くことはもう無い。
        if (shu.equals(Basho.URU)) {
            event.setCancelled(true);
            player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "売却は銀行(金ブロック)に移りました。金ブロックを押してください"));
            return;
        }

        // ブロックの種類と種別が食い違っていたら何もしない。
        // (マーカーだけ残ってブロックが別物になっている場合)
        Material shurui = block.getType();

        if (shu.equals(Basho.MISE) && shurui == Material.EMERALD_BLOCK) {
            // イベントを打ち切る。手に持っている物を置いてしまうのを防ぐ。
            event.setCancelled(true);
            // 開く直前に、その時の中央の時代に合わせて棚を並べ直してから開く
            shop.hiraku(player, kane);
            getLogger().info("販売所を開いた: " + player.getName() + " at " + k);
            return;
        }

        // ★★ 銃器専門店。販売所と同じエメラルドブロック ★★
        //   ここが分かれるのは【登録された種別】だけ。ブロックは同じ。
        //   登録を取り違えると「押したら違う店が開く」ので、
        //   /jidai list で種別ごとの件数を必ず確かめること。
        if (shu.equals(Basho.JUKI) && shurui == Material.EMERALD_BLOCK) {
            event.setCancelled(true);
            juki.hiraku(player, kane);
            getLogger().info("銃器専門店を開いた: " + player.getName() + " at " + k);
            return;
        }

        if (shu.equals(Basho.GACHA) && shurui == Material.DIAMOND_BLOCK) {
            event.setCancelled(true);
            gacha.hiraku(player, kane);
            getLogger().info("ガチャを開いた: " + player.getName() + " at " + k);
        }
    }

    /**
     * 画面を閉じた時に、サーバーが呼ぶ。
     * ガチャは1人ずつ画面を作るので、閉じたら忘れる。
     * ★ 忘れないと、入ったことのある人の分だけ記録が増え続ける。
     */
    /**
     * 入り直した人に、リーダーの目印を付け直す。
     *
     * ★ 目印(tag)はプレイヤーに保存され、切断しても消えない。
     *   だから「離れている間に外された元リーダー」が目印を持ったままになる。
     *   入った時に必ず見直すことで、その穴をふさぐ。
     */
    /**
     * クラフトを全面的に禁止する。
     *
     * ★★ この企画では物を「作る」ことができない ★★
     *   道具も食べ物も建材も、すべて販売所・ガチャ・売却で手に入れる。
     *   自分で作れてしまうと経済（金と石油）を通さずに済んでしまい、
     *   勢力に所属する意味も、時代を進める意味も薄れる。
     *
     * ★ ふさぐ場所は3つ。1つでも空いていると抜け道になる。
     *   (1) 作業台の 3x3   (2) 持ち物の中の 2x2   (3) 作業台を開くこと
     *   (1)(2) はどちらも下の2つのイベントを通る。(3) は右クリックで止めている。
     */
    /**
     * 作ろうとしている物を見て、建材でなければ出来上がりを消す。
     *
     * ★ 消すのであって断るのではない。作れる物だけが並びに現れるので、
     *   「作れそうに見えるのに取れない」という分かりにくさが起きない。
     */
    /**
     * 人を傷つけようとした。★ 同じ勢力の仲間なら止める（2026-08-24 のご指示）。
     *
     * ★ 判定は「同じ【勢力】か」なので、
     *   勢力に入っていない人は素通りする。
     * ★ 矢や弾は、撃った人まで辿ってから見る（Tatakai.utta）。
     * ★ TaCZ の弾がこのイベントに来るかは実機でしか分からない。
     *   来なくても、データパックの friendlyFire=false が保険になる。
     */
    @EventHandler(ignoreCancelled = true)
    public void onNakamaUchi(EntityDamageByEntityEvent event) {
        if (tatakai == null || !kane.junbiOK()) {
            return;
        }
        if (tatakai.fusegu(event.getDamager(), event.getEntity(), kane)) {
            event.setCancelled(true);
        }
    }

    /**
     * 人が死んだ。★ 殺したのが人なら、相手の【個人の金】の10%をもらう。
     *   勢力の貯金からは1円も取らない（ご指示）。
     */
    @EventHandler
    public void onShinda(PlayerDeathEvent event) {
        if (tatakai == null) {
            return;
        }
        tatakai.koroshita(event.getEntity(), event.getEntity().getKiller(), kane);
    }

    @EventHandler
    public void onCraftJunbi(PrepareItemCraftEvent event) {
        if (!craftKinshi) {
            return;
        }
        ItemStack dekiagari = event.getInventory().getResult();
        if (dekiagari != null && tsukutteYoi(dekiagari.getType())) {
            return;
        }
        event.getInventory().setResult(null);
    }

    /**
     * 実際に取り出す操作。上で消しそこねた分をここで止める。
     *
     * ★★ 2つ要る ★★
     *   上は「見た目」を作らせない。こちらは「実行」を通さない。
     *   MOD が独自に出来上がりを差し込む場合、上を素通りすることがある。
     */
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!craftKinshi) {
            return;
        }
        ItemStack dekiagari = event.getInventory().getResult();
        if (dekiagari != null && tsukutteYoi(dekiagari.getType())) {
            return;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player p) {
            p.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "作れるのは建材だけです（ハーフブロック・石レンガ・磨かれた系・柵）"));
        }
    }

    /**
     * 物を作る画面そのものを開かせない。
     *
     * ★ 右クリックの遮断(TSUKURU_DAI)と二重に見えるが、役割が違う。
     *   右クリック … 置いてある台を押した時
     *   こちら     … それ以外の開き方（コマンド・MOD・乗り物の中など）
     *   どちらか片方だと必ず抜け道が残る。
     */
    @EventHandler
    public void onInvOpen(InventoryOpenEvent event) {
        if (!craftKinshi) {
            return;
        }
        // ★ WORKBENCH（作業台の3x3）はここから外した。
        //   建材を作るのに要るため。中身は tsukutteYoi が絞っている。
        InventoryType t = event.getInventory().getType();
        if (t == InventoryType.STONECUTTER
                || t == InventoryType.SMITHING || t == InventoryType.ANVIL
                || t == InventoryType.LOOM || t == InventoryType.CARTOGRAPHY
                || t == InventoryType.GRINDSTONE) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "これは使えません（作れるのは建材だけ）"));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        seiryoku.mejirushi(event.getPlayer());

        // ★ 十連の途中で切断した人に、渡し損ねた結果を渡す
        gacha.sanka(event.getPlayer());

        // ★ 初めて入った人に、時代のツルハシを1本 配る。
        //   運営が何か打つ必要はない。2回目からは配らない。
        if (tsuruhashi.hajimete(event.getPlayer())) {
            getLogger().info("時代のツルハシを配った: " + event.getPlayer().getName());
        }
    }

    /**
     * 死んだ時。★ 時代のツルハシだけは、その場に落とさない。
     *
     * ★★ 落とすと拾われて「掘れない人」が生まれる ★★
     *   クラフトが禁止で、店にもツルハシは無いので、
     *   一度取られると二度と掘れなくなる。そこだけは守る。
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        int nuita = tsuruhashi.shinda(event.getDrops());
        // ★ 特殊アイテムも落とさない（2026-08-24 のご指示）。預かって生き返った時に返す。
        nuita += Shouri.shinda(event.getEntity().getName(), event.getDrops());
        if (nuita > 0) {
            getLogger().info("ツルハシを落とさせなかった: "
                    + event.getEntity().getName() + " (" + nuita + "本)");
        }
    }

    /**
     * 生き返った時。手元にツルハシが無ければ戻す。
     * ★ 既に持っていれば何もしない（keepInventory の時に増えないように）。
     */
    /** 遺物「テンプル騎士団の盾」: 勢力の人が受けるダメージを減らす。 */
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        Ibutsu.damageUketa(event, kane);
    }

    /** 遺物「ペニシリン」: 勢力の人の自然回復を増やす。 */
    @EventHandler
    public void onRegain(EntityRegainHealthEvent event) {
        Ibutsu.kaifuku(event, kane);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        tsuruhashi.ikikaetta(event.getPlayer());
        // ★ 勢力のリスポーンを拠点にしてある時は、そこへ（Kanri が判定する）
        kanri.ikikaeru(event);
        // ★ 預かっていた特殊アイテムを返す（2026-08-24）
        Shouri.ikikaetta(event.getPlayer());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Gacha) {
            // ★ 入れ物ごと渡す。十連の結果画面(27枠)を途中で閉じた人には
            //   その場で景品を渡す必要があるため（Gacha.tojita）。
            gacha.tojita(event.getPlayer().getName(), event.getInventory());
        }
        if (event.getInventory().getHolder() instanceof Sensen) {
            sensen.tojita(event.getPlayer().getName());
        }
        if (event.getInventory().getHolder() instanceof Ginko) {
            // ★ 入れ物ごと渡す。開き直した時の「閉じた」知らせで
            //   自分を忘れないようにするため（2026-08-24）。
            ginko.tojita(event.getPlayer().getName(), event.getInventory());
        }
        if (event.getInventory().getHolder() instanceof Shinko) {
            shinko.tojita(event.getPlayer().getName());
        }
    }

    /**
     * ブロックが壊された時に、サーバーが呼ぶ。
     * 登録済みの販売所なら壊させない。
     *
     * ★ 販売所が壊れると、登録だけが残って「反応しない座標」になる。
     *   運営にも参加者にも原因が分からない状態になるので、先に防ぐ。
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // ビーコンは本体だけでなく、上のガラスと下の鉄ブロック3×3も守る。
        // 土台を1つ抜かれるとビームが消えてしまうため。
        if (basho.beaconNoIchibu(event.getBlock())) {
            if (beaconKowaseruKa(event.getPlayer(), event.getBlock())) {
                return;      // 条件を満たしたので壊させる（中で植民地化まで済む）
            }
            event.setCancelled(true);
            return;
        }

        // --- 拠点の銀行のまわりは掘らせない（2026-09-18 のご指示）-------
        //
        // ★★ なぜ要るか ★★
        //   銀行そのものは施設として守っているが、**まわりは素の地面**。
        //   足元を掘って落とす・覆って押せなくする、が通ってしまう。
        //
        // ★ 施設そのもの（shu != null）はここでは弾かない。
        //   銀行を壊す＝略奪、という【戦争の入口】を塞いでしまうため。
        // ★★ ネザライトの抽選より【前】に置くこと ★★
        //   後ろに置くと、壊せないのにドロップだけ出て【無限に湧く】。
        String shu = basho.shurui(Basho.kagi(event.getBlock()));
        // ★ 運営（op）は素通り。守りのせいで直せなくなる事故を避ける逃げ道。
        if (shu == null && !event.getPlayer().isOp()
                && basho.ginkoNoMawari(event.getBlock(), GINKO_MAMORU)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("[銀行] 銀行のまわり "
                    + GINKO_MAMORU + " マスは掘れません");
            return;
        }

        // --- 石を掘った時のネザライト -----------------------------
        // ★ 0.1% で1個 出る。銀行で 30 で売れる。
        //   落とす形にしているのは、持ち物がいっぱいでも消えないため。
        Material horeta = event.getBlock().getType();
        int nezaWari = (horeta == Material.DEEPSLATE) ? NEZA_KAKURITSU_SHINSO : NEZA_KAKURITSU;
        if (NEZA_ISHI.contains(horeta)
                && ishiRan().nextInt(nezaWari) == 0) {
            event.getBlock().getWorld().dropItemNaturally(
                    event.getBlock().getLocation().add(0.5, 0.5, 0.5),
                    new ItemStack(Material.NETHERITE_INGOT, 1));
            event.getPlayer().sendMessage(Enshutsu.kazaru(Enshutsu.ATARI,
                    "石の中からネザライトインゴットが出た（銀行で 30）"));
            Enshutsu.oto(event.getPlayer(), Enshutsu.OTO_DAI);
        }

        // --- 遺物「ロンバルディアの鉄王冠」: 貴金属のおまけの抽選 ------
        //   データパックの本来の抽選に加えて、20% でもう1回。期待値が 1.2倍になる。
        if (NEZA_ISHI.contains(horeta)) {
            for (Material omake : Ibutsu.horiOmake(kane.kinkoMei(event.getPlayer()),
                    horeta == Material.DEEPSLATE, ishiRan())) {
                event.getBlock().getWorld().dropItemNaturally(
                        event.getBlock().getLocation().add(0.5, 0.5, 0.5), new ItemStack(omake, 1));
            }
        }

        if (shu == null) {
            return;
        }

        // --- 他の勢力の銀行を壊した = 略奪（2026-09-09 のご指示）-------
        //
        // ★★ ブロックは壊させない ★★
        //   金ブロックが本当に消えると、その拠点は二度と略奪できなくなり、
        //   運営にも参加者にも原因が分からない状態になる。
        //   壊す動作そのものを1回と数え、演出だけ「壊れた」ように見せる。
        //   Effect.STEP_SOUND は壊れた時の粒と音を両方 出す
        //   （paper-api-1.20.1.jar を開いて実在を確認した）。
        // ★ ginko の null 確認は、検証ハーネス（Unsafe で作るのでフィールドが入らない）
        //   のため。onShinda の tatakai と同じ書き方にそろえてある。
        if (shu.equals(Basho.GINKO) && ginko != null
                && ginko.kowasareta(event.getPlayer(), event.getBlock(), kane, seiryoku)) {
            // ★★ 2026-09-18 のご指示: 壊させて、3秒で戻す ★★
            //   前は壊させずに演出だけ出していた。実際に壊れて戻る方が伝わる。
            // ★ 落とし物は出さない。出すと金ブロックが増えてしまう。
            // ★ 戻るまでの間に埋められないのは、まわり3マスの守りが
            //   銀行そのものにも効いているため（置く方は座標が一致すれば止まる）。
            event.setDropItems(false);
            ginkoModosuYoyaku(event.getBlock());
            return;
        }
        // ★ 販売所・売却所・銀行・ガチャの4種すべてを守る。
        //   データパックが処理する施設でも、壊されたら動かなくなるのは同じ。
        event.setCancelled(true);
        event.getPlayer().sendMessage("[" + Basho.shuruiMei(shu) + "] ここは施設です。壊せません");
    }

    /**
     * 壊された銀行を、GINKO_MODORU tick 後に元へ戻す約束をする。
     *
     * ★ 戻すまでの間にサーバーが止まったら戻らない。
     *   そのまま消えると、**まわりの守りのせいで運営でも置き直せない**ので、
     *   onDisable でまとめて戻している。
     */
    private void ginkoModosuYoyaku(Block block) {
        final org.bukkit.Location basho2 = block.getLocation();
        final Material moto = block.getType();
        modosuMachi().put(basho2, moto);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            modosuMachi().remove(basho2);
            basho2.getBlock().setType(moto);
        }, GINKO_MODORU);
    }

    /** ★ Unsafe で作ると初期化子を通らないので、使う時に作る。 */
    private java.util.Map<org.bukkit.Location, Material> modosuMachi() {
        if (ginkoModosu == null) {
            ginkoModosu = new java.util.HashMap<>();
        }
        return ginkoModosu;
    }

    /**
     * ブロックが置かれた時に、サーバーが呼ぶ。
     *
     * ★★ 2026-09-18 のご指示: 銀行のまわりは覆わせない ★★
     *   壊す方だけ止めても、箱やブロックで囲めば銀行は押せなくなる。
     *   置く方も同じ範囲で止める。
     *
     * ★ basho の null 確認は検証ハーネス（Unsafe で作るのでフィールドが入らない）
     *   のため。onShinda の tatakai と同じ書き方にそろえてある。
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (basho == null) {
            return;
        }
        if (!event.getPlayer().isOp()
                && basho.ginkoNoMawari(event.getBlock(), GINKO_MAMORU)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("[銀行] 銀行のまわり "
                    + GINKO_MAMORU + " マスにはブロックを置けません");
        }
    }

    /**
     * ビーコンを壊してよいかを見て、よければ植民地化まで済ませる。
     * 壊してよければ true（呼び出し元はイベントを通す）。
     *
     * ★★ 勝利条件その1「戦争勝利」の入口 ★★
     *   壊せるのは、次の3つが【同時に】そろった時だけ。
     *     1. 壊す人が勢力に入っている
     *     2. その勢力が、ビーコンの持ち主と【交戦中】(sensou=2)
     *     3. 攻める側が、相手の銀行を 占領_必要回数（100）回 壊した
     *        ★ 2026-09-09 に「持ち主の貯金が 100 以下」から替えた（ご指示）。
     *          1回1%の略奪では貯金は 0 にならない（100回でも約37%残る）ので、
     *          貯金を条件にすると永久に落とせなくなる。
     *   1つでも欠けたら、理由を出して断る。黙って壊せないのが一番困る。
     *
     * ★ 壊した後: 植民地の印・勝利判定はデータパック（jidai:sensou/shokuminchi）。
     *   プラグインは続けて【戦争を終わらせ】、花火5発と「終戦」の合図を全員に出す
     *   （2026-08-22 のご指示。制圧できずに時間切れの時は、この演出は出ない）。
     */
    /**
     * ビーコンを壊せるようになるまでに、銀行を壊す回数の【予備の値】。
     *
     * ★ 本来の値はデータパックの `占領_必要回数 settei`。ここはそれが
     *   読めなかった時（データパックが未読み込み等）にだけ使う。
     *   プラグインに数字を持たせると、設定を変えた時に片方だけ古くなる。
     */
    static final int BEACON_KAISU_YOBI = 100;

    /**
     * 拠点の銀行を守る範囲（マス）。この中は置くのも壊すのもできない。
     *
     * ★ 2026-09-18 のご指示。銀行そのものは施設として守っているが、
     *   まわりを掘られる・覆われると使えなくなるため。
     *   縦横高さのうち一番大きい差で見る（7×7×7 の立方体）。
     */
    static final int GINKO_MAMORU = 3;

    /**
     * 壊された銀行が戻るまでの時間（tick）。20 tick = 1秒なので 60 = 3秒。
     * ★ 2026-09-18 のご指示「3秒で再設置される」。
     */
    static final int GINKO_MODORU = 60;

    /**
     * 戻す約束をした銀行（場所 → 元のブロック）。
     *
     * ★ Unsafe で作ると初期化子を通らないので、**使う時に作る**（modosuMachi）。
     *   このプロジェクトで3回 踏んでいる罠。
     */
    private java.util.Map<org.bukkit.Location, Material> ginkoModosu;

    /** 制圧の花火。5発を 12tick おき（約3秒で打ち終わる）。 */
    static final int SEIATSU_HANABI = 5;

    private boolean beaconKowaseruKa(Player player, Block block) {

        String mochinushi = basho.beaconNoKuni(block);
        if (mochinushi == null) {
            player.sendMessage("[目印] ここは目印のビーコンです。壊せません");
            return false;
        }

        String jibun = kane.teamMei(player);
        if (jibun == null || !Seiryoku.seiryokuKa(jibun)) {
            player.sendMessage("[目印] 勢力に入っていない人は拠点を落とせません");
            return false;
        }
        if (jibun.equals(mochinushi)) {
            player.sendMessage("[目印] 自分の勢力の拠点です。壊せません");
            return false;
        }

        String jibunMei = Seiryoku.mei(jibun);
        String aiteMei = Seiryoku.mei(mochinushi);

        // (2) 交戦中か。sensou の 2 が【交戦】
        if (kane.sensouJotai(jibunMei) != 2 || kane.sensou(jibunMei, aiteMei) != 2) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO,
                    aiteMei + " と交戦中でなければ、拠点は落とせません"));
            return false;
        }

        // (3) 攻める側が、相手の銀行を 占領_必要回数 だけ壊したか（2026-09-09 のご指示）
        //
        // ★ 回数は【戦争のマーカー】が持っている。マーカーのスコアはプラグインからは
        //   読めないので、データパックに固定の作業用へ写してもらってから読む
        //   （jidai:sensou/aite_yomu と同じ型。「呼んでから読む」）。
        int semeNo = Seiryoku.bangou(jibun, kane);
        int mamoruNo = Seiryoku.bangou(mochinushi, kane);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "scoreboard players set #q_kuni sagyou " + semeNo);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "scoreboard players set #q_aite sagyou " + mamoruNo);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "function jidai:sensou/kai_yomu");
        int kaisu = kane.sagyou("#q_kai");
        int hitsuyou = kane.settei("占領_必要回数");
        if (hitsuyou <= 0) {
            hitsuyou = BEACON_KAISU_YOBI;      // データパックがまだ読めていない時
        }
        if (kaisu < hitsuyou) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO,
                    aiteMei + " の銀行を壊した回数が足りません (" + kaisu + "/" + hitsuyou + ")。"
                            + "あと " + (hitsuyou - kaisu) + " 回 壊してから、このビーコンを壊してください"));
            return false;
        }

        // --- 条件がそろった。壊させて、植民地にする ---
        getLogger().info("拠点が落ちた: " + jibunMei + " → " + aiteMei
                + " (" + player.getName() + " / 壊した回数 " + kaisu + ")");
        String cmd = "scoreboard players set #s_kuni sagyou "
                + Seiryoku.bangou(jibun, kane);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "scoreboard players set #s_aite sagyou " + Seiryoku.bangou(mochinushi, kane));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "function jidai:sensou/shokuminchi");

        // --- 制圧 = 終戦。戦争を【再戦禁止】へ進め、花火と合図を出す ---
        //
        // ★ 戦争のマーカーはデータパックの物なので、読まずに命令で状態だけ変える。
        //   w_<番号> のタグで「この2勢力の戦争」だけを選べる（CONTRACT §2-3）。
        //   消さずに 3（再戦禁止）へ進めるのは、すぐ宣戦し直せないようにするため。
        int jibunNo = Seiryoku.bangou(jibun, kane);
        int aiteNo = Seiryoku.bangou(mochinushi, kane);
        String erabi = "@e[type=marker,tag=jidai_sensou,tag=w_" + jibunNo + ",tag=w_" + aiteNo + "]";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "execute as " + erabi + " run scoreboard players set @s sensou 3");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "execute as " + erabi + " run scoreboard players operation @s sensou_byou = 戦争_禁止秒 settei");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "function jidai:sensou/youyaku");

        seiatsuEnshutsu(block.getLocation().add(0.5, 1, 0.5), jibunMei, aiteMei);
        return true;
    }

    /**
     * 制圧の演出。落ちた拠点で花火5発、全員に「終戦」のタイトルと音。
     *
     * ★ 制圧できずに時間切れになった戦争には出さない（データパックの
     *   「交戦終了」の1行だけ）。派手なのは【落とした時だけ】。
     */
    void seiatsuEnshutsu(org.bukkit.Location basho, String kachi, String make) {
        Enshutsu.hanabiRenpatsu(this, basho, SEIATSU_HANABI, 12,
                org.bukkit.Color.RED, org.bukkit.Color.YELLOW);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§c§l終戦", "§f" + kachi + " が " + make + " を制圧した", 10, 80, 20);
        }
        Enshutsu.otoZenin(Enshutsu.OTO_DAI);
        Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.SENSO,
                "終戦 ── " + kachi + " が " + make + " の拠点を落とした。この戦争は終わった"));
        getLogger().info("制圧: " + kachi + " → " + make);
    }

    /**
     * 何かの画面の中をクリックするたびに、サーバーが呼ぶ。
     * 自分の持ち物入れ、普通のチェスト、かまど、他のプラグインの画面 ——
     * すべてここに来る。だから最初に「販売所かどうか」を確かめる。
     */
    @EventHandler
    public void onShopClick(InventoryClickEvent event) {

        // (1) 開いている上段の画面の持ち主が Shop でなければ、無関係。
        //
        //     instanceof は「その型かどうか」を確かめる書き方。
        //     ここで確かめないと、**普通のチェストまで操作できなくなる。**
        //     ★ 一番危ない所。他人の倉庫を壊しかねない。
        Object mochinushi = event.getInventory().getHolder();
        if (!(mochinushi instanceof Shop) && !(mochinushi instanceof Gacha)
                && !(mochinushi instanceof Sensen) && !(mochinushi instanceof Ginko)
                && !(mochinushi instanceof Shinko)) {
            return;
        }

        // (2) 販売所が開いている間は、どのクリックも通さない。これで取り出せない。
        //     ★ シフトクリック・数字キー・Qキーもこのイベントとして飛んでくる。
        event.setCancelled(true);

        // (3) 押した本人。getWhoClicked() は「人間」までしか分からないので、
        //     プレイヤーかどうかを確かめてから player という名前で受け取る。
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // (4) どの枠を押したか。
        //     getRawSlot() は上段(販売所)と下段(自分の持ち物)を通し番号で返す。
        //     ★ getSlot() は上段と下段で番号が重なるので、ここでは使わない。
        int slot = event.getRawSlot();

        // 下段(自分の持ち物)を触っただけなら、黙って何もしない。
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        // (5) 時代を進める画面なら、申請する。
        if (mochinushi instanceof Shinko) {
            shinko.oshita(player, slot, kane);
            return;
        }

        // (5) 銀行の画面なら、預けるか売る。
        if (mochinushi instanceof Ginko) {
            ginko.oshita(player, slot, kane);
            return;
        }

        // (6) 宣戦の画面なら、選んだ勢力へ宣戦する。
        if (mochinushi instanceof Sensen) {
            sensen.erabu(player, slot, kane);
            return;
        }

        // (6) ガチャなら、押された枠の意味は Gacha 側で見分けてもらう。
        //
        // ★★ 入れ物ごと渡すこと ★★
        //   ガチャは画面が2つある（単発の18枠と、十連の結果の27枠）。
        //   持ち主はどちらも Gacha なので、枠の番号だけでは見分けられない。
        //   枠の数で分けるために、入れ物そのものを渡す。
        if (mochinushi instanceof Gacha) {
            gacha.oshita(player, event.getInventory(), slot, kane);
            return;
        }

        // (7) 店なら購入を試みる。買えるかどうかの判断と返事は Shop 側が行う。
        //
        // ★★ shop ではなく【画面の持ち主】に頼むこと ★★
        //   店は2軒あるので、shop.kau(...) と書くと
        //   銃器専門店で押しても販売所の品を買ってしまう。
        if (mochinushi instanceof Shop mise) {
            mise.kau(player, slot, kane);
        }
    }
}
