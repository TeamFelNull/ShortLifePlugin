package dev.felnull.shortlifeplugin.commands;

import dev.felnull.shortlifeplugin.SLPermissions;
import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.MatchMode;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import dev.felnull.shortlifeplugin.match.map.MatchMapLoader;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 試合コマンド
 *
 * @author MORIMORI0317
 */
public class MatchCommand implements SLCommand {

    @Override
    public CommandAPICommand create() {
        // https://commandapi.jorel.dev/9.0.3/commandregistration.html <-参考
        CommandAPICommand list = new CommandAPICommand("list")
                .executes(this::matchList);

        CommandAPICommand info = new CommandAPICommand("info")
                .withArguments(matchArgument("match"))
                .executes(this::matchInfo);

        CommandAPICommand join = new CommandAPICommand("join")
                .withArguments(matchArgument("match"))
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("player"))
                .executes(this::matchJoin);

        CommandAPICommand leave = new CommandAPICommand("leave")
                .withArguments(matchArgument("match"))
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("player"))
                .executes(this::matchLeave);

        CommandAPICommand start = new CommandAPICommand("start")
                .withArguments(matchArgument("match"))
                .executes(this::matchStart);

        CommandAPICommand finish = new CommandAPICommand("finish")
                .withArguments(matchArgument("match"))
                .executes(this::matchFinish);

        CommandAPICommand remove = new CommandAPICommand("remove")
                .withArguments(matchArgument("match"))
                .executes(this::matchRemove);

        CommandAPICommand map = new CommandAPICommand("map")
                .withSubcommands(new CommandAPICommand("list")
                        .executes(this::mapList))
                .withSubcommands(new CommandAPICommand("info")
                        .withArguments(mapArgument("map"))
                        .executes(this::mapInfo));

        // FIXME リリース前に削除
        CommandAPICommand test = new CommandAPICommand("aikiso")
                .executes((sender, args) -> {
                 /*   if (sender instanceof Player player) {
                        mainIkisugi(player);
                    }*/
                   /* if (sender instanceof Player player) {
                        MatchMap matchMap = MatchUtils.getMatchManager().getMapLoader().getMap("test");
                        MatchUtils.getMatchManager().addMatch("team-test", MatchModes.TEST, matchMap).join(player, true);
                    }*/
                /*    if (sender instanceof Player player) {
                        MapTest.test(player);
                    }*/

                    /*if (sender instanceof Player player) {
                     *//* MatchMap matchMap = MatchUtils.getMatchManager().getMapLoader().getMap("test");
                        MatchUtils.getMatchManager().addMatch("team-test", MatchModes.TEAM, matchMap).join(player);*//*

                        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
                        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

                        Team ikisugiTeam = scoreboard.registerNewTeam("ikisugi");

                        // scoreboard.getS()
                    }*/
                });


        return new CommandAPICommand("match")
                .withAliases("slm")
                .withPermission(SLPermissions.COMMANDS_MATCH)
                .withSubcommands(list, info, join, leave, finish, start, remove, map, test);
    }

    @Override
    public void unregister() {
        CommandAPI.unregister("match");
    }

    private Argument<Match> matchArgument(String nodeName) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            Match match = MatchManager.getInstance().getMatch(info.input());

            if (match == null) {
                throw CustomArgument.CustomArgumentException
                        .fromMessageBuilder(new CustomArgument.MessageBuilder("不明な試合です: ").appendArgInput());
            } else {
                return match;
            }
        }).replaceSuggestions(ArgumentSuggestions.strings(info -> MatchManager.getInstance().getAllMatch().keySet().toArray(String[]::new)));
    }

    private Argument<MatchMap> mapArgument(String nodeName) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            MatchMap matchMap = MatchManager.getInstance().getMapLoader().getMap(info.input());

            if (matchMap == null) {
                throw CustomArgument.CustomArgumentException
                        .fromMessageBuilder(new CustomArgument.MessageBuilder("不明な試合マップです: ").appendArgInput());
            } else {
                return matchMap;
            }
        }).replaceSuggestions(ArgumentSuggestions.strings(info -> MatchManager.getInstance().getMapLoader().getAllMap().keySet().toArray(String[]::new)));
    }


    private void matchList(CommandSender sender, CommandArguments args) {
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

    private void matchInfo(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));

        sender.sendRichMessage(String.format("%sの試合情報:", match.getId()));
        List<Component> list = new LinkedList<>();
        match.appendInfoDesc(list);
        list.forEach(sender::sendMessage);
    }

    private void matchJoin(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));
        getSenderOrSelectedPlayers(sender, args).ifPresent(players -> {
            if (players.size() == 1) {
                Player player = players.get(0);

                if (match.hasParticipation(player)) {
                    if (player == sender) {
                        sender.sendRichMessage("既に参加済みです");
                    } else {
                        sender.sendRichMessage(String.format("%sは既に参加済みです", player.getName()));
                    }
                    return;
                }

                Match jointedMatch = MatchManager.getInstance().getJointedMach(player);

                if (jointedMatch != null) {
                    if (player == sender) {
                        sender.sendRichMessage(String.format("%sに参加済みです", jointedMatch.getId()));
                    } else {
                        sender.sendRichMessage(String.format("%sは%sに参加済みです", player.getName(), jointedMatch.getId()));
                    }
                    return;
                }

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

            } else {
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
        });

    }

    private void matchLeave(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));

        getSenderOrSelectedPlayers(sender, args).ifPresent(players -> {
            if (players.size() == 1) {
                Player player = players.get(0);

                if (!match.hasParticipation(player)) {
                    sender.sendRichMessage(String.format("%sは試合に参加していません", player.getName()));
                    return;
                }

                if (match.leave(player, true)) {
                    sender.sendRichMessage(String.format("%sを%sから退出させました", player.getName(), match.getId()));
                } else {
                    sender.sendRichMessage(String.format("%sを%sから退出できませんでした", player.getName(), match.getId()));
                }

            } else {
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
        });
    }

    private void matchStart(CommandSender sender, CommandArguments args) {
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

    private void matchFinish(CommandSender sender, CommandArguments args) {
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

    private void matchRemove(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));
        match.destroy();
        sender.sendRichMessage(String.format("%sを削除します", match.getId()));
    }

    private void mapList(CommandSender sender, CommandArguments args) {
        MatchManager matchManager = MatchManager.getInstance();
        MatchMapLoader mapLoader = matchManager.getMapLoader();
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

    private void mapInfo(CommandSender sender, CommandArguments args) {
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
