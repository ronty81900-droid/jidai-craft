// =============================================================
// Shop.java ── 販売所の「中身」
//
//   何を、いくつ、いくらで、どの金で売るか。画面の組み立て。買う手続き。
//   値段を変えたい時に見る場所は、このファイルの IPPAN / JUKIHIN 一覧だけ。
//
//   ★★ 店は2軒ある ★★
//     販売所      … 生活・ピッケル・防具       (IPPAN / 36枠)
//     銃器専門店  … 銃5丁と弾1種              (JUKIHIN / 18枠)
//   どちらもエメラルドブロック。見分けているのは【登録された座標】で、
//   ブロックの種類ではない。Basho.MISE / Basho.JUKI が対応する。
//
//   3ファイルの分担:
//     JidaiCraft.java … 入口。イベントの登録と受け取り
//     Shop.java       … 店の中身。商品・画面・買う手続き
//     Kane.java       … 金の出し入れ（データパックとの接点）
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.List;

// Paper が同梱している文字表現。別途ライブラリを足しているわけではない。

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 販売所1軒ぶん。段階4では全プレイヤーで1軒を共有する。
 *
 * implements InventoryHolder
 *   「この画面の持ち主は誰か」をサーバーに教えるための資格。
 *   クリックされた画面が販売所かどうかを、持ち主で判定できるようになる。
 */
public final class Shop implements InventoryHolder {

    /**
     * 商品1つぶんの情報をまとめて持つ型。
     *
     * record
     *   「値を持つだけの小さな型」を1行で作る書き方。
     *   6つの値を持ち、値を取り出す仕組みも自動で用意される。
     *   取り出す時は s.namae() のように、名前のうしろに () を付ける。
     *
     *   slot     … 画面の何番目に置くか (左上が 0)
     *   material … 何のアイテムか
     *   kazu     … 何個渡すか
     *   namae    … 画面に出す日本語名
     *   nedan    … 値段
     *   seiryoku … true なら勢力の金、false なら個人の金で払う
     *   kaikin   … 【自分の勢力】がこの時代になったら買える
     *               (1=鉄器 2=中世 3=近代 4=現代)
     *               ★ 2026-08-20 に、中央の時代から勢力の時代へ変えた。
     *                 先に進んだ勢力から先に良い装備が買える。
     *   tsuchi   … 買った時の知らせ方。Enshutsu の種別を入れる。
     *               購入      … 勢力の中だけ(勢力の金) / 本人だけ(個人の金)
     *               戦争宣誓  … ★サーバー全員に流す
     *               下剋上    … ★サーバー全員に流す
     *   modItem  … MOD(TaCZ)のアイテムなら、give に渡す文字列。
     *               バニラの品なら null。
     *               ★★ なぜ文字列なのか ★★
     *                 Bukkit の Material は決め打ちの一覧で、MOD のアイテムを
     *                 表せない。そのため銃と弾だけは give コマンドで渡す。
     *                 画面に出す絵(material)はバニラの代用品で、
     *                 実際に手に入るのは modItem の方。
     *   memo     … 説明に足す1行。銃なら「弾の種類と装弾数」。無ければ null。
     *   page     … どのタブに出すか（1 か 2）
     */
    private record Shohin(int slot, Material material, int kazu, String namae,
                          int nedan, boolean seiryoku, int kaikin, String tsuchi,
                          String modItem, String memo, int page) {
    }

