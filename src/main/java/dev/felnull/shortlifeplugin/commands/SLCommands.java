package dev.felnull.shortlifeplugin.commands;

import com.google.common.collect.ImmutableList;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * このプラグインのコマンド登録処理など
 *
 * @author MORIMORI0317, Quarri6343
 */
public class SLCommands {

    private SLCommands() {
        throw new AssertionError();
    }
    
    /**
     * コマンドの登録
     *
     * @see <a href="https://commandapi.jorel.dev/9.0.3/commandregistration.html">参考</a>
     */
    public static void register() {
        for (SLCommandsList command : SLCommandsList.values()) {
            command.create().register();
        }
    }

    /**
     * コマンドの登録を解除
     *
     * @see <a href="https://commandapi.jorel.dev/9.1.0/commandunregistration.html">参考</a>
     */
    public static void unregisterAll() {
        for (SLCommandsList command : SLCommandsList.values()) {
            command.unregister();
        }
    }

    /**
     * 実行者もしくは引数でプレイヤーセレクタで指定されたプレイヤーを取得
     *
     * @param sender CommandSender
     * @param args   CommandArguments
     * @return オプショナルなプレイヤーリスト
     */
    static Optional<List<Player>> getSenderOrSelectedPlayers(CommandSender sender, CommandArguments args) {
        @SuppressWarnings("unchecked")
        Optional<List<Player>> players = args.getOptional("player")
                .map(it -> (List<Player>) it);

        if (players.isEmpty()) {
            if (!(sender instanceof Player player)) {
                sender.sendRichMessage("プレイヤーセレクタを指定しない場合は、プレイヤーのみ実行可能です");
                return Optional.empty();
            }

            return Optional.of(ImmutableList.of(player));
        }

        return players;
    }
}
