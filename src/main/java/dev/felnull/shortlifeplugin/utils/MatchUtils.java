package dev.felnull.shortlifeplugin.utils;

import dev.felnull.shortlifeplugin.SLConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 試合関係のユーティリティクラス
 *
 * @author MORIMORI0317
 */
public final class MatchUtils {

    /**
     * チーム選定で、優先的に使用される順番に格納されたチームカラー
     */
    public static final NamedTextColor[] TEAM_COLORS = {
            NamedTextColor.RED,
            NamedTextColor.BLUE,
            NamedTextColor.GREEN,
            NamedTextColor.YELLOW,
            NamedTextColor.LIGHT_PURPLE,
            NamedTextColor.AQUA,
            NamedTextColor.WHITE,
            NamedTextColor.BLACK,
            NamedTextColor.DARK_RED,
            NamedTextColor.DARK_BLUE,
            NamedTextColor.DARK_GREEN,
            NamedTextColor.GOLD,
            NamedTextColor.DARK_PURPLE,
            NamedTextColor.DARK_AQUA,
            NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY
    };

    /**
     * デフォルトの退出時強制移動先ワールド
     */
    private static final NamespacedKey DEFAULT_FORCE_TELEPORT_WORLD = NamespacedKey.minecraft("overworld");

    private MatchUtils() {
    }

    /**
     * プレイヤーを試合から退出するためにテレポートさせる
     *
     * @param player     プレイヤー
     * @param leaveWorld 退出したいワールド
     */
    public static void teleportToLeave(@NotNull Player player, @Nullable World leaveWorld) {
        boolean needForceTeleport = false;

        // 死亡している場合は強制リスポーン
        if (player.isDead()) {
            player.spigot().respawn();
            needForceTeleport = true;
        }

        if (!player.performCommand(SLConfig.getMatchLeavePerformCommand())) {
            // コマンドの実行に失敗した場合
            needForceTeleport = true;
        } else if (leaveWorld != null && leaveWorld == player.getWorld()) {
            // コマンドの実行は出来たが、まだ退出したいワールドにいる場合
            needForceTeleport = true;
        }

        // 強制移動
        if (needForceTeleport) {
            World backWorld = Bukkit.getWorld(Objects.requireNonNull(NamespacedKey.fromString(SLConfig.getMatchLeaveForceTeleportWorld())));

            // コンフィグで指定したワールドが存在しない場合
            if (backWorld == null) {
                backWorld = Bukkit.getWorld(DEFAULT_FORCE_TELEPORT_WORLD);
            }

            Triple<Integer, Integer, Integer> backPos = SLConfig.getMatchLeaveForceTeleportPos();
            Location location = new Location(backWorld, backPos.getLeft(), backPos.getMiddle(), backPos.getRight());

            player.teleport(location);
        }

        // 落下距離リセット
        player.setFallDistance(0);
    }
}
