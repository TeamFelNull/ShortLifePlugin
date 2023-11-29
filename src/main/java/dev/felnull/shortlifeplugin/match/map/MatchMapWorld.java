package dev.felnull.shortlifeplugin.match.map;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * マップのワールドの作成、保持用クラス
 *
 * @author MORIMORI0317
 */
public class MatchMapWorld {

    /**
     * ランダム
     */
    private static final Random RANDOM = new Random();

    /**
     * 試合用マップ
     */
    @NotNull
    private final MatchMap matchMap;

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
     * @param matchMap  試合用マップ
     * @param world     ワールド
     * @param markerSet マーカーの集まり
     */
    protected MatchMapWorld(@NotNull MatchMap matchMap, @NotNull World world, @NotNull MapMarkerSet markerSet) {
        this.matchMap = matchMap;
        this.world = world;
        this.markerSet = markerSet;
    }

    public @NotNull World getWorld() {
        return world;
    }

    /**
     * ポイント名からマーカのコレクションを取得
     *
     * @param pointName ポイント名
     * @return マーカーのコレクション
     */
    @NotNull
    @Unmodifiable
    public Collection<MapMarker> getMarker(NamespacedKey pointName) {
        return markerSet.makers().get(pointName);
    }

    /**
     * 指定されたポイント名のマーカーコレクションからランダムで一つ選び返す
     *
     * @param pointName ポイント名
     * @return マーカー
     */
    public Optional<MapMarker> getMakerRandom(NamespacedKey pointName) {
        Collection<MapMarker> markers = getMarker(pointName);

        if (markers.isEmpty()) {
            return Optional.empty();
        }

        List<MapMarker> makersList = new ArrayList<>(markers);

        if (makersList.size() == 1) {
            return Optional.of(makersList.get(0));
        }

        return Optional.of(makersList.get(RANDOM.nextInt(makersList.size())));
    }

    /**
     * マップのオフセットを考慮した座標を取得
     *
     * @param position 座標
     * @return 補正済み座標
     */
    public BlockVector3 offsetCorrection(BlockVector3 position) {
        return position.add(matchMap.offset());
    }
}
