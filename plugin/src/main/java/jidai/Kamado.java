// =============================================================
// Kamado.java ── エンダーかまど（1人1枚の、自分専用の精錬枠）
//
//   拠点のかまどを押すと、その人だけのかまどが開く。
//   同じブロックを別の人が押しても、中身は混ざらない。
//   置いたまま離れても焼き続け、あとで取りに戻れる。
//
//   ★★ なぜ要るか ★★
//     この企画ではクラフトが禁止なので、
//     「鉱石 → インゴット」は精錬しか道が無い。
//     ここが経済の入口になる。
//     普通のかまどだと味方に中身を持っていかれるので、
//     Shotbow の ANNI と同じく「自分専用」にしてある。
//
//   ★ 時代が進むほど速く焼ける（鉄器 40% → 現代 80% 短縮）。
//   ★ 燃料は要る。クラフトが禁止なので、石炭・原木・木の板が現実的な燃料。
//
//   ★ 中身はサーバーを止めても消えない（config に書き出す）。
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public final class Kamado implements InventoryHolder {

    /** かまどの画面の枠。0=材料 1=燃料 2=出来上がり（バニラと同じ並び） */
    private static final int WAKU_ZAIRYO = 0;
    private static final int WAKU_NENRYO = 1;
    private static final int WAKU_DEKI = 2;

    /**
     * 1つ焼くのにかかる tick。★ 速さを変えるならここ。
     * 添字は時代 1〜4。バニラは 200 なので、鉄器で 40%、現代で 80% 短い。
     */
    private static final int[] YAKU_TICK = {0, 120, 95, 65, 40};

    /**
     * 燃料と、燃える長さ（tick）。★ 燃料を増やすならここ。
     *
     * ★ バニラの値に合わせてある。クラフトが禁止なので、
     *   実際に手に入るのは 石炭・木炭・原木・木の板 あたりになる。
     */
    private static final Map<Material, Integer> NENRYO = new HashMap<>();

    static {
        NENRYO.put(Material.COAL, 1600);
        NENRYO.put(Material.CHARCOAL, 1600);
        NENRYO.put(Material.COAL_BLOCK, 16000);
        NENRYO.put(Material.BLAZE_ROD, 2400);
        NENRYO.put(Material.LAVA_BUCKET, 20000);
        NENRYO.put(Material.STICK, 100);
        // 原木と木の板は種類が多いので、下の nenryoNagasa() でまとめて拾う
    }

    /** 原木・木の板の燃える長さ（バニラと同じ 300） */
    private static final int KI_NAGASA = 300;

    /** 拠点の持ち主を表す印の頭 */
    private static final String KYOTEN = "jidai_kyoten_";

    private final JidaiCraft plugin;

    /** 材料 → 出来上がり。起動時にサーバーのレシピから作る */
    private final Map<Material, ItemStack> reshipi = new HashMap<>();

    /** 名前 → その人のかまど */
    private final Map<String, Hitotsu> kamado = new HashMap<>();

    /**
     * かまど1つぶんの状態。
     *
     * ★ record ではなく class にしてあるのは、中身が変わるため。
     *   record は「作ったら変わらない」入れ物なので、ここには向かない。
     */
    private static final class Hitotsu {
        Inventory inv;
        int moeNokori;    // 今の燃料があと何tick燃えるか
        int moeGoukei;    // その燃料が満タンで何tickだったか（炎の絵の高さに使う）
        int yakiSusumi;   // 今の1個を何tickぶん焼いたか

        // ★★ 最後に画面へ送った値（2026-08-26 の監査で足した）★★
        //   毎tick・全件で送っていたが、送る中身が前と同じなら
        //   クライアントは既にその絵を出している。送る意味が無い。
        //   ★ -1 から始めるので、1回目は必ず送る。
        int okuriMoe = -1;
        int okuriGoukei = -1;
        int okuriYaki = -1;
        int okuriHitsuyo = -1;
    }

    public Kamado(JidaiCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, InventoryType.FURNACE, moji("エンダーかまど"));
    }

    /**
     * サーバーが持っている精錬のレシピを写しておく。
     *
     * ★ レシピ表を自分で書かない。バニラが変わったら自動で追従する。
     *   recipeIterator() は全種類のレシピを返すので、
     *   かまど用（CookingRecipe）だけを拾う。
     */
    public void reshipiYomikomi() {
        reshipi.clear();
        // ★★ ここで落ちてもプラグイン全体を巻き添えにしないこと ★★
        //   onEnable の途中なので、例外が出ると以降が全部止まり、
        //   コマンドも購買も動かなくなる（v0.5.0 で実際に起きた型の事故）。
        //   かまどが焼けないだけなら、他は今までどおり使える。
        Iterator<Recipe> it;
        try {
            it = Bukkit.recipeIterator();
        } catch (RuntimeException e) {
            plugin.getLogger().warning("精錬のレシピを読めませんでした: " + e);
            return;
        }
        if (it == null) {
            plugin.getLogger().warning("精錬のレシピが取れませんでした（かまどは焼けません）");
            return;
        }
        while (it.hasNext()) {
            Recipe r = it.next();
            if (!(r instanceof CookingRecipe<?> ck)) {
                continue;
            }
            ItemStack moto = ck.getInput();
            if (moto == null || moto.getType() == Material.AIR) {
                continue;
            }
            // 同じ材料に複数のレシピがある時は、最初の1つだけ採る
            reshipi.putIfAbsent(moto.getType(), ck.getResult());
        }
        plugin.getLogger().info("エンダーかまど: 精錬のレシピを " + reshipi.size() + " 種類 読み込んだ");
    }

    /**
     * かまどが押された。
     *
     * ★ 開けるのはその拠点の勢力の人だけ。ANNI と同じ決まり。
     */
    public void osareta(Player player, Block block, Kane kane) {
        String jibun = kane.teamMei(player);
        String nushi = kyotenNoNushi(block);
        if (nushi != null && !nushi.equals(jibun)) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "ここは " + Seiryoku.mei(nushi) + " のかまどです。自分の拠点で使ってください"));
            Enshutsu.oto(player, Enshutsu.OTO_DAME);
            return;
        }
        if (jibun == null) {
            player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU, "勢力に入っていません"));
            return;
        }
        player.openInventory(toru(player.getName()).inv);
    }

    /** その人のかまどを取り出す。無ければ作る。 */
    private Hitotsu toru(String namae) {
        Hitotsu h = kamado.get(namae);
        if (h == null) {
            h = new Hitotsu();
            h.inv = Bukkit.createInventory(this, InventoryType.FURNACE, moji("エンダーかまど"));
            kamado.put(namae, h);
        }
        return h;
    }

    /** この かまど が「どの勢力の拠点か」。分からなければ null。 */
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
     * 1tick ぶん進める。jidai:clock ではなく、プラグインの毎tickの予約から呼ぶ。
     *
     * ★ 画面を閉じていても焼き続ける。それが「置いて帰れる」ということ。
     *   だから開いている人だけを回すのではなく、全員ぶんを回す。
     */
    public void susumu(Kane kane) {
        // ★ 1秒に1回、覚えていた時代を捨てる。次に要る時 引き直される。
        //   毎tick 引き直すと、かまど100個で 1秒 8,000回になる（実測）。
        if (++tick >= 20) {
            tick = 0;
            if (oboetaJidai != null) {
                oboetaJidai.clear();
            }
        }
        for (Map.Entry<String, Hitotsu> e : kamado.entrySet()) {
            hitotsuSusumu(e.getKey(), e.getValue(), kane);
        }
    }

    /** かまど1つぶんを1tick進める。 */
    private void hitotsuSusumu(String namae, Hitotsu h, Kane kane) {
        ItemStack zairyo = h.inv.getItem(WAKU_ZAIRYO);
        ItemStack deki = h.inv.getItem(WAKU_DEKI);
        ItemStack dekiru = (zairyo == null) ? null : reshipi.get(zairyo.getType());

        // 出来上がりの枠に入れられるか（別の品や満杯なら焼かない）
        boolean okeru = dekiru != null
                && (deki == null || deki.getType() == Material.AIR
                    || (deki.getType() == dekiru.getType()
                        && deki.getAmount() + dekiru.getAmount() <= deki.getMaxStackSize()));

        // --- 燃料に火を点ける ---------------------------------
        if (h.moeNokori <= 0 && okeru) {
            ItemStack nenryo = h.inv.getItem(WAKU_NENRYO);
            int nagasa = (nenryo == null) ? 0 : nenryoNagasa(nenryo.getType());
            if (nagasa > 0) {
                herasu(h.inv, WAKU_NENRYO);
                h.moeNokori = nagasa;
                h.moeGoukei = nagasa;
            }
        }

        // --- 燃えている間だけ焼ける ---------------------------
        if (h.moeNokori > 0) {
            h.moeNokori--;
            if (okeru) {
                h.yakiSusumi++;
                int hitsuyo = yakuTick(namae, kane);
                if (h.yakiSusumi >= hitsuyo) {
                    h.yakiSusumi = 0;
                    herasu(h.inv, WAKU_ZAIRYO);
                    if (deki == null || deki.getType() == Material.AIR) {
                        h.inv.setItem(WAKU_DEKI, dekiru.clone());
                    } else {
                        deki.setAmount(deki.getAmount() + dekiru.getAmount());
                    }
                }
            } else {
                h.yakiSusumi = 0;
            }
        } else {
            // 火が消えたら、焼きかけは少しずつ戻る（バニラと同じ感触）
            h.yakiSusumi = Math.max(0, h.yakiSusumi - 2);
        }

        hyoujiKousin(namae, h, kane);
    }

    /**
     * 開いている人の画面に、炎と矢印の絵を反映する。
     *
     * ★ 中身だけ動かしても、絵は勝手には動かない。
     *   setProperty で「今どれくらい燃えているか」を送って初めて動く。
     */
    private void hyoujiKousin(String namae, Hitotsu h, Kane kane) {
        // ★★ 変わっていなければ、何も引かず・何も送らない（2026-08-26 の監査）★★
        //   ここは【毎tick・全件】走る。しかも下では
        //     Bukkit.getPlayerExact（名前からプレイヤーを引く）
        //     h.inv.getViewers()（閲覧者の一覧を作る）
        //     setProperty ×4（クライアントへパケット）
        //   を毎回 行っていた。125人が一度ずつ開けば、閉じても減らない表に対して
        //   これが 2,500回/秒。**空のかまどでも同じだけ走っていた。**
        //   ★ 4つとも前と同じなら、絵は既にその形になっている。送らなくてよい。
        //     焼ける速さも火の点き方も1tick も変わらない（送信だけを間引く）。
        int hitsuyo = yakuTick(namae, kane);
        if (h.moeNokori == h.okuriMoe && h.moeGoukei == h.okuriGoukei
                && h.yakiSusumi == h.okuriYaki && hitsuyo == h.okuriHitsuyo) {
            return;
        }
        Player p = Bukkit.getPlayerExact(namae);
        if (p == null) {
            return;
        }
        // ★★ 2026-08-22: 「同じ画面か」を == で比べるのをやめた ★★
        //   Arclight は getTopInventory() のたびに【新しい包み】を返すことがあり、
        //   保存してある h.inv と == で比べると、同じ中身でも毎回 false になる。
        //   その結果ここで引き返し続け、炎と矢印が一度も送られなかった
        //   （実機で「燃料は減るのに炎が出ない」。警告ログが出ないのも、
        //     setProperty まで到達していなかったから）。
        //   「h.inv を見ている人の一覧に、この人がいるか」で判定する。
        //   一覧は包みを作り直しても中身（本体の閲覧者リスト）を読むので、ずれない。
        if (!h.inv.getViewers().contains(p)) {
            return;   // 別の画面を見ている
        }
        InventoryView view = p.getOpenInventory();
        boolean todoita = view.setProperty(InventoryView.Property.BURN_TIME, h.moeNokori);
        view.setProperty(InventoryView.Property.TICKS_FOR_CURRENT_FUEL,
                Math.max(1, h.moeGoukei));
        view.setProperty(InventoryView.Property.COOK_TIME, h.yakiSusumi);
        view.setProperty(InventoryView.Property.TICKS_FOR_CURRENT_SMELTING, hitsuyo);
        // ★ 送れた物だけを控える。送れなかった時に控えると、次から永久に送らなくなる。
        if (todoita) {
            h.okuriMoe = h.moeNokori;
            h.okuriGoukei = h.moeGoukei;
            h.okuriYaki = h.yakiSusumi;
            h.okuriHitsuyo = hitsuyo;
        }

        // ★★ 2026-08-22: 炎が出ないという報告の切り分け ★★
        //   焼く処理そのものは KamadoTest で正しいことを確かめた
        //   （燃料が減り、120tick で焼き上がる）。
        //   残る疑いは「画面へ状態を送る所」。setProperty は真偽を返すので、
        //   届かなかったことが1度でもあれば、その場で1回だけ知らせる。
        //   ★ 毎tick 出すとログが埋まるので、1回だけにしてある。
        if (!todoita && !okurenaiToTsutaeta) {
            okurenaiToTsutaeta = true;
            plugin.getLogger().warning(
                    "エンダーかまど: 炎の状態を画面へ送れませんでした"
                    + "（setProperty が false）。焼く処理そのものは動いています。");
        }
    }

    /** 「送れなかった」と1度でも知らせたか。ログを埋めないための札。 */
    private boolean okurenaiToTsutaeta;

    /** その人の勢力の時代でかかる tick。 */
    /**
     * その人の時代で決まる「1個 焼くのに要る tick」。
     *
     * ★★ 1秒に1回だけ引き直す（2026-08-26 の実測）★★
     *   前は【毎tick・全かまど】で引いていた。中で
     *     Bukkit.getPlayerExact（名前から人を引く）
     *     Kane.jidaiOf → チームの検索 ＋ スコアの読み
     *   を通るので、かまど100個で **1秒あたり 8,000回** になっていた。
     *
     *   ★ 時代が上がるのは数十分に1回。1秒 遅れても焼き速度は変わらない。
     *     時代が変わった瞬間の1秒だけ、前の速さで焼く。それだけ。
     */
    // ★ final にしない。検査は Unsafe でこの物を作るので
    //   【フィールドの初期化子を通らない】（この置き場の既知の罠）。
    //   使う所で null なら作る形にしておく。
    private Map<String, Integer> oboetaJidai;

    /** 何tick たったか。20 で割り切れる時だけ引き直す。 */
    private int tick;

    private int yakuTick(String namae, Kane kane) {
        if (oboetaJidai == null) {
            oboetaJidai = new HashMap<>();
        }
        Integer oboe = oboetaJidai.get(namae);
        if (oboe == null) {
            Player p = Bukkit.getPlayerExact(namae);
            int jidai = (p == null) ? 1 : kane.jidaiOf(p);
            oboe = Math.max(1, Math.min(4, jidai));
            oboetaJidai.put(namae, oboe);
        }
        return YAKU_TICK[oboe];
    }

    /**
     * 燃料が燃える長さ。燃料でなければ 0。
     *
     * ★ ItemStack ではなく Material を受け取る。
     *   ItemStack は中身を差し替えられない作りなので、
     *   Material で受けた方が試験でそのまま測れる。
     */
    static int nenryoNagasa(Material m) {
        if (m == null || m == Material.AIR) {
            return 0;
        }
        Integer kimatta = NENRYO.get(m);
        if (kimatta != null) {
            return kimatta;
        }
        // 原木・木の板は種類が多い。名前で拾う（樫でも松でも同じ長さ）
        String namae = m.name();
        if (namae.endsWith("_LOG") || namae.endsWith("_PLANKS")
                || namae.endsWith("_WOOD")) {
            return KI_NAGASA;
        }

        // ★★ ハーフは「木のハーフ」だけ燃える（2026-08-22 に直した）★★
        //   それまで _SLAB で終わる名前を全部 燃料にしていたので、
        //   バニラでは燃えない【石のハーフ】まで燃料になっていた。
        //   木かどうかは「同じ名前の板があるか」で見分ける。
        //   こうしておくと、新しい木の種類が増えても勝手に付いてくるし、
        //   石化した樫のハーフ（PETRIFIED_OAK_SLAB）のような
        //   「木の名前だが燃えない物」も、板が無いので正しく外れる。
        if (namae.endsWith("_SLAB")) {
            String ita = namae.substring(0, namae.length() - "_SLAB".length()) + "_PLANKS";
            return Material.getMaterial(ita) != null ? KI_NAGASA / 2 : 0;
        }
        return 0;
    }

    /** その枠の品を1つ減らす。0になったら空にする。 */
    private static void herasu(Inventory inv, int waku) {
        ItemStack item = inv.getItem(waku);
        if (item == null) {
            return;
        }
        if (item.getAmount() <= 1) {
            inv.setItem(waku, null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }

    // --- サーバーを止めても中身を失わないための出し入れ ------------

    /**
     * 中身を設定ファイルへ書き出す。
     *
     * ★ これが無いと、サーバーを止めた瞬間に全員の材料が消える。
     *   焼きかけの進み具合は捨てる（品物さえ残れば実害が無いため）。
     */
    public void hozon() {
        plugin.getConfig().set("kamado", null);
        for (Map.Entry<String, Hitotsu> e : kamado.entrySet()) {
            Inventory inv = e.getValue().inv;
            List<ItemStack> naka = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                naka.add(inv.getItem(i));
            }
            boolean karappo = true;
            for (ItemStack it : naka) {
                if (it != null && it.getType() != Material.AIR) {
                    karappo = false;
                }
            }
            if (!karappo) {
                plugin.getConfig().set("kamado." + e.getKey(), naka);
            }
        }
        plugin.saveConfig();
    }

    /** 書き出してあった中身を読み戻す。 */
    @SuppressWarnings("unchecked")
    public void yomikomi() {
        kamado.clear();
        var bu = plugin.getConfig().getConfigurationSection("kamado");
        if (bu == null) {
            return;
        }
        for (String namae : bu.getKeys(false)) {
            List<?> naka = plugin.getConfig().getList("kamado." + namae);
            if (naka == null) {
                continue;
            }
            Hitotsu h = toru(namae);
            for (int i = 0; i < 3 && i < naka.size(); i++) {
                if (naka.get(i) instanceof ItemStack it) {
                    h.inv.setItem(i, it);
                }
            }
        }
        plugin.getLogger().info("エンダーかまど: " + kamado.size() + " 人ぶんの中身を読み戻した");
    }

    /** 今いくつのかまどを抱えているか。検証で使う。 */
    public int kazu() {
        return kamado.size();
    }

    /** 文字列を画面に出せる形に変換する。 */
    private static String moji(String s) {
        return Enshutsu.MODOSU + s;
    }
}
