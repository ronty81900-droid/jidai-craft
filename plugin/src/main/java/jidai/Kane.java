// =============================================================
// Kane.java ── 金の出し入れ（データパックとの接点）
//
//   このファイルだけが、データパックのスコアボードに触る。
//   ★ スコアボードの名前を決めるのはデータパック側。
//     プラグインは名前を勝手に作らない。ここに書いてある名前は
//     datapacks/jidai_craft/data/jidai/function/load.mcfunction と
//     kane/kinko_yomu.mcfunction を読んで写したもの (データパック v4)。
//
//   ※ v4 で名前が変わった。以前は kane_seiryoku / 丘陵の金庫 だったが、
//     今は chokin / 丘陵 になっている。直したのはこのファイルだけ。
//
//   データパック側の名前が変わったら、直すのはこのファイルの
//   一番上の定数だけで済む。
// =============================================================

package jidai;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * 個人の金と勢力の金を読み書きする係。
 *
 * データパック側の作りに合わせてある:
 *   個人の金 … 目的 kane_kojin。持ち主はプレイヤー本人
 *   勢力の金 … 目的 kane_seiryoku。持ち主は「丘陵の金庫」のような
 *              勢力ごとのダミープレイヤー名（プレイヤーではない）
 *   所属     … チーム kyuryo / shinrin で決まる
 */
public final class Kane {

    // --- データパック側が決めた名前（写しただけ。勝手に作らない） ---

    /** 個人の金。持ち主はプレイヤー本人 */
    private static final String KOJIN = "kane_kojin";

    /** 勢力の金（貯金）。持ち主は下の勢力名 */
    private static final String SEIRYOKU = "chokin";

    /** 運営が決めた設定値の置き場（腐敗の猶予・戦争の準備時間など） */
    private static final String SETTEI = "settei";

    /**
     * 中央の時代。1=鉄器 / 2=中世 / 3=近代 / 4=現代。
     *
     * ★ 商品の解禁はこれで決まる。勢力ごとの時代(jidai)ではない。
     *   データパックが時代を上げた時に
     *   「この時代の装備が全勢力に解禁された」と流しているとおり、
     *   解禁は全勢力に同時に起きる。
     */
    private static final String CHUO = "chuo";

    /** 中央の時代の保持者名。プレイヤーではなく、ただの名前 */
    private static final String SEKAI = "世界";

    // --- 戦争まわり（データパックの load.mcfunction を読んで写した） ---
    //
    // ★ 戦争の判定・進行はすべてデータパックが持つ。
    //   プラグインは**読むだけ**で、1つも書き込まない。
    //
    // ★★ 保持者の形が2種類ある ★★
    //   ・勢力ごと … 保持者は勢力の日本語名（丘陵・森林・…）
    //   ・勢力ごと … 保持者は勢力の名前（"丘陵" など）
    //
    //   ★★ 1.20.1 移行で、戦争の持ち方が変わった（2026-08-19） ★★
    //     1.21 では保持者 "丘陵>森林" のスコアを読んでいた。その名前は
    //     マクロでしか作れず、1.20.1 にマクロが無い（実測）。
    //     いまは【マーカー1体＝戦争1件】で、データパックが毎秒
    //     勢力名の保持者へ写し直している（jidai:sensou/youyaku）。
    //     ★プラグインはマーカーを読まない。写されたものだけを見る。

    /** 勢力ごと。0=なし 1=準備 2=交戦 3=再戦禁止 */
    private static final String SENSOU = "sensou";

    /** 勢力ごと。交戦相手の**番号**。0=戦争していない */
    private static final String SENSOU_AITE = "sensou_aite";

    /** 勢力ごと。今の状態の残り秒（準備の残り・交戦の残り・禁止の残り） */
    private static final String SENSOU_BYOU = "sensou_byou";

    /** 勢力ごと。0＝占領されていない／それ以外＝占領している勢力の番号 */
    private static final String SENRYOU = "senryou";

    /**
     * 人ごと。略奪してから次に略奪できるまでの残り秒。
     *
     * ★★ これが「略奪が成立したか」の合図になる（2026-08-23）★★
     *   データパックの jidai:sensou/ryakudatsu は、断る時（準備中・
     *   クールダウン中・上限に達した）は【何も置かずに引き返す】。
     *   実際に金や石油を動かした時にだけ、最後にこの秒数を置く。
     *   押す前が 0 で、押した後が 1以上なら、確かに奪えている。
     *   ★ 新しい取り決めを増やしていない。向こうが元から持っている印を読むだけ。
     */
    private static final String RYAKUDATSU_KAN = "ryakudatsu_kan";

