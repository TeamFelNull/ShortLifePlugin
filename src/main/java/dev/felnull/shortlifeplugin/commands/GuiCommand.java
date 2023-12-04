package dev.felnull.shortlifeplugin.commands;

import dev.felnull.shortlifeplugin.gui.SLGuis;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import xyz.xenondevs.invui.window.Window;

import java.util.Objects;

/**
 * GUIコマンド
 *
 * @author MORIMORI0317, Quarri6343
 */
public class GuiCommand {

    private GuiCommand() {
        throw new AssertionError();
    }
    
    /**
     * guiの引数
     *
     * @return guiの引数
     */
    public static Argument<SLGuis.WindowProvider> guiArgument() {
        return new CustomArgument<>(new StringArgument("gui"), 
                info -> SLGuis.getWindowProvider(info.input())
                        .orElseThrow(() -> CustomArgument.CustomArgumentException.fromMessageBuilder(new CustomArgument.MessageBuilder("不明なGUIです: ").appendArgInput())))
                .replaceSuggestions(ArgumentSuggestions.strings(info -> SLGuis.getAllGuiIds().toArray(String[]::new)));
    }

    /**
     * guiを開く
     *
     * @param sender コマンド送信者
     * @param args 引数
     */
    public static void guiOpen(CommandSender sender, CommandArguments args) {
        SLCommands.getSenderOrSelectedPlayers(sender, args).ifPresent(players -> players.forEach(player -> {
            Window window = ((SLGuis.WindowProvider) Objects.requireNonNull(args.get("gui"))).provide(player);
            window.open();
        }));
    }
}
