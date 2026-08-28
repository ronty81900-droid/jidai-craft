// =============================================================
// Enshutsu.java ── 見せ方（音・通知の書式・時代進行の演出）
//
//   判断の基準は「配信画面で、見ている人に何が起きたか伝わるか」。
//   遊ぶ人の便利さではなく、**視聴者の理解**を優先している。
//
//   ★ 音のIDは、このファイルの上のほうに全部まとめてある。
//     後で差し替えたくなったら、そこだけ直せばよい。
//     バニラに元からある音しか使っていない（配布物が要らない）。
// =============================================================

package jidai;

import java.time.Duration;
import java.util.List;


import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

/**
 * 演出の部品置き場。音を鳴らす・通知を整える・時代進行を見せる。
 */
public final class Enshutsu {

    // =========================================================
    //  ★ 音のID（差し替えるならここだけ）
    // =========================================================

    /** ガチャが回っている間、1コマごとに鳴る短い音 */
    public static final String OTO_KAITEN = "ui.button.click";

    /** 止まった時の音（等級ごと） */
    public static final String OTO_HAZURE = "block.note_block.bass";
    public static final String OTO_SHO = "entity.item.pickup";
    public static final String OTO_CHU = "entity.player.levelup";
    public static final String OTO_DAI = "ui.toast.challenge_complete";

    /** 中央の時代が進んだ瞬間。重い音を全員に */
    public static final String OTO_JIDAI = "entity.ender_dragon.growl";

    /** 断られた時 */
    public static final String OTO_DAME = "entity.villager.no";

    /** 十連で「中」をめくった時の、軽い当たりの音 */
    public static final String OTO_MEKURI_CHU = "entity.experience_orb.pickup";

    /** 運営がゲームを止めた時。ビーコンが消える音 */
    public static final String OTO_TEISHI = "block.beacon.deactivate";

    // =========================================================
    //  通知の書式
    // =========================================================

    /**
     * 通知の種別。★ 記号と色はこの下の kazaru() だけで決める。
     * 視聴者が画面を見て「何の通知か」を色と記号で見分けられるようにするため。
     */
    // =========================================================
    //  色の付け方
    // =========================================================
    //
    // ★★ 2026-08-20 に Adventure(Paper 専用) から変えました ★★
    //   MOD と一緒に動かすため、土台を Paper から Arclight(ハイブリッド)へ
    //   移します。Arclight は Bukkit/Spigot の API しか持っておらず、
    //   Adventure の Component が使えません(実測で確認)。
    //
    //   代わりに Minecraft が昔から持っている「色コード」を使います。
    //   「§」(セクション記号)の後ろの1文字で、色や太字が決まります。
    //   見た目は Component の時と同じです。
    //
    //   ★ この定数を使わずに直接 "§7" と書かないこと。
    //     どの色がどこで使われているかが追えなくなります。

    /** 灰色。ふつうの出来事 */
    private static final String HAI = "§7";

    /** 水色。納金 */
    private static final String MIZU = "§b";

    /** 濃い赤。戦争宣誓と下剋上だけ */
    private static final String KOI_AKA = "§4";

    /** 金色。大当たり */
    private static final String KIN = "§6";

    /** 黄色。時代 */
    private static final String KI = "§e";

    /** 白。本文 */
    private static final String SHIRO = "§f";

    /** 太字にする */
    private static final String FUTOJI = "§l";

    /**
     * 飾りを元に戻す。
     * ★ アイテムの名前と説明は、何もしないと【斜体】で出ます。
     *   これを頭に付けると、まっすぐな字になります。
     */
    // =========================================================
    //  画面の見出し（リソースパックの絵と差し替える）
    // =========================================================
    //
    // ★★ 仕組み ★★
    //   リソースパックが、既定フォントに「絵そのもの」の文字を足している。
    //   その文字を GUI のタイトルに入れると、画面いっぱいに絵が出る。
    //   だから、こちらは【ただの文字列】を渡すだけでよい。
    //
    //   UI_MODOSU … 左へ 8px 戻す文字（絵の左端を画面の左端に合わせる）
    //   UI_<画面> … その画面の絵
    //
    // ★ 文字の割り当ては、リソースパックの title_map.json と同じ。
    //   4案どれを入れても同じ文字なので、案を選び直しても
    //   ここを直す必要はない。
    //
    // ★ 白・太字なし・斜体なしで出すこと。色を付けると絵が染まる。
    //   §f がその指定。

    private static final String UI_MODOSU = "§f\uf801";