    /**
     * 商品の一覧。★ 商品と値段を変えるのはここだけ。
     *
     * 値段は仮の数字。データパック側は 鉄の剣=3 / 鉄のツルハシ=30 なので、
     * それに合わせて後で調整する余地がある。
     */
    private static final Shohin[] IPPAN = {
            // ===== タブ1「生活」 生活・勢力としての行為 =====
            //
            // ★★ 2026-08-22: 6品 足した（ご指示）★★
            //   鉄器: 原木×2 10 / 石レンガ×5 12 / 松明×16 5
            //   中世: 焼肉×5 30 / ガラス×5 25
            //   近代: 金リンゴ 100
            //   並びは【時代順】。枠は 2段目(9〜13) と 4段目(27〜31)。
            //   ★ 順番は UI 側のカード（clientmod/tools/mise_kumikae.py の
            //     SHINAMONO）と 1対1。hyou_tsukuru.py が名前まで突き合わせる。
            //
            //           枠 アイテム                 数 名前            値段 勢力 解禁 知らせ方          MOD 説明
            new Shohin(9, Material.BREAD,          3, "パン",          2,   false, 1, Enshutsu.KOUNYU,   null, null, 1),
            new Shohin(10, Material.COAL,          1, "石炭",          5,   false, 1, Enshutsu.KOUNYU,   null, null, 1),
            new Shohin(11, Material.OAK_LOG,       2, "オークの原木",  10,  false, 1, Enshutsu.KOUNYU,   null, null, 1),
            new Shohin(12, Material.STONE_BRICKS,  5, "石レンガ",      12,  false, 1, Enshutsu.KOUNYU,   null, null, 1),
            new Shohin(13, Material.TORCH,        16, "松明",          5,   false, 1, Enshutsu.KOUNYU,   null, null, 1),
            new Shohin(27, Material.COOKED_BEEF,   5, "焼肉",          30,  false, 2, Enshutsu.KOUNYU,   null, null, 1),
            new Shohin(28, Material.GLASS,         5, "ガラス",        25,  false, 2, Enshutsu.KOUNYU,   null, null, 1),
            new Shohin(29, Material.RED_BANNER,    1, "戦争宣誓",      100, true,  2, Enshutsu.SENSO,    null, null, 1),
            new Shohin(30, Material.GOLDEN_APPLE,  1, "金リンゴ",      100, false, 3, Enshutsu.KOUNYU,   null, null, 1),
            new Shohin(31, Material.WHITE_BANNER,  1, "下剋上",        150, true,  3, Enshutsu.GEKOKUJO, null, null, 1),

            // ===== タブ3「武器」 =====
            //   ★★ 2026-08-22: 剣を専用のタブへ移した（ご指示）★★
            //     どちらも【勢力の金】。防具と同じ考え方で、
            //     前線に出る人を勢力が武装させる形にする。
            //     個人の金はガチャ1回5〜50 の桁なので、100/500 は個人では届かない。
            //   ★ 並びは【時代順】。鉄器で買える物を先に置く。
            new Shohin(9, Material.IRON_SWORD,     1, "鉄の剣",     100, true,  1, Enshutsu.KOUNYU,   null, null, 3),
            new Shohin(10, Material.BOW,           1, "弓",         150, true,  1, Enshutsu.KOUNYU,   null, null, 3),
            //   ★ 矢だけ【個人の金】。撃つたびに減る消耗品なので、
            //     勢力の貯金から出すと時代進行が止まる（弾と同じ考え方）。
            new Shohin(11, Material.ARROW,        10, "矢",          30, false, 1, Enshutsu.KOUNYU,   null, null, 3),
            new Shohin(12, Material.DIAMOND_SWORD, 1, "ダイヤの剣", 500, true,  2, Enshutsu.KOUNYU,   null, null, 3),
            new Shohin(13, Material.CROSSBOW,      1, "クロスボウ", 150, true,  2, Enshutsu.KOUNYU,   null, null, 3),

            // ===== 2段目 (9〜12) は空けてある =====
            //
            // ★★ ピッケルの販売は 2026-08-20 に取りやめた ★★
            //   代わりに「時代のツルハシ」を全員に1本ずつ配る形にした。
            //   壊れず、死んでも落とさないので、買い直す必要がそもそも無い。
            //   詳しくは Tsuruhashi.java。

            // ===== 3段目 (18〜25) チェーンと鉄の防具 =====
            //   ★ 2段目(9〜17)・4段目(27〜35)・6段目(45〜53) は
            //     名前と価格を描くための帯。商品は置かない。
            //
            // ★ 防具は【勢力の金】。前線に出る人を勢力が装備させる形にする。
            // ★ 鉄の胸当て 30 は v0.12.0 からの据え置き。他はそれに合わせた。
            new Shohin(9, Material.CHAINMAIL_HELMET,     1, "チェーンの兜",     15, true, 1, Enshutsu.KOUNYU, null, null, 2),
            new Shohin(10, Material.CHAINMAIL_CHESTPLATE, 1, "チェーンの胸当て", 25, true, 1, Enshutsu.KOUNYU, null, null, 2),
            new Shohin(11, Material.CHAINMAIL_LEGGINGS,   1, "チェーンの脚当て", 20, true, 1, Enshutsu.KOUNYU, null, null, 2),
            new Shohin(12, Material.CHAINMAIL_BOOTS,      1, "チェーンの靴",     10, true, 1, Enshutsu.KOUNYU, null, null, 2),
            new Shohin(14, Material.IRON_HELMET,          1, "鉄の兜",           25, true, 2, Enshutsu.KOUNYU, null, null, 2),
            new Shohin(15, Material.IRON_CHESTPLATE,      1, "鉄の胸当て",       30, true, 2, Enshutsu.KOUNYU, null, null, 2),
            new Shohin(16, Material.IRON_LEGGINGS,        1, "鉄の脚当て",       30, true, 2, Enshutsu.KOUNYU, null, null, 2),
            new Shohin(17, Material.IRON_BOOTS,           1, "鉄の靴",           20, true, 2, Enshutsu.KOUNYU, null, null, 2),

            // ===== 5段目 (36〜39) ダイヤの防具 =====
            //
            // ★★ 胸当てだけ【近代】で解禁。他は【現代】 ★★
            //   ご指示どおり。近代のうちは「胸だけダイヤ」の姿になり、
            //   現代へ進む動機がそのまま見た目に出る。
            new Shohin(27, Material.DIAMOND_HELMET,     1, "ダイヤの兜",       300, true, 4, Enshutsu.KOUNYU, null, null, 2),
            new Shohin(28, Material.DIAMOND_CHESTPLATE, 1, "ダイヤの胸当て",   500, true, 3, Enshutsu.KOUNYU, null, "★近代で解禁", 2),
            new Shohin(29, Material.DIAMOND_LEGGINGS,   1, "ダイヤの脚当て",   400, true, 4, Enshutsu.KOUNYU, null, null, 2),
            new Shohin(30, Material.DIAMOND_BOOTS,      1, "ダイヤの靴",       200, true, 4, Enshutsu.KOUNYU, null, null, 2),
    };

