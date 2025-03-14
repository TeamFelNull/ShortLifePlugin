package dev.felnull.shortlifeplugin.match.map;

import com.google.common.collect.ImmutableMap;
import dev.felnull.fnjl.util.FNDataUtil;
import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchMode;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.*;

/**
 * 試合用マップの管理クラス
 *
 * @author MORIMORI0317, Quarri6343
 */
public class MatchMapHandler {

    /**
     * ワールド名の接頭辞
     */
    public static final String WORLD_NAME_PREFIX = "world_match/";

    /**
     * ランダム
     */
    private static final Random RANDOM = new Random();

    /**
     * 読み込まれた全てのマップ
     */
    private Map<String, MatchMap> maps = new HashMap<>();

    /**
     * マップのフォルダ管理クラス
     */
    private final MatchMapFolderHandler mapFolderHandler = new MatchMapFolderHandler();

    /**
     * マップのインスタンスのローダー
     */
    private final MatchMapInstanceLoader mapInstanceLoader = new MatchMapInstanceLoader();

    /**
     * 初期化処理
     *
     * @param plugin プラグイン
     */
    public void init(ShortLifePlugin plugin) {
        mapFolderHandler.clearMatchWorldFolder();

        FNDataUtil.wishMkdir(SLFiles.schematicFolder());

        try {
            maps = mapFolderHandler.loadMap();
        } catch (IOException ex) {
            SLUtils.reportError(ex, MsgHandler.get("system-all-map-load-failed"));
        }

        plugin.getLogger().info(MsgHandler.get("system-all-map-loaded"));
    }

    /**
     * Tick処理
     */
    public void tick() {
        this.mapInstanceLoader.tick();
    }

    /**
     * マップインスタンスを作成
     *
     * @param match         試合
     * @param mapInstanceId マップインスタンスID
     * @param matchMap      試合用マップ
     * @return マップインスタンス
     */
    public MatchMapInstance createMapInstance(@NotNull Match match, @NotNull String mapInstanceId, @NotNull MatchMap matchMap) {
        MatchMapInstance matchMapInstance = new MatchMapInstance(mapInstanceId, matchMap);
        matchMapInstance.setMapWorld(mapInstanceLoader.load(match.getMatchMode(), matchMapInstance));
        return matchMapInstance;
    }

    /**
     * 破棄処理
     */
    public void dispose() {
        mapInstanceLoader.stopAsyncExecutor();
        mapFolderHandler.clearMatchWorldFolder();
    }

    /**
     * 読み込み済みのマップを取得
     *
     * @param mapId マップID
     * @return 試合用マップ
     */
    public Optional<MatchMap> getMap(@NotNull String mapId) {
        return Optional.ofNullable(this.maps.get(mapId));
    }

    @Unmodifiable
    @NotNull
    public Map<String, MatchMap> getAllMap() {
        return ImmutableMap.copyOf(this.maps);
    }

    /**
     * 指定した試合モードで利用可能なマップが存在するか確認
     *
     * @param matchMode 試合モード
     * @return 利用可能なマップが存在すればtrue、しなければfalse
     */
    public boolean isAvailableMapExists(@NotNull MatchMode matchMode) {
        return this.maps.values().stream()
                .anyMatch(map -> map.availableMatchModes().contains(matchMode));
    }

    /**
     * 指定したゲームモードで利用可能なマップリストを取得する
     *
     * @param matchMode 試合モード
     * @return 利用可能なマップのリスト
     */
    @Unmodifiable
    @NotNull
    public List<MatchMap> getAvailableMaps(@NotNull MatchMode matchMode) {
        Objects.requireNonNull(matchMode);

        return getAllMap().values().stream()
                .filter(it -> it.availableMatchModes().contains(matchMode))
                .toList();
    }
}