    public static final String UI_GACHA   = UI_MODOSU + "\uede1";
    public static final String UI_MISE    = UI_MODOSU + "\uede2";
    /** 販売所のタブ2枚目（防具）。タブごとに絵を変えて、選択中を示す。 */
    public static final String UI_MISE_2  = UI_MODOSU + "\uede7";
    public static final String UI_SHINKO  = UI_MODOSU + "\uede3";
    public static final String UI_GINKO   = UI_MODOSU + "\uede4";
    public static final String UI_JUKI    = UI_MODOSU + "\uede5";
    /** 銃器専門店のタブ2枚目（弾）。 */
    public static final String UI_JUKI_2  = UI_MODOSU + "\uede8";
    public static final String UI_SENSEN  = UI_MODOSU + "\uede6";

    /**
     * リソースパックの絵を使うか。
     *
     * ★★ 既定は false（ふつうの文字） ★★
     *   パックを配る前に true にすると、参加者の画面では
     *   タイトルが「□□」に見える。入れ終わってから jidai ui on で切り替える。
     *   ★ 保存はしない。再起動すると false に戻る。
     *     配り終えたら、この既定値そのものを true に変える。
     */
    private static boolean uiPack = false;

    public static boolean uiPackKa() {
        return uiPack;
    }

    public static void uiPackSet(boolean tsukau) {
        uiPack = tsukau;
    }

    /**
     * 画面の見出しを返す。
     *   e   … リソースパックの絵の文字（UI_GACHA など）
     *   moji… パックを使わない時に出す ふつうの名前
     */
    public static String gamenMei(String e, String moji) {
        return uiPack ? e : MODOSU + moji;
    }

    public static final String MODOSU = "§r";

    public static final String KOUNYU = "購入";

    /**
     * ガチャのふつうの結果。
     *
     * ★★ なぜ足したか（2026-08-22）★★
     *   それまでガチャは何が出ても ATARI（大当たり）の見出しで知らせていた。
     *   丸石が出ても「大当たり」と出るので、言葉が意味を失っていた。
     *   ふつうの結果はこちら、大当たりはお金と特殊アイテムだけ、と分けた。
     *   kazaru() に分岐を足していないので、既定の 灰色・● が付く。
     */
    public static final String GACHA = "ガチャ";
    public static final String NOKIN = "納金";
    public static final String SENSO = "戦争宣誓";
    public static final String GEKOKUJO = "下剋上";
    public static final String ATARI = "大当たり";
    public static final String JIDAI = "時代";

    /**
     * 通知を1つの形に整える。
     *
     *   ● 購入      灰   ふつうの出来事
     *   ◆ 納金      水色 ふつうの出来事
     *   ✖ 戦争宣誓  濃赤 ★他と明確に変える。事故ではなく戦犯として残す
     *   ✖ 下剋上    濃赤 ★同上
     *   ★ 大当たり  金
     *   ■ 時代      黄
     */
    public static String kazaru(String shurui, String honbun) {
        String kigou = "●";
        String iro = HAI;
        String futoji = "";

        if (shurui.equals(NOKIN)) {
            kigou = "◆";
            iro = MIZU;
        } else if (shurui.equals(SENSO) || shurui.equals(GEKOKUJO)) {
            // ★ 戦争宣誓と下剋上だけ、はっきり違う色にする
            kigou = "✖";
            iro = KOI_AKA;
            futoji = FUTOJI;
        } else if (shurui.equals(ATARI)) {
            kigou = "★";
            iro = KIN;
            futoji = FUTOJI;
        } else if (shurui.equals(JIDAI)) {
            kigou = "■";
            iro = KI;
            futoji = FUTOJI;
        }

        // 頭は色つき(必要なら太字)、本文は白。MODOSU で太字を切ってから白にする。
        return iro + futoji + kigou + " [" + shurui + "] " + MODOSU + SHIRO + honbun;
    }

    // =========================================================
    //  音と見た目の部品
    // =========================================================

    /** 1人に音を鳴らす。 */
    public static void oto(Player player, String otoId) {
        player.playSound(player.getLocation(), otoId, SoundCategory.MASTER, 1.0f, 1.0f);
    }

    /** 1人に音を鳴らす（高さを変える。回転音の変化に使う）。 */
    public static void oto(Player player, String otoId, float takasa) {
        player.playSound(player.getLocation(), otoId, SoundCategory.MASTER, 0.7f, takasa);
    }

