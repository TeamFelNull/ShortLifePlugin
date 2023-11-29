package dev.felnull.shortlifeplugin.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.CommandArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.wrappers.CommandResult;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;

/**
 * 報酬サブコマンドのデータ
 *
 * @author Quarri6343
 */
public enum RewardSubCommands {
    /**
     * ノーマル
     */
    NORMAL("normal"),

    /**
     * 特殊
     */
    SPECIAL("special"),

    /**
     * ストリーク
     */
    STREAK("streak"),

    /**
     * 勝利
     */
    WINNER("winner");

    /**
     * Gsonインスタンス
     */
    private static final Gson GSON = new Gson();
    
    /**
     * サブコマンド名
     */
    private final String name;

    RewardSubCommands(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * サブコマンドを構築する
     *
     * @return サブコマンド
     */
    public CommandAPICommand construct() {
        return new CommandAPICommand(getName())
                .withArguments(new CommandArgument("command"))
                .executes((CommandExecutor) (sender, args) -> reward(args, this));
    }

    /**
     * サブコマンドに対応するJsonのPropertyの名前
     *
     * @return JsonのPropertyの名前
     */
    public String getJsonPropertyName() {
        return name + "Reward";
    }

    /**
     * サブコマンドの引数として渡された報酬のコマンドをセーブする
     *
     * @param args サブコマンドの引数
     * @author Quarri6343
     */
    private static void reward(CommandArguments args, RewardSubCommands subCommand) {
        String command = useCommand(args);

        try {
            setConfig(subCommand, command);
        } catch (IOException e) {
            SLUtils.getLogger().info(String.valueOf(e));
        }
    }

    /**
     * 報酬コマンドコンフィグ保存JSONの値を書き換える
     *
     * @param functionName 変更対象の機能名
     * @param changeValue  変更後の値
     * @author raindazo
     */
    private static void setConfig(@NotNull RewardSubCommands functionName, String changeValue) throws IOException {
        File savedJsonFile = SLFiles.rewardCommandConfigJson();
        JsonObject json;
        if (!savedJsonFile.exists() || savedJsonFile.isDirectory()) {
            json = new JsonObject();
        } else {
            json = GSON.fromJson(Files.readString(savedJsonFile.toPath()), JsonObject.class);
        }

        String jsonPropertyName = functionName.getJsonPropertyName();
        json.remove(jsonPropertyName);
        json.addProperty(jsonPropertyName, changeValue);

        // Json書き込み
        try (Writer writer = new BufferedWriter(new FileWriter(savedJsonFile))) {
            GSON.toJson(json, writer);
        }
    }

    /**
     * コマンド引数配列の最初のコマンドを文字列にする
     *
     * @param args コマンド
     * @author raindazo
     */
    private static String useCommand(CommandArguments args) {
        CommandResult commandResult = Optional.ofNullable((CommandResult) args.get(0)).orElseThrow();

        return commandResult.command().getName() + StringUtils.SPACE + StringUtils.join(Objects.requireNonNull(commandResult).args(), StringUtils.SPACE);
    }
}