    /**
     * 銃器専門店の品揃え。★ 銃と弾を変えるのはここだけ。
     *
     * ★ GunId / AmmoId は銃パックと TaCZ 本体の実ファイルから読んだ値。
     *   hamster: が銃パック(第一次大戦)、tacz: が本体の既定パック。
     *   綴りが1文字違ってもコンパイルは通るので、
     *   tests/buki_kakunin.py が起動前に実物の jar と突き合わせている。
     */
    private static final Shohin[] JUKIHIN = {
            // ===== 銃5丁。左から時代順(＝強い順) =====
            //
            // ★★ 2026-08-30: 9丁6種 → 5丁1種に整理 ★★
            //   消したかったのは2つ。
            //     ・実銃の名前だけでは「どれが強いか」が分からない
            //     ・銃と弾の組み合わせを覚えないと撃てない
            //   呼び名を役割にして、実銃名は説明の行へ回した。
            //   弾は1種類だけにしたので、組み合わせを間違えようがない。
            //
            // ★ 並びは【毎秒の威力】を実物のパックから測って決めた。
            //   18 → 45 → 60 → 79 → 108 と必ず上がる。
            //   M107(毎秒367)と散弾銃(1発で10粒・毎秒1750)は桁が違うので外した。
            //
            // ★ 威力そのものは tools/jyu_chousei.py が銃パック側で1割 下げている。
            //   ここに書く数字は【値段】であって威力ではない。
            new Shohin(9,  Material.CROSSBOW, 1, "拳銃",       800,   true, 2, Enshutsu.KOUNYU,
                    "tacz:modern_kinetic_gun{GunId:\"hamster:nagantm1895\"}", "ナガン M1895 / 装弾数 7", 1),
            new Shohin(10, Material.BOW,      1, "小銃",       3000,  true, 3, Enshutsu.KOUNYU,
                    "tacz:modern_kinetic_gun{GunId:\"hamster:sks\"}", "SKS / 装弾数 10", 1),
            new Shohin(11, Material.TRIDENT,  1, "連射銃",     5000,  true, 3, Enshutsu.KOUNYU,
                    "tacz:modern_kinetic_gun{GunId:\"hamster:mp18\"}", "MP18 / 装弾数 32", 1),
            new Shohin(12, Material.TRIDENT,  1, "自動小銃",   8000,  true, 4, Enshutsu.KOUNYU,
                    "tacz:modern_kinetic_gun{GunId:\"tacz:m4a1\"}", "M4A1 / 装弾数 30", 1),
            new Shohin(13, Material.SPYGLASS, 1, "狙撃銃",     12000, true, 4, Enshutsu.KOUNYU,
                    "tacz:modern_kinetic_gun{GunId:\"tacz:ai_awp\"}", "AWP / 装弾数 5", 1),

            // ===== 弾1種 =====
            //   ★ 弾は【個人の金】で買う消耗品。理由は3つ(2026-08-20 から変えていない)。
            //     1. 勢力の貯金は時代進行に要る。弾で削ると企画が止まる
            //     2. 個人の金の使い道がパンと石炭とピッケルしか無い
            //     3. リーダーが弾を握ると、撃てる人を選べてしまう
            //   ★ 財布を勢力にするなら false → true。
            new Shohin(9, Material.ARROW, 30, "弾 30発", 20, false, 2, Enshutsu.KOUNYU,
                    "tacz:ammo{AmmoId:\"tacz:556x45\"}", "どの銃にも使えます", 2),
    };

    /**
     * まだ買えない枠に置くアイテム。
     * ★「買えない物を、あらかじめ見せておく」のが狙い。
     *   次の時代に何が来るかが分かるので、時代を進める動機になる。
     */
    /**
     * タブ1枚ぶん。
     *   slot … 1段目のどこに置くか
     *   page … 押した時に開くページ
     */
    private record Tab(int slot, Material material, String namae, int page) {
    }

    /**
     * タブの一覧。★ 1段目（枠0〜8）はタブ専用。商品は置かない。
     *
     * ★★ なぜタブを分けたか ★★
     *   商品を1段おきに置くと、1ページに置ける数が減る。
     *   ページを分ければ、どちらの段にも「名前と価格の帯」を確保できる。
     *   タブは押した瞬間に画面を開き直すだけで、状態は人ごとに持つ。
     */
    private static final Tab[] TAB_IPPAN = {
            new Tab(0, Material.BREAD, "生活", 1),
            new Tab(1, Material.IRON_CHESTPLATE, "防具", 2),
            // ★ 2026-08-22: 剣を専用のタブへ移した
            new Tab(2, Material.IRON_SWORD, "武器", 3),
    };

    private static final Tab[] TAB_JUKI = {
            new Tab(0, Material.CROSSBOW, "銃", 1),
            new Tab(1, Material.ARROW, "弾", 2),
    };

    /**
     * 誰が今どのページを見ているか。
     *
     * ★★ 画面も人ごとに作る ★★
     *   1つの画面を全員で使い回していると、誰かがタブを押した瞬間に
     *   他の人の画面まで切り替わってしまう。
     */
    private java.util.Map<String, Integer> pageOf;

    /**
     * 人ごとのページ。初めて必要になった時に1回だけ作る。
     *
     * ★★ final にして = new HashMap<>() と書いてはいけない ★★
     *   検証ハーネスは Unsafe でコンストラクタを通さずに Shop を作る。
     *   その時 初期化が走らず null のまま落ちる（実際に落ちた。
     *   ツルハシの鍵・石掘りのくじと同じ罠）。
     */
    private java.util.Map<String, Integer> pageOf() {
        if (pageOf == null) {
            pageOf = new java.util.HashMap<>();
        }
        return pageOf;
    }

    private static final Material MIKAIKIN = Material.GRAY_STAINED_GLASS_PANE;

