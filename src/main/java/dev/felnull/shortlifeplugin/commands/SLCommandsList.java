package dev.felnull.shortlifeplugin.commands;

import dev.felnull.shortlifeplugin.SLPermissions;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.executors.CommandExecutor;

/**
 * このプラグインのコマンドリスト
 *
 * @author MORIMORI0317, Quarri6343
 */
public enum SLCommandsList {
    /**
     * 装備グループコマンド
     */
    EQUIPMENT("equipmentgroup") {
        @Override
        public CommandAPICommand create() {
            return new CommandAPICommand(name)
                    .withAliases("sleg")
                    .withPermission(SLPermissions.COMMANDS_EQUIPMENT_GROUP.get())
                    .withSubcommands(EquipmentGroupSubCommands.ADD.get(), EquipmentGroupSubCommands.MODIFY.get(),
                            EquipmentGroupSubCommands.REMOVE.get(), EquipmentGroupSubCommands.LIST.get(), EquipmentGroupSubCommands.INFO.get());
        }
    },

    /**
     * 試合コマンド
     */
    MATCH("match") {
        @Override
        public CommandAPICommand create() {
            // https://commandapi.jorel.dev/9.0.3/commandregistration.html <-参考
            return new CommandAPICommand(name)
                    .withAliases("slm")
                    .withPermission(SLPermissions.COMMANDS_MATCH.get())
                    .withSubcommands(MatchSubCommands.LIST.get(), MatchSubCommands.INFO.get(), MatchSubCommands.JOIN.get(), MatchSubCommands.LEAVE.get(),
                            MatchSubCommands.FINISH.get(), MatchSubCommands.START.get(), MatchSubCommands.REMOVE.get(), MatchSubCommands.MAP.get());
        }
    },

    /**
     * guiコマンド
     */
    gui("gui") {
        @Override
        public CommandAPICommand create() {
            return new CommandAPICommand(name)
                    .withAliases("slg")
                    .withPermission(SLPermissions.COMMANDS_GUI.get())
                    .withSubcommands(new CommandAPICommand("open")
                            .withArguments(GuiCommand.guiArgument())
                            .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("player"))
                            .executes(GuiCommand::guiOpen));
        }
    },

    /**
     * 部屋コマンド
     */
    ROOM("room") {
        @Override
        public CommandAPICommand create() {
            CommandAPICommand join = new CommandAPICommand("join")
                    .withArguments(RoomCommand.roomIdArgument())
                    .executes(RoomCommand::join);

            CommandAPICommand leave = new CommandAPICommand("leave")
                    .executes((CommandExecutor) (sender, args) -> RoomCommand.leave(sender));

            return new CommandAPICommand(name)
                    .withAliases("slr")
                    .withPermission(SLPermissions.COMMANDS_ROOM.get())
                    .withSubcommands(join, leave);
        }
    },

    /**
     * 報酬コマンド
     */
    REWARD("reward") {
        @Override
        public CommandAPICommand create() {
            final CommandAPICommand rewardCommand = new CommandAPICommand(name)
                    .withPermission(SLPermissions.COMMANDS_REWARD.get());

            for (RewardSubCommands value : RewardSubCommands.values()) {
                rewardCommand.withSubcommand(value.construct());
            }
            return rewardCommand;
        }
    },

    TEXTURE("sltexture") {
        @Override
        public CommandAPICommand create() {
            return new CommandAPICommand(name)
                    .withAliases("slt")
                    .withPermission(SLPermissions.COMMANDS_TEXTURE.get())
                    .withSubcommands(new CommandAPICommand("reload")
                            .executes(TextureCommand::reload));
        }
    };

    /**
     * コマンド名
     */
    public final String name;

    SLCommandsList(String name) {
        this.name = name;
    }

    /**
     * コマンドを作成
     *
     * @return 作成されたコマンド
     */
    public abstract CommandAPICommand create();

    /**
     * コマンドを登録解除
     */
    public void unregister() {
        CommandAPI.unregister(name);
    }
}
