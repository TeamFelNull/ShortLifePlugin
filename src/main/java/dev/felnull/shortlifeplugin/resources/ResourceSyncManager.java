package dev.felnull.shortlifeplugin.resources;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.felnull.shortlifeplugin.SLExecutors;
import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

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
     * Http処理用クライアント
     */
    private final HttpClient httpClient;

    /**
     * マッピング名とカスタムモデルのエントリのマップ
     */
    private final Map<String, CustomModel> customModels = new HashMap<>();

    /**
     * 最後に読み込んだリリースバージョン
     */
    private String lastReleaseVersion;

    /**
     * 動作中のリソースマッピングローダー
     */
    private ResourceMappingLoader mappingLoader;

    /**
     * Tickエクスキューター
     */
    private Executor tickExecutor;

    /**
     * 更新カウント
     */
    private int updateCount = 0;

    /**
     * コンストラクタ
     */
    public ResourceSyncManager() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .executor(SLExecutors.IO)
                .build();
    }

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
     *
     * @param plugin プラグイン
     */
    public void init(ShortLifePlugin plugin) {
        this.tickExecutor = Bukkit.getScheduler().getMainThreadExecutor(plugin);

        // キャッシュファイルが存在する場合は読み込む
        if (MAPPING_CACHE_FILE.exists() && !MAPPING_CACHE_FILE.isDirectory()) {
            try (Reader reader = new FileReader(MAPPING_CACHE_FILE)) {
                JsonObject jo = GSON.fromJson(reader, JsonObject.class);
                this.lastReleaseVersion = jo.get("release_version").getAsString();
                JsonObject resMapJo = jo.getAsJsonObject("mapping");
                ResourceMappingLoadResult loadResult = loadMapping(resMapJo);
                setResult(loadResult);

                SLUtils.getLogger().info("リソースマッピングのキャッシュを読み込みました");
            } catch (IOException | RuntimeException e) {
                SLUtils.reportError(e, "リソースマッピングのキャッシュ読み込みに失敗");
                this.lastReleaseVersion = null;
            }
        }

        Bukkit.getScheduler().runTaskTimer(plugin, task -> this.tick(), 1, 1);
    }

    private void tick() {
        if (this.mappingLoader != null) {
            ResourceMappingLoadResult loadResult = this.mappingLoader.getLoadResult();
            // ローダーが読み込み終わっていれば結果を反映する
            if (loadResult != null) {
                SLUtils.getLogger().info("リソースマッピングが更新されました");
                setResult(loadResult);
            }

            // 読み込み処理が完了していればローダーを破棄
            if (this.mappingLoader.isDone()) {
                this.mappingLoader.destory();
                this.mappingLoader = null;
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
        if (this.mappingLoader != null) {
            /* 既に読み込み中の場合 */

            // 最後の読み込みバージョンと同じバージョンのリソースマップは読み込みなおさない
            if (version.equals(this.lastReleaseVersion)) {
                return;
            }
            this.lastReleaseVersion = version;

            // 現在動作中のローダーを止める
            this.mappingLoader.destory();
            this.mappingLoader = null;
        }

        // 読み込み開始
        this.mappingLoader = new ResourceMappingLoader(url, version);
        this.mappingLoader.start();
    }

    /**
     * 読み込み結果を反映させる
     *
     * @param loadResult 読み込み結果
     */
    private void setResult(ResourceMappingLoadResult loadResult) {
        this.updateCount++;

        this.customModels.clear();
        this.customModels.putAll(loadResult.customModels());
    }

    /**
     * Jsonからマッピングを読み込んで、結果を返す
     *
     * @param jo Json
     * @return 読み込み結果
     */
    private ResourceMappingLoadResult loadMapping(JsonObject jo) {
        int version = jo.get("version").getAsInt();
        if (version != 0) {
            throw new RuntimeException("サポートしていないマッピングバージョンです");
        }

        ImmutableMap.Builder<String, CustomModel> customModelBuilder = new ImmutableMap.Builder<>();

        // カスタムモデルのマッピングを読み取り
        JsonObject customModelsJo = jo.getAsJsonObject("custom_model");
        customModelsJo.entrySet().forEach(entry -> {
            JsonObject customModelJo = entry.getValue().getAsJsonObject();
            NamespacedKey model = NamespacedKey.fromString(customModelJo.get("model").getAsString());
            CustomModel customModel = new CustomModel(Objects.requireNonNull(model), customModelJo.get("num").getAsInt());
            customModelBuilder.put(entry.getKey(), customModel);
        });

        return new ResourceMappingLoadResult(customModelBuilder.build());
    }

    /**
     * 指定されたIDのカスタムモデルエントリを取得<br/>
     *
     * @param id マッピングID
     * @return カスタムモデルエントリ
     */
    @Nullable
    public CustomModel getCustomModel(@NotNull String id) {
        return this.customModels.get(Objects.requireNonNull(id));
    }

    /**
     * 破棄処理
     */
    public void dispose() {
        if (this.mappingLoader != null) {
            this.mappingLoader.destory();
        }
    }

    /**
     * 更新カウントを取得
     *
     * @return 更新カウント
     */
    public int getUpdateCount() {
        return updateCount;
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

    /**
     * リソースマップを読み込むローダー
     *
     * @author MORIMORI0317
     */
    private class ResourceMappingLoader {


        /**
         * 破棄フラグ
         */
        private final AtomicBoolean destroyed = new AtomicBoolean();


        /**
         * 読み込み先URL
         */
        private final String url;

        /**
         * このローダーが読み込んでるリソースのバージョン
         */
        private final String version;

        /**
         * 読み込んだリソースマップの結果
         */
        private ResourceMappingLoadResult loadResult;

        /**
         * 完了フラグ
         */
        private boolean done = false;

        protected ResourceMappingLoader(String url, String version) {
            this.url = url;
            this.version = version;
        }

        protected void start() {
            CompletableFuture.supplyAsync(() -> {
                /* 非同期(IO)でURLからJsonを取得 */
                assertDestory();

                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .version(HttpClient.Version.HTTP_1_1)
                        .GET()
                        .build();

                JsonObject ret;

                try {
                    HttpResponse<InputStream> res = ResourceSyncManager.this.httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());

                    try (Reader reader = new InputStreamReader(new BufferedInputStream(res.body()))) {
                        ret = GSON.fromJson(reader, JsonObject.class);
                    }

                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                Objects.requireNonNull(ret);

                return ret;
            }, SLExecutors.IO).thenApplyAsync((json) -> {
                /* 非同期(デフォルト)でJsonからマッピングを読み込む */
                assertDestory();

                return Pair.of(ResourceSyncManager.this.loadMapping(json), json);
            }, SLExecutors.DEFAULT).thenApplyAsync((resultAndJson) -> {
                /* 非同期(IO)でキャッシュファイルを作成 */
                assertDestory();

                // 破棄後もしくは破棄中に保存されることを避ける
                synchronized (this.destroyed) {
                    assertDestory();
                    try (Writer writer = new BufferedWriter(new FileWriter(MAPPING_CACHE_FILE))) {
                        JsonObject jo = new JsonObject();
                        jo.addProperty("release_version", this.version);
                        jo.add("mapping", resultAndJson.right());

                        GSON.toJson(jo, writer);
                    } catch (IOException e) {
                        SLUtils.reportError(e, "リソースマッピングのキャッシュ作成に失敗");
                    }
                }

                return resultAndJson.left();
            }, SLExecutors.IO).whenCompleteAsync((result, error) -> {
                /* Tick同期で完了処理 */
                this.done = true;

                assertDestory();

                if (error == null) {
                    this.loadResult = result;
                } else {
                    SLUtils.reportError(error, "リソースマッピングの読み込みに失敗");
                }
            }, tickExecutor);
        }

        /**
         * 読み込み結果を取得<br/>
         * 読み込みが完了するまでnull
         *
         * @return 読み込み結果
         */
        @Nullable
        public ResourceMappingLoadResult getLoadResult() {
            return loadResult;
        }

        private void assertDestory() {
            if (this.destroyed.get()) {
                throw new RuntimeException("停止済み");
            }
        }

        private void destory() {
            synchronized (this.destroyed) {
                this.destroyed.set(true);
            }
        }

        protected boolean isDone() {
            return done;
        }
    }

    /**
     * リソースマップの読み込み結果
     *
     * @param customModels カスタムモデル定義
     * @author MORIMORI0317
     */
    private record ResourceMappingLoadResult(Map<String, CustomModel> customModels) {
    }
}