    // =========================================================
    //  銃器専門店の解放（2026-08-22 のご指示）
    // =========================================================
    //
    // ★★ 中世に入っただけでは銃を買えない ★★
    //   勢力の貯金が【一度でも】10,000 を超えたら、その勢力は恒久的に開く。
    //   開いたあとは、今までどおり時代ごとに銃が順に解禁される。
    //
    // ★ 「今の残高が 10,000 以上の間だけ」にはしなかった。
    //   12,000 の M107 を買った瞬間に店が閉じてしまい、
    //   「買えたのに買えなくなる」という説明のつかない挙動になるため。

    /** 銃器専門店が開くのに要る勢力の貯金。★ 変えるならここだけ。 */
    public static final int JUKI_KAIHOU = 10000;

    /**
     * 銃器専門店が開いた勢力。
     *
     * ★ static なのは、店が2軒あっても記録は勢力ごとに1つだから。
     * ★ static の初期化子は Unsafe でも走るので、検証ハーネスでも空の集合になる。
     */
    private static final java.util.Set<String> JUKI_AITA = new java.util.HashSet<>();

    /** その勢力で銃器専門店が開いているか。 */
    public static boolean jukiKaihou(String kuni) {
        return kuni != null && JUKI_AITA.contains(kuni);
    }

    /**
     * 貯金を見て、条件を満たしていたら開ける。
     *
     * @return 開いた【その瞬間】だけ true。すでに開いていれば false。
     *
     * ★ 貯金を引数で受けるのは、検証で境界（10000 と 10001）を
     *   そのまま測れるようにするため。
     * ★ plugin が null なら書き出さない（検証用）。
     */
    public static boolean jukiKaihouMiru(JidaiCraft plugin, String kuni, int chokin) {
        if (kuni == null || JUKI_AITA.contains(kuni)) {
            return false;
        }
        if (chokin <= Ibutsu.jukiShikii(kuni)) {
            // ★「超えた時」なので、ちょうど 10,000 では開かない。
            //   遺物「武器商人の手形」を持つ勢力は Ibutsu.JUKI_SHIKII_TEGATA（5,500）。
            //   ★ 数字はここに書かず jukiShikii() に聞く（2026-08-23: コメントが 5,000 のままずれていた）
            return false;
        }
        JUKI_AITA.add(kuni);
        if (plugin != null) {
            plugin.getConfig().set("juki_kaihou." + kuni, true);
            plugin.saveConfig();
        }
        return true;
    }

    /** 解放の記録を全部取り消す（運営のリセット）。次の起動でも開いたままにならない。 */
    public static void jukiRisetto(JidaiCraft plugin) {
        JUKI_AITA.clear();
        if (plugin != null) {
            plugin.getConfig().set("juki_kaihou", null);
            plugin.saveConfig();
        }
    }

    /** 書き出してあった解放の記録を読み戻す。★ onEnable から呼ぶ。 */
    public static void jukiYomikomi(JidaiCraft plugin) {
        JUKI_AITA.clear();
        var bu = plugin.getConfig().getConfigurationSection("juki_kaihou");
        if (bu == null) {
            return;
        }
        for (String kuni : bu.getKeys(false)) {
            if (plugin.getConfig().getBoolean("juki_kaihou." + kuni, false)) {
                JUKI_AITA.add(kuni);
            }
        }
        plugin.getLogger().info("銃器専門店: " + JUKI_AITA.size() + " 勢力ぶんの解放を読み戻した");
    }

    /**
     * 一般の販売所の枠数。54 = 6段（チェストの最大）。
     *
     * ★★ 商品は1段おきに置く（2026-08-21） ★★
     *   枠の間隔は 18px、アイテムは 16px なので、
     *   商品を詰めて並べると【下に文字を描く隙間が 2px しか無い】。
     *   1段おきにすると、空いた段まるごと 18px が使えるので、
     *   リソースパックの絵で「名前と価格」を焼き込める。
     *   商品の並びは固定なので、絵に焼いてもずれない。
     *
     *     1段目 (0〜8)   商品   生活と勢力の行為
     *     2段目 (9〜17)  帯     ↑の名前と価格（絵）
     *     3段目 (18〜26) 商品   チェーンと鉄の防具
     *     4段目 (27〜35) 帯     ↑の名前と価格（絵）
     *     5段目 (36〜44) 商品   ダイヤの防具
     *     6段目 (45〜53) 帯     ↑の名前と価格（絵）
     */
    private static final int WAKU_IPPAN = 54;

    /**
     * 銃器専門店の枠数。36 = 4段。
     *
     *     1段目 (0〜8)   銃9丁
     *     2段目 (9〜17)  帯（絵）
     *     3段目 (18〜23) 弾6種
     *     4段目 (27〜35) 帯（絵）
     */
    private static final int WAKU_JUKI = 36;


    /**
     * この店の種類（Basho.MISE / Basho.JUKI）。
     *
     * ★ final にしていない。検証ハーネスが Unsafe でコンストラクタを
     *   通さずに Shop を作るため、後から入れられる必要がある。
     *   （本番では生成時に1回入るだけで、以後は変わらない）
     */
    private String shurui;

    /** この店の品揃え。種類で決まる。 */
    private Shohin[] shohin;

    /** 画面と文面に出す店の名前。 */
    private String namae;

    /**
     * 組み立て済みの画面。開くたびに中身だけ並べ直して使い回す。
     *
     * ★ final にしていない。jidai ui on/off で見出しを切り替えた時に、
     *   作り直す必要があるため（見出しは作る時にしか決められない）。
     */
    private Inventory inventory;

    /** 勢力の状態。下剋上を買った時に権利を足すために持つ */
    private Seiryoku seiryoku;

