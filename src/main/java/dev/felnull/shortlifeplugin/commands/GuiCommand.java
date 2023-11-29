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

    @Override
    public CommandAPICommand create() {
        return new CommandAPICommand("gui")
                .withAliases("slg")
                .withPermission(SLPermissions.COMMANDS_GUI.get())
                .withSubcommands(new CommandAPICommand("open")
                        .withArguments(guiArgument("gui"))
                        .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("player"))
                        .executes(this::guiOpen));
    }

    @Override
    public void unregister() {
        CommandAPI.unregister("gui");
    }

    private Argument<SLGuis.WindowProvider> guiArgument(String nodeName) {
        return new CustomArgument<>(new StringArgument(nodeName), 
                info -> SLGuis.getWindowProvider(info.input())
                        .orElseThrow(() -> CustomArgument.CustomArgumentException.fromMessageBuilder(new CustomArgument.MessageBuilder("不明なGUIです: ").appendArgInput())))
                .replaceSuggestions(ArgumentSuggestions.strings(info -> SLGuis.getAllGuiIds().toArray(String[]::new)));
    }

    private void guiOpen(CommandSender sender, CommandArguments args) {
        getSenderOrSelectedPlayers(sender, args).ifPresent(players -> players.forEach(player -> {
            Window window = ((SLGuis.WindowProvider) Objects.requireNonNull(args.get("gui"))).provide(player);
            window.open();
        }));
    }
}
