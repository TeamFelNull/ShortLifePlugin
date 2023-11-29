package dev.felnull.shortlifeplugin.match.map;

import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.NamespacedKey;

/**
 * マップマーカーのポイント名定義
 *
 * @author MORIMORI0317
 */
public enum MapMarkerPoints {
    /**
     * スポーン地点
     */
    SPAWN(SLUtils.plLoc("spawn")),
    
    /**
     * 1番チームのスポーン地点
     */
    SPAWN_TEAM1(SLUtils.plLoc("spawn-team1")),

    /**
     * 2番チームのスポーン地点
     */
    SPAWN_TEAM2(SLUtils.plLoc("spawn-team2"));

    /**
     * ポイント名
     */
    private NamespacedKey name;
    
    MapMarkerPoints(NamespacedKey name) {
        this.name = name;
    }

    /**
     * ポイント名を取得
     *
     * @return ポイント名
     */
    public NamespacedKey get() {
        return name;
    }
}