    /** 戦争宣誓の「相手を選ぶ画面」。 */
    private Sensen sensen;

    /** 画面を閉じる予約を入れるために持つ（下の説明を参照）。 */
    private JidaiCraft plugin;

    /** 起動時に1回だけ渡す。 */
    public void seiryokuWatasu(JidaiCraft p, Seiryoku s, Sensen sen) {
        this.plugin = p;
        this.seiryoku = s;
        this.sensen = sen;
    }

    /**
     * 販売所を1軒作る。
     * ★ サーバーが起動しきってからでないと画面を作れないので、
     *   呼ぶのは JidaiCraft の onEnable() の中。
     */
    public Shop(String shurui) {
        this.shurui = shurui;
        boolean juki = shurui.equals(Basho.JUKI);
        this.shohin = juki ? JUKIHIN : IPPAN;
        this.namae = juki ? "銃器専門店" : "販売所";

        // 第1引数の this が「この画面の持ち主は自分だ」の宣言。
        // 中身はまだ入れない。開く直前に、その時の時代に合わせて並べ直す。
        this.inventory = tsukuruGamen(1);
    }

    /**
     * 見出しを作り直す。
     *
     * ★ 販売所は画面を1つ作って使い回しているので、
     *   jidai ui on/off で切り替えても、作り直さないと見出しが変わらない。
     *   （他の画面は開くたびに作っているので、そのまま切り替わる）
     */
    public Inventory namaeKousin() {
        // ★ 画面は開くたびに作り直すので、ここで作り直す必要はもう無い。
        //   jidai ui on/off の直後に開けば、新しい見出しで作られる。
        //   （呼び出し側との約束を変えないため、メソッドは残してある）
        return tsukuruGamen(1);
    }

    /** この店はどちらか。Basho.MISE か Basho.JUKI。 */
    public String shurui() {
        return shurui;
    }

    /** 画面に出す店の名前。断りの文にも使う。 */
    public String namae() {
        return namae;
    }

    /**
     * 今の【その人の勢力の】時代に合わせて棚を並べ直してから、本人に開く。
     *
     * ★ 人によって並びが変わるので、開くたびに組み直すこの作り方が要る。
     *   画面は1つを使い回しているが、開く直前に必ず組み直すので、
     *   別の勢力の人どうしが同時に開いても、それぞれ自分の時代で見える。
     *
     * ★ 開くたびに並べ直している。
     *   時代が上がった瞬間に画面を作り直す仕組みを別に持つより、
     *   「開いた時が最新」の方が単純で、ずれようがない。
     *   並べ直すのは5個だけなので、速さは問題にならない。
     */
    public void hiraku(Player player, Kane kane) {
        hiraku(player, kane, pageOf().getOrDefault(player.getName(), 1));
    }

    /** ページを指定して開く。タブを押した時はこちらが呼ばれる。 */
    private void hiraku(Player player, Kane kane, int page) {
        pageOf().put(player.getName(), page);
        int jidai = kane.jidaiOf(player);

        Inventory inv = tsukuruGamen(page);
        for (Tab t : tabs()) {
            inv.setItem(t.slot(), tabItem(t, page));
        }
        inv.setItem(WAKU_KANE, kaneItem(kane, player));
        for (Shohin s : shohin) {
            if (s.page() == page) {
                inv.setItem(s.slot(), tsukuru(s, jidai, jukiAiteru(kane, player)));
            }
        }
        player.openInventory(inv);
    }

    /**
     * この人にとって、この店が開いているか。
     * ★ 販売所は常に開いている。銃器専門店だけ貯金の条件が付く。
     */
    private boolean jukiAiteru(Kane kane, Player player) {
        if (!Basho.JUKI.equals(shurui)) {
            return true;
        }
        return jukiKaihou(kane.teamMei(player));
    }

    /** この店のタブ。 */
    private Tab[] tabs() {
        return Basho.JUKI.equals(shurui) ? TAB_JUKI : TAB_IPPAN;
    }

    /**
     * 画面を1つ作る。★ ページごとに見出しの絵が違う。
     *   絵が違うので、どのタブを見ているかが見た目で分かる。
     */
    private Inventory tsukuruGamen(int page) {
        boolean juki = Basho.JUKI.equals(shurui);
        String e;
        if (juki) {
            e = page == 2 ? Enshutsu.UI_JUKI_2 : Enshutsu.UI_JUKI;
        } else {
            e = page == 2 ? Enshutsu.UI_MISE_2 : Enshutsu.UI_MISE;
        }
        this.inventory = Bukkit.createInventory(this,
                juki ? WAKU_JUKI : WAKU_IPPAN,
                Enshutsu.gamenMei(e, this.namae));
        return this.inventory;
    }

    /**
     * 勢力の金を出す枠。タブ列のいちばん右。
     *
     * ★★ なぜ画面の中に出すか（2026-08-22）★★
     *   勢力の金（chokin）は、サイドバーには【文字の行として】しか出ていない。
     *   目的そのものがクライアントへ届かないので、
     *   クライアント MOD からは読めなかった（「勢力 ?」と出ていた）。
     *   ここに置けば、MOD はこの枠の名前を読むだけで済む。
     *   MOD を入れていない人にも、残高が見えて得になる。
     */
    private static final int WAKU_KANE = 8;

