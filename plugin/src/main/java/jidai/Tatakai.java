// =============================================================
// Tatakai.java ── 人を殴った時・殺した時の決まり（2026-08-24 のご指示）
//
//   「同じ勢力の人間は殺せないようなコマンドを組んでください。
//     そして、敵勢力を倒すと相手の所持金の10％奪える
//     （勢力貯金からは取れないようにしてください。）感じでお願いします。

//
//   決まりは2つだけ。
//     1) 同じ【勢力】に属する2人は、傷つけ合えない
//     2) 人を殺したら、相手の【個人の金】の10% をもらう
//
//   ★★ 勢力に入っていない人（運営・見学者）は素通りする ★★
//     「同じ勢力か」で見ているので、勢力を持たない者は守られない。
// =============================================================

package jidai;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;

/**
 * 同士討ちの禁止と、殺した時の分捕りを預かる係。
 *
 * ★ このクラスはスコアボードの名前を1つも持たない。金の出し入れは Kane に任せる。
 */
public final class Tatakai {

    /**
     * 殺した時に奪える割合（百分率）。
     *
     * ★ 奪うのは【個人の金】だけ。勢力の貯金には指1本 触れない（ご指示）。
     *   貯金から取れると、1人を狩り続けるだけで勢力の時代進行を止められてしまう。
     */
    static final int UBAU_WARIAI = 10;

    private final JidaiCraft plugin;

    public Tatakai(JidaiCraft plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    //  1) 同じ勢力の人間は傷つけ合えない
    // =========================================================

    /**
     * 傷つけようとしている2人が、同じ勢力か。
     *
     * ★★ 「同じチーム」ではなく「同じ【勢力】」で見る ★★
     *   チーム名が同じでも、5勢力のどれかでなければ守らない。
     *
     * ★ どちらかが勢力に居なければ false（＝傷つけられる）。
     */
    boolean onajiSeiryokuKa(Player a, Player b, Kane kane) {
        if (a == null || b == null || a.getName().equals(b.getName())) {
            return false;                    // 自分自身は対象外（自爆などを止めない）
        }
        String ta = kane.teamMei(a);
        String tb = kane.teamMei(b);
        return ta != null && ta.equals(tb) && Seiryoku.seiryokuKa(ta);
    }

    /**
     * 殴った側の「人」を取り出す。
     *
     * ★ 矢や弾で撃った場合、殴った物は【飛び道具】であって人ではない。
     *   撃った人（shooter）まで辿らないと、味方への誤射が素通りする。
     * ★ TaCZ の弾がここに来るかは実機でしか分からない。
     *   来なくても、チームの friendlyFire=false（データパック側）が保険になる。
     */
    static Player utta(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile ya) {
            ProjectileSource moto = ya.getShooter();
            if (moto instanceof Player p) {
                return p;
            }
        }
        return null;
    }

    /**
     * 同じ勢力なら傷つけない。止めたら true（呼び出し側が打ち切る）。
     *
     * ★ 知らせは殴った側にだけ、しかも連打で埋まらないよう1秒に1回だけ出す。
     */
    boolean fusegu(org.bukkit.entity.Entity damager, org.bukkit.entity.Entity ukeru, Kane kane) {
        if (!(ukeru instanceof Player uke)) {
            return false;
        }
        Player utsu = utta(damager);
        if (utsu == null || !onajiSeiryokuKa(utsu, uke, kane)) {
            return false;
        }
        long ima = System.currentTimeMillis();
        Long mae = shirase.get(utsu.getName());
        if (mae == null || ima - mae > 1000) {
            shirase.put(utsu.getName(), ima);
            utsu.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO,
                    "同じ勢力の仲間は傷つけられません（" + Seiryoku.mei(kane.teamMei(utsu)) + "）"));
            Enshutsu.oto(utsu, Enshutsu.OTO_DAME);
        }
        return true;
    }

    /** 「同じ勢力です」の知らせを出した時刻。連打で画面が埋まらないように。 */
    private final java.util.Map<String, Long> shirase = new java.util.HashMap<>();

    // =========================================================
    //  2) 殺したら、相手の個人の金の 10% をもらう
    // =========================================================

    /**
     * 人が死んだ。殺したのが人なら、その人へ【個人の金】の10%を移す。
     *
     * ★★ 勢力の貯金からは1円も取らない（ご指示）★★
     *   取れるようにすると、1人を狩り続けるだけで相手の時代進行を止められる。
     *
     * ★ 同じ勢力どうしは そもそも傷つけ合えないが、
     *   落下などで殺した扱いになる筋が残るので、ここでも一応 見る。
     *
     * ★ 端数は切り捨て。0 なら何も起きないし、何も知らせない
     *   （0円の人を殺すたびに全員へ通知が飛ぶと、うるさいだけ）。
     */
    void koroshita(Player shinda, Player koroshita, Kane kane) {
        if (koroshita == null || !kane.junbiOK()) {
            return;
        }
        if (shinda.getName().equals(koroshita.getName())) {
            return;                          // 自滅では奪えない
        }
        if (onajiSeiryokuKa(shinda, koroshita, kane)) {
            return;                          // 味方からは奪わない
        }

        int motte = kane.kojinZandaka(shinda);
        int ubau = motte * UBAU_WARIAI / 100;    // 切り捨て
        if (ubau <= 0) {
            return;
        }

        kane.kojinKousin(shinda, motte - ubau);
        int mae = kane.kojinZandaka(koroshita);
        kane.kojinKousin(koroshita, mae + ubau);

        koroshita.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO,
                shinda.getName() + " を倒して " + ubau + " を奪った（個人の金 "
                        + mae + " → " + (mae + ubau) + "）"));
        Enshutsu.oto(koroshita, Enshutsu.OTO_CHU);
        shinda.sendMessage(Enshutsu.kazaru(Enshutsu.SENSO,
                "§c" + koroshita.getName() + " に " + ubau + " を奪われた（個人の金 "
                        + motte + " → " + (motte - ubau) + "）"));
        Enshutsu.oto(shinda, Enshutsu.OTO_DAME);

        // ★ 全体へは出さない。戦闘は頻繁に起きるので、出すとチャットが流れて
        //   略奪や時代進行の知らせが読めなくなる。
        plugin.getLogger().info("討ち取り: " + koroshita.getName() + " ← "
                + shinda.getName() + " (" + ubau + ")");
    }

    /** 運営が数字を確かめる用。 */
    static int ubauGaku(int motteru) {
        return Math.max(0, motteru) * UBAU_WARIAI / 100;
    }
}
