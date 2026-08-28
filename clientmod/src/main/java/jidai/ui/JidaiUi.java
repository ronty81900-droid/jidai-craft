package jidai.ui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * MOD の入口。
 *
 * いまは「J キーで試作画面を開く」だけ。
 * 確かめたいのは【ビルドが通って、実機で画面が出るか】の一点なので、
 * ここでは作り込まない。
 */
@Mod(JidaiUi.ID)
public class JidaiUi {

    /** mods.toml の modId と必ず同じにする。ずれると読み込まれない。 */
    public static final String ID = "jidaiui";

    public JidaiUi() {
        // いまは何もしない。キー入力は下の Kii が受ける。
    }

    /**
     * キー入力を受ける。
     *
     * ★ KeyMapping を登録する形にしていないのは、
     *   いま確かめたいのが「画面が出るか」だけで、
     *   キーの設定画面への登録は後でよいため。
     *   （本番では設定から変えられるようにする）
     *
     * ★★ Ctrl+Shift を必須にした（2026-08-22）★★
     *   ここで開く2つは【作り手の道具】で、参加者には要らない。
     *   素の J / K のままだと、50人が遊んでいる最中に
     *   誰かが押して知らない画面が出る。実際、K で開く測定画面が
     *   きっかけでクラッシュ報告が来た。
     *   本番で暴発しない組み合わせにして、道具は残す。
     */
    @Mod.EventBusSubscriber(modid = ID, value = Dist.CLIENT)
    public static class Kii {

        /** GLFW の J キー。数値は GLFW の決まり（GLFW_KEY_J = 74）。 */
        private static final int KEY_J = 74;

        /** K キー。文字の収まりを測り直す時に使う。 */
        private static final int KEY_K = 75;
        /** GLFW_PRESS = 1（押した瞬間。押しっぱなしは 2） */
        private static final int OSHITA = 1;

        /** GLFW の修飾キー。SHIFT=0x1 / CONTROL=0x2。両方 押している時だけ通す。 */
        private static final int SHIFT = 0x1;
        private static final int CTRL = 0x2;

        @SubscribeEvent
        public static void osareta(InputEvent.Key e) {
            if (e.getAction() != OSHITA) {
                return;
            }
            // ★ ほかの画面が開いている時に割り込むと、チャットの入力などを壊す
            if (Mc.imaNoGamen() != null) {
                return;
            }
            // ★ 作り手の道具。Ctrl+Shift を押していない限り開かない
            if ((e.getModifiers() & (SHIFT | CTRL)) != (SHIFT | CTRL)) {
                return;
            }
            if (e.getKey() == KEY_J) {
                Mc.gamenWoDasu(new UiGamen(0));
            } else if (e.getKey() == KEY_K) {
                Mc.gamenWoDasu(new SokuteiGamen());
            }
        }
    }
}
