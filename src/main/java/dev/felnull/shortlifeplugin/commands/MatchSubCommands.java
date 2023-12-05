package dev.felnull.shortlifeplugin.commands;

import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.MatchMode;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import dev.felnull.shortlifeplugin.match.map.MatchMapHandler;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.CommandExecutor;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 試合サブコマンド
 *
 * @author MORIMORI0317, Quarri6343
 */
public enum MatchSubCommands {

    /**
     * 列挙
     */
    LIST(new CommandAPICommand("list")
            .executes((CommandExecutor) (sender, args) -> matchList(sender))),

    /**
     * 情報
     */
    INFO(new CommandAPICommand("info")
            .withArguments(matchArgument())
            .executes(MatchSubCommands::matchInfo)),

    /**
     * 参加
     */
    JOIN(new CommandAPICommand("join")
            .withArguments(matchArgument())
            .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("player"))
            .executes(MatchSubCommands::onMatchJoinCommand)),

    /**
     * 退出
     */
    LEAVE(new CommandAPICommand("leave")
            .withArguments(matchArgument())
            .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("player"))
            .executes(MatchSubCommands::onMatchLeaveCommand)),

    /**
     * 開始
     */
    START(new CommandAPICommand("start")
            .withArguments(matchArgument())
            .executes(MatchSubCommands::matchStart)),

    /**
     * 終了
     */
    FINISH(new CommandAPICommand("finish")
            .withArguments(matchArgument())
            .executes(MatchSubCommands::matchFinish)),

    /**
     * 削除
     */
    REMOVE(new CommandAPICommand("remove")
            .withArguments(matchArgument())
            .executes(MatchSubCommands::matchRemove)),

    /**
     * マップ
     */
    MAP(new CommandAPICommand("map")
            .withSubcommands(new CommandAPICommand("list")
                    .executes((CommandExecutor) (sender, args) -> mapList(sender))
                    .withSubcommands(new CommandAPICommand("info")
                            .withArguments(mapArgument())
                            .executes(MatchSubCommands::mapInfo))));


    /**
     * サブコマンド本体
     */
    private final CommandAPICommand subCommand;

    MatchSubCommands(CommandAPICommand command) {
        this.subCommand = command;
    }

    /**
     * サブコマンドを構築する
     *
     * @return サブコマンド
     */
    public CommandAPICommand get() {
        return subCommand;
    }

    /**
     * 試合の引数
     *
     * @return 試合引数
     */
    private static Argument<Match> matchArgument() {
        return new CustomArgument<>(new StringArgument("match"),
                info -> MatchManager.getInstance().getMatch(info.input())
                        .orElseThrow(() -> CustomArgument.CustomArgumentException
                                .fromMessageBuilder(new CustomArgument.MessageBuilder(MsgHandler.get("cmd-match-unknown")).appendArgInput())))
                .replaceSuggestions(ArgumentSuggestions.strings(info -> MatchManager.getInstance().getAllMatch().keySet().toArray(String[]::new)));
    }

    /**
     * マップの引数
     *
     * @return マップ引数
     */
    private static Argument<MatchMap> mapArgument() {
        return new CustomArgument<>(new StringArgument("map"),
                info -> MatchManager.getInstance().getMapLoader().getMap(info.input())
                        .orElseThrow(() -> CustomArgument.CustomArgumentException
                                .fromMessageBuilder(new CustomArgument.MessageBuilder(MsgHandler.get("cmd-match-map-unknown")).appendArgInput())))
                .replaceSuggestions(ArgumentSuggestions.strings(info -> MatchManager.getInstance().getMapLoader().getAllMap().keySet().toArray(String[]::new)));
    }

    /**
     * 試合リストをコマンド送信者に表示
     *
     * @param sender 送信者
     */
    private static void matchList(CommandSender sender) {
        MatchManager matchManager = MatchManager.getInstance();
        Map<String, Match> matches = matchManager.getAllMatch();

        if (matches.isEmpty()) {
            sender.sendRichMessage(MsgHandler.get("cmd-match-not-exists"));
        } else {
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-count", matches.size()));

            matches.values().forEach(match -> sender.sendMessage(Component.text(String.format("- %s (", match.getId()))
                    .append(Component.text(match.getMatchMode().name()))
                    .append(Component.text(")"))));

        }
    }

    /**
     * 試合情報を表示
     *
     * @param sender コマンド送信者
     * @param args   試合引数
     */
    private static void matchInfo(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));

        sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-info", match.getId()));
        List<Component> list = new LinkedList<>();
        match.appendInfoDesc(list);
        list.forEach(sender::sendMessage);
    }

    /**
     * コマンドを元に試合に参加させる
     *
     * @param sender コマンド送信者
     * @param args   試合引数
     */
    private static void onMatchJoinCommand(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));
        SLCommands.getSenderOrSelectedPlayers(sender, args).ifPresent(players -> {
            if (players.size() == 1) {
                matchJoinPlayer(sender, match, players.get(0));
            } else {
                matchJoinPlayers(sender, match, players);
            }
        });

    }

    /**
     * プレイヤー一人を試合に参加させる
     *
     * @param sender コマンド送信者
     * @param match  試合
     * @param player 参加させるプレイヤー
     */
    private static void matchJoinPlayer(CommandSender sender, Match match, Player player) {
        if (match.hasParticipation(player)) {
            if (player == sender) {
                sender.sendRichMessage(MsgHandler.get("cmd-match-already-joined"));
            } else {
                sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-player-already-joined", player.getName()));
            }
            return;
        }

        MatchManager.getInstance().getJoinedMatch(player).ifPresentOrElse(joinedMatch -> {
            if (player == sender) {
                sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-already-joined-to-match", joinedMatch.getId()));
            } else {
                sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-player-already-joined-to-match", player.getName(), joinedMatch.getId()));
            }
        }, () -> {
            if (match.join(player, player != sender)) {
                if (player == sender) {
                    sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-joined", match.getId()));
                } else {
                    sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-player-joined", player.getName(), match.getId()));
                }
            } else {
                if (player == sender) {
                    sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-cannot-join", match.getId()));
                } else {
                    sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-player-cannot-join", player.getName(), match.getId()));
                }
            }
        });
    }

    /**
     * プレイヤー複数人を試合に参加させる
     *
     * @param sender  コマンド送信者
     * @param match   試合
     * @param players プレイヤー達
     */
    private static void matchJoinPlayers(CommandSender sender, Match match, List<Player> players) {
        long joinCount = players.stream()
                .filter(player -> match.join(player, true))
                .count();

        if (joinCount != 0) {
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-players-joined", joinCount, match.getId()));
        } else {
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-players-cannot-join", match.getId()));
        }
    }

    private static void onMatchLeaveCommand(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));

        SLCommands.getSenderOrSelectedPlayers(sender, args).ifPresent(players -> {
            if (players.size() == 1) {
                matchLeavePlayer(sender, match, players.get(0));
            } else {
                matchLeavePlayers(sender, match, players);
            }
        });
    }

    /**
     * プレイヤー一人を試合から退出させる
     *
     * @param sender 送信者
     * @param match  試合
     * @param player プレイヤー
     */
    private static void matchLeavePlayer(CommandSender sender, Match match, Player player) {
        if (!match.hasParticipation(player)) {
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-player-not-joined", player.getName()));
            return;
        }

        if (match.leave(player, true)) {
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-player-leave", player.getName(), match.getId()));
        } else {
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-player-cannot-leave", player.getName(), match.getId()));
        }
    }

    /**
     * プレイヤー複数人を試合から退出させる
     *
     * @param sender  送信者
     * @param match   試合
     * @param players プレイヤーのリスト
     */
    private static void matchLeavePlayers(CommandSender sender, Match match, List<Player> players) {
        int leaveCount = (int) players.stream()
                .filter(player -> match.leave(player, true))
                .count();

        if (leaveCount != 0) {
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-players-leave", leaveCount, match.getId()));
        } else {
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-players-cannot-leave", match.getId()));
        }
    }

    private static void matchStart(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));

        if (match.getStatus() != Match.Status.NONE) {
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-not-ready", match.getId()));
        } else {
            if (match.start()) {
                sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-started", match.getId()));
            } else {
                sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-cannot-start", match.getId()));
            }
        }

    }

    private static void matchFinish(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));

        if (match.getStatus() != Match.Status.STARTED) {
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-not-able-to-finish", match.getId()));
        } else {
            if (match.finish()) {
                sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-finished", match.getId()));
            } else {
                sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-cannot-finish", match.getId()));
            }
        }
    }

    private static void matchRemove(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));
        match.destroy();
        sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-remove", match.getId()));
    }

    private static void mapList(CommandSender sender) {
        MatchManager matchManager = MatchManager.getInstance();
        MatchMapHandler mapLoader = matchManager.getMapLoader();
        Map<String, MatchMap> maps = mapLoader.getAllMap();

        if (maps.isEmpty()) {
            sender.sendRichMessage(MsgHandler.get("cmd-match-map-not-exist"));
        } else {
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-map-count", maps.size()));
            maps.values().forEach(match -> sender.sendMessage(Component.text(String.format("- %s (", match.id()))
                    .append(Component.text(match.name()))
                    .append(Component.text(")"))));
        }
    }

    private static void mapInfo(CommandSender sender, CommandArguments args) {
        MatchMap map = (MatchMap) Objects.requireNonNull(args.get("map"));
        String availableMatchModesText = "[" + String.join(",", map.availableMatchModes().stream()
                .map(MatchMode::id)
                .toList()) + "]";

        sender.sendRichMessage(MsgHandler.getFormatted("cmd-match-map-id-info", map.id()));
        sender.sendRichMessage(MsgHandler.get("cmd-match-map-schematic") + map.schematic());
        sender.sendRichMessage(MsgHandler.get("cmd-match-map-offset") + String.format("[%s, %s, %s]", map.offset().getX(), map.offset().getY(), map.offset().getZ()));
        sender.sendRichMessage(MsgHandler.get("cmd-match-map-available") + availableMatchModesText);
    }
}