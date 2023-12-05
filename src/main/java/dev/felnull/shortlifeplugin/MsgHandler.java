package dev.felnull.shortlifeplugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

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
    private static Map<String, String> msgMap = new HashMap<>();
    
    private MsgHandler() {
        throw new AssertionError();
    }

    /**
     * 読み込み
     *
     * @param plugin プラグイン
     */
    public static void load(Plugin plugin) {
        Optional.ofNullable(plugin.getResource("ja_JP.yml")).ifPresentOrElse(inputStream -> {
            FileConfiguration translations = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));
            translations.getKeys(false)
                    .forEach(translation -> msgMap.put(translation, translations.getString(translation)));
        }, () -> {
            throw new NullPointerException("言語ファイルが見つかりません。プラグインをシャットダウンします"); //Logger起動前にメッセージを読み込むためLoggerは使えない
        });
    }

    /**
     * フォーマットされたメッセージ取得 (MsgHandler#getとString#Formatの短縮形)
     *
     * @param key キー
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
        return Optional.ofNullable(msgMap.get(key))
                .map(s -> ChatColor.translateAlternateColorCodes('&', s)).orElse(key);
    }
}
