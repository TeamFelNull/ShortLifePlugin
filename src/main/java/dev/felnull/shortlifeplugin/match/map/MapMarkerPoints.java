package dev.felnull.shortlifeplugin.match.map;

import dev.felnull.shortlifeplugin.ShortLifePlugin;
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
    public static final NamespacedKey SPAWN = new NamespacedKey(ShortLifePlugin.PLUGIN_ID, "spawn");

    private MapMarkerPoints() {
    }
}
