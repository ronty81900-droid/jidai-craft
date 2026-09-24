package jidai.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;

/**
 * Minecraft 本体を呼ぶところだけを、この1ファイルに閉じ込める。
 *
 * ★★ なぜこのファイルがあるか ★★
 *   Forge の本番環境では、Minecraft のメソッド名が m_280488_ のような
 *   「SRG 名」になっている。名前を見ても何のメソッドか分からない。
 *   ForgeGradle を使えば本来の名前で書けるが、この環境では Gradle が
 *   動かない（Java の NIO Selector が塞がれている）ので、SRG 名で呼ぶしかない。
 *
 *   そこで【読めなくなるのはこのファイルだけ】にして、
 *   ほかのファイルは日本語の名前だけで書けるようにする。
 *
 *   ★ ここに並ぶ SRG 名は推測ではない。
 *     Mojang 公式の対応表（client.txt）と Forge 同梱の対応表を
 *     突き合わせて出したもの。引き直すときは:
 *         python tools/namae.py 引く <クラス> <メソッド名>
 */
public final class Mc {

    private Mc() {
        // 道具置き場なので、実体は作らない
    }

    /** Minecraft#getInstance() */
    public static Minecraft honntai() {
        return Minecraft.m_91087_();
    }

    /** Minecraft.font ── 画面に字を書くための書体 */
    public static Font shotai() {
        return honntai().f_91062_;
    }

    /** Minecraft.screen ── いま開いている画面。何も開いていなければ null */
    public static Screen imaNoGamen() {
        return honntai().f_91080_;
    }

    /** Minecraft#setScreen(Screen) ── 画面を出す。null を渡すと閉じる */
    public static void gamenWoDasu(Screen gamen) {
        honntai().m_91152_(gamen);
    }

    /** Component#literal(String) ── ただの文字から見出しを作る */
    public static Component moji(String s) {
        return Component.m_237113_(s);
    }

    /**
     * GuiGraphics#fill(x1, y1, x2, y2, 色) ── 四角を塗る。
     * 右端と下端は含まない。色は 0xAARRGGBB（先頭2桁が不透明度）。
     */
    public static void nuru(GuiGraphics g, int x1, int y1, int x2, int y2, int iro) {
        g.m_280509_(x1, y1, x2, y2, iro);
    }

    /** GuiGraphics#drawString(Font, String, x, y, 色) ── 素の大きさで字を書く（高さ約9px） */
    public static void kaku(GuiGraphics g, String s, int x, int y, int iro) {
        g.m_280488_(shotai(), s, x, y, iro);
    }

    /** Font#width(String) ── その文字が横 何px になるか */
    public static int haba(String s) {
        return shotai().m_92895_(s);
    }

    /**
     * Font#split(FormattedText, 幅) ── その幅で折り返した時の行を返す。
     *
     * ★ 折り返しを自分で書かないのは、Minecraft の折り返しと1文字でもずれると
     *   「測った時は収まっていたのに、実際ははみ出す」という、
     *   いちばん見つけにくい形の食い違いになるため。本体と同じ判断を使う。
     */
    public static List<FormattedCharSequence> oru(String s, int haba) {
        return shotai().m_92923_(moji(s), haba);
    }

    /**
     * GuiGraphics#drawString(Font, FormattedCharSequence, x, y, 色, 影) ── 折り返した1行を描く。
     *
     * ★ 影を付けない（最後の false）。
     *   引数を省く形だと影が付くが、この UI は平らなドット絵で、
     *   1pxの枠線のすぐ横に文字が来る場所がある。影を出すと枠に触れて濁る。
     *   付けたくなったら、ここを true にするだけでよい。
     */
    public static void kakuGyou(GuiGraphics g, FormattedCharSequence gyou, int x, int y, int iro) {
        g.m_280649_(shotai(), gyou, x, y, iro, false);
    }

    /** Font#width(FormattedCharSequence) ── 折り返した1行が横 何px になるか */
    public static int habaGyou(FormattedCharSequence gyou) {
        return shotai().m_92724_(gyou);
    }

    /**
     * 絵の置き場所。
     * ★ 毎フレーム new すると無駄なので、1回作ったら覚えておく。
     */
    private static final Map<String, ResourceLocation> BASHO = new HashMap<>();

    private static ResourceLocation basho(String moto) {
        ResourceLocation r = BASHO.get(moto);
        if (r == null) {
            r = new ResourceLocation("jidaiui", moto);
            BASHO.put(moto, r);
        }
        return r;
    }

    /**
     * GuiGraphics#blit(...) ── 絵を丸ごと1枚 貼る。
     *
     * ★ 引数の並びは (置き場所, x, y, 絵のどこから, 何px ぶん, 絵の実寸)。
     *   絵を切り出さずに丸ごと使うので、切り出し位置は 0,0、
     *   貼る大きさと絵の実寸は同じ値を渡す。
     */
    public static void haru(GuiGraphics g, String moto, int x, int y, int w, int h) {
        g.m_280163_(basho(moto), x, y, 0.0F, 0.0F, w, h, w, h);
    }