    /**
     * 勢力の金を見せるだけの品。
     * ★ 押しても何も起きない（この画面のクリックは全部 塞いである）。
     */
    private ItemStack kaneItem(Kane kane, Player player) {
        // ★ 金庫の持ち主は【日本語名】。チーム名を渡すと必ず 0 になる（実機で踏んだ）
        String kinko = kane.kinkoMei(player);
        int aru = (kinko == null) ? 0 : kane.seiryokuZandaka(kinko);
        ItemStack it = new ItemStack(Material.GOLD_INGOT, 1);
        ItemMeta m = it.getItemMeta();
        // ★ 数字はそのまま書く。桁区切りは見る側（MOD）で付ける。
        m.setDisplayName(moji("勢力の金: " + aru));
        m.setLore(List.of(moji("この勢力の貯金です"), moji("押しても何も起きません")));
        it.setItemMeta(m);
        return it;
    }

    /** タブのアイテム。今 開いているタブには印を付ける。 */
    private ItemStack tabItem(Tab t, int ima) {
        ItemStack it = new ItemStack(t.material(), 1);
        ItemMeta m = it.getItemMeta();
        boolean erabu = t.page() == ima;
        m.setDisplayName(moji((erabu ? "▶ " : "") + t.namae()));
        m.setLore(List.of(moji(erabu ? "今 見ています" : "クリックで切り替え")));
        it.setItemMeta(m);
        return it;
    }

    /**
     * InventoryHolder を名乗った以上、必ず用意しないといけないメソッド。
     * サーバーが「持ち主の画面を出せ」と言ってきた時にこれを返す。
     */
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // =========================================================
    //  買う手続き
    // =========================================================

    /**
     * 枠をクリックした人に対して、購入を試みる。
     * 買えない時は、必ず理由を本人に伝えてから何もせずに帰る。
     *
     * ★ ここに来る player は、クリックを起こした本人そのもの。
     *   「近くの誰か」から推定していないので、同時操作で取り違えない。
     */
    public void kau(Player player, int slot, Kane kane) {
        // タブを押したら、ページを切り替えて開き直す
        int page = pageOf().getOrDefault(player.getName(), 1);
        for (Tab t : tabs()) {
            if (t.slot() == slot) {
                if (t.page() != page) {
                    // ★ クリックの処理中に画面を開き直してはいけない決まりがある。
                    //   次の tick に回す（Sensen・下剋上と同じ作法）。
                    Bukkit.getScheduler().runTask(plugin,
                            () -> hiraku(player, kane, t.page()));
                }
                return;
            }
        }

        Shohin s = sagasu(page, slot);
        if (s == null) {
            player.sendMessage("[" + namae + "] そこには商品がありません");
            return;
        }

        // データパックが読み込まれていなければ、金の目的が存在しない。
        // 何も確かめずに進むとサーバーのログにエラーが出るので、先に止める。
        if (!kane.junbiOK()) {
            player.sendMessage("[" + namae + "] データパックが読み込まれていません（管理者に連絡してください）");
            return;
        }

        // まだ解禁されていない商品は買えない。
        // ★ 画面で灰色に見えていても、ここで必ずもう一度確かめる。
        //   開いたまま放置した古い並びからクリックされても通さないため。
        // ★★ 見るのは【自分の勢力の時代】 ★★
        //   勢力に入っていない人(運営・見学者)は中央の時代で見る。
        //   その面倒は Kane.jidaiOf が見ている。
        // ★★ 銃器専門店は、勢力の貯金が 10,000 を超えるまで開かない ★★
        //   画面で灰色に見えていても、ここで必ずもう一度 確かめる。
        if (!jukiAiteru(kane, player)) {
            player.sendMessage("[" + namae + "] この店はまだ開いていません"
                    + " (勢力の貯金が " + JUKI_KAIHOU + " を超えると開きます)");
            return;
        }

        int jidai = kane.jidaiOf(player);
        if (jidai < s.kaikin()) {
            player.sendMessage("[" + namae + "] まだ解禁されていません (自分の勢力が "
                    + Kane.jidaiMei(s.kaikin()) + " になると解禁)");
            return;
        }

        // 持ち物がいっぱいだと、渡した商品が消える。金だけ減るのが最悪なので先に断る。
        // firstEmpty() は空き枠の番号を返し、空きが無ければ -1 を返す。
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("[" + namae + "] 持ち物がいっぱいです。空けてから買ってください");
            return;
        }

        // --- 財布は身分で自動的に決まる。選ばせない ----------------
        //
        // ★ 財布は商品ごとの指定どおり（勢力の商品は勢力の金でしか買えない）。
        // --- 下剋上の権利は、ここでは買わない --------------------
        // ★★ 値段も上限も解禁時代も、データパックが持っている ★★
        //   (jidai:sensou/gekokujo_kau … 150・1勢力1つまで・近代以降)
        //   プラグインが金を引くと二重取りになる。だから丸ごと任せる。
        //   断りの文と全体通知も向こうが出す。
        //   ★ 上の「解禁されているか」だけはプラグイン側でも見ている。
        //     画面で灰色にするために必要なため。値段150・解禁は近代(3)で、
        //     データパックの settei（下剋上_値段 / 下剋上_解禁時代）と同じ。
        //     どちらが正しいかで迷ったら、データパックが正しい。
        if (s.tsuchi().equals(Enshutsu.GEKOKUJO)) {
            // ★ 画面を閉じるのは次の tick。クリックの処理中に
            //   closeInventory を呼んではいけない決まりのため（Sensen と同じ）。
            Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
            String cmd = "execute as " + player.getName() + " at " + player.getName()
                    + " run function jidai:sensou/gekokujo_kau";
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            return;
        }