    /** 勢力ごと。勢力の番号（1=丘陵 2=森林 …）。番号はデータパックが配る */
    private static final String BANGOU = "bangou";

    /** 勢力ごと。下剋上の権利を持っているか 0/1（1勢力につき1つまで） */
    private static final String GEKOKUJO = "gekokujo";

    // ★★ 2026-08-22: チーム名と金庫名の対応表を、ここから消した ★★
    //   Seiryoku.SEIRYOKU と同じ物を2つ持っていたせいで、
    //   5勢力に増やした時に【こちらだけ2勢力のまま】取り残されていた。
    //   その結果、川・内海・岩場の人は kinkoMei が null を返し、
    //   銀行・宣戦・勢力の金での買い物が、まるごと使えなくなっていた。
    //   （コンパイルも検査も通るので、実機で人が試すまで気付けない形の事故）
    //
    //   正本を Seiryoku 側ひとつにして、二度と食い違わないようにした。
    //   データパックの kane/kinko_yomu.mcfunction と揃っていることは、
    //   UneiTest が両方を読んで突き合わせている。

    /**
     * サーバー共通のスコアボードを取り出す。
     * データパックが作った目的は、すべてここに入っている。
     *
     * ★ プレイヤーごとの scoreboard ではなく「メイン」を使う。
     *   メイン以外を見ると、データパックが書いた数字が見えない。
     */
    /**
     * 主スコアボード。★ 一度 引いたら使い回す（2026-08-26 の実測）。
     *
     *   前は呼ばれるたびに Bukkit.getScoreboardManager().getMainScoreboard()
     *   を引き直していた。かまど100個で **1秒あたり 8,000回**（4,000 + 4,000）。
     *   主スコアボードは起動中ずっと同じ物なので、引き直す意味が無い。
     *
     * ★ null のまま覚えない。データパックより先に呼ばれると null が返るので、
     *   取れた時だけ覚える（取れなければ次回また引く）。
     */
    private Scoreboard oboeta;

    private Scoreboard ban() {
        if (oboeta == null) {
            org.bukkit.scoreboard.ScoreboardManager m = Bukkit.getScoreboardManager();
            if (m != null) {
                oboeta = m.getMainScoreboard();
            }
        }
        return oboeta;
    }

    /**
     * 目的を名前で探す。無ければ null を返す。
     * データパックが読み込まれていない時に、いきなり落ちないようにするため。
     */
    private Objective mokuteki(String namae) {
        return ban().getObjective(namae);
    }

    /**
     * データパックが読み込まれていて、金の目的が両方あるか。
     * false なら、購入処理に進んではいけない。
     */
    public boolean junbiOK() {
        return mokuteki(KOJIN) != null && mokuteki(SEIRYOKU) != null;
    }

    // --- 個人の金 ------------------------------------------------

    /**
     * 個人の残高を読む。
     *
     * getScore(名前) は、まだ一度も金を持ったことがない人でも
     * 0 を返してくれる（データパックの clock が毎秒 add 0 しているので、
     * 実際にはすぐ 0 が入る）。
     */
    public int kojinZandaka(Player player) {
        return mokuteki(KOJIN).getScore(player.getName()).getScore();
    }

    /** 個人の残高を書き換える。 */
    public void kojinKousin(Player player, int atai) {
        mokuteki(KOJIN).getScore(player.getName()).setScore(atai);
    }

    // --- 勢力の金 ------------------------------------------------

    /**
     * その人の所属勢力の「金庫の名前」を返す。
     * どの勢力にも入っていなければ null。
     *
     * getEntryTeam(名前) が、その人が入っているチームを返す。
     * データパックの kinko_yomu.mcfunction が
     * team=kyuryo を見ているのと同じことをしている。
     */
    public String kinkoMei(Player player) {
        Team team = ban().getEntryTeam(player.getName());
        return team == null ? null : kinkoMeiTeam(team.getName());
    }

    /**
     * チーム名 → 貯金の持ち主名（丘陵・森林・川・内海・岩場）。
     * 勢力でないチームなら null。
     *
     * ★ 正本は Seiryoku。ここで名前を並べ直さない。
     */
    public static String kinkoMeiTeam(String teamMei) {
        return Seiryoku.seiryokuKa(teamMei) ? Seiryoku.mei(teamMei) : null;
    }

    /**
     * 勢力の貯金の残高を読む。
     * 保持者はプレイヤーではなくただの名前なので、文字列で指定する。
     */
    public int seiryokuZandaka(String kinko) {
        return mokuteki(SEIRYOKU).getScore(kinko).getScore();
    }