    /**
     * 絵の実寸（texW×texH）を、w×h に縮めて貼る。
     * ★ 遺物の絵は 64×64（高密度）。16×16 の枠に縮めて置く。
     *   haru() で貼ると実寸の左上 16×16 しか出ない（u/v の範囲を絵の大きさと決め打ちしているため）。
     * GuiGraphics#blit(ResourceLocation, x, y, w, h, u, v, uW, vH, texW, texH)
     */
    public static void haruOokisa(GuiGraphics g, String moto, int x, int y, int w, int h,
                                  int texW, int texH) {
        g.m_280411_(basho(moto), x, y, w, h, 0.0F, 0.0F, texW, texH, texW, texH);
    }

    /**
     * 絵の【一部】を切り出して貼る。u,v から uW×vH ぶんを、x,y に w×h で置く。
     *
     * ★ haruOokisa は絵の全体しか貼れない。ボスバーの中身は
     *   「左端から 進み具合ぶんだけ」切るので、u/v を渡せる口が要る。
     * GuiGraphics#blit(ResourceLocation, x, y, w, h, u, v, uW, vH, texW, texH)
     */
    public static void haruKiri(GuiGraphics g, String moto, int x, int y, int w, int h,
                                float u, float v, int uW, int vH, int texW, int texH) {
        g.m_280411_(basho(moto), x, y, w, h, u, v, uW, vH, texW, texH);
    }

    /** Font.lineHeight ── 1行の高さ（ふつう 9px）。バニラはボスバーの名前を バーの上 この分だけ上に置く */
    public static int gyouTakasa() {
        return shotai().f_92710_;
    }

    /** 作ったアイテム。毎フレーム作り直さないよう覚えておく。 */
    private static final Map<String, ItemStack> MONO = new HashMap<>();

    /**
     * アイテムを1つ作る。
     *
     * ★ 名前（minecraft:bread など）から引く。
     *   番号で持つと、版が変わった時に黙って別の物になる。
     *   知らない名前を渡すと空気（AIR）が返るので、何も描かれないだけで落ちない。
     */
    public static ItemStack aitem(String id, int kazu) {
        String kagi = id + "x" + kazu;
        ItemStack mono = MONO.get(kagi);
        if (mono == null) {
            Item moto = BuiltInRegistries.f_257033_.m_7745_(new ResourceLocation(id));
            mono = new ItemStack(moto, Math.max(1, kazu));
            MONO.put(kagi, mono);
        }
        return mono;
    }

    /**
     * MOD のアイテムを作る。`tacz:modern_kinetic_gun{GunId:"hamster:sks"}` の形。
     *
     * ★★ なぜこちらで作るか ★★
     *   サーバー（Bukkit）は MOD のアイテムを作れない。Material はバニラ
     *   決め打ちの一覧だからで、そのためチェスト画面には
     *   クロスボウなどの【代用品】しか置けなかった。
     *   クライアントには TaCZ が入っているので、ここで組み立てれば
     *   本物の銃の見た目が出せる。
     *
     * ★ その MOD が入っていなければ null を返す。呼ぶ側は代用品へ落とす。
     * ★ 見つからなかったことも覚えておく（毎フレーム 探し直さないため）。
     */
    public static ItemStack modAitem(String shitei, int kazu) {
        String kagi = shitei + "x" + kazu;
        if (MONO.containsKey(kagi)) {
            return MONO.get(kagi);
        }
        int nami = shitei.indexOf('{');
        String id = (nami < 0) ? shitei : shitei.substring(0, nami);
        Item moto = BuiltInRegistries.f_257033_.m_7745_(new ResourceLocation(id));
        ItemStack mono = new ItemStack(moto, Math.max(1, kazu));
        if (mono.m_41619_()) {
            mono = null;                       // その MOD が入っていない
        } else if (nami >= 0) {
            try {
                mono.m_41751_(TagParser.m_129359_(shitei.substring(nami)));
            } catch (Exception e) {
                // ★ 中身が読めなくても、アイテムそのものは出す。
                //   絵が出ないより、素の銃が出るほうがまし。
            }
        }
        MONO.put(kagi, mono);
        return mono;
    }

    /** GuiGraphics#renderItem(ItemStack, x, y) ── アイテムの絵を 16x16 で描く */
    public static void aitemWoEgaku(GuiGraphics g, ItemStack mono, int x, int y) {
        g.m_280480_(mono, x, y);
    }

    /** GuiGraphics#renderItemDecorations(Font, ItemStack, x, y) ── 右下の個数を重ねる */
    public static void aitemNoKazu(GuiGraphics g, ItemStack mono, int x, int y) {
        g.m_280370_(shotai(), mono, x, y);
    }

