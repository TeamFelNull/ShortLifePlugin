package dev.felnull.shortlifeplugin.commands;

import dev.felnull.shortlifeplugin.SLPermissions;
import dev.felnull.shortlifeplugin.gui.SLGuis;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import xyz.xenondevs.invui.window.Window;

import java.util.Objects;

/**
 * GUIコマンド
 *
 * @author MORIMORI0317
 */
public class GuiCommand implements SLCommand {

    /**
     * コマンド名
     */
    private static final String NAME = "gui";
    
    @Override
    public CommandAPICommand create() {
        return new CommandAPICommand(NAME)
                .withAliases("slg")
                .withPermission(SLPermissions.COMMANDS_GUI.get())
                .withSubcommands(new CommandAPICommand("open")
                        .withArguments(guiArgument())
                        .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("player"))
                        .executes(GuiCommand::guiOpen));
    }

    @Override
    public void unregister() {
        CommandAPI.unregister(NAME);
    }

    private Argument<SLGuis.WindowProvider> guiArgument() {
        return new CustomArgument<>(new StringArgument("gui"), 
                info -> SLGuis.getWindowProvider(info.input())
                        .orElseThrow(() -> CustomArgument.CustomArgumentException.fromMessageBuilder(new CustomArgument.MessageBuilder("不明なGUIです: ").appendArgInput())))
                .replaceSuggestions(ArgumentSuggestions.strings(info -> SLGuis.getAllGuiIds().toArray(String[]::new)));
    }

    private static void guiOpen(CommandSender sender, CommandArguments args) {
        SLCommand.getSenderOrSelectedPlayers(sender, args).ifPresent(players -> players.forEach(player -> {
            Window window = ((SLGuis.WindowProvider) Objects.requireNonNull(args.get("gui"))).provide(player);
            window.open();
        }));
    }
}
