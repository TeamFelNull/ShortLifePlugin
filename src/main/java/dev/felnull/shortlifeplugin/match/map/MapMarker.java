package dev.felnull.shortlifeplugin.match.map;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

/**
 * マップの座標、向きを指定するマーカー
 *
 * @author MORIMORI0317
 */
public interface MapMarker {

    /**
     * マーカーの名前を取得
     *
     * @return マーカー名
     */
    @NotNull
    NamespacedKey getName();

    /**
     * 地点の名前を取得
     *
     * @return 地点名
     */
    @NotNull
    NamespacedKey getPointName();

    /**
     * マーカーの場所を取得
     *
     * @return 場所の座標
     */
    @NotNull
    BlockVector3 getPosition();

    /**
     * マーカーの向きを取得
     *
     * @return マーカーの向き
     */
    @NotNull
    BlockFace getDirection();
}
