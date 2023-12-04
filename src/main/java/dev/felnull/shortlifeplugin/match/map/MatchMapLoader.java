package dev.felnull.shortlifeplugin.match.map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.felnull.fnjl.util.FNDataUtil;
import dev.felnull.fnjl.util.FNStringUtil;
import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchMode;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * 試合用マップの読み込み関係
 *
 * @author MORIMORI0317
 */
public class MatchMapLoader {

    /**
     * ワールド名の接頭辞
     */
    public static final String WORLD_NAME_PREFIX = "world_match/";
    
    /**
     * GSON
     */
    private static final Gson GSON = new Gson();

    /**
     * ランダム
     */
    private static final Random RANDOM = new Random();

    /**
     * 読み込まれた全てのマップ
     */
    private final Map<String, MatchMap> maps = new HashMap<>();

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
        clearMatchWorldFolder();

        FNDataUtil.wishMkdir(SLFiles.schematicFolder());

        try {
            loadMap();
        } catch (IOException ex) {
            SLUtils.reportError(ex, "全ての試合用マップの読み込みに失敗");
        }

        plugin.getLogger().info("マップの読み込み完了");
    }


    /**
     * マップフォルダの全てのファイルから、マップを読み込み
     *
     * @throws IOException 読み込み失敗
     */
    private void loadMap() throws IOException {
        File mapFolder = SLFiles.mapFolder();
        FNDataUtil.wishMkdir(mapFolder);
        
        try (Stream<Path> paths = Files.walk(mapFolder.toPath())) {
            Stream<File> mapFiles = getMapFiles(paths);

            mapFiles.forEach(file -> loadMapFromJson(mapFolder, file));
        }
    }

    /**
     * パスの内マップファイルであるものを全て取得
     *
     * @param paths パス
     * @return マップファイル
     */
    @NotNull
    private static Stream<File> getMapFiles(Stream<Path> paths) {
        return paths.map(Path::toFile) // ファイルへ変換
                .filter(it -> !it.isDirectory()) // ディレクトリを除外
                .filter(it -> "json".equalsIgnoreCase(FNStringUtil.getExtension(it.getName())));
    }

    /**
     * Jsonから試合用マップ情報を読み込む
     *
     * @param mapFolder マップフォルダ
     * @param file マップファイル
     */
    private void loadMapFromJson(File mapFolder, File file) {
        String id = SLUtils.getIdFromPath(file, mapFolder);
        
        try (InputStream stream = new FileInputStream(file); Reader reader = new BufferedReader(new InputStreamReader(stream))) {
            JsonObject jo = GSON.fromJson(reader, JsonObject.class);
            MatchMap matchMap = MatchMap.of(id, jo);
            maps.put(id, matchMap);

            SLUtils.getLogger().info(String.format("試合用マップを読み込みました: %s", id));
        } catch (IOException | RuntimeException e) {
            SLUtils.reportError(e, String.format("試合用マップの読み込みに失敗: %s", id));
        }
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
        MatchMapInstance matchMapInstance = new MatchMapInstance();
        matchMapInstance.setMapWorld(mapInstanceLoader.load(match.getMatchMode(), matchMapInstance, mapInstanceId, matchMap));
        return matchMapInstance;
    }

    /**
     * 破棄処理
     */
    public void dispose() {
        mapInstanceLoader.stopAsyncExecutor();
        clearMatchWorldFolder();
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
     * 指定した試合モードが利用可能なマップをランダムで取得
     *
     * @param matchMode 試合モード
     * @return 試合マップ
     */
    public Optional<MatchMap> getRandomMap(@NotNull MatchMode matchMode) {
        List<MatchMap> availableMaps = this.maps.values().stream()
                .filter(map -> map.availableMatchModes().contains(matchMode))
                .toList();

        if (availableMaps.isEmpty()) {
            return Optional.empty();
        } else if (availableMaps.size() == 1) {
            return Optional.of(availableMaps.get(0));
        } else {
            return Optional.of(availableMaps.get(RANDOM.nextInt(availableMaps.size())));
        }
    }

    /**
     * 試合用ワールドフォルダを削除
     */
    private void clearMatchWorldFolder() {
        File matchWorldFolder = new File("./" + WORLD_NAME_PREFIX);

        try {
            FileUtils.deleteDirectory(matchWorldFolder);
        } catch (IOException e) {
            SLUtils.reportError(e, "試合用ワールドフォルダーの削除に失敗");
        }
    }
}
