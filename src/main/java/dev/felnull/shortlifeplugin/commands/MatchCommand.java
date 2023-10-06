package dev.felnull.shortlifeplugin.commands;

import com.google.common.collect.ImmutableList;
import dev.felnull.shortlifeplugin.gui.SLGuis;
import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.MatchModes;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.window.Window;

import java.util.*;

/**
 * 試合コマンド
 *
 * @author MORIMORI0317
 */
public final class MatchCommand {

    private MatchCommand() {
    }

    /**
     * コマンドを作成
     *
     * @return コマンド
     */
    static CommandAPICommand create() {
        // https://commandapi.jorel.dev/9.0.3/commandregistration.html <-参考

        CommandAPICommand gui = new CommandAPICommand("gui")
                .withSubcommands(new CommandAPICommand("open")
                        .withArguments(guiArgument("gui"))
                        .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("player"))
                        .executes(MatchCommand::openGui));

        CommandAPICommand list = new CommandAPICommand("list")
                .executes(MatchCommand::matchList);

        CommandAPICommand info = new CommandAPICommand("info")
                .withArguments(matchArgument("match"))
                .executes(MatchCommand::matchInfo);

        CommandAPICommand join = new CommandAPICommand("join")
                .withArguments(matchArgument("match"))
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("player"))
                .executes(MatchCommand::matchJoin);

        CommandAPICommand leave = new CommandAPICommand("leave")
                .withArguments(matchArgument("match"))
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("player"))
                .executes(MatchCommand::matchLeave);

        CommandAPICommand start = new CommandAPICommand("start")
                .withArguments(matchArgument("match"))
                .executes(MatchCommand::matchStart);

        CommandAPICommand finish = new CommandAPICommand("finish")
                .withArguments(matchArgument("match"))
                .executes(MatchCommand::matchFinish);

        CommandAPICommand remove = new CommandAPICommand("remove")
                .withArguments(matchArgument("match"))
                .executes(MatchCommand::matchRemove);

        // FIXME リリース前に削除
        CommandAPICommand test = new CommandAPICommand("aikiso")
                .executes((sender, args) -> {
                /*    if (sender instanceof Player player) {
                        MapTest.test(player);
                    }*/

                    if (sender instanceof Player player) {
                        MatchMap matchMap = SLUtils.getMatchManager().getMapLoader().getMap("test");
                        SLUtils.getMatchManager().addMatch("ikisugi", MatchModes.TEST, matchMap).join(player);
                    }
                });


        return new CommandAPICommand("match")
                .withAliases("slm")
                .withPermission(CommandPermission.OP)
                .withSubcommands(gui, list, info, join, leave, finish, start, remove, test);
    }

    private static Argument<SLGuis.WindowProvider> guiArgument(String nodeName) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            SLGuis.WindowProvider windowProvider = SLGuis.getWindowProvider(info.input());

            if (windowProvider == null) {
                throw CustomArgument.CustomArgumentException
                        .fromMessageBuilder(new CustomArgument.MessageBuilder("Unknown gui: ").appendArgInput());
            } else {
                return windowProvider;
            }
        }).replaceSuggestions(ArgumentSuggestions.strings(info -> SLGuis.getAllGuiIds().toArray(String[]::new)));
    }

    private static Argument<Match> matchArgument(String nodeName) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            Match match = SLUtils.getMatchManager().getMatch(info.input());

            if (match == null) {
                throw CustomArgument.CustomArgumentException
                        .fromMessageBuilder(new CustomArgument.MessageBuilder("Unknown match: ").appendArgInput());
            } else {
                return match;
            }
        }).replaceSuggestions(ArgumentSuggestions.strings(info -> SLUtils.getMatchManager().getAllMatch().keySet().toArray(String[]::new)));
    }


    /**
     * 実行者もしくは引数でプレイヤーセレクタで指定されたプレイヤーを取得
     *
     * @param sender CommandSender
     * @param args   CommandArguments
     * @return オプショナルなプレイヤーリスト
     */
    private static Optional<List<Player>> getSenderOrSelectedPlayers(CommandSender sender, CommandArguments args) {
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

    private static void openGui(CommandSender sender, CommandArguments args) {
        getSenderOrSelectedPlayers(sender, args).ifPresent(players -> players.forEach(player -> {
            Window window = ((SLGuis.WindowProvider) Objects.requireNonNull(args.get("gui"))).provide(player);
            window.open();
        }));
    }

    private static void matchList(CommandSender sender, CommandArguments args) {
        MatchManager matchManager = SLUtils.getMatchManager();
        Map<String, Match> matches = matchManager.getAllMatch();

        if (matches.isEmpty()) {
            sender.sendRichMessage("試合は存在しません");
        } else {
            sender.sendRichMessage(String.format("%d個の試合が存在します", matches.size()));

            matches.values().forEach(match -> sender.sendMessage(Component.text(String.format("- %s (", match.getId()))
                    .append(match.getMatchMode().name())
                    .append(Component.text(")"))));

        }
    }

    private static void matchInfo(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));

        sender.sendRichMessage(String.format("%sの試合情報:", match.getId()));
        List<Component> list = new LinkedList<>();
        match.appendInfoDesc(list);
        list.forEach(sender::sendMessage);
    }

    private static void matchJoin(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));
        getSenderOrSelectedPlayers(sender, args).ifPresent(players -> {
            if (players.size() == 1) {
                Player player = players.get(0);

                if (match.getAllJoinPlayers().contains(player)) {
                    sender.sendRichMessage(String.format("%sは既に参加済みです", player.getName()));
                    return;
                }

                Match jointedMatch = SLUtils.getMatchManager().getJointedMach(player);

                if (jointedMatch != null) {
                    sender.sendRichMessage(String.format("%sは%sに参加済みです", player.getName(), jointedMatch.getId()));
                    return;
                }

                if (match.join(player)) {
                    sender.sendRichMessage(String.format("%sを%sに参加させました", player.getName(), match.getId()));
                } else {
                    sender.sendRichMessage(String.format("%sを%sに参加できませんでした", player.getName(), match.getId()));
                }

            } else {
                int joinCount = 0;

                for (Player player : players) {
                    if (match.join(player)) {
                        joinCount++;
                    }
                }

                if (joinCount != 0) {
                    sender.sendRichMessage(String.format("%d人のプレイヤーを%sに参加させました", joinCount, match.getId()));
                } else {
                    sender.sendRichMessage(String.format("プレイヤーを%sに参加できませんでした", match.getId()));
                }
            }
        });

    }

    private static void matchLeave(CommandSender sender, CommandArguments args) {
        Match match = (Match) Objects.requireNonNull(args.get("match"));

        getSenderOrSelectedPlayers(sender, args).ifPresent(players -> {
            if (players.size() == 1) {
                Player player = players.get(0);

                if (!match.getAllJoinPlayers().contains(player)) {
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
}
