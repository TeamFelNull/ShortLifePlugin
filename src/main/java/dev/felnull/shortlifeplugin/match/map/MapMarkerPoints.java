package dev.felnull.shortlifeplugin.match.map;

import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.NamespacedKey;

/**
 * マップマーカーのポイント名定義
 *
 * @author MORIMORI0317
 */
public final class MapMarkerPoints {
    /**
     * スポーン地点
     */
    public static final NamespacedKey SPAWN = SLUtils.plLoc("spawn");

    /**
     * 1番チームのスポーン地点
     */
    public static final NamespacedKey SPAWN_TEAM1 = SLUtils.plLoc("spawn-team1");

    /**
     * 2番チームのスポーン地点
     */
    public static final NamespacedKey SPAWN_TEAM2 = SLUtils.plLoc("spawn-team2");

    private MapMarkerPoints() {
    }
}