    // =========================================================
    //  サーバーが開いた画面（チェスト）を触る
    // =========================================================
    //
    // ★★ この MOD はサーバーと独自のやり取りをしない ★★
    //   プラグインは今までどおりチェスト画面を開く。
    //   こちらはその上に絵を被せ、押されたら【その枠を左クリックした】
    //   ことにして送るだけ。買う処理はプラグイン側のまま動く。
    //
    //   こうすると: ①サーバーを1行も変えずに済む
    //   ②MOD を入れていない人は今までどおりチェスト画面で買える
    //   ③買う判定は 655項目で確かめた既存のものがそのまま効く

    /**
     * アイテムの説明（名前＋説明文）を、素の文字の並びで返す。
     *
     * ★★ なぜ説明文が要るか ★★
     *   値段・条件・残高といった肝心な数字は、プラグインが
     *   **アイテムの説明文（lore）**に書いている。
     *   古地図の画面は文字を並べる余地が狭いので、
     *   カーソルを乗せた時だけ吹き出しで出す。
     *   ★ ここでも新しい通信は要らない。説明文はもう手元に届いている。
     */
    public static List<String> setsumei(ItemStack mono) {
        List<String> r = new ArrayList<>();
        if (mono == null || mono.m_41619_()) {
            return r;
        }
        // ItemStack#getTooltipLines(Player, TooltipFlag)
        for (Component c : mono.m_41651_(honntai().f_91074_, TooltipFlag.f_256752_)) {
            String s = c.getString();
            if (!s.isEmpty()) {
                r.add(s);
            }
        }
        return r;
    }

    /**
     * サーバーへコマンドを送る（頭のスラッシュは付けない）。
     *
     * ★★ これが、この MOD がサーバーへ物を言う唯一の口 ★★
     *   独自の通信は作らない決まりなので、「この画面を開きたい」のような
     *   お願いは、プラグインのコマンドを本人として打つ形で伝える。
     *   誰が打ったかはサーバーが分かっているので、偽れない。
     *   LocalPlayer#connection → ClientPacketListener#sendCommand(String)
     */
    public static void meirei(String cmd) {
        if (honntai().f_91074_ == null) {
            return;
        }
        honntai().f_91074_.f_108617_.m_246623_(cmd);
    }

    /** AbstractContainerMenu#getItems() ── その枠に何が入っているか。無ければ null */
    public static ItemStack wakuNoMono(AbstractContainerMenu menu, int waku) {
        if (menu == null || waku < 0) {
            return null;
        }
        List<ItemStack> naka = menu.m_38927_();
        return waku < naka.size() ? naka.get(waku) : null;
    }

    /**
     * MultiPlayerGameMode#handleInventoryMouseClick ──
     * その枠を左クリックしたことにして、サーバーへ送る。
     */
    public static void wakuWoOsu(AbstractContainerMenu menu, int waku) {
        if (menu == null || waku < 0) {
            return;
        }
        honntai().f_91072_.m_171799_(menu.f_38840_, waku, 0,
                ClickType.PICKUP, honntai().f_91074_);
    }

    /**
     * スコアボードの点数を読む。無ければ null。
     *
     * ★★ なぜこれで所持金が読めるか ★★
     *   個人の金は `kane_kojin` という目的に入っている。
     *   サーバーはそれを【Tab の一覧】に出しているので、
     *   その目的と点数はクライアントにも届いている（docs/SCOREBOARD.md）。
     *   だから独自の通信を作らなくても読める。
     *
     * ★ 勢力の金（chokin）は、サイドバーに【文字の行として】出ているだけで、
     *   目的そのものは届かない。だからこの方法では読めない。
     */
    public static Integer tensuu(String mochinushi, String mokuteki) {
        if (honntai().f_91073_ == null) {
            return null;
        }
        var ban = honntai().f_91073_.m_6188_();              // Level#getScoreboard()
        var moku = ban.m_83477_(mokuteki);                    // getObjective(名前)
        if (moku == null) {
            return null;
        }
        return ban.m_83471_(mochinushi, moku).m_83400_();     // 点数
    }

    /**
     * スコアボードで自分を指す名前。
     *
     * ★ getName()（表示名）ではなく getScoreboardName() を使う。
     *   表示名はチームの色や接頭辞で変わることがあり、
     *   点数の持ち主の名前とは別物になる。
     */
    public static String jibunNoNamae() {
        return honntai().f_91074_ == null ? null : honntai().f_91074_.m_6302_();
    }

    /**
     * 「持ち物を開くキー」（既定は E）が押されたか。
     *
     * ★ 69（E の番号）と直に比べない。キーを替えている人がいるので、
     *   Options.keyInventory（設定そのもの）に聞く。
     */
    public static boolean shimauKiKa(int kii, int sukyan) {
        return honntai().f_91066_.f_92092_.m_90832_(kii, sukyan);
    }

