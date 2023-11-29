package dev.felnull.shortlifeplugin.commands;

import dev.felnull.shortlifeplugin.SLPermissions;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;

import static dev.felnull.shortlifeplugin.commands.MatchSubCommands.*;

/**
 * 試合コマンド
 *
 * @author MORIMORI0317
 */
public class MatchCommand implements SLCommand {

    /**
     * コマンド名
     */
    private static final String NAME = "match";

    @Override
    public CommandAPICommand create() {
        // https://commandapi.jorel.dev/9.0.3/commandregistration.html <-参考
        return new CommandAPICommand(NAME)
                .withAliases("slm")
                .withPermission(SLPermissions.COMMANDS_MATCH.get())
                .withSubcommands(LIST.get(), INFO.get(), JOIN.get(), LEAVE.get(), FINISH.get(), START.get(), REMOVE.get(), MAP.get());
    }

    @Override
    public void unregister() {
        CommandAPI.unregister(NAME);
    }
}
