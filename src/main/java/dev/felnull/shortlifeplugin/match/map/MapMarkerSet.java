package dev.felnull.shortlifeplugin.match.map;


import com.google.common.collect.Multimap;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * 複数のマーカーが集まった集合レコード
 *
 * @param makers 地点名とマーカーのマルチマップ
 * @author MORIMORI0317
 */
public record MapMarkerSet(@NotNull @Unmodifiable Multimap<NamespacedKey, MapMarker> makers) {
}
