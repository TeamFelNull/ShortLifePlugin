package dev.felnull.shortlifeplugin.commands;

import dev.felnull.shortlifeplugin.SLPermissions;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;

import static dev.felnull.shortlifeplugin.commands.RewardSubCommands.values;

/**
 * 報酬コマンド
 *
 * @author raindazo
 */
public class RewardCommand implements SLCommand {

    /**
     * 報酬コマンド本体の名前
     */
    private static final String NAME = "reward";

    @Override
    public CommandAPICommand create() {

        final CommandAPICommand rewardCommand = new CommandAPICommand(NAME)
                .withPermission(SLPermissions.COMMANDS_REWARD.get());

        for (RewardSubCommands value : values()) {
            rewardCommand.withSubcommand(value.construct());
        }
        return rewardCommand;
    }

    @Override
    public void unregister() {
        CommandAPI.unregister(NAME);
    }
}