    /**
     * 自分のサイドバーに出ている目的（勢力ごとの hyouji_N）。無ければ null。
     *
     * ★ 表示枠の番号は 1 が全員共通のサイドバー、3+色番号 がチーム別。
     *   この企画は勢力の色ごとに別の目的を出しているので、まずチームの色で探す。
     */
    public static Objective sidebarMokuteki() {
        if (honntai().f_91073_ == null || honntai().f_91074_ == null) {
            return null;
        }
        var ban = honntai().f_91073_.m_6188_();               // Level#getScoreboard()
        var team = honntai().f_91074_.m_5647_();              // Entity#getTeam()
        if (team instanceof PlayerTeam pt) {
            int iro = pt.m_7414_().m_126656_();               // 色の番号 0..15
            if (iro >= 0 && iro < 16) {
                Objective o = ban.m_83416_(3 + iro);          // sidebar.team.<色>
                if (o != null) {
                    return o;
                }
            }
        }
        return ban.m_83416_(1);                                // ふつうのサイドバー
    }

    /** Objective#getDisplayName() ── 目的の見出し（勢力名） */
    public static String mokutekiMidashi(Objective o) {
        return o.m_83322_().getString();
    }

    /** その目的の行（保持者の名前 → 点数）。 */
    public static Map<String, Integer> mokutekiGyou(Objective o) {
        Map<String, Integer> de = new HashMap<>();
        var ban = honntai().f_91073_.m_6188_();
        for (Score s : ban.m_83498_(o)) {                      // getPlayerScores(Objective)
            de.put(s.m_83405_(), s.m_83400_());                // getOwner() / getScore()
        }
        return de;
    }

    /** Player#closeContainer() ── サーバーへ「画面を閉じた」と伝える */
    public static void tojiru() {
        if (honntai().f_91074_ != null) {
            honntai().f_91074_.m_6915_();
        }
    }

    /**
     * その枠が「まだ買えない」印か。
     * ★ プラグインは未解禁の枠を【灰色の板ガラス】で塞ぐ（Shop.MIKAIKIN）。
     *   こちらは中身を見て判断するので、解禁の条件を二重に持たなくて済む。
     */
    public static boolean mikaikinKa(ItemStack mono) {
        return mono != null && !mono.m_41619_()
                && mono.m_41720_() == aitem("minecraft:gray_stained_glass_pane", 1).m_41720_();
    }

    /**
     * ここから先の描画を、まとめて動かして拡大する。
     * 使い終わったら必ず {@link #orosu} を呼ぶ（呼ばないと以降の描画が全部ずれる）。
     */
    public static void tsumu(GuiGraphics g, int x, int y, float bai) {
        PoseStack t = g.m_280168_();                 // pose()
        t.m_85836_();                                // pushPose()
        t.m_252880_(x, y, 0.0F);                     // translate(x, y, 0)
        t.m_85841_(bai, bai, 1.0F);                  // scale(倍率, 倍率, 1)
    }

    /** {@link #tsumu} で積んだものを下ろす。 */
    public static void orosu(GuiGraphics g) {
        g.m_280168_().m_85849_();                    // pose().popPose()
    }

    /**
     * 字を【好きな大きさで】書く。
     *
     * ★★ ここがチェスト型 GUI との決定的な違い ★★
     *   チェスト型では、絵に焼き込んだ文字しか出せなかった。
     *   1商品ぶんの枠が 18px しかないため、日本語が 7px まで縮み、
     *   漢字の線がくっついて読めなくなっていた。
     *   MOD なら拡大率を自由に決められるので、つぶれない。
     *
     * @param bairitsu 1.0 が素の大きさ。2.0 なら倍の大きさ。
     */
    public static void kakuOokiku(GuiGraphics g, String s, int x, int y, int iro, float bairitsu) {
        PoseStack tsumiki = g.m_280168_();          // pose()
        tsumiki.m_85836_();                          // pushPose()
        tsumiki.m_252880_(x, y, 0.0F);               // translate(x, y, 0)
        tsumiki.m_85841_(bairitsu, bairitsu, 1.0F);  // scale(倍率, 倍率, 1)
        g.m_280488_(shotai(), s, 0, 0, iro);         // drawString(書体, 文字, 0, 0, 色)
        tsumiki.m_85849_();                          // popPose()
    }

    /** Screen.width ── 画面の横幅（GUIスケールを掛けたあとの論理px） */
    public static int gamenHaba(Screen gamen) {
        return gamen.f_96543_;
    }

    /** Screen.height ── 画面の高さ */
    public static int gamenTakasa(Screen gamen) {
        return gamen.f_96544_;
    }
}
