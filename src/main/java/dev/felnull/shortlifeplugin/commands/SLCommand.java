package dev.felnull.shortlifeplugin.commands;

import com.google.common.collect.ImmutableList;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * このプラグインのコマンド
 *
 * @author MORIMORI0317
 */
public interface SLCommand {

    /**
     * コマンドを作成
     *
     * @return コマンド
     */
    CommandAPICommand create();

    /**
     * コマンドの登録解除
     */
    void unregister();

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
