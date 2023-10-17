package dev.felnull.shortlifeplugin.match;

import com.google.common.collect.ImmutableMap;
import dev.felnull.shortlifeplugin.match.map.MapMarkerPoints;
import dev.felnull.shortlifeplugin.match.map.MatchMapWorld;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 試合モードのレジストリ
 *
 * @author MORIMORI0317
 */
public final class MatchModes {

    /**
     * テスト用試合モード
     */
    public static final MatchMode TEST =
            new MatchMode("test", Component.text("テスト"), Material.COMMAND_BLOCK, MatchType.PVP, 1000 * 60 * 3,
                    1, 114514, TestMatch::new, mapPointCheck(MapMarkerPoints.SPAWN));

    /**
     * チームポイント制試合モード
     */
    public static final MatchMode TEAM_POINT =
            new MatchMode("team_point", Component.text("チーム-ポイント制"), Material.RED_BANNER, MatchType.PVP, 1000 * 60 * 10,
                    2, 30, TeamPointMatch::new, mapPointCheck(MapMarkerPoints.SPAWN_TEAM1, MapMarkerPoints.SPAWN_TEAM2));

    /**
     * 試合モードのレジストリマップ
     */
    private static final Map<String, MatchMode> MODE_REGISTRY = new HashMap<>();

    private MatchModes() {
    }

    /**
     * 初期処理
     */
    public static void init() {
        register(TEST);
        register(TEAM_POINT);
    }

    public static Map<String, MatchMode> getAllModes() {
        return ImmutableMap.copyOf(MODE_REGISTRY);
    }


    /**
     * 試合モードを取得
     *
     * @param id 試合モードID
     * @return 試合モード
     */
    @Nullable
    public static MatchMode getMode(String id) {
        return MODE_REGISTRY.get(id);
    }

    private static void register(@NotNull MatchMode matchMode) {
        MODE_REGISTRY.put(matchMode.id(), matchMode);
    }


    private static Predicate<MatchMapWorld> mapPointCheck(NamespacedKey point, NamespacedKey... points) {
        return matchMapWorld -> {
            if (matchMapWorld.getMarker(point).isEmpty()) {
                return false;
            }

            for (NamespacedKey key : points) {
                if (matchMapWorld.getMarker(key).isEmpty()) {
                    return false;
                }
            }

            return true;
        };
    }
}
