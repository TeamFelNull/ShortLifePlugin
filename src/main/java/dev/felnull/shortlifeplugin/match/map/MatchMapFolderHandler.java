package dev.felnull.shortlifeplugin.match.map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.felnull.fnjl.util.FNDataUtil;
import dev.felnull.fnjl.util.FNStringUtil;
import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static dev.felnull.shortlifeplugin.match.map.MatchMapHandler.WORLD_NAME_PREFIX;

/**
 * 試合用マップフォルダ管理
 *
 * @author MORIMORI0317, Quarri6343
 */
public class MatchMapFolderHandler {

    /**
     * GSON
     */
    private static final Gson GSON = new Gson();

    /**
     * マップフォルダの全てのファイルから、マップを読み込み
     *
     * @return 読み込まれた全てのマップ
     * @throws IOException 読み込み失敗
     */
    public Map<String, MatchMap> loadMap() throws IOException {
        File mapFolder = SLFiles.mapFolder();
        FNDataUtil.wishMkdir(mapFolder);
        Map<String, MatchMap> maps = new HashMap<>();

        try (Stream<Path> paths = Files.walk(mapFolder.toPath())) {
            Stream<File> mapFiles = getMapFiles(paths);

            mapFiles.forEach(file -> loadMapFromJson(mapFolder, file, maps));
        }

        return maps;
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
     * @param file      マップファイル
     * @param maps      読み込まんだマップを格納するjavaのマップ
     */
    private void loadMapFromJson(File mapFolder, File file, Map<String, MatchMap> maps) {
        String id = SLUtils.getIdFromPath(file, mapFolder);

        try (InputStream stream = new FileInputStream(file); Reader reader = new BufferedReader(new InputStreamReader(stream))) {
            JsonObject jo = GSON.fromJson(reader, JsonObject.class);
            MatchMap matchMap = MatchMap.of(id, jo);
            maps.put(id, matchMap);

            SLUtils.getLogger().info(MsgHandler.getFormatted("system-map-loaded", id));
        } catch (IOException | RuntimeException e) {
            SLUtils.reportError(e, MsgHandler.getFormatted("system-map-load-failed", id));
        }
    }

    /**
     * 試合用ワールドフォルダを削除
     */
    public void clearMatchWorldFolder() {
        File matchWorldFolder = new File("./" + WORLD_NAME_PREFIX);

        try {
            FileUtils.deleteDirectory(matchWorldFolder);
        } catch (IOException e) {
            SLUtils.reportError(e, MsgHandler.get("system-map-folder-deletion-failed"));
        }
    }
}
