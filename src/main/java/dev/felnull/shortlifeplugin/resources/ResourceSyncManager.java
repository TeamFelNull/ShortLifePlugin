package dev.felnull.shortlifeplugin.resources;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * クライアントとのリソース同期管理
 *
 * @author MORIMORI0317
 */
public class ResourceSyncManager {

    /**
     * リソースマッピングのキャッシュ保存先
     */
    private static final File MAPPING_CACHE_FILE = new File(SLFiles.textureReleaseCacheFolder(), "resourceMapping.json");

    /**
     * GSON
     */
    private static final Gson GSON = new Gson();

    /**
     * マッピング名とカスタムモデルのエントリのマップ
     */
    private final Map<String, CustomModel> customModels = new HashMap<>();

    /**
     * リソースマッピング情報が更新された際にカウントアップを行う
     */
    private int updateCounter = 0;

    /**
     * 最後に読み込んだリリースバージョン
     */
    private String lastReleaseVersion;

    /**
     * インスタンス取得
     *
     * @return リソース同期マネージャー
     */
    public static ResourceSyncManager getInstance() {
        ResourceSyncManager instance = SLUtils.getSLPlugin().getResourceSyncManager();

        if (instance == null) {
            throw new IllegalStateException("インスタンスが作成されていません");
        }

        return instance;
    }

    /**
     * 初期化
     */
    public void init() {
        // キャッシュファイルが存在する場合は読み込む
        if (MAPPING_CACHE_FILE.exists() && !MAPPING_CACHE_FILE.isDirectory()) {
            try (Reader reader = new FileReader(MAPPING_CACHE_FILE)) {
                JsonObject jo = GSON.fromJson(reader, JsonObject.class);
                this.lastReleaseVersion = jo.get("release_version").getAsString();
                JsonObject resMapJo = jo.getAsJsonObject("mapping");
                load(resMapJo);
                
                SLUtils.getLogger().info("リソースマッピングのキャッシュを読み込みました");
            } catch (IOException | RuntimeException e) {
                SLUtils.reportError(e, "リソースマッピングのキャッシュ読み込みに失敗");
                this.lastReleaseVersion = null;
                clear();
            }
        }
    }

    /**
     * 非同期でリソースマッピングを読み込む<br>
     * 既に読み込み中の場合は、中止され再び読み込み開始する
     *
     * @param version バージョン
     * @param url     読み込み先URL
     */
    protected void loadAsync(@NotNull String version, @NotNull String url) {
        // 最後の読み込みバージョンと同じバージョンのリソースマップは読み込まない
        if (version.equals(this.lastReleaseVersion)) {
            return;
        }

        this.lastReleaseVersion = version;


    }

    private void reload(JsonObject jo) {
        updateCounter++;

        try {

        } catch (RuntimeException ex1) {

        }

    }

    private void load(JsonObject jo) {
        clear();

        int version = jo.get("version").getAsInt();
        if (version != 0) {
            throw new RuntimeException("サポートしていないマッピングバージョンです");
        }

        // カスタムモデルのマッピングを読み取り
        JsonObject customModelsJo = jo.getAsJsonObject("custom_model");
        customModelsJo.entrySet().forEach(entry -> {
            JsonObject customModelJo = entry.getValue().getAsJsonObject();
            NamespacedKey model = NamespacedKey.fromString(customModelJo.get("model").getAsString());
            CustomModel customModel = new CustomModel(Objects.requireNonNull(model), customModelJo.get("num").getAsInt());
            this.customModels.put(entry.getKey(), customModel);
        });
    }

    private void clear() {
        this.customModels.clear();
    }

    public int getUpdateCounter() {
        return updateCounter;
    }

    /**
     * 指定されたIDのカスタムモデルエントリを取得<br/>
     * 値を保持する場合は、{@link #getUpdateCounter()}が更新されていないか監視を行う必要がある
     *
     * @param id マッピングID
     * @return カスタムモデルエントリ
     */
    @Nullable
    public CustomModel getCustomModel(@NotNull String id) {
        return this.customModels.get(Objects.requireNonNull(id));
    }

    /**
     * カスタムモデルのエントリ
     *
     * @param model  モデルのロケーション
     * @param number カスタムモデル番号
     * @author MORIMORI0317
     */
    public record CustomModel(@NotNull NamespacedKey model, int number) {
    }

    private class ResourceMappingLoader {

        private void destory() {

        }
    }
}