    /** 勢力の貯金の残高を書き換える。 */
    public void seiryokuKousin(String kinko, int atai) {
        mokuteki(SEIRYOKU).getScore(kinko).setScore(atai);
    }

    // --- 石油 ----------------------------------------------------

    /**
     * 今の中央の時代を返す。1〜4。
     * データパックが読み込まれていなければ 1 を返す
     * （＝最初の時代の商品しか出さない。安全側に倒す）。
     */
    public int chuoJidai() {
        Objective o = mokuteki(CHUO);
        if (o == null) {
            return 1;
        }
        int n = o.getScore(SEKAI).getScore();
        return n < 1 ? 1 : n;
    }

    // --- 戦争の状態を読む（書き込みは一切しない） ------------------

    /** 目的が無ければ 0 を返す（データパックがまだ作っていない時）。 */
    private int yomu(String mokutekiMei, String mochinushi) {
        Objective o = mokuteki(mokutekiMei);
        return o == null ? 0 : o.getScore(mochinushi).getScore();
    }

    /**
     * スコアを1つ書く。目的が無ければ何もしない（false を返す）。
     *
     * ★ 運営のリセット（Kanri）だけが使う。
     *   ふつうの処理は kojinKousin / seiryokuKousin のような
     *   名前の付いた口を使うこと。何を書いたかが読めなくなるため。
     */
    public boolean kaku(String mokutekiMei, String mochinushi, int atai) {
        Objective o = mokuteki(mokutekiMei);
        if (o == null) {
            return false;
        }
        o.getScore(mochinushi).setScore(atai);
        return true;
    }

    /** スコアを持っている名前の全部（オフラインの人や勢力名も含む）。 */
    public java.util.Set<String> zenHojisha() {
        return ban().getEntries();
    }

    /** その名前が、その目的のスコアを【実際に持っている】か（0 と未設定を区別する）。 */
    public boolean scoreAru(String mokutekiMei, String mochinushi) {
        Objective o = mokuteki(mokutekiMei);
        return o != null && o.getScore(mochinushi).isScoreSet();
    }

    /**
     * a と b が「いま戦っている相手どうし」なら true。
     *
     * ★ 要約には「自分の状態」と「相手の番号」しか入っていない。
     *   だから a の相手が b か、b の相手が a か、のどちらかを確かめる。
     *   （どちらか片方しか書かれていない事故に備えて両方見る）
     */
    private boolean kumi(String aMei, String bMei) {
        int aNo = yomu(BANGOU, aMei);
        int bNo = yomu(BANGOU, bMei);
        if (aNo == 0 || bNo == 0) {
            return false;   // 番号が配られていない＝勢力として登録されていない
        }
        return yomu(SENSOU_AITE, aMei) == bNo || yomu(SENSOU_AITE, bMei) == aNo;
    }

    /**
     * 2つの勢力の間の戦争の状態。0=なし 1=準備 2=交戦 3=再戦禁止。
     *
     * ★ 別の相手と戦争していても 0 を返す。「この2つの間の状態」を聞いている。
     */
    public int sensou(String aMei, String bMei) {
        if (!kumi(aMei, bMei)) {
            return 0;
        }
        return Math.max(yomu(SENSOU, aMei), yomu(SENSOU, bMei));
    }

    /**
     * その勢力が今どういう状態か。0=なし 1=準備 2=交戦 3=再戦禁止。
     * ★ 相手が誰かは問わない。「戦争しているか」だけを聞く時に使う。
     */
    public int sensouJotai(String kuniMei) {
        return yomu(SENSOU, kuniMei);
    }

    /** 今の状態の残り秒（準備・交戦・再戦禁止のどれかの残り）。 */
    public int sensouByou(String aMei, String bMei) {
        if (!kumi(aMei, bMei)) {
            return 0;
        }
        return Math.max(yomu(SENSOU_BYOU, aMei), yomu(SENSOU_BYOU, bMei));
    }

    /**
     * その人が次に略奪できるまでの残り秒。0 なら今すぐ略奪できる。
     *
     * ★ 目的が無ければ 0 を返す（yomu の決まり）。
     *   データパックが入っていない時に「ずっと待たされる」ことにはならない。
     */
    public int ryakudatsuKan(Player player) {
        return yomu(RYAKUDATSU_KAN, player.getName());
    }

