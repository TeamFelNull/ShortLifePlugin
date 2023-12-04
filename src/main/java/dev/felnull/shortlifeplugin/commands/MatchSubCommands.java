package dev.felnull.shortlifeplugin.commands;

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
 * @author Quarri6343
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
                                .fromMessageBuilder(new CustomArgument.MessageBuilder("不明な試合です: ").appendArgInput())))
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
                                .fromMessageBuilder(new CustomArgument.MessageBuilder("不明な試合マップです: ").appendArgInput())))
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
            sender.sendRichMessage("試合は存在しません");
        } else {
            sender.sendRichMessage(String.format("%d個の試合が存在します", matches.size()));

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

        sender.sendRichMessage(String.format("%sの試合情報:", match.getId()));
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
                sender.sendRichMessage("既に参加済みです");
            } else {
                sender.sendRichMessage(String.format("%sは既に参加済みです", player.getName()));
            }
            return;
        }

        MatchManager.getInstance().getJoinedMatch(player).ifPresentOrElse(joinedMatch -> {
            if (player == sender) {
                sender.sendRichMessage(String.format("%sに参加済みです", joinedMatch.getId()));
            } else {
                sender.sendRichMessage(String.format("%sは%sに参加済みです", player.getName(), joinedMatch.getId()));
            }
        }, () -> {
            if (match.join(player, player != sender)) {
                if (player == sender) {
                    sender.sendRichMessage(String.format("%sに参加しました", match.getId()));
                } else {
                    sender.sendRichMessage(String.format("%sを%sに参加させました", player.getName(), match.getId()));
                }
            } else {
                if (player == sender) {
                    sender.sendRichMessage(String.format("%sに参加できませんでした", match.getId()));
                } else {
                    sender.sendRichMessage(String.format("%sを%sに参加させられませんでした", player.getName(), match.getId()));
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
        int joinCount = 0;

        for (Player player : players) {
            if (match.join(player, true)) {
                joinCount++;
            }
        }

        if (joinCount != 0) {
            sender.sendRichMessage(String.format("%d人のプレイヤーを%sに参加させました", joinCount, match.getId()));
        } else {
            sender.sendRichMessage(String.format("プレイヤーを%sに参加させられませんでした", match.getId()));
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
            sender.sendRichMessage(String.format("%sは試合に参加していません", player.getName()));
            return;
        }

        if (match.leave(player, true)) {
            sender.sendRichMessage(String.format("%sを%sから退出させました", player.getName(), match.getId()));
        } else {
            sender.sendRichMessage(String.format("%sを%sから退出できませんでした", player.getName(), match.getId()));
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
        int leaveCount = 0;

        for (Player player : players) {
            if (match.leave(player, true)) {
                leaveCount++;
            }
        }

        if (leaveCount != 0) {
            sender.sendRichMessage(String.format("%d人のプレイヤーを%sから退出させました", leaveCount, match.getId()));
        } else {
            sender.sendRichMessage(String.format("プレイヤーを%sから退出できませんでした", match.getId()));
        }
    }

    private static void matchStart(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));

        if (match.getStatus() != Match.Status.NONE) {
            sender.sendRichMessage(String.format("%sは開始できる状態ではありません", match.getId()));
        } else {
            if (match.start()) {
                sender.sendRichMessage(String.format("%sを開始させました", match.getId()));
            } else {
                sender.sendRichMessage(String.format("%sを開始できませんでした", match.getId()));
            }
        }

    }

    private static void matchFinish(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));

        if (match.getStatus() != Match.Status.STARTED) {
            sender.sendRichMessage(String.format("%sは終了できる状態ではありません", match.getId()));
        } else {
            if (match.finish()) {
                sender.sendRichMessage(String.format("%sを終了させました", match.getId()));
            } else {
                sender.sendRichMessage(String.format("%sを終了できませんでした", match.getId()));
            }
        }
    }

    private static void matchRemove(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));
        match.destroy();
        sender.sendRichMessage(String.format("%sを削除します", match.getId()));
    }

    private static void mapList(CommandSender sender) {
        MatchManager matchManager = MatchManager.getInstance();
        MatchMapHandler mapLoader = matchManager.getMapLoader();
        Map<String, MatchMap> maps = mapLoader.getAllMap();

        if (maps.isEmpty()) {
            sender.sendRichMessage("試合用マップは存在しません");
        } else {
            sender.sendRichMessage(String.format("%d個の試合用マップが存在します", maps.size()));
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

        sender.sendRichMessage(String.format("%sの試合用マップ情報:", map.id()));
        sender.sendRichMessage("スケマティック: " + map.schematic());
        sender.sendRichMessage("オフセット: " + String.format("[%s, %s, %s]", map.offset().getX(), map.offset().getY(), map.offset().getZ()));
        sender.sendRichMessage("利用可能なマップ: " + availableMatchModesText);
    }
}