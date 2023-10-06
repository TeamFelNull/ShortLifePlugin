package dev.felnull.shortlifeplugin.match.map;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * マップのワールドの作成、保持用クラス
 *
 * @author MORIMORI0317
 */
public class MatchMapWorld {
    /**
     * ディメンションワールド
     */
    @NotNull
    private final World world;

    /**
     * マーカーの集合
     */
    @NotNull
    private final MapMarkerSet markerSet;

    /**
     * コンストラクタ
     *
     * @param world     ワールド
     * @param markerSet マーカーの集まり
     */
    protected MatchMapWorld(@NotNull World world, @NotNull MapMarkerSet markerSet) {
        this.world = world;
        this.markerSet = markerSet;
    }

    public @NotNull World getWorld() {
        return world;
    }

    public @NotNull MapMarkerSet getMarkerSet() {
        return markerSet;
    }
}
