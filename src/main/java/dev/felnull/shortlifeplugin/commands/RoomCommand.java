package dev.felnull.shortlifeplugin.commands;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.felnull.shortlifeplugin.SLPermissions;
import dev.felnull.shortlifeplugin.gui.MatchSelectorGui;
import dev.felnull.shortlifeplugin.gui.item.MatchRoomSelectItem;
import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.MatchType;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.CommandExecutor;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 部屋コマンド
 *
 * @author MORIMORI0317
 */
public class RoomCommand implements SLCommand {

    /**
     * 試合中、未試合中関係なく全ての部屋IDと名前
     */
    private static final Supplier<Map<String, String>> ALL_ID_NAME = Suppliers.memoize(() -> {
        ImmutableMap.Builder<String, String> idAndNames = new ImmutableMap.Builder<>();

        // 全PVPルームを取得
        for (int i = 0; i < MatchSelectorGui.PVP_ROOM_SIZE; i++) {
            idAndNames.put(MatchSelectorGui.getRoomMatchId(MatchType.PVP, i), MatchRoomSelectItem.getRoomName(MatchType.PVP, i));
        }

        // 全PVEルームを取得
        for (int i = 0; i < MatchSelectorGui.PVE_ROOM_SIZE; i++) {
            idAndNames.put(MatchSelectorGui.getRoomMatchId(MatchType.PVE, i), MatchRoomSelectItem.getRoomName(MatchType.PVE, i));
        }

        return idAndNames.build();
    });

    @Override
    public CommandAPICommand create() {
        CommandAPICommand join = new CommandAPICommand("join")
                .withArguments(roomIdArgument("room id"))
                .executes(this::join);

        CommandAPICommand leave = new CommandAPICommand("leave")
                .executes((CommandExecutor) (sender, args) -> leave(sender));

        return new CommandAPICommand("room")
                .withAliases("slr")
                .withPermission(SLPermissions.COMMANDS_ROOM)
                .withSubcommands(join, leave);
    }

    @Override
    public void unregister() {
        CommandAPI.unregister("room");
    }

    private Argument<String> roomIdArgument(String nodeName) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            List<String> roomIds = roomIds();
            String roomId = info.input();

            if (StringUtils.isEmpty(roomId) || !roomIds.contains(roomId)) {

                String errorMessage;
                if (ALL_ID_NAME.get().containsKey(roomId)) {
                    errorMessage = "試合中ではない部屋です: ";
                } else {
                    errorMessage = "存在しない試合部屋です: ";
                }

                throw CustomArgument.CustomArgumentException
                        .fromMessageBuilder(new CustomArgument.MessageBuilder(errorMessage).appendArgInput());
            } else {
                return roomId;
            }
        }).replaceSuggestions(ArgumentSuggestions.strings(info -> roomIds().toArray(String[]::new)));
    }

    private List<String> roomIds() {
        MatchManager matchManager = MatchManager.getInstance();
        ImmutableList.Builder<String> ids = new ImmutableList.Builder<>();

        // 全PVPルームを取得
        for (int i = 0; i < MatchSelectorGui.PVP_ROOM_SIZE; i++) {
            String matchId = MatchSelectorGui.getRoomMatchId(MatchType.PVP, i);
            if (matchManager.getMatch(matchId) != null) {
                ids.add(matchId);
            }
        }

        // 全PVEルームを取得
        for (int i = 0; i < MatchSelectorGui.PVE_ROOM_SIZE; i++) {
            String matchId = MatchSelectorGui.getRoomMatchId(MatchType.PVE, i);
            if (matchManager.getMatch(matchId) != null) {
                ids.add(matchId);
            }
        }

        return ids.build();
    }

    private void join(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) {
            sender.sendRichMessage("このコマンドを使用できるのはプレイヤーのみです");
            return;
        }
        String roomId = (String) Objects.requireNonNull(args.get("room id"));

        MatchManager matchManager = MatchManager.getInstance();
        Match match = matchManager.getMatch(roomId);

        if (match == null) {
            player.sendMessage(Component.text("試合を取得できませんでした"));
            return;
        }

        Match jointedMatch = matchManager.getJoinedMatch(player);

        if (match == jointedMatch) {
            player.sendMessage(Component.text("既に参加しています"));
            return;
        } else if (jointedMatch != null) {
            player.sendMessage(Component.text("既に別の試合に参加しています"));
            return;
        }

        if (!match.join(player, true)) {
            sender.sendRichMessage("参加できませんでした");
        }
    }

    private void leave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendRichMessage("このコマンドを使用できるのはプレイヤーのみです");
            return;
        }

        MatchManager matchManager = MatchManager.getInstance();
        Match jointedMatch = matchManager.getJoinedMatch(player);

        if (jointedMatch != null) {
            if (!jointedMatch.leave(player, true)) {
                player.sendMessage(Component.text("退出できませんでした"));
            }
        } else {
            player.sendMessage(Component.text("試合に参加していません"));
        }
    }
}
