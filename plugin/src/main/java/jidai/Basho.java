// =============================================================
// Basho.java ── 施設の場所の管理
//
//   どの座標が販売所／銀行／ガチャなのかを覚えておく係。
//   手で登録する道(/jidai add)と、
//   ワールドのマーカーを走査して一括登録する道(/jidai scan)がある。
//
//   ★ なぜ走査が要るか
//     データパックが拠点を自動設置し、マーカーも置く。
//     プラグインが手動登録だけだと、5拠点×3種で十数回コマンドを打つことになり、
//     打ち漏らした店だけ黙って動かなくなる。
//     マーカーを読めば、設置と登録のずれが原理的に無くなる。
//
//   ★ データパックはファイルを書けないので、座標の受け渡しは
//     「ワールドに置かれたマーカー」経由になる。これが唯一の共有手段。
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.TextDisplay;


/**
 * 施設の座標一覧。3種類をそれぞれ別の並びで持つ。
 */
public final class Basho {

    /** 種別。設定ファイルの見出しにもそのまま使う */

    /** 販売所（中央のエメラルド）。**買う専用。プラグインが画面を開く** */
    public static final String MISE = "hanbaijo";

    /** 売却所（拠点のエメラルド）。**データパックが処理する。プラグインは触らない** */
    public static final String URU = "urikaijo";

    /** 銀行（拠点の金ブロック）。**データパックが処理する。プラグインは触らない** */
    public static final String GINKO = "ginko";

    /**
     * 銃器専門店（拠点のエメラルド）。**プラグインが画面を開く**
     *
     * ★★ 販売所と同じエメラルドブロック ★★
     *   見分けているのは【登録された座標】であって、ブロックの種類ではない。
     *   両方 EMERALD_BLOCK なので、登録を取り違えると
     *   「押したら違う店が開く」という分かりにくい壊れ方をする。
     */
    public static final String JUKI = "juki";

    /** ガチャ（拠点のダイヤ）。**プラグインが画面を開く** */
    public static final String GACHA = "gacha";

    /** 時代を進める鉄ブロック */
    public static final String SHINKO = "shinko";

    /** エンダーかまど（1人1枚の専用の精錬枠） */
    public static final String KAMADO = "kamado";

    /** 拠点のビーコン（目印）。効果は絶対に使わせない。壊させない */
    public static final String BEACON = "beacon";

    /** 中央のビーコン（目印 兼 中央の時代の表示）。ガラスの色が時代で変わる */
    public static final String BEACON_CHUO = "beacon_chuo";

    /**
     * マーカーのタグ と 種別 の対応表。
     *
     * ★ データパックの setup/kyoten.mcfunction と setup/kyoten_1.mcfunction を
     *   読んで写したもの。プラグインが勝手に決めた名前は1つも無い。
     *     jidai_uru       … 拠点のエメラルドブロック(売却所/販売所)
     *     jidai_uru_chuo  … 中央のエメラルドブロック(買う専用)
     *     jidai_ginko     … 金ブロック3つに共通で付く(_10 _50 _zenbu と併用)
     *     jidai_gacha     … ダイヤブロック
     *
     * ★ 銀行の中身(いくら預けるか)はデータパックが処理する。
     *   プラグインが銀行を覚えるのは「壊されないようにする」ためだけ。
     */
    private static final String[][] TAG = {
            // ★ 順番に意味がある。jidai_uru_chuo を先に見ること。
            //   中央のマーカーには jidai_uru は付いていないが、
            //   将来どちらも付いた時に「中央＝買う」を優先させるため。
            {"jidai_uru_chuo",    MISE},
            {"jidai_mise",        MISE},
            {"jidai_uru",         URU},
            {"jidai_ginko",       GINKO},
            {"jidai_juki",        JUKI},
            {"jidai_gacha",       GACHA},
            {"jidai_shinko",      SHINKO},
            {"jidai_kamado",      KAMADO},
            // ★ ビーコンもここで拾う。データパックが置いたら /jidai scan で登録される。
            //   中央を先に見ること（中央のビーコンだけ色が変わるため）。
            {"jidai_beacon_chuo", BEACON_CHUO},
            {"jidai_beacon",      BEACON},
    };

