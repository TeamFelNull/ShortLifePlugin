package dev.felnull.shortlifeplugin.match;

import com.google.common.collect.ImmutableMap;
import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.match.map.MapMarkerPoints;
import dev.felnull.shortlifeplugin.match.map.MatchMapValidator;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
            new MatchMode("test", MsgHandler.get("match-mode-test"), Material.COMMAND_BLOCK, MatchType.PVP, 1000 * 60 * 3,
                    1, 114514, TestMatch::new, mapPointCheck(MapMarkerPoints.SPAWN.get()), true);

    /**
     * チームポイント制試合モード
     */
    public static final MatchMode TEAM_POINT =
            new MatchMode("team_point", MsgHandler.get("match-mode-team-point"), Material.RED_BANNER, MatchType.PVP, 1000 * 60 * 10,
                    2, 30, TeamPointMatch::new, mapPointCheck(MapMarkerPoints.SPAWN_TEAM1.get(), MapMarkerPoints.SPAWN_TEAM2.get()), false);

    /**
     * FFA試合モード
     */
    public static final MatchMode FFA =
            new MatchMode("ffa", MsgHandler.get("match-mode-FFA"), Material.IRON_SWORD, MatchType.PVP, 1000 * 60 * 10,
                    2, 30, FFAMatch::new, mapPointCheck(MapMarkerPoints.SPAWN.get()), false);

    /**
     * 試合モードのレジストリマップ<br/>
     * 今後FNJLのレジストリシステムに置き換え予定
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
        register(FFA);
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
    public static Optional<MatchMode> getMode(String id) {
        return Optional.ofNullable(MODE_REGISTRY.get(id));
    }

    private static void register(@NotNull MatchMode matchMode) {
        MODE_REGISTRY.put(matchMode.id(), matchMode);
    }


    private static MatchMapValidator mapPointCheck(NamespacedKey point, NamespacedKey... points) {
        return matchMapWorld -> {

            if (matchMapWorld.getMarker(point).isEmpty()) {
                throw new RuntimeException(String.format(MsgHandler.get("system-marker-not-exists"), point.asString()));
            }

            for (NamespacedKey key : points) {
                if (matchMapWorld.getMarker(key).isEmpty()) {
                    throw new RuntimeException(String.format(MsgHandler.get("system-marker-not-exists"), key.asString()));
                }
            }
        };
    }
}
