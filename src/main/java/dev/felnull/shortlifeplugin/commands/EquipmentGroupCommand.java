package dev.felnull.shortlifeplugin.commands;

import dev.felnull.shortlifeplugin.SLPermissions;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;

import static dev.felnull.shortlifeplugin.commands.EquipmentGroupSubCommands.*;

/**
 * 装備グループコマンド
 *
 * @author MORIMORI0317
 */
public class EquipmentGroupCommand implements SLCommand {

    /**
     * コマンド名
     */
    private static final String NAME = "equipmentgroup";
    

    @Override
    public CommandAPICommand create() {

        return new CommandAPICommand(NAME)
                .withAliases("sleg")
                .withPermission(SLPermissions.COMMANDS_EQUIPMENT_GROUP.get())
                .withSubcommands(ADD.get(), MODIFY.get(), REMOVE.get(), LIST.get(), INFO.get());
    }

    @Override
    public void unregister() {
        CommandAPI.unregister(NAME);
    }
}
