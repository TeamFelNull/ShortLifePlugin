package dev.felnull.shortlifeplugin.commands;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.gui.MatchSelectorGui;
import dev.felnull.shortlifeplugin.gui.item.MatchRoomSelectItem;
import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.MatchType;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
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
 * @author MORIMORI0317, Quarri6343
 */
public class RoomCommand {
    
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

    private RoomCommand() {
        throw new AssertionError();
    }

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

    /**
     * 部屋ID引数
     *
     * @return 部屋ID引数
     */
    public static Argument<String> roomIdArgument() {
        return new CustomArgument<>(new StringArgument("room id"), info -> {
            List<String> roomIds = getExistingRoomIds();
            String roomId = info.input();

            if (StringUtils.isEmpty(roomId) || !roomIds.contains(roomId)) {

                String errorMessage;
                if (ALL_ID_NAME.get().containsKey(roomId)) {
                    errorMessage = MsgHandler.get("cmd-room-not-in-game");
                } else {
                    errorMessage = MsgHandler.get("cmd-room-not-exist");
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
    public static void join(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) {
            sender.sendRichMessage(MsgHandler.get("cmd-general-player-only"));
            return;
        }
        String roomId = (String) Objects.requireNonNull(args.get("room id"));

        MatchManager matchManager = MatchManager.getInstance();

        matchManager.getMatch(roomId).ifPresentOrElse(matchToJoin ->
                joinMatch(player, matchManager, matchToJoin), 
                () -> player.sendMessage(Component.text(MsgHandler.get("cmd-room-cannot-get-match"))));
    }

    /**
     * プレイヤーを試合に参加させる
     *
     * @param player プレイヤー
     * @param matchManager マッチマネージャー
     * @param matchToJoin 参加させたい試合
     */
    public static void joinMatch(Player player, MatchManager matchManager, @NotNull Match matchToJoin) {
        matchManager.getJoinedMatch(player).ifPresentOrElse(joinedMatch -> {
            if (matchToJoin == joinedMatch) {
                player.sendMessage(Component.text(MsgHandler.get("cmd-room-match-already-joined")));
            } else {
                player.sendMessage(Component.text(MsgHandler.get("cmd-room-other-match-already-joined")));
            }
        }, () -> {
            if (!matchToJoin.join(player, true)) {
                player.sendRichMessage(MsgHandler.get("cmd-room-failed-to-join"));
            }
        });
    }

    /**
     * 部屋から離脱させる
     *
     * @param sender コマンド送信者
     */
    public static void leave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendRichMessage(MsgHandler.get("cmd-general-player-only"));
            return;
        }

        MatchManager matchManager = MatchManager.getInstance();
        matchManager.getJoinedMatch(player).ifPresentOrElse(joinedMatch -> leaveMatch(player, joinedMatch),
                () -> player.sendMessage(Component.text(MsgHandler.get("cmd-room-match-not-joined"))));
    }

    /**
     * 試合から離脱させる
     *
     * @param player プレイヤー
     * @param joinedMatch 参加した試合
     */
    private static void leaveMatch(Player player, Match joinedMatch) {
        if (!joinedMatch.leave(player, true)) {
            player.sendMessage(Component.text(MsgHandler.get("cmd-room-match-failed-to-leave")));
        }
    }
}
