// =============================================================
// Ibutsu.java ── 遺物（効果のある特殊アイテム）
//
//   ★★ 何者か（2026-08-22 のご指示）★★
//     ガチャから出る特殊アイテムは2系統ある。
//       ・集める5種 … 5種そろえると勝ち（Shouri.TOKUSHU_HYOU）
//       ・遺物 11種  … 手に入れた【勢力】に、戦争やゲームで有利な効果が付く（Shouri.IBUTSU_HYOU）
//     ここは遺物の「効果」を担当する。名前・絵・番号の表は Shouri が正本。
//
//   ★★ 効果は「勢力」に付く。紙そのものには付かない ★★
//     手に入れた時点で勢力の記録に残り、以後その勢力の人 全員に効く（入り直しても効く）。
//     紙は戦利品。奪われても効果は移らない。
//
//   ★ 記録は Gacha の「勢力ごとに各1個」の記録（config の gacha_tokushu）をそのまま写す。
//     別の台帳を持つと、片方だけ消した時にズレる。
//
//   効果の出し方（種類ごと）
//     SPEED / HASTE / NIGHT … ポーション効果を毎秒かけ直す（スピードI = +20% がちょうど 1.2倍）
//     STRENGTH             … 攻撃力の属性に「×1.10」の修正子を付ける（ポーションだと +3 固定で 10% にならない）
//     RESISTANCE           … 受けるダメージのイベントで ×0.85（ポーションの耐性I は −20% で合わない）
//     REGEN                … 回復のイベントで ×1.2（自然回復だけ）
//     HORI                 … 石を掘った時、20% で貴金属の「おまけの抽選」をもう1回（期待値 1.2倍）
//     URI / JUKI / NEBIKI / JOHO … それぞれ銀行・銃器専門店・/joho が聞きに来る
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class Ibutsu {

    private Ibutsu() {
        // 静かな台帳なので、実体は作らない
    }

    // ── 効果の種類（Shouri.IBUTSU_HYOU の5列目）──
    public static final String SPEED = "SPEED";           // 移動速度 1.2倍
    public static final String HORI = "HORI";             // 貴金属が出る確率 1.2倍
    public static final String STRENGTH = "STRENGTH";     // 近接の攻撃力 +10%
    public static final String RESISTANCE = "RESISTANCE"; // 被ダメージ −15%
    public static final String JUKI = "JUKI";             // 銃器専門店の解放 10,000 → 5,500
    public static final String HASTE = "HASTE";           // 採掘速度
    public static final String URI = "URI";               // 貴金属の売値 1.2倍
    public static final String NEBIKI = "NEBIKI";         // 銃器専門店で 10% 戻る
    public static final String REGEN = "REGEN";           // 自然回復 +20%
    public static final String NIGHT = "NIGHT";           // 暗視
    public static final String JOHO = "JOHO";             // 他勢力の貯金と石油が見える

    // ── 数字はここだけ ──
    /** ポーション効果を毎秒かけ直す時の長さ（tick）。1秒ごとに 3秒ぶん入れるので途切れない。 */
    static final int NAGASA = 60;

    /**
     * 暗視だけの長さ（tick）。**30秒**（2026-08-26 のご指示・必須）。
     *
     * ★★ なぜ暗視だけ長いか ★★
     *   Minecraft の暗視は【残り10秒を切ると画面が点滅する】。
     *   ほかの効果と同じ3秒だと、毎秒かけ直しても常に点滅域にいて、
     *   ずっとチカチカして見づらい（実機の指摘）。
     *   30秒 入れておけば点滅域に入らず、毎秒かけ直すので減りもしない。
     *
     * ★ 代わりに、リセットの後 消えるまで最大30秒かかる（ほかは3秒）。
     *   点滅の見づらさより、そちらの方がましという判断。
     */
    static final int NAGASA_ANSHI = 600;

    /** その効果を何 tick 入れるか。★ 暗視だけ長い（点滅させないため）。 */
    static int nagasa(String kouka) {
        return NIGHT.equals(kouka) ? NAGASA_ANSHI : NAGASA;
    }
    /** 銃器専門店の解放に要る貯金。手形があればこれ。 */
    static final int JUKI_SHIKII_TEGATA = 5500;
    /** 貴金属の売値の倍率（百分率）。金庫があれば 120。 */
    static final int URI_BAIRITSU_KINKO = 120;
    /** 銃器専門店で戻る割合（百分率）。手引書があれば 10。 */
    static final int NEBIKI_WARI = 10;
    /** 近接の攻撃力に足す割合。0.10 = +10%。 */
    static final double KOUGEKI_WARI = 0.10;
    /** 受けるダメージに掛ける。0.85 = −15%。 */
    static final double BOUGYO_WARI = 0.85;
    /** 自然回復に掛ける。1.2 = +20%。 */
    static final double KAIFUKU_BAI = 1.2;
    /** 鉄王冠: おまけの抽選をする確率（百分率）。20 なら期待値が 1.2倍。 */
    static final int HORI_OMAKE_WARI = 20;
    /** 深層岩の倍率（tests/horu_tsukuru.py と同じ）。 */
    static final double HORI_SHINSO_BAI = 1.5;

    /**
     * 鉄王冠の「おまけの抽選」に使う表（百分率）。
     * ★★ データパックの stone.json と同じ値でなければならない ★★
     *   正本は tests/horu_tsukuru.py の HYOU。IbutsuTest がそれを読んで突き合わせる。
     */
    static final Object[][] HORI_HYOU = {
            {Material.IRON_INGOT, 15},
            {Material.LAPIS_LAZULI, 8},
            {Material.GOLD_INGOT, 4},
            {Material.DIAMOND, 1},
    };

    /** 攻撃力の修正子の名札。同じ物を二重に付けない／消す時に見分けるために固定する。 */
    static final String KOUGEKI_FUDA = "jidai_excalibur";
    private static final UUID KOUGEKI_UUID = UUID.fromString("7d5c2a6e-1f3b-4c9a-9e2d-0b8a6c4e2f11");

    /** 勢力名 → 持っている遺物の名前。★ Gacha の記録の写し。 */
    private static final Map<String, Set<String>> MOTTERU = new HashMap<>();

    // =========================================================
    //  台帳
    // =========================================================

    /** Gacha の記録から写す（起動時と、記録が変わった時）。 */
    static void yomikomi(Map<String, Set<String>> kiroku) {
        MOTTERU.clear();
        for (Map.Entry<String, Set<String>> e : kiroku.entrySet()) {
            for (String na : e.getValue()) {
                kiroku(e.getKey(), na);
            }
        }
    }

    /** 勢力が遺物を手に入れた（遺物でない名前は無視する）。 */
    static void kiroku(String kuni, String namae) {
        if (kuni == null || Shouri.ibutsuKouka(namae) == null) {
            return;
        }
        MOTTERU.computeIfAbsent(kuni, x -> new HashSet<>()).add(namae);
    }

    /** 全部 忘れる（運営の全部リセット）。ポーションは3秒で、属性は次の秒で消える。 */
    static void risetto() {
        MOTTERU.clear();
    }

    /** その勢力がその遺物を持っているか。 */
    public static boolean motteru(String kuni, String namae) {
        Set<String> s = kuni == null ? null : MOTTERU.get(kuni);
        return s != null && s.contains(namae);
    }

    /** その勢力が持っている、その効果の遺物の名前。無ければ null。 */
    static String koukaNoIbutsu(String kuni, String kouka) {
        Set<String> s = kuni == null ? null : MOTTERU.get(kuni);
        if (s == null) {
            return null;
        }
        for (String na : s) {
            if (kouka.equals(Shouri.ibutsuKouka(na))) {
                return na;
            }
        }
        return null;
    }

    // =========================================================
    //  聞きに来る口（銀行・銃器専門店・掘り・/joho）
    // =========================================================

    /** 貴金属の売値の倍率（百分率）。100 が素。 */
    public static int uriBairitsu(String kuni) {
        return koukaNoIbutsu(kuni, URI) != null ? URI_BAIRITSU_KINKO : 100;
    }

    /** 銃器専門店が開く貯金。手形があれば 5,500。 */
    public static int jukiShikii(String kuni) {
        return koukaNoIbutsu(kuni, JUKI) != null ? JUKI_SHIKII_TEGATA : Shop.JUKI_KAIHOU;
    }

    /**
     * 銃器専門店で買った時に戻る金。手引書が無ければ 0。
     * ★ 値引きではなく【戻し】にしてある。画面の値段（MOD の絵の文字）はそのままで、
     *   払った直後に 1割 戻る。表示と請求がずれない。
     */
    public static int nebikiModori(String kuni, int nedan) {
        return koukaNoIbutsu(kuni, NEBIKI) != null ? nedan * NEBIKI_WARI / 100 : 0;
    }

    /** 他勢力の貯金と石油を見てよいか。 */
    /**
     * 情報が見える遺物の名前。★ 正本は Shouri.IBUTSU_HYOU。
     *   ここで打ち間違えると「持っているのに効かない」になるので、表から引く。
     */
    static final String JOHO_MEI = Shouri.ibutsuNoMei(JOHO);

    public static boolean johoMieru(String kuni) {
        return koukaNoIbutsu(kuni, JOHO) != null;
    }

    /**
     * 鉄王冠: 石を掘った時の「おまけの抽選」。落とす物の一覧を返す（無ければ空）。
     * ★ 純粋な計算。検証が 10万回 回して期待値を測る。
     *
     * @param shinso 深層岩なら true（1.5倍）
     */
    static List<Material> horiOmake(String kuni, boolean shinso, Random ran) {
        List<Material> r = new ArrayList<>();
        if (koukaNoIbutsu(kuni, HORI) == null) {
            return r;
        }
        if (ran.nextInt(100) >= HORI_OMAKE_WARI) {
            return r;
        }
        for (Object[] g : HORI_HYOU) {
            double p = (Integer) g[1] / 100.0 * (shinso ? HORI_SHINSO_BAI : 1.0);
            if (ran.nextDouble() < p) {
                r.add((Material) g[0]);
            }
        }
        return r;
    }

    // =========================================================
    //  毎秒: ポーションと属性
    // =========================================================

    /**
     * 効果の種類 → ポーション効果。ポーションで表せない物は null。
     * ★ 純粋な対応表。検証が直接 呼ぶ。
     */
    static PotionEffectType potion(String kouka) {
        if (kouka == null) {
            return null;
        }
        switch (kouka) {
            case SPEED: return PotionEffectType.SPEED;
            case HASTE: return PotionEffectType.FAST_DIGGING;
            case NIGHT: return PotionEffectType.NIGHT_VISION;
            default:    return null;
        }
    }

    /**
     * 1秒に1回。勢力の人 全員に、持っている遺物のポーション効果をかけ直し、
     * 攻撃力の修正子を付け外しする。
     *
     * ★ 粒子は出さない（戦場で居場所がばれる）。アイコンだけ出す。
     * ★ 属性の修正子はプレイヤーに保存される。持っていない勢力の人から必ず外す
     *   （リセットした後や、記録が無い人に残らないように）。
     */
    public static void byoumai(Kane kane) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String kuni = kane.kinkoMei(p);
            Set<String> s = kuni == null ? null : MOTTERU.get(kuni);
            if (s != null) {
                for (String na : s) {
                    String kouka = Shouri.ibutsuKouka(na);
                    PotionEffectType t = potion(kouka);
                    if (t != null) {
                        // ★ 長さは効果ごと（暗視だけ30秒。点滅させないため）
                        p.addPotionEffect(new PotionEffect(t, nagasa(kouka), 0, true, false, true));
                    }
                }
            }
            kougekiAwaseru(p, koukaNoIbutsu(kuni, STRENGTH) != null);
        }
    }

    /** 攻撃力の修正子を、持っているなら付け、持っていないなら外す。 */
    static void kougekiAwaseru(Player p, boolean motsu) {
        AttributeInstance inst = p.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (inst == null) {
            return;
        }
        AttributeModifier ima = null;
        for (AttributeModifier m : inst.getModifiers()) {
            if (KOUGEKI_FUDA.equals(m.getName())) {
                ima = m;
            }
        }
        if (motsu && ima == null) {
            inst.addModifier(new AttributeModifier(KOUGEKI_UUID, KOUGEKI_FUDA, KOUGEKI_WARI,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        } else if (!motsu && ima != null) {
            inst.removeModifier(ima);
        }
    }

    // =========================================================
    //  イベント: 被ダメージと回復
    // =========================================================

    /** テンプル騎士団の盾: 勢力の人が受けるダメージを 15% 減らす。 */
    public static void damageUketa(EntityDamageEvent e, Kane kane) {
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }
        if (koukaNoIbutsu(kane.kinkoMei(p), RESISTANCE) != null) {
            e.setDamage(e.getDamage() * BOUGYO_WARI);
        }
    }

    /** ペニシリン: 勢力の人の自然回復を 20% 増やす。ポーションや金リンゴの回復は触らない。 */
    public static void kaifuku(EntityRegainHealthEvent e, Kane kane) {
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }
        if (e.getRegainReason() != EntityRegainHealthEvent.RegainReason.SATIATED
                && e.getRegainReason() != EntityRegainHealthEvent.RegainReason.REGEN) {
            return;
        }
        if (koukaNoIbutsu(kane.kinkoMei(p), REGEN) != null) {
            e.setAmount(e.getAmount() * KAIFUKU_BAI);
        }
    }

    // =========================================================
    //  /joho（スマートフォン）
    // =========================================================

    /** 他勢力の貯金と石油を出す。スマートフォンが無ければ断る。 */
    public static void joho(CommandSender sender, Player p, Kane kane) {
        // ★★ 2026-08-24 の実機の指摘 ★★
        //   「スマホを持っているのに、情報が見れない」。
        //   前は【勢力が遺物を獲得した記録】しか見ていなかったので、
        //     ・手元に紙があっても効かない（運営が出した物・買った物・奪った物）
        //     ・勢力に入っていない人は、何をしても永久に使えない
        //   /joho は本人が自分で打つ物なので、**紙を持っている人**にも許す。
        //   ★ 受け身の効果（速さ・攻撃力など）は今までどおり勢力のもの。
        //     紙を渡しても移らない、という決まりは変えていない。
        String kuni = kane.kinkoMei(p);
        boolean kami = Shouri.kamiMotteru(p, JOHO_MEI);
        if (!johoMieru(kuni) && !kami) {
            sender.sendMessage(Enshutsu.kazaru(Enshutsu.KOUNYU,
                    "他勢力の情報を見るには、遺物「" + JOHO_MEI + "」が要ります"
                            + "（勢力が持っているか、自分で持っているか）"));
            Enshutsu.oto(p, Enshutsu.OTO_DAME);
            return;
        }
        sender.sendMessage("§6── 他勢力の情報（スマートフォン）──");
        for (String team : Seiryoku.zenTeam()) {
            String mei = Seiryoku.mei(team);
            sender.sendMessage(String.format("  §f%-4s §7貯金 §e%,d円 §7/ 石油 §d%dℓ §7/ 時代 §f%s",
                    mei, kane.seiryokuZandaka(mei), kane.sekiyuGokei(mei),
                    Kane.jidaiMei(kane.seiryokuJidai(mei))));
        }
    }

    // =========================================================
    //  手に入れた時の知らせ
    // =========================================================

    /**
     * 手に入れた時の知らせ。勢力の中には効果まで、全体には名前だけ。
     * ★ 渡す時（演出が終わった後）に呼ぶ。抽選の時に呼ぶと十連の伏せ札がばれる。
     */
    public static void teniireta(Player player, Kane kane, String namae) {
        String kouka = Shouri.ibutsuKouka(namae);
        if (kouka == null) {
            return;
        }
        String kuni = kane.kinkoMei(player);
        String shirase = Enshutsu.kazaru(Enshutsu.ATARI,
                "遺物「" + namae + "」を手に入れた ── " + Shouri.ibutsuSetsumei(namae));
        for (Player nakama : kane.onajiSeiryoku(player)) {
            nakama.sendMessage(shirase);
            Enshutsu.oto(nakama, Enshutsu.OTO_DAI);
        }
        Enshutsu.zeninTsuchi(Enshutsu.kazaru(Enshutsu.ATARI,
                (kuni == null ? player.getName() : kuni) + " が遺物「" + namae + "」を手に入れた"));
    }
}
