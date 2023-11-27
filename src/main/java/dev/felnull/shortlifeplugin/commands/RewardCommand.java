package dev.felnull.shortlifeplugin.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.felnull.shortlifeplugin.SLPermissions;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.CommandArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.wrappers.CommandResult;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 報酬コマンド
 *
 * @author raindazo
 */
public class RewardCommand implements SLCommand {

    /**
     * Gsonインスタント
     */
    private static final Gson GSON = new Gson();

    /**
     * ノーマル
     */
    private static final String NORMAL = "normal";

    /**
     * 勝利
     */
    private static final String WINNER = "winner";


    /**
     * 特殊
     */
    private static final String SPECIAL = "special";

    /**
     * ストリーク
     */
    private static final String STREAK = "streak";

    @Override
    public CommandAPICommand create() {

        CommandAPICommand normalReward = new CommandAPICommand("normal")
                .withArguments(new CommandArgument("command"))
                .executes(this::normalReward);

        CommandAPICommand chanceReward = new CommandAPICommand("special")
                .withArguments(new CommandArgument("command"))
                .executes(this::specialReward);

        CommandAPICommand winnerReward = new CommandAPICommand("winner")
                .withArguments(new CommandArgument("command"))
                .executes(this::winnerReward);

        CommandAPICommand streakReward = new CommandAPICommand("streak")
                .withArguments(new CommandArgument("command"))
                .executes(this::streakReward);

        return new CommandAPICommand("reward")
                .withPermission(SLPermissions.COMMANDS_REWARD)
                .withSubcommands(normalReward, chanceReward, winnerReward, streakReward);
    }

    @Override
    public void unregister() {

    }

    /**
     * 通常報酬のコマンドを設定する
     *
     * @param sender 発信者
     * @param args   チャットの引数
     * @author raindazo
     */
    private void normalReward(CommandSender sender, CommandArguments args) {
        String command = useCommand(args);

        try {
            this.setConfig(NORMAL, command);
        } catch (IOException e) {
            SLUtils.getLogger().info(String.valueOf(e));
        }
    }

    /**
     * 特殊報酬を付与する
     *
     * @param sender 発信者
     * @param args   チャットの引数
     * @author raindazo
     */
    private void specialReward(CommandSender sender, CommandArguments args) {
        String command = useCommand(args);

        try {
            this.setConfig(SPECIAL, command);
        } catch (IOException e) {
            SLUtils.getLogger().info(String.valueOf(e));
        }
    }

    /**
     * 勝利時の報酬を付与する
     *
     * @param sender 発信者
     * @param args   チャットの引数
     * @author raindazo
     */
    private void winnerReward(CommandSender sender, CommandArguments args) {
        String command = useCommand(args);
        try {
            this.setConfig(WINNER, command);
        } catch (IOException e) {
            SLUtils.getLogger().info(String.valueOf(e));
        }
    }

    /**
     * ストリート報酬のコマンドを設定する
     *
     * @param sender 発信者
     * @param args   チャットの引数
     * @author raindazo
     */
    private void streakReward(CommandSender sender, CommandArguments args) {
        String command = useCommand(args);

        try {
            this.setConfig(STREAK, command);
        } catch (IOException e) {
            SLUtils.getLogger().info(String.valueOf(e));
        }
    }

    /**
     * JSONの値を書き換える
     *
     * @param functionName 変更対象の機能名
     * @param changeValue  変更後の値
     * @author raindazo
     */
    private void setConfig(String functionName, String changeValue) throws IOException {
        File savedJsonFile = SLFiles.rewardCommandConfigJson();
        JsonObject json;
        if (!savedJsonFile.exists() || savedJsonFile.isDirectory()) {
            json = new JsonObject();
        } else {
            json = GSON.fromJson(Files.readString(savedJsonFile.toPath()), JsonObject.class);
        }

        switch (functionName) {
            case "normal" -> {
                json.remove("normalReward");
                json.addProperty("normalReward", changeValue);
            }
            case "special" -> {
                json.remove("specialReward");
                json.addProperty("specialReward", changeValue);
            }
            case "winner" -> {
                json.remove("winnerReward");
                json.addProperty("winnerReward", changeValue);
            }
            case "streak" -> {
                json.remove("streakReward");
                json.addProperty("streakReward", changeValue);
            }
            default -> throw new IllegalStateException("Unexpected value: " + functionName);
        }
        // Json書き込み
        try (Writer writer = new BufferedWriter(new FileWriter(savedJsonFile))) {
            GSON.toJson(json, writer);
        }
    }

    /**
     * コマンドを返却
     *
     * @param args コマンド
     * @author raindazo
     */
    private String useCommand(CommandArguments args) {
        CommandResult command = (CommandResult) args.get(0);
        List<String> argumentList = Arrays.asList(Objects.requireNonNull(command).args());
        String useCommand = command.command().getName() + " ";
        useCommand += argumentList.stream()
                .map(value -> value + " ")
                .collect(Collectors.joining());

        return useCommand;
    }
}
