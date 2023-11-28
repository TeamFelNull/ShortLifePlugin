package dev.felnull.shortlifeplugin.utils;

import dev.felnull.fnjl.util.FNStringUtil;
import dev.felnull.shortlifeplugin.ShortLifePlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.CharacterAndFormat;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * このプラグインのユーティリティクラス
 *
 * @author MORIMORI0317
 */
public final class SLUtils {

    private SLUtils() {
        throw new AssertionError();
    }

    /**
     * ShortLifeのプラグインインスタンスを取得
     *
     * @return プラグインインスタンス
     */
    public static ShortLifePlugin getSLPlugin() {
        return (ShortLifePlugin) Bukkit.getPluginManager().getPlugin(ShortLifePlugin.PLUGIN_NAME);
    }

    /**
     * ロガーを取得
     *
     * @return ロガー
     */
    public static Logger getLogger() {
        return getSLPlugin().getLogger();
    }

    /**
     * 時間をTick数へ変換
     *
     * @param unit 単位
     * @param time 時間数
     * @return Tick数
     */
    public static int toTick(TimeUnit unit, long time) {
        return (int) (unit.toMillis(time) / 50);
    }

    /**
     * エラー報告<br/>
     * エラーに関する報告を行う
     *
     * @param throwable 例外
     * @throws RuntimeException 投げられた例外
     */
    public static void reportError(@NotNull Throwable throwable) throws RuntimeException {
        reportError(throwable, null);
    }

    /**
     * エラー報告<br/>
     * エラーに関する報告を行う
     *
     * @param throwable 例外
     * @param message   例外メッセージ
     * @throws RuntimeException 投げられた例外
     */
    public static void reportError(@NotNull Throwable throwable, @Nullable String message) throws RuntimeException {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("エラー発生！以下のログを開発者に報告してください。\n");
        errorMessage.append("--------------------------------------------\n");

        if (message != null) {
            errorMessage.append(message).append("\n");
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        throwable.printStackTrace(printWriter);
        printWriter.flush();

        errorMessage.append(stringWriter).append("\n");
        errorMessage.append("--------------------------------------------\n");

        getLogger().warning(errorMessage.toString());
    }


    /**
     * パスからIDを取得
     *
     * @param target 対象のパス
     * @param folder ID付け開始位置のフォルダパス
     * @return ID
     */
    public static String getIdFromPath(File target, File folder) {
        // 絶対パス
        Path absoluteTarget = target.toPath().toAbsolutePath();
        Path absoluteFolder = folder.toPath().toAbsolutePath();

        if (!absoluteTarget.startsWith(absoluteFolder)) {
            throw new RuntimeException("対象のファイルが含まれていないフォルダです");
        }

        // フォルダに対して相対的な対象ファイルのパス
        Path relativePath = absoluteFolder.relativize(absoluteTarget);

        String id = relativePath.toString().replace(relativePath.getFileSystem().getSeparator(), "/");

        // "/"から始まる場合は最初の文字を削除
        if (id.startsWith("/")) {
            id = id.substring(1);
        }

        // "/"で終わる場合は最後の文字を削除
        if (id.endsWith("/")) {
            id = id.substring(0, id.length() - 1);
        }

        // 拡張子を削除
        String extension = FNStringUtil.getExtension(target.getName());

        if (extension != null) {
            id = id.substring(0, id.length() - extension.length() - 1);
        }

        return id;
    }

    /**
     * このプラグインのNamespacedKeyを作成
     *
     * @param path パス
     * @return NamespacedKey
     */
    public static NamespacedKey plLoc(@NotNull String path) {
        return new NamespacedKey(ShortLifePlugin.PLUGIN_ID, path);
    }

    /**
     * このプラグインのNamespacedKey形式文字列(shortlifeplugin:xxxx)を取得
     *
     * @param path パス
     * @return NamespacedKey形式文字列
     */
    @SuppressWarnings("unused")
    public static String plLocStr(@NotNull String path) {
        return ShortLifePlugin.PLUGIN_ID + ":" + path;
    }

    /**
     * 指定したチームが登録されているか確認
     *
     * @param team チーム
     * @return 登録されていればtrue、なければfalse
     */
    @SuppressWarnings("unused")
    public static boolean isTeamRegistered(@NotNull Team team) {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();
        return scoreboard.getTeams().contains(team);
    }

    /**
     * テキストカラーを#CharacterAndFormatへ変換ｎ\
     *
     * @param textColor テキストカラー
     * @return 文字とフォーマット
     */
    @Nullable
    public static CharacterAndFormat getCharacterAndFormatByTextColor(TextColor textColor) {

        NamedTextColor convertColor;
        if (textColor instanceof NamedTextColor namedTextColor) {
            convertColor = namedTextColor;
        } else {
            // 必要であれば、TextColor#nearestColorToを使用して実装してください。
            throw new RuntimeException("未サポートのテキストカラー");
        }

        return CharacterAndFormat.defaults().stream()
                .filter(it -> it.format() == convertColor)
                .limit(1)
                .findFirst().orElse(null);
    }
}
