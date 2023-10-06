package dev.felnull.shortlifeplugin.commands;

/**
 * このプラグインのコマンド登録処理など
 *
 * @author MORIMORI0317
 */
public final class SLCommands {
    private SLCommands() {
    }

    /**
     * 初期呼び出し
     *
     * @see <a href="https://commandapi.jorel.dev/9.0.3/commandregistration.html">参考</a>
     */
    public static void init() {
        MatchCommand.create().register();
    }
}