    /** 種別ごとに、そこに在るはずのブロック。走査した座標の確認に使う */
    private static final String[][] BLOCK = {
            {MISE,        "EMERALD_BLOCK"},
            {JUKI,        "EMERALD_BLOCK"},
            {URU,         "EMERALD_BLOCK"},
            {GINKO,       "GOLD_BLOCK"},
            {GACHA,       "DIAMOND_BLOCK"},
            {BEACON,      "BEACON"},
            {BEACON_CHUO, "BEACON"},
            {SHINKO,      "IRON_BLOCK"},
            {KAMADO,      "FURNACE"},
    };

    /** 走査の結果をまとめて返すための型。 */
    public record Kekka(int mise, int uru, int ginko, int gacha, int beacon, int marker,
                        int world, int forceChunk, boolean karappo,
                        int shinko, int kamado, int juki) {
        public int goukei() {
            return mise + uru + ginko + gacha + beacon + shinko + kamado + juki;
        }
    }

    private List<String> mise = new ArrayList<>();
    private List<String> uru = new ArrayList<>();
    private List<String> ginko = new ArrayList<>();
    private List<String> gacha = new ArrayList<>();
    private List<String> beacon = new ArrayList<>();
    private List<String> beaconChuo = new ArrayList<>();
    private List<String> shinko = new ArrayList<>();
    private List<String> kamado = new ArrayList<>();
    private List<String> juki = new ArrayList<>();

    /**
     * ビーコン1つ1つが「どの勢力の拠点か」。
     *   "world,x,y,z=kyuryo" の並びで持つ（設定ファイルにそのまま書ける形）。
     *
     * ★★ 勝利条件その1「戦争勝利」に要る ★★
     *   交戦中の相手の貯金を0にすると、その勢力のビーコンだけが壊せる。
     *   どのビーコンが誰のものかを知らないと、判定のしようがない。
     * ★ 目印そのものの一覧(beacon)とは別に持つ。
     *   目印は中央のぶんもあり、そちらに勢力は無いため。
     */
    private List<String> beaconKuni = new ArrayList<>();

    /**
     * 勢力ごとの「拠点の立ち位置」。 "kyuryo=world,x,y,z" の並び。
     *
     * ★★ 走査で【実物の施設マーカーから】求める ★★
     *   データパックは施設マーカーに jidai_kyoten_<チーム名> の印を付けている。
     *   その印が付いたマーカーの平均の位置から、2ブロック手前（+z）を立ち位置にする。
     *   座標を手で書くと、地図を作り直した時に必ずずれる（setup/zahyou の教訓）。
     * ★ kyotenTe は運営が手で決めた物。こちらが優先。走査では上書きしない。
     */
    private List<String> kyoten = new ArrayList<>();
    private List<String> kyotenTe = new ArrayList<>();

    /** 石油プラントの目印の位置（"world,x,y,z"）。 */
    private String plant = "";

    // --- 出し入れ -------------------------------------------------

    public List<String> ichiran(String shurui) {
        if (shurui.equals(URU)) {
            return uru;
        }
        if (shurui.equals(GINKO)) {
            return ginko;
        }
        if (shurui.equals(JUKI)) {
            return juki;
        }
        if (shurui.equals(GACHA)) {
            return gacha;
        }
        if (shurui.equals(BEACON)) {
            return beacon;
        }
        if (shurui.equals(BEACON_CHUO)) {
            return beaconChuo;
        }
        if (shurui.equals(SHINKO)) {
            return shinko;
        }
        if (shurui.equals(KAMADO)) {
            return kamado;
        }
        return mise;
    }

    /** 種別の全部（表示や走査で順に回すため）。 */
    public static String[] zenShurui() {
        return new String[]{MISE, JUKI, URU, GINKO, GACHA, BEACON, BEACON_CHUO,
                SHINKO, KAMADO};
    }

    /**
     * ブロック1個を、設定ファイルに書ける1行の文字列にする。
     * 例: "world,10,64,-20"
     *
     * ★ 世界の名前も入れている。ネザーと現世で同じ座標があるため、
     *   座標だけだと別の場所を同じ施設と誤認する。
     */
    public static String kagi(Block block) {
        return kagi(block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ());
    }