    /** サーバー全員に音を鳴らす。 */
    public static void otoZenin(String otoId) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), otoId, SoundCategory.MASTER, 1.0f, 1.0f);
        }
    }

    /** 全員に同じ通知を送る。 */
    public static void zeninTsuchi(String moji) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(moji);
        }
    }

    /**
     * その人の頭上に花火を1発上げる。大当たりの合図。
     * ★ 花火は「勝手に爆発するアイテム」なので、置くだけでよい。
     */
    public static void hanabi(Player player) {
        hanabi(player.getWorld(), player.getLocation().add(0, 1, 0), Color.YELLOW, Color.ORANGE);
    }

    /**
     * 決まった場所で、花火を何発も上げる（1発ずつ間を空けて）。
     * ★ 制圧（拠点を落とした）の演出に使う。場所は落ちた拠点のビーコン。
     */
    public static void hanabiRenpatsu(JidaiCraft plugin, org.bukkit.Location basho, int hassu,
                                      int kankakuTick, Color a, Color b) {
        for (int i = 0; i < hassu; i++) {
            double zure = (i % 2 == 0 ? 1 : -1) * (i + 1) * 0.8;
            org.bukkit.Location ten = basho.clone().add(zure, 1, (i % 3 - 1) * 1.2);
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> hanabi(ten.getWorld(), ten, a, b), (long) i * kankakuTick);
        }
    }

    private static void hanabi(World w, org.bukkit.Location ten, Color a, Color b) {
        Firework f = w.spawn(ten, Firework.class);
        FireworkMeta m = f.getFireworkMeta();
        m.addEffect(FireworkEffect.builder()
                .withColor(a, b)
                .with(FireworkEffect.Type.BALL_LARGE)
                .withTrail()
                .build());
        m.setPower(1);
        f.setFireworkMeta(m);
    }

    // =========================================================
    //  時代進行の演出（この企画で最も大事な瞬間）
    // =========================================================

    private final JidaiCraft plugin;
    private final Kane kane;

    /**
     * 前回見た中央の時代。これと違っていたら「進んだ」と判断する。
     * ★ 起動時に今の値で埋める。埋めないと、起動のたびに演出が出てしまう。
     */
    private int maeNoJidai;

    public Enshutsu(JidaiCraft plugin, Kane kane) {
        this.plugin = plugin;
        this.kane = kane;
        this.maeNoJidai = kane.chuoJidai();
    }

    /**
     * 1秒に1回、サーバーから呼ばれる。中央の時代が進んでいたら演出を出す。
     *
     * ★ 進行そのものはデータパックの担当。プラグインは「変わったこと」を
     *   見つけて、視聴者に分かる形で見せるだけ。判定には一切関わらない。
     */
    public void byoumai() {
        int ima = kane.chuoJidai();
        if (ima <= maeNoJidai) {
            return;   // 変わっていない（下がることは無いが、念のため <= で見る）
        }
        maeNoJidai = ima;
        jidaiShinko(ima);
    }

    /** 中央の時代が進んだ瞬間に、全員へ見せる。 */
    private void jidaiShinko(int jidai) {
        String mei = Kane.jidaiMei(jidai);

        // (1) 画面中央に大きく出す
        // ★ 時間の単位は tick(20分の1秒)。0.4秒で出て、2.6秒とどまり、0.8秒で消える。
        //   8 / 52 / 16 tick がそれにあたる。
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(KIN + FUTOJI + "時代が進んだ",
                    KI + "中央は " + mei + " へ", 8, 52, 16);
        }

        // (2) 全サーバーに重い音
        otoZenin(OTO_JIDAI);

        // (3) 全体チャット。何が解禁されたかを名指しする
        zeninTsuchi(kazaru(JIDAI, "中央が " + mei + " へ移った"));
        // ★★ ここでは入荷を知らせない (2026-08-20) ★★
        //   販売所の解禁は【自分の勢力の時代】で決まるようになった。
        //   中央が進んだ時に「入荷しました」と流すと、
        //   まだその時代に達していない勢力にも「買える」と誤解させる。
        //   入荷の知らせは、その勢力が時代を進めた時に
        //   データパック(jidai:shinko/shounin_*)が出している。

        // (4) 中央のビーコンのガラスを、その時代の色に塗り替える
        //     ★ 中央プラントの建築とは無関係。建物が無くても色は変わる。
        int nutta = plugin.chuoIroNuru(jidai);
        if (nutta > 0) {
            plugin.getLogger().info("中央のビーコンの色を " + mei + " に変えました");
        }

        // (5) 中央プラントのまわりにパーティクル
        //     ★ 常時は出さない。この瞬間だけ。重くなるし映像も濁るため。
        chuoParticle();
    }

    /**
     * 中央プラントの位置にパーティクルを出す。
     * 位置はデータパックのマーカー（jidai_plant）から取る。
     * 見つからなければ何もしない。
     */
    private void chuoParticle() {
        for (World w : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Marker m : w.getEntitiesByClass(org.bukkit.entity.Marker.class)) {
                if (!m.getScoreboardTags().contains("jidai_plant")) {
                    continue;
                }
                Location l = m.getLocation().add(0, 2, 0);
                w.spawnParticle(Particle.END_ROD, l, 120, 3.0, 3.0, 3.0, 0.08);
                w.spawnParticle(Particle.FLASH, l, 6, 1.0, 1.0, 1.0, 0.0);
            }
        }
    }

    /** 検証から今の記憶値を確かめるため。 */
    public int maeNoJidai() {
        return maeNoJidai;
    }
}
