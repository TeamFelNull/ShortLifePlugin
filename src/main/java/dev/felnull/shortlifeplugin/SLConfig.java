package dev.felnull.shortlifeplugin;

import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * このプラグインのコンフィグ
 *
 * @author MORIMORI0317
 */
public final class SLConfig {

    private SLConfig() {
        throw new AssertionError();
    }

    /**
     * 初期化
     *
     * @param plugin プラグイン
     */
    static void init(ShortLifePlugin plugin) {
        plugin.saveDefaultConfig();
    }

    /**
     * テストモードかどうか
     *
     * @return テストモードであればtrue、でなければfalse
     */
    public static boolean isTestMode() {
        return getConfig().getBoolean("test-mode");
    }

    /**
     * 試合から退出する際に実行されるコマンド
     *
     * @return コマンド
     */
    public static String getMatchLeavePerformCommand() {
        return getConfig().getString("match.leave.perform-command");
    }

    /**
     * 退出時の強制移動先ワールド
     *
     * @return ワールド名
     */
    public static String getMatchLeaveForceTeleportWorld() {
        return getConfig().getString("match.leave.force-teleport-world");
    }

    /**
     * 退出時の強制移動先座標
     *
     * @return 座標
     */
    public static Triple<Integer, Integer, Integer> getMatchLeaveForceTeleportPos() {
        List<Integer> pos = getConfig().getIntegerList("match.leave.force-teleport-pos");
        return Triple.of(pos.get(0), pos.get(1), pos.get(2));
    }

    private static FileConfiguration getConfig() {
        return SLUtils.getSLPlugin().getConfig();
    }
}