    public static String kagi(String world, int x, int y, int z) {
        return world + "," + x + "," + y + "," + z;
    }

    /** その座標の種別。どこにも登録されていなければ null。 */
    public String shurui(String k) {
        for (String shu : zenShurui()) {
            if (ichiran(shu).contains(k)) {
                return shu;
            }
        }
        return null;
    }

        /** 手で1件足す。既にどこかに登録されていれば false。 */
    public boolean tsuika(String shurui, String k) {
        if (shurui(k) != null) {
            return false;
        }
        ichiran(shurui).add(k);
        return true;
    }

    /** 1件外す。どの種別でも外せる。 */
    public boolean sakujo(String k) {
        boolean keshita = false;
        for (String shu : zenShurui()) {
            keshita |= ichiran(shu).remove(k);
        }
        return keshita;
    }

    public int kensuu() {
        int n = 0;
        for (String shu : zenShurui()) {
            n += ichiran(shu).size();
        }
        return n;
    }

    // --- 保存と読み戻し -------------------------------------------

    /** 設定ファイルから読み戻す。 */
    public void yomikomi(JidaiCraft plugin) {
        mise = new ArrayList<>(plugin.getConfig().getStringList(MISE));
        uru = new ArrayList<>(plugin.getConfig().getStringList(URU));
        ginko = new ArrayList<>(plugin.getConfig().getStringList(GINKO));
        gacha = new ArrayList<>(plugin.getConfig().getStringList(GACHA));
        beacon = new ArrayList<>(plugin.getConfig().getStringList(BEACON));
        beaconChuo = new ArrayList<>(plugin.getConfig().getStringList(BEACON_CHUO));
        shinko = new ArrayList<>(plugin.getConfig().getStringList(SHINKO));
        kamado = new ArrayList<>(plugin.getConfig().getStringList(KAMADO));
        juki = new ArrayList<>(plugin.getConfig().getStringList(JUKI));
        beaconKuni = new ArrayList<>(plugin.getConfig().getStringList("beacon_kuni"));
        kyoten = new ArrayList<>(plugin.getConfig().getStringList("kyoten"));
        kyotenTe = new ArrayList<>(plugin.getConfig().getStringList("kyoten_te"));
        plant = plugin.getConfig().getString("plant", "");
    }

    /**
     * 設定ファイルへ書き出す。
     * ★ set しただけでは書かれない。saveConfig() まで呼んで初めてファイルになる。
     */
    public void hozon(JidaiCraft plugin) {
        plugin.getConfig().set(MISE, mise);
        plugin.getConfig().set(URU, uru);
        plugin.getConfig().set(GINKO, ginko);
        plugin.getConfig().set(GACHA, gacha);
        plugin.getConfig().set(BEACON, beacon);
        plugin.getConfig().set(BEACON_CHUO, beaconChuo);
        plugin.getConfig().set(SHINKO, shinko);
        plugin.getConfig().set(KAMADO, kamado);
        plugin.getConfig().set(JUKI, juki);
        plugin.getConfig().set("beacon_kuni", beaconKuni);
        plugin.getConfig().set("kyoten", kyoten);
        plugin.getConfig().set("kyoten_te", kyotenTe);
        plugin.getConfig().set("plant", plant);
        plugin.saveConfig();
    }

    // --- 拠点の立ち位置と石油プラント -----------------------------

    /** 拠点の印（データパックが施設マーカーに付ける）。 */
    private static final String KYOTEN_TAG = "jidai_kyoten_";

    /**
     * 勢力の拠点の立ち位置。手で決めた物 → 走査で求めた物 の順。無ければ null。
     * ★ 世界が見つからなければ最初の世界で読む（世界は1つしか無い）。
     */
    public Location kyotenIchi(String team) {
        String k = hiku(kyotenTe, team);
        if (k == null) {
            k = hiku(kyoten, team);
        }
        return k == null ? null : kagiKaraIchi(k);
    }

