package dev.felnull.shortlifeplugin.commands;

import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.resources.TextureReleaseWatcher;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;

/**
 * テクスチャ関係コマンド
 *
 * @author MORIMORI0317
 */
public class TextureCommand {
    private TextureCommand() {
        throw new AssertionError();
    }


    /**
     * テクスチャリロード
     *
     * @param sender 送信者
     * @param args   引数
     */
    protected static void reload(CommandSender sender, CommandArguments args) {
        TextureReleaseWatcher releaseWatcher = TextureReleaseWatcher.getInstance();

        if (releaseWatcher.isReleaseChecking()) {
            sender.sendRichMessage(MsgHandler.get("cmd-texture-reload-already"));
            return;
        }

        if (releaseWatcher.manualReleaseCheck()) {
            sender.sendRichMessage(MsgHandler.get("cmd-texture-reload-start"));
        } else {
            sender.sendRichMessage(MsgHandler.get("cmd-texture-reload-failure"));
        }
    }
}