        // --- 戦争宣誓は、ここでは買わない ------------------------
        // 相手を選んで初めて購入が確定する。画面を開くだけ。
        if (s.tsuchi().equals(Enshutsu.SENSO)) {
            if (sensen == null) {
                player.sendMessage("[" + namae + "] 宣戦の画面が用意できていません");
                return;
            }
            String kinko0 = kane.kinkoMei(player);
            if (kinko0 == null) {
                player.sendMessage("[" + namae + "] 勢力に入っていないため、宣戦できません");
                return;
            }
            int mochi = kane.seiryokuZandaka(kinko0);
            if (mochi < s.nedan()) {
                player.sendMessage("[" + namae + "] 勢力の金が足りません (必要 " + s.nedan()
                        + " / 金庫 " + mochi + ")");
                return;
            }
            sensen.hiraku(player, kane, s.nedan());
            return;
        }

        if (s.seiryoku()) {
            kauSeiryoku(player, s, kane);
        } else {
            kauKojin(player, s, kane);
        }
    }

    /**
     * 個人の金で買う。買えたことは本人にだけ伝える。
     */
    private void kauKojin(Player player, Shohin s, Kane kane) {
        int mae = kane.kojinZandaka(player);

        if (mae < s.nedan()) {
            player.sendMessage("[" + namae + "] 個人の金が足りません (必要 " + s.nedan()
                    + " / 所持 " + mae + ")");
            return;
        }

        // ★★ 先に渡し、渡せたことを確かめてから金を引く ★★
        //   逆にすると「金だけ引かれて品が来ない」が起きる。ガチャと同じ順序。
        if (!watasu(player, s)) {
            player.sendMessage("[" + namae + "] 商品を渡せませんでした。金は引いていません（運営に連絡してください）");
            return;
        }

        int ato = mae - s.nedan();
        kane.kojinKousin(player, ato);

        if (s.tsuchi().equals(Enshutsu.KOUNYU)) {
            // ふつうの買い物。本人にだけ
            player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    s.namae() + " ×" + s.kazu() + " を購入 (個人の金 " + mae + " → " + ato + ")"));
            tebikiModosu(player, kane, s, false);
        } else {
            // ★ 下剋上のような取り返しのつかない買い物は、サーバー全員に流す。
            //   事故ではなく戦犯として残す（色も他と変えてある）。
            Enshutsu.zeninTsuchi(Enshutsu.kazaru(s.tsuchi(),
                    player.getName() + " が「" + s.namae() + "」を購入した"));
        }
    }

    /**
     * 勢力の金で買う。
     *
     * ★ 勢力の金は所属している全員が使えるので、必ず勢力の全員に知らせる。
     *   記録が無いと「金庫が減った事実」だけが残り、誰が使ったか分からなくなる。
     *   追及は人間がやる。プラグインは事実を残すだけ。
     *
     * ★ 知らせる範囲は「勢力の中だけ」。サーバー全員には流さない。
     *   全員に流すと、他勢力に何をいくつ買ったかが筒抜けになるため。
     */
    private void kauSeiryoku(Player player, Shohin s, Kane kane) {

        String kinko = kane.kinkoMei(player);
        if (kinko == null) {
            player.sendMessage("[" + namae + "] 勢力に入っていないため、勢力の金は使えません");
            return;
        }

        int mae = kane.seiryokuZandaka(kinko);

        if (mae < s.nedan()) {
            player.sendMessage("[" + namae + "] 勢力の金が足りません (必要 " + s.nedan()
                    + " / 金庫 " + mae + ")");
            return;
        }

        // ★ 個人の金と同じく、渡せたことを確かめてから引く。
        if (!watasu(player, s)) {
            player.sendMessage("[" + namae + "] 商品を渡せませんでした。勢力の金は引いていません（運営に連絡してください）");
            return;
        }

        int ato = mae - s.nedan();
        kane.seiryokuKousin(kinko, ato);
        tebikiModosu(player, kane, s, true);

        // ★ ここに来るのは、ふつうの買い物だけ。
        //   戦争宣誓は相手選択の画面へ、下剋上はデータパックへ、
        //   どちらもこの手前で抜けている。
        //   知らせは勢力の中だけに流す。全体に流すと手の内が筒抜けになるため。
        var shirase = Enshutsu.kazaru(Enshutsu.KOUNYU,
                player.getName() + " が 勢力の金 で " + s.namae() + " ×" + s.kazu()
                        + " を購入 (" + kinko + " " + mae + " → " + ato + ")");
        for (Player nakama : kane.onajiSeiryoku(player)) {
            nakama.sendMessage(shirase);
        }
    }

    /**
     * 遺物「死の商人の手引書」: 銃器専門店で買った代金の 1割 を、払った財布へ戻す。
     *
     * ★ 値引きではなく【戻し】。画面の値段はそのままで、払った直後に戻る。
     *   MOD の画面は値段を絵の文字として持っているので、値引きにすると表示と請求がずれる。
     */
    private void tebikiModosu(Player player, Kane kane, Shohin s, boolean seiryokuSaifu) {
        if (!Basho.JUKI.equals(shurui)) {
            return;
        }
        String kinko = kane.kinkoMei(player);
        int modori = Ibutsu.nebikiModori(kinko, s.nedan());
        if (modori <= 0) {
            return;
        }
        if (seiryokuSaifu) {
            kane.seiryokuKousin(kinko, kane.seiryokuZandaka(kinko) + modori);
        } else {
            kane.kojinKousin(player, kane.kojinZandaka(player) + modori);
        }
        player.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                "死の商人の手引書で " + modori + " 戻った"));
    }

    /**
     * 商品を実際に手元へ渡す。渡せたら true。
     *
     * ★★ バニラの品と MOD の品で、渡し方が違う ★★
     *   バニラ … そのまま持ち物へ入れる
     *   MOD    … give コマンドで渡す
     *
     *   なぜ分かれるか: Bukkit の Material は「バニラのアイテム一覧」を
     *   決め打ちで持っており、MOD が足した銃や弾を表す手段が無い。
     *   コマンドなら文字列で指定できるので、そこだけ迂回している。
     *
     * ★ MOD の品は、渡した後に持ち物の総数が増えたかを数えて確かめる。
     *   コマンドが「成功した」と答えても実際には入っていない場合があり、
     *   その言葉を信じると金だけ引かれる。数えるのが一番確かめになる。
     */
    private boolean watasu(Player player, Shohin s) {

        if (s.modItem() == null) {
            player.getInventory().addItem(new ItemStack(s.material(), s.kazu()));
            return true;
        }

        int mae = mochimonoKazu(player);
        String cmd = "give " + player.getName() + " " + s.modItem() + " " + s.kazu();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        return mochimonoKazu(player) > mae;
    }

    /** 持ち物に入っているアイテムの総数。渡せたかを数えるためだけに使う。 */
    private static int mochimonoKazu(Player player) {
        int gokei = 0;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it != null) {
                gokei += it.getAmount();
            }
        }
        return gokei;
    }

        /** 枠の番号から商品を探す。無ければ null。 */
    private Shohin sagasu(int page, int slot) {
        for (Shohin s : shohin) {
            if (s.page() == page && s.slot() == slot) {
                return s;
            }
        }
        return null;
    }

    // =========================================================
    //  画面の組み立て
    // =========================================================

    /**
     * 商品1つを、値段の説明付きのアイテムに仕立てる。
     */
    private ItemStack tsukuru(Shohin s, int jidai, boolean jukiOK) {

        // --- 銃器専門店がまだ開いていない -------------------------
        // ★ 中身は伏せたまま「店はある」ことだけ見せ、貯金を貯める動機にする。
        if (!jukiOK) {
            ItemStack fusagi = new ItemStack(MIKAIKIN, 1);
            ItemMeta m = fusagi.getItemMeta();
            m.setDisplayName(moji("？？"));
            m.setLore(List.of(
                    moji("勢力の貯金が " + JUKI_KAIHOU + " を超えると店が開きます"),
                    moji("(開いたあとは、時代ごとに順に解禁)")
            ));
            fusagi.setItemMeta(m);
            return fusagi;
        }

        // --- まだ解禁されていない枠 -------------------------------
        // 中身は見せず、灰色のガラス板に差し替える。
        // 何が来るかは伏せたまま「枠がある」ことだけを見せ、
        // 次の時代へ進む動機にする。
        if (jidai < s.kaikin()) {
            ItemStack fusagi = new ItemStack(MIKAIKIN, 1);
            ItemMeta m = fusagi.getItemMeta();
            m.setDisplayName(moji("？？"));
            String dare = "自分の勢力が";
            m.setLore(List.of(
                    moji(dare + " " + Kane.jidaiMei(s.kaikin()) + " になると解禁"),
                    moji("(今は " + Kane.jidaiMei(jidai) + ")")
            ));
            fusagi.setItemMeta(m);
            return fusagi;
        }

        // --- 解禁済みの商品 ---------------------------------------
        ItemStack item = new ItemStack(s.material(), s.kazu());

        // 「付箋」にあたる部分。名前や説明はここに書く。
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(moji(s.namae() + " ×" + s.kazu()));

        // 値段の説明 (lore)。アイテムにカーソルを合わせると出る。
        // ★ 財布は身分で決まるので、両方を書いておく。
        String saifu = s.seiryoku() ? "勢力の金" : "個人の金";
        List<String> setsumei = new ArrayList<>();
        // ★ 「円」を付ける（2026-08-26 のご指示）。MOD の画面と同じ読み方に揃える。
        setsumei.add(moji("値段: " + saifu + " " + s.nedan() + "円"));
        if (s.memo() != null) {
            setsumei.add(moji(s.memo()));
        }
        if (s.modItem() != null) {
            // ★ 絵はバニラの代用品なので、そのことを本人に伝えておく。
            //   「弓を買ったのに銃が出た」と誤解されないため。
            setsumei.add(moji("(絵は仮です。実物の銃・弾が手に入ります)"));
        }
        setsumei.add(moji("クリックで購入"));
        meta.setLore(setsumei);

        // ★ 付箋は書き換えただけでは反映されない。アイテムに貼り直す。
        //   この1行を忘れると、名前も値段も出ない。
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 文字列を、画面に出せる形に変換する。
     *
     * なぜ素の文字列のままではいけないか:
     *   文字列を直接渡すやり方 (setLore など) は Paper で非推奨になっており、
     *   いずれ使えなくなる。実際にコンパイラが警告を出すことを確認済み。
     *   ★ Adventure(Paper 専用)は 2026-08-20 に外した。
     *     MOD と一緒に動かす土台(Arclight)が持っていないため。
     *
     * decoration(ITALIC, false)
     *   これを付けないと、名前も説明も斜体で表示される。
     */
    private static String moji(String s) {
        return Enshutsu.MODOSU + s;
    }
}
