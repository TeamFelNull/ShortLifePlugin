package dev.felnull.shortlifeplugin;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * プラグインのメッセージ管理クラス<br>
 * I18nにならってstatic
 *
 * @author Quarri6343
 */
public class MsgHandler {

    /**
     * メッセージのマップ
     */
    private static final Map<String, String> MSG_MAP = new HashMap<>();

    /**
     * 言語ファイルのパス<br>
     * （他言語に対応予定がないのでrootに置いている）
     */
    private static final String FILE_PATH = "ja_JP.yml";

    private MsgHandler() {
        throw new AssertionError();
    }

    /**
     * 言語ファイルから読み込み
     *
     * @param plugin プラグイン
     */
    public static void load(Plugin plugin) {
        Optional.ofNullable(plugin.getResource(FILE_PATH)).ifPresentOrElse(inputStream -> {
            FileConfiguration translations = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));
            translations.getKeys(false)
                    .forEach(translation -> MSG_MAP.put(translation, translations.getString(translation)));
        }, () -> {
            throw new NullPointerException("言語ファイルが見つかりません。プラグインをシャットダウンします"); //Logger起動前にメッセージを読み込むためLoggerは使えない
        });
    }

    /**
     * フォーマットされたメッセージ取得 (MsgHandler#getとString#Formatの短縮形)
     *
     * @param key  キー
     * @param args フォーマット用オブジェクト
     * @return フォーマットされたメッセージ
     */
    public static String getFormatted(String key, Object... args) {
        return String.format(get(key), args);
    }

    /**
     * メッセージ取得
     *
     * @param key キー
     * @return 対応したメッセージ
     */
    @SuppressWarnings("deprecation")
    public static String get(String key) {
        return Optional.ofNullable(MSG_MAP.get(key))
                .map(s -> ChatColor.translateAlternateColorCodes('&', s)).orElse(key);
    }

    /**
     * メッセージのコンポーネントを取得
     *
     * @param key キー
     * @return 対応したメッセージのコンポーネント
     */
    @NotNull
    public static Component getComponent(String key) {
        return Component.text(get(key));
    }
}
