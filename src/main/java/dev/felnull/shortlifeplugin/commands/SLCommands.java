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
     * コマンドの登録
     *
     * @see <a href="https://commandapi.jorel.dev/9.0.3/commandregistration.html">参考</a>
     */
    public static void register() {
        MatchCommand.create().register();
    }

    /**
     * コマンドの登録を解除
     *
     * @see <a href="https://commandapi.jorel.dev/9.1.0/commandunregistration.html">参考</a>
     */
    public static void unregister() {
        MatchCommand.unregister();
    }
}
