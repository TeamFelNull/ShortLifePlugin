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
import org.jetbrains.annotations.NotNull;

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
     * コマンド名
     */
    private static final String NAME = "room";

    /**
     * 試合中、未試合中関係なく全ての部屋IDと名前
     */
    private static final Supplier<Map<String, String>> ALL_ID_NAME = Suppliers.memoize(() -> {
        ImmutableMap.Builder<String, String> idAndNames = new ImmutableMap.Builder<>();

        // 全PVPルームを取得
        getAllRoomsIDAndName(MatchSelectorGui.PVP_ROOM_SIZE, idAndNames, MatchType.PVP);

        // 全PVEルームを取得
        getAllRoomsIDAndName(MatchSelectorGui.PVE_ROOM_SIZE, idAndNames, MatchType.PVE);

        return idAndNames.build();
    });

    /**
     * 全ての特定のタイプの試合の部屋を取得
     *
     * @param roomSize 部屋の最大数
     * @param idAndNames 部屋IDと名前を格納するmap
     * @param matchType 部屋の試合のタイプ
     */
    private static void getAllRoomsIDAndName(int roomSize, ImmutableMap.Builder<String, String> idAndNames, MatchType matchType) {
        for (int i = 0; i < roomSize; i++) {
            idAndNames.put(MatchSelectorGui.getRoomMatchId(matchType, i), MatchRoomSelectItem.getRoomName(matchType, i));
        }
    }

    @Override
    public CommandAPICommand create() {
        CommandAPICommand join = new CommandAPICommand("join")
                .withArguments(roomIdArgument())
                .executes(RoomCommand::join);

        CommandAPICommand leave = new CommandAPICommand("leave")
                .executes((CommandExecutor) (sender, args) -> leave(sender));

        return new CommandAPICommand(NAME)
                .withAliases("slr")
                .withPermission(SLPermissions.COMMANDS_ROOM.get())
                .withSubcommands(join, leave);
    }

    @Override
    public void unregister() {
        CommandAPI.unregister(NAME);
    }

    private static Argument<String> roomIdArgument() {
        return new CustomArgument<>(new StringArgument("room id"), info -> {
            List<String> roomIds = getExistingRoomIds();
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
        }).replaceSuggestions(ArgumentSuggestions.strings(info -> getExistingRoomIds().toArray(String[]::new)));
    }

    /**
     * 全ての存在する部屋IDを取得
     */
    private static List<String> getExistingRoomIds() {
        ImmutableList.Builder<String> ids = new ImmutableList.Builder<>();

        // 全PVPルームを取得
        getExistingRoomIdsOfType(MatchSelectorGui.PVP_ROOM_SIZE, MatchType.PVP, ids);

        // 全PVEルームを取得
        getExistingRoomIdsOfType(MatchSelectorGui.PVE_ROOM_SIZE, MatchType.PVE, ids);

        return ids.build();
    }

    /**
     * 全ての存在する特定の種類の部屋IDを取得
     *
     * @param roomSize 部屋のとりうる最大数
     * @param matchType 試合の種類
     * @param ids 部屋IDを格納するList
     */
    private static void getExistingRoomIdsOfType(int roomSize, MatchType matchType, ImmutableList.Builder<String> ids) {
        MatchManager matchManager = MatchManager.getInstance();
        
        for (int i = 0; i < roomSize; i++) {
            String matchId = MatchSelectorGui.getRoomMatchId(matchType, i);
            if (matchManager.getMatch(matchId).isPresent()) {
                ids.add(matchId);
            }
        }
    }

    /**
     * 部屋に参加させる
     *
     * @param sender コマンド送信者
     * @param args 引数
     */
    private static void join(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) {
            sender.sendRichMessage("このコマンドを使用できるのはプレイヤーのみです");
            return;
        }
        String roomId = (String) Objects.requireNonNull(args.get("room id"));

        MatchManager matchManager = MatchManager.getInstance();

        matchManager.getMatch(roomId).ifPresentOrElse(matchToJoin ->
                joinMatch(player, matchManager, matchToJoin), () -> player.sendMessage(Component.text("試合を取得できませんでした")));
    }

    /**
     * プレイヤーを試合に参加させる
     *
     * @param player プレイヤー
     * @param matchManager マッチマネージャー
     * @param matchToJoin 参加させたい試合
     */
    private static void joinMatch(Player player, MatchManager matchManager, @NotNull Match matchToJoin) {
        matchManager.getJoinedMatch(player).ifPresentOrElse(joinedMatch -> {
            if (matchToJoin == joinedMatch) {
                player.sendMessage(Component.text("既に参加しています"));
            } else {
                player.sendMessage(Component.text("既に別の試合に参加しています"));
            }
        }, () -> {
            if (!matchToJoin.join(player, true)) {
                player.sendRichMessage("参加できませんでした");
            }
        });
    }

    /**
     * 部屋から離脱させる
     *
     * @param sender コマンド送信者
     */
    private static void leave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendRichMessage("このコマンドを使用できるのはプレイヤーのみです");
            return;
        }

        MatchManager matchManager = MatchManager.getInstance();
        matchManager.getJoinedMatch(player).ifPresentOrElse(joinedMatch -> leaveMatch(player, joinedMatch),
                () -> player.sendMessage(Component.text("試合に参加していません")));
    }

    /**
     * 試合から離脱させる
     *
     * @param player プレイヤー
     * @param joinedMatch 参加した試合
     */
    private static void leaveMatch(Player player, Match joinedMatch) {
        if (!joinedMatch.leave(player, true)) {
            player.sendMessage(Component.text("退出できませんでした"));
        }
    }
}
