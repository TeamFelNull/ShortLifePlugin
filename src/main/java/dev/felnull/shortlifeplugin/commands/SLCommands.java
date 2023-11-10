package dev.felnull.shortlifeplugin.commands;

import java.util.LinkedList;
import java.util.List;

/**
 * このプラグインのコマンド登録処理など
 *
 * @author MORIMORI0317
 */
public final class SLCommands {

    /**
     * 登録されたすべてのコマンド
     */
    private static final List<SLCommand> COMMAND_PROVIDERS = new LinkedList<>();

    private SLCommands() {
    }

    /**
     * コマンドの登録
     *
     * @see <a href="https://commandapi.jorel.dev/9.0.3/commandregistration.html">参考</a>
     */
    public static void register() {
        COMMAND_PROVIDERS.add(new MatchCommand());
        COMMAND_PROVIDERS.add(new GuiCommand());
        COMMAND_PROVIDERS.add(new EquipmentGroupCommand());
        COMMAND_PROVIDERS.add(new RoomCommand());

        COMMAND_PROVIDERS.forEach(commandProvider -> commandProvider.create().register());
    }

    /**
     * コマンドの登録を解除
     *
     * @see <a href="https://commandapi.jorel.dev/9.1.0/commandunregistration.html">参考</a>
     */
    public static void unregister() {
        COMMAND_PROVIDERS.forEach(SLCommand::unregister);
    }
}