    /**
     * データパックに「戦争の要約を作り直せ」と言う。
     *
     * ★★ これを呼ばないと、宣戦した直後の状態が読めない ★★
     *   要約はデータパックの時計が毎秒作り直しているが、
     *   GUI の返事は1秒も待てない。宣戦の関数を呼んだ直後に
     *   これを呼んでから読み直すこと（返金の判定がここに懸かっている）。
     */
    public void sensouYouyaku() {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "function jidai:sensou/youyaku");
        } catch (RuntimeException e) {
            // 読み直しができないだけ。呼び出し側が「始まらなかった」と判断して
            // 返金するので、金が消えることはない。
            Bukkit.getLogger().warning("[JidaiCraft] 戦争の要約を作り直せませんでした: " + e);
        }
    }

    /** その勢力を占領している勢力の**番号**。0＝占領されていない。 */
    public int senryou(String kuniMei) {
        return yomu(SENRYOU, kuniMei);
    }

    /**
     * データパックの作業用の目的 `sagyou` を読む。
     *
     * ★ ここは「途中結果」を覗くための窓。ふだんは使わない。
     *   いま使っているのは1つだけ:
     *     #shisetsu … jidai:setup/kyoten が「何個 置けたか」を残す数
     *   置く命令は読み込まれていない区画では黙って失敗するので、
     *   本当に置けたかは、この数を読むしかない。
     */
    public int sagyou(String mochinushi) {
        return yomu("sagyou", mochinushi);
    }

    /** 勢力の番号。★ 番号を決めるのはデータパック。プラグインは覚えない。 */
    public int bangou(String kuniMei) {
        return yomu(BANGOU, kuniMei);
    }

    /** 下剋上の権利を持っているか（1で持っている）。★ 権利もデータパックのもの。 */
    public int gekokujoKenri(String kuniMei) {
        return yomu(GEKOKUJO, kuniMei);
    }

    /**
     * その人の勢力の時代（1〜4）。
     *
     * ★ 中央の時代とは別物。勢力ごとに進み方が違う。
     *   ガチャの値段と景品はこれで切り替わる。
     */
    public int jidaiOf(Player player) {
        String team = teamMei(player);
        // ★★ 勢力に入っていない人は【中央の時代】で見る ★★
        //   運営や見学者がここに落ちる。
        //   ここを勢力名として読むと jidai が 0 になり、
        //   いつまでも鉄器のままになる（販売所の解禁が勢力の時代に
        //   なった 2026-08-20 に、この穴が効くようになった）。
        if (team == null || !Seiryoku.seiryokuKa(team)) {
            return chuoJidai();
        }
        int n = yomu("jidai", Seiryoku.mei(team));
        return n < 1 ? 1 : n;
    }

    // --- 時代進行の3条件を読む（すべてデータパックが書いた値） -----

    /** その勢力の時代（1〜4）。保持者は勢力の日本語名。 */
    public int seiryokuJidai(String kuniMei) {
        int n = yomu("jidai", kuniMei);
        return n < 1 ? 1 : n;
    }

    /** その勢力の石油の合計本数。データパックが毎秒作り直している。 */
    public int sekiyuGokei(String kuniMei) {
        return yomu("sekiyu_gokei", kuniMei);
    }

    /** その勢力の拠点に置かれた建築ブロックの数。 */
    public int kenchiku(String kuniMei) {
        return yomu("kenchiku", kuniMei);
    }

    /**
     * 時代を進めるのに必要な量。★ 数字はデータパックの settei にある。
     *   プラグインは覚えない。未来(5)は進めないので 0 を返す。
     *
     * ★ 2026-08-23: 現代(4) → 未来(5) を足した（ご指示）。
     *   「現代へ着くこと」は勝ちではなく、未来へ進めた勢力が勝つ。
     */
    public int hitsuyoSekiyu(int jidai) {
        return hitsuyo("進行_石油_", jidai);
    }

    public int hitsuyoChokin(int jidai) {
        return hitsuyo("進行_貯金_", jidai);
    }

    public int hitsuyoKenchiku(int jidai) {
        return hitsuyo("進行_建築_", jidai);
    }

    /**
     * 「進行_○○_鉄器」のような保持者名を組み立てて読む。
     *
     * ★ 名前はデータパックの settei の保持者名そのもの。
     *   「その時代から次へ進むのに要る量」なので、現代(4)の欄は
     *   【現代 → 未来】の必要量を指す。
     */
    private int hitsuyo(String atama, int jidai) {
        String[] mei = {"", "鉄器", "中世", "近代", "現代"};
        if (jidai < 1 || jidai >= mei.length) {
            return 0;   // 未来(5)から先は無い
        }
        return yomu(SETTEI, atama + mei[jidai]);
    }

    /** 最後の時代。ここへ着いた勢力が勝つ（未来到達勝利）。 */
    public static final int JIDAI_MIRAI = 5;

    /** 時代の呼び名。画面に出すためだけに使う。 */
    public static String jidaiMei(int jidai) {
        switch (jidai) {
            case 1: return "鉄器";
            case 2: return "中世";
            case 3: return "近代";
            case 4: return "現代";
            default: return "未来";
        }
    }

    /**
     * もう勝者が決まっているか。
     * ★ shouri は保持者＝世界 / 値＝勝った勢力の番号。0 なら まだ。
     *   データパックが書く。プラグインは読むだけ。
     */
    public boolean shouriSumi() {
        Objective o = mokuteki("shouri");
        return o != null && o.getScore(SEKAI).getScore() > 0;
    }

    // --- 石油の栓（運営がコマンドで動かす） ----------------------
    //
    // ★★ ここは【プラグインが settei に書き込む唯一の場所】 ★★
    //   他の settei は load.mcfunction が決めた値をそのまま使う。
    //   この2つだけ書いてよい理由は、データパック側が
    //     scoreboard players add 石油_停止 settei 0
    //   のように「無ければ作る・あればそのまま」で置いているため、
    //   毎秒上書きされない。CONTRACT.md にも書いてある。

    /** 石油を止めているか。 */
    private static final String TEISHI = "石油_停止";

    /** 湧く速さ。百分率で 100 が通常。 */
    private static final String BAIRITSU = "石油_倍率";

    /** 石油の栓を触れる状態か（データパックが読み込まれているか）。 */
    public boolean sekiyuSenJunbiOK() {
        return mokuteki(SETTEI) != null;
    }

    /** 今 止まっているか。 */
    public boolean sekiyuTomatteruKa() {
        return mokuteki(SETTEI).getScore(TEISHI).getScore() >= 1;
    }

    /** 今の湧く速さ（百分率。100 が通常）。 */
    public int sekiyuBairitsu() {
        int n = mokuteki(SETTEI).getScore(BAIRITSU).getScore();
        return n < 1 ? 100 : n;   // 0 だとデータパックが割り算できない
    }

    /** 止める / 再開する。 */
    public void sekiyuTomeru(boolean tomeru) {
        mokuteki(SETTEI).getScore(TEISHI).setScore(tomeru ? 1 : 0);
    }

    /**
     * 湧く速さを変える。
     * ★ 1〜100 に切り詰める。0 を入れるとデータパックの割り算が
     *   できずに間隔が前のまま残り、「抑えたつもりで抑わっていない」
     *   という一番たちの悪い壊れ方をする。
     */
    public void sekiyuBairitsuSet(int bairitsu) {
        int n = Math.max(1, Math.min(100, bairitsu));
        mokuteki(SETTEI).getScore(BAIRITSU).setScore(n);
    }

    // --- チームの出し入れ ----------------------------------------

    /** 今その人が入っているチーム名。どこにも入っていなければ null。 */
    public String teamMei(Player player) {
        Team team = ban().getEntryTeam(player.getName());
        return team == null ? null : team.getName();
    }

    /**
     * その人と同じ勢力に居る、今ログイン中の全員を返す。**本人も含む。**
     *
     * 勢力の金を使った時の知らせを送る相手を決めるために使う。
     *
     * ★ 以前はサーバー全員に知らせていたが、勢力の中だけに変えた。
     *   全員に流すと、他勢力に「何をいくつ買ったか」が筒抜けになるため。
     *   知らせる目的は「勢力の金を誰が使ったかを、その勢力が把握する」ことなので、
     *   勢力の中に届けば足りる。
     *
     * どの勢力にも入っていない場合は、本人だけを返す。
     * (勢力の金は使えないので、実際にはここへ来ない。念のための備え)
     */
    public List<Player> onajiSeiryoku(Player player) {
        List<Player> nakama = new ArrayList<>();

        Team team = ban().getEntryTeam(player.getName());
        if (team == null) {
            nakama.add(player);
            return nakama;
        }

        // getOnlinePlayers() は「今サーバーに居る人」全員。
        // その中から、同じチームに入っている人だけを拾う。
        // hasEntry(名前) が「その人はこのチームに入っているか」。
        for (Player hito : Bukkit.getOnlinePlayers()) {
            if (team.hasEntry(hito.getName())) {
                nakama.add(hito);
            }
        }
        return nakama;
    }
}