    /** 運営が手で決めた拠点の立ち位置。走査では消えない。 */
    public void kyotenTeKimeru(String team, Location l) {
        kyotenTe.removeIf(s -> s.startsWith(team + "="));
        kyotenTe.add(team + "=" + kagi(l.getWorld().getName(),
                l.getBlockX(), l.getBlockY(), l.getBlockZ()));
    }

    /** 石油プラントの目印の位置。走査していなければ null。 */
    public Location plantIchi() {
        return (plant == null || plant.isEmpty()) ? null : kagiKaraIchi(plant);
    }

    /** 走査で求めた拠点の一覧（表示用）。 "kyuryo=world,x,y,z" の並び。 */
    public List<String> kyotenIchiran() {
        List<String> r = new ArrayList<>(kyoten);
        for (String t : kyotenTe) {
            r.removeIf(s -> s.startsWith(t.substring(0, t.indexOf('=') + 1)));
            r.add(t + " (手動)");
        }
        return r;
    }

    private static String hiku(List<String> ichiran, String team) {
        for (String s : ichiran) {
            if (s.startsWith(team + "=")) {
                return s.substring(team.length() + 1);
            }
        }
        return null;
    }

    /** "world,x,y,z" → 立てる位置（ブロックの真ん中）。 */
    static Location kagiKaraIchi(String k) {
        String[] bu = k.split(",");
        if (bu.length != 4) {
            return null;
        }
        World w = Bukkit.getWorld(bu[0]);
        if (w == null && !Bukkit.getWorlds().isEmpty()) {
            w = Bukkit.getWorlds().get(0);
        }
        if (w == null) {
            return null;
        }
        try {
            return new Location(w, Integer.parseInt(bu[1]) + 0.5,
                    Integer.parseInt(bu[2]), Integer.parseInt(bu[3]) + 0.5);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // --- マーカーの走査 -------------------------------------------

    /**
     * ワールド中のマーカーを走査して、座標一覧を作り直す。
     *
     * ★★ 一番大事な決まり ★★
     *   **1件も見つからなかった時は、今の一覧を消さない。**
     *   マーカーは「読み込まれている区画」にある分しか取れない。
     *   起動直後などに0件だった時に上書きしてしまうと、
     *   **全部の店が黙って死ぬ。** 消さずに残し、警告だけ出す。
     *
     * ★ 区画の読み込みについて（実測した事実）
     *   データパックの jidai:load が jidai:setup/forceload を毎回呼んでおり、
     *   5拠点と中央の区画を forceload（常時読み込み）にしている。
     *   forceload の指定はワールドに保存されるので、2回目以降の起動では
     *   プラグインが動く前に読み込まれている。
     *   ただし**まっさらなワールドの初回起動**では、データパックが
     *   forceload を掛ける前にプラグインが走る可能性がある。
     *   そのため 0件でも一覧を壊さず、運営が /jidai scan を打てば直る形にした。
     */
    public Kekka scan() {
        // ★★ 種別ごとの入れ物を【機械的に】用意する ★★
        //   以前は if の連鎖で振り分けていたが、その書き方だと
        //   **一覧に無い種類がすべて「販売所」に落ちる**。
        //   実際に、時代を進める鉄ブロックとエンダーかまどが
        //   全部「販売所」になった(実機で発覚)。
        //   zenShurui() から作れば、種類を足した時に必ず追従する。
        Map<String, List<String>> atarashii = new LinkedHashMap<>();
        for (String shu : zenShurui()) {
            atarashii.put(shu, new ArrayList<>());
        }
        int markerKazu = 0;
        int forceChunk = 0;

        // 拠点ごとの施設マーカーの位置の合計と数（平均を出すため）
        Map<String, int[]> kyotenGoukei = new LinkedHashMap<>();
        String nPlant = null;

        for (World w : Bukkit.getWorlds()) {
            forceChunk += w.getForceLoadedChunks().size();

            // getEntitiesByClass は「読み込まれている区画」のものだけを返す。
            for (Marker m : w.getEntitiesByClass(Marker.class)) {
                markerKazu++;

                // ★ 拠点の印とプラントは、施設の種別より前に拾う。
                //   プラントは施設ではないので、下の continue で飛ばされるため。
                for (String t : m.getScoreboardTags()) {
                    if (t.startsWith(KYOTEN_TAG)) {
                        String team = t.substring(KYOTEN_TAG.length());
                        int[] g = kyotenGoukei.computeIfAbsent(team + "@" + w.getName(),
                                x -> new int[4]);
                        g[0] += m.getLocation().getBlockX();
                        g[1] = Math.max(g[1], m.getLocation().getBlockY());
                        g[2] += m.getLocation().getBlockZ();
                        g[3]++;
                    }
                    if (t.equals("jidai_plant")) {
                        nPlant = kagi(w.getName(),
                                m.getLocation().getBlockX(),
                                m.getLocation().getBlockY(),
                                m.getLocation().getBlockZ());
                    }
                }

                String shu = tagKara(m.getScoreboardTags());
                if (shu == null) {
                    continue;   // 石油プラント等、施設でないマーカーは無視
                }
                List<String> saki = atarashii.get(shu);
                if (saki == null) {
                    // zenShurui() に入れ忘れた種類。黙って捨てず、必ず知らせる。
                    Bukkit.getLogger().warning("[JidaiCraft] 種別 " + shu
                            + " の入れ物がありません。zenShurui() に足してください");
                    continue;
                }
                String k = kagi(w.getName(),
                        m.getLocation().getBlockX(),
                        m.getLocation().getBlockY(),
                        m.getLocation().getBlockZ());
                // 同じ座標に2つマーカーがあっても1件にする（重複しない）
                if (!saki.contains(k)) {
                    saki.add(k);
                }
            }
        }

        List<String> nMise = atarashii.get(MISE);
        List<String> nUru = atarashii.get(URU);
        List<String> nGinko = atarashii.get(GINKO);
        List<String> nGacha = atarashii.get(GACHA);
        List<String> nBeacon = atarashii.get(BEACON);
        List<String> nBeaconChuo = atarashii.get(BEACON_CHUO);
        List<String> nShinko = atarashii.get(SHINKO);
        List<String> nKamado = atarashii.get(KAMADO);
        List<String> nJuki = atarashii.get(JUKI);

        int zenbu = 0;
        for (List<String> v : atarashii.values()) {
            zenbu += v.size();
        }
        boolean karappo = zenbu == 0;

        // ★★ 0件なら今の一覧を守る。絶対に壊さない。 ★★
        if (!karappo) {
            // 拠点の立ち位置 = 施設マーカーの平均から 2ブロック手前（+z）。
            // ★ 施設は拠点の中心から見て +z 側の一列に並び、南(+z)を向いている
            //   (setup/kyoten_1)。その手前に立たせれば、目の前に施設がある。
            List<String> nKyoten = new ArrayList<>();
            for (Map.Entry<String, int[]> e : kyotenGoukei.entrySet()) {
                String team = e.getKey().substring(0, e.getKey().indexOf('@'));
                String world = e.getKey().substring(e.getKey().indexOf('@') + 1);
                int[] g = e.getValue();
                nKyoten.add(team + "=" + kagi(world, g[0] / g[3], g[1], g[2] / g[3] + 2));
            }
            if (!nKyoten.isEmpty()) {
                kyoten = nKyoten;
            }
            if (nPlant != null) {
                plant = nPlant;
            }
            mise = nMise;
            uru = nUru;
            ginko = nGinko;
            gacha = nGacha;
            beacon = nBeacon;
            beaconChuo = nBeaconChuo;
            shinko = nShinko;
            kamado = nKamado;
            juki = nJuki;
        }

        return new Kekka(nMise.size(), nUru.size(), nGinko.size(), nGacha.size(),
                nBeacon.size() + nBeaconChuo.size(),
                markerKazu, Bukkit.getWorlds().size(), forceChunk, karappo,
                nShinko.size(), nKamado.size(), nJuki.size());
    }

    /**
     * そのビーコンを、ある勢力の拠点の目印として覚える。
     * 既に覚えていれば上書きする（登録し直せるように）。
     */
    public void beaconKuniTouroku(String kagi, String team) {
        beaconKuni.removeIf(x -> x.startsWith(kagi + "="));
        beaconKuni.add(kagi + "=" + team);
    }

    /**
     * そのブロックが、どの勢力のビーコン（本体・土台・ガラス）か。
     * どの勢力のものでもなければ null。
     *
     * ★ beaconNoIchibu と同じ範囲を見る。土台を抜かれてもビームが
     *   消えるので、本体だけ守っても意味がないのと同じ理屈。
     */
    public String beaconNoKuni(Block block) {
        for (String x : beaconKuni) {
            int i = x.lastIndexOf('=');
            if (i < 0) {
                continue;
            }
            if (kagiNiFukumu(x.substring(0, i), block)) {
                return x.substring(i + 1);
            }
        }
        return null;
    }

    /** 「その鍵のビーコンの一部か」を1件ぶん見る。 */
    private boolean kagiNiFukumu(String kagi, Block block) {
        String[] bu = kagi.split(",");
        if (bu.length != 4 || !bu[0].equals(block.getWorld().getName())) {
            return false;
        }
        int x = Integer.parseInt(bu[1]);
        int y = Integer.parseInt(bu[2]);
        int z = Integer.parseInt(bu[3]);
        if (block.getX() == x && block.getY() == y && block.getZ() == z) {
            return true;                       // 本体
        }
        if (block.getX() == x && block.getY() == y + 1 && block.getZ() == z) {
            return true;                       // 上のガラス
        }
        return block.getY() == y - 1
                && Math.abs(block.getX() - x) <= 1
                && Math.abs(block.getZ() - z) <= 1;   // 下の土台 3x3
    }

    /**
     * マーカーの付いているタグから種別を決める。
     * 対応表に無いタグ（石油プラント等）は null を返して無視する。
     *
     * ★ jidai_uru_chuo は jidai_uru を含む文字列ではないので、
     *   前方一致ではなく「完全に一致するか」で見ている。
     */
    private String tagKara(Set<String> tags) {
        for (String[] gyou : TAG) {
            if (tags.contains(gyou[0])) {
                return gyou[1];
            }
        }
        return null;
    }

        /** 種別に対して、そこに在るはずのブロックの日本語名。メッセージ用。 */
    public static String blockMei(String shurui) {
        if (shurui.equals(GINKO)) {
            return "金ブロック";
        }
        if (shurui.equals(GACHA)) {
            return "ダイヤブロック";
        }
        if (shurui.equals(BEACON) || shurui.equals(BEACON_CHUO)) {
            return "ビーコン";
        }
        if (shurui.equals(SHINKO)) {
            return "鉄ブロック";
        }
        if (shurui.equals(KAMADO)) {
            return "かまど";
        }
        return "エメラルドブロック";
    }

    /** 種別の日本語名。 */
    public static String shuruiMei(String shurui) {
        if (shurui.equals(URU)) {
            return "売却所";
        }
        if (shurui.equals(BEACON)) {
            return "目印";
        }
        if (shurui.equals(BEACON_CHUO)) {
            return "中央の目印";
        }
        if (shurui.equals(GINKO)) {
            return "銀行";
        }
        if (shurui.equals(JUKI)) {
            return "銃器専門店";
        }
        if (shurui.equals(GACHA)) {
            return "ガチャ";
        }
        if (shurui.equals(SHINKO)) {
            return "時代を進める";
        }
        if (shurui.equals(KAMADO)) {
            return "エンダーかまど";
        }
        return "販売所";
    }

    /**
     * エンダーかまどに、エンダーチェストと同じ粒を出す。
     *
     * ★ 見た目だけの処理。普通のかまどと見分けが付かないと、
     *   「自分専用」だと気づいてもらえないため。
     *
     * ★ 1秒に1回呼ばれる前提。毎tick出すと粒が濃すぎるし、
     *   50人ぶんの拠点で無駄に重くなる。
     */
    public void kamadoNoTsubu() {
        for (String k : ichiran(KAMADO)) {
            String[] bu = k.split(",");
            if (bu.length != 4) {
                continue;
            }
            World w = Bukkit.getWorld(bu[0]);
            if (w == null) {
                continue;
            }
            double x = Integer.parseInt(bu[1]) + 0.5;
            double y = Integer.parseInt(bu[2]) + 1.1;
            double z = Integer.parseInt(bu[3]) + 0.5;
            // PORTAL がエンダーチェストの粒。数・広がり・速さの順に渡す。
            w.spawnParticle(org.bukkit.Particle.PORTAL, x, y, z, 12, 0.3, 0.3, 0.3, 0.4);
        }
    }

    // --- 浮遊テキスト（拠点の可視化） -----------------------------

    /**
     * プラグインが置いた浮遊テキストに付ける印。
     * 置き直す時に、自分が置いた分だけを消すために使う。
     */
    private static final String FUYU_TAG = "jidai_fuyu";

    /**
     * 施設の上に、名前の浮遊テキストを置き直す。置いた数を返す。
     *
     * ★ 視聴者向けの機能。拠点が5つあると、配信を見ている人には
     *   どれが何の建物か分からない。名前が浮いていれば一目で分かる。
     *
     * ★ 毎tick の処理は一切していない。**置いたら終わり**で、あとは
     *   クライアントが表示するだけ。だから数が増えても TPS に響かない。
     *   （常時パーティクルを使わないのはこのため。あれは毎回サーバーが送る）
     *
     * ★ 置き直す前に、前に自分が置いた分を必ず消す。
     *   消さないと、走査のたびに二重三重に重なる。
     */
    public int fuyuText() {
        // 前に置いた分を消す
        for (World w : Bukkit.getWorlds()) {
            for (TextDisplay d : w.getEntitiesByClass(TextDisplay.class)) {
                if (d.getScoreboardTags().contains(FUYU_TAG)) {
                    d.remove();
                }
            }
        }

        int oita = 0;
        for (String shu : zenShurui()) {
            for (String k : ichiran(shu)) {
                String[] bu = k.split(",");
                if (bu.length != 4) {
                    continue;
                }
                World w = Bukkit.getWorld(bu[0]);
                if (w == null) {
                    continue;
                }
                Location l = new Location(w,
                        Integer.parseInt(bu[1]) + 0.5,
                        Integer.parseInt(bu[2]) + 1.4,
                        Integer.parseInt(bu[3]) + 0.5);

                TextDisplay d = w.spawn(l, TextDisplay.class);
                d.setText(iro(shu) + Enshutsu.MODOSU + shuruiMei(shu));
                // どこから見ても正面を向く。看板のように裏返らない
                d.setBillboard(Display.Billboard.CENTER);
                d.setSeeThrough(false);
                d.addScoreboardTag(FUYU_TAG);
                oita++;
            }
        }
        return oita;
    }

    /**
     * 種別ごとの文字色。視聴者が色で見分けられるようにする。
     * ★ 「§」の後ろの1文字が色。Adventure を外したので数字と文字で書く。
     */
    private static String iro(String shurui) {
        if (shurui.equals(URU)) {
            return "§a";      // 緑
        }
        if (shurui.equals(GINKO)) {
            return "§e";      // 黄
        }
        if (shurui.equals(GACHA)) {
            return "§d";      // 明るい紫
        }
        return "§b";          // 水色
    }

    // --- ビーコン（目印） ------------------------------------------

    /**
     * ビーコンとして守る範囲かどうか。
     *
     * ★ マーカーはビーコン1つぶんしか無いが、守るのは11ブロック。
     *     ビーコンそのもの (x, y, z)
     *     その上のガラス   (x, y+1, z)
     *     下の鉄ブロック3×3 (x±1, y-1, z±1)
     *   土台を1つ抜かれるとビームが消えるので、基盤ごと守る必要がある。
     */
    public boolean beaconNoIchibu(Block block) {
        for (String shu : new String[]{BEACON, BEACON_CHUO}) {
            for (String k : ichiran(shu)) {
                String[] bu = k.split(",");
                if (bu.length != 4 || !bu[0].equals(block.getWorld().getName())) {
                    continue;
                }
                int x = Integer.parseInt(bu[1]);
                int y = Integer.parseInt(bu[2]);
                int z = Integer.parseInt(bu[3]);

                if (block.getX() == x && block.getY() == y && block.getZ() == z) {
                    return true;      // ビーコン本体
                }
                if (block.getX() == x && block.getY() == y + 1 && block.getZ() == z) {
                    return true;      // 上のガラス
                }
                if (block.getY() == y - 1
                        && Math.abs(block.getX() - x) <= 1
                        && Math.abs(block.getZ() - z) <= 1) {
                    return true;      // 下の鉄ブロック3×3
                }
            }
        }
        return false;
    }

    /**
     * 登録済みのビーコンから、選ばれている効果を消す。
     *
     * ★★ ビーコンの効果は絶対に付けさせない ★★
     *   レベル1でも範囲20の採掘速度上昇が付き、
     *   「1分に20個」を前提にした経済が崩れる。
     *   右クリックは JidaiCraft 側で打ち切っているので普通は選べないが、
     *   コマンドやデータパックで設定された場合の保険として、ここでも消す。
     *   ★ 効果が無くてもビーム自体は出る。目印としては何も困らない。
     */
    public int koukaKesu() {
        int keshita = 0;
        for (String shu : new String[]{BEACON, BEACON_CHUO}) {
            for (String k : ichiran(shu)) {
                Block b = blockKara(k);
                if (b == null || b.getType() != Material.BEACON) {
                    continue;
                }
                if (!(b.getState() instanceof Beacon bc)) {
                    continue;
                }
                if (bc.getPrimaryEffect() == null && bc.getSecondaryEffect() == null) {
                    continue;
                }
                bc.setPrimaryEffect(null);
                bc.setSecondaryEffect(null);
                bc.update();
                keshita++;
            }
        }
        return keshita;
    }

    /**
     * 中央のビーコンの上のガラスを、中央の時代の色に塗り替える。塗った数を返す。
     *
     * ★ 中央プラントの建築とは無関係に動く。建物が無くても色は変わる。
     *
     *   鉄器 = 白 / 中世 = 黄 / 近代 = 橙 / 現代 = 赤
     */
    public int chuoIro(int jidai) {
        Material iro = jidai >= 4 ? Material.RED_STAINED_GLASS
                : jidai == 3 ? Material.ORANGE_STAINED_GLASS
                : jidai == 2 ? Material.YELLOW_STAINED_GLASS
                : Material.WHITE_STAINED_GLASS;

        int nutta = 0;
        for (String k : ichiran(BEACON_CHUO)) {
            Block b = blockKara(k);
            if (b == null) {
                continue;
            }
            Block ue = b.getWorld().getBlockAt(b.getX(), b.getY() + 1, b.getZ());
            if (ue.getType() == iro) {
                continue;   // もうその色
            }
            // ★ 空気か、色ガラスの時だけ塗る。
            //   関係ないブロックがあった時に潰さないため。
            if (ue.getType() != Material.AIR && !ue.getType().name().endsWith("_STAINED_GLASS")) {
                continue;
            }
            ue.setType(iro);
            nutta++;
        }
        return nutta;
    }

    /** "world,x,y,z" からブロックを取り出す。取れなければ null。 */
    private Block blockKara(String k) {
        String[] bu = k.split(",");
        if (bu.length != 4) {
            return null;
        }
        World w = Bukkit.getWorld(bu[0]);
        if (w == null) {
            return null;
        }
        return w.getBlockAt(Integer.parseInt(bu[1]),
                Integer.parseInt(bu[2]), Integer.parseInt(bu[3]));
    }

    /**
     * 走査した座標に、本当にそのブロックが在るかを数える。
     * 「マーカーはあるがブロックが無い」を運営が気づけるようにするため。
     */
    public int zureNoKazu() {
        int zure = 0;
        for (String[] gyou : BLOCK) {
            for (String k : ichiran(gyou[0])) {
                String[] bu = k.split(",");
                if (bu.length != 4) {
                    continue;
                }
                World w = Bukkit.getWorld(bu[0]);
                if (w == null) {
                    zure++;
                    continue;
                }
                Block b = w.getBlockAt(Integer.parseInt(bu[1]),
                        Integer.parseInt(bu[2]), Integer.parseInt(bu[3]));
                if (b.getType() != Material.valueOf(gyou[1])) {
                    zure++;
                }
            }
        }
        return zure;
    }
}
