package dev.felnull.shortlifeplugin;

import com.google.gson.*;
import dev.felnull.fnjl.util.FNDataUtil;
import dev.felnull.shortlifeplugin.listener.CommonListener;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * テクスチャのリリースを監視するシステム
 *
 * @author MORIMORI0317
 */
public class TextureReleaseWatcher {

    /**
     * ShortLifeTextureのレポジトリ
     */
    private static final String REPOSITORY = "TeamFelnull/ShortLifeTexture";

    /**
     * 最終リリース確認用URL<br/>
     * 1時間に60回まで (認証すればもう少し増える？)
     */
    private static final String LAST_RELEASE_CHECK_URL = String.format("https://api.github.com/repos/%s/releases/latest", REPOSITORY);

    /**
     * テクスチャのリリース名
     */
    private static final String TEXTURE_PACK_NAME = "TexturePack.zip";

    /**
     * リソースマッピングのリリース名
     */
    private static final String PACK_MAPPING_NAME = "pack_map.json";

    /**
     * 最終リリースの情報を保持するファイル
     */
    private static final File LAST_RELEASE_INFO_FILE = new File(SLFiles.textureReleaseCacheFolder(), "lastRelease.json");

    /**
     * GSON
     */
    private static final Gson GSON = new Gson();

    /**
     * 監視を行う間隔
     */
    private static final long WATCH_INTERVAL = 1000 * 60 * 15;

    /**
     * Http処理用クライアント
     */
    private final HttpClient httpClient;

    /**
     * 破棄済みかどうか
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Tickエクスキューター
     */
    private Executor tickExecutor;

    /**
     * 監視タスク
     */
    private BukkitTask watchTask;

    /**
     * 最終バージョン
     */
    private String lastVersion;

    /**
     * コンストラクタ
     */
    protected TextureReleaseWatcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .executor(SLExecutors.IO)
                .build();
    }

    /**
     * 初期化
     *
     * @param plugin プラグイン
     */
    public void init(ShortLifePlugin plugin) {
        this.tickExecutor = Bukkit.getScheduler().getMainThreadExecutor(plugin);

        // 最終起動時のリリース情報を読み込み
        ReleaseInfo lastStartupReleaseInfo = loadLastReleaseInfo();

        if (lastStartupReleaseInfo != null) {
            SLUtils.getLogger().info(String.format("前回起動時の最終リリース情報を読み込みました: %s", lastStartupReleaseInfo.version()));

            this.lastVersion = lastStartupReleaseInfo.version();
            CommonListener.onTextureRelease(null,
                    lastStartupReleaseInfo.version(), lastStartupReleaseInfo.texturePackUrl(), lastStartupReleaseInfo.packMapUrl());
        }

        releaseCheck();
    }

    /**
     * 前回終了時の最終リリース情報を読み取り
     */
    private ReleaseInfo loadLastReleaseInfo() {
        if (LAST_RELEASE_INFO_FILE.exists() && !LAST_RELEASE_INFO_FILE.isDirectory()) {
            try {
                JsonObject lastResJo;

                try (Reader reader = new BufferedReader(new FileReader(LAST_RELEASE_INFO_FILE))) {
                    lastResJo = GSON.fromJson(reader, JsonObject.class);
                }

                String version = lastResJo.getAsJsonPrimitive("version").getAsString();
                String texturePackUrl = lastResJo.getAsJsonPrimitive("texture_pack_url").getAsString();
                String packMapUrl = lastResJo.getAsJsonPrimitive("pack_map_url").getAsString();

                return new ReleaseInfo(version, texturePackUrl, packMapUrl);
            } catch (IOException | JsonSyntaxException e) {
                SLUtils.reportError(e, "前回終了時の最終リリース情報取得に失敗");
            }
        }

        return null;
    }

    /**
     * 最終リリース情報書き込み
     *
     * @param version        バージョン
     * @param texturePackUrl テクスチャパックのURL
     * @param packMapUrl     パックマッピングのURL
     */
    private void saveLastReleaseInfo(String version, String texturePackUrl, String packMapUrl) {
        JsonObject lastResJo = new JsonObject();

        lastResJo.addProperty("version", version);
        lastResJo.addProperty("texture_pack_url", texturePackUrl);
        lastResJo.addProperty("pack_map_url", packMapUrl);

        FNDataUtil.wishMkdir(LAST_RELEASE_INFO_FILE.getParentFile());

        try (Writer writer = new BufferedWriter(new FileWriter(LAST_RELEASE_INFO_FILE))) {
            GSON.toJson(lastResJo, writer);
        } catch (IOException e) {
            SLUtils.reportError(e, "最終リリース情報の書き込みに失敗");
        }
    }

    private BukkitTask addReleaseCheckTask(ShortLifePlugin plugin) {
        return Bukkit.getScheduler().runTaskLater(plugin, this::releaseCheck, SLUtils.toTick(TimeUnit.MILLISECONDS, WATCH_INTERVAL));
    }

    /**
     * リリース確認
     */
    private void releaseCheck() {
        try {
            // 非同期監視実行
            releaseCheckAsync(this.lastVersion)
                    .whenCompleteAsync((ret, error) -> {
                        /* 非同期実行後にTick同期で結果を処理 */
                        assertDispose();

                        try {
                            if (error != null) {
                                SLUtils.reportError(error, "リリース監視タスク実行中にエラーが発生");
                            } else if (ret != null) {
                                CommonListener.onTextureRelease(this.lastVersion, ret.version(), ret.texturePackUrl(), ret.packMapUrl());
                            }
                        } catch (Exception ex) {
                            SLUtils.reportError(ex, "リリース監視の結果処理に失敗");
                        } finally {
                            if (ret != null) {
                                this.lastVersion = ret.version();
                            }
                            this.watchTask = addReleaseCheckTask(SLUtils.getSLPlugin());
                        }

                    }, this.tickExecutor);
        } catch (Exception ex) {
            /* タスク実行時にエラーが発生した場合 */
            SLUtils.reportError(ex, "リリース監視タスク実行に失敗");
            this.watchTask = addReleaseCheckTask(SLUtils.getSLPlugin());
        }
    }

    /**
     * 非同期でテクスチャリリースを確認
     *
     * @param lastVersion 最終リリースバージョン
     * @return 非同期コンプリータブルフューチャー
     */
    private CompletableFuture<ReleaseInfo> releaseCheckAsync(@Nullable String lastVersion) {
        return CompletableFuture.supplyAsync(() -> {
                    /* 非同期(IO)でテクスチャのリリース情報取得 */
                    assertDispose();

                    HttpRequest req = HttpRequest.newBuilder(URI.create(LAST_RELEASE_CHECK_URL))
                            .GET()
                            .header("X-GitHub-Api-Version", "2022-11-28")
                            .build();

                    JsonObject ret;

                    try {
                        HttpResponse<InputStream> res = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());

                        try (Reader reader = new InputStreamReader(new BufferedInputStream(res.body()))) {
                            ret = GSON.fromJson(reader, JsonObject.class);
                        }

                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    return ret;
                }, SLExecutors.IO)
                .thenApplyAsync((retJson) -> {
                    /* 非同期(デフォルト)でリリース情報解析 */
                    /* バージョンが無効、もしくは最終バージョンと同じであればnullを返す */
                    assertDispose();

                    if (!retJson.has("tag_name")) {
                        throw new RuntimeException(retJson.get("message").getAsString());
                    }

                    String version = retJson.getAsJsonPrimitive("tag_name").getAsString();

                    // バージョンがvから始まるか確認  例: v1.0
                    if (!version.startsWith("v")) {
                        return null;
                    }

                    // vを削除 v1.0 -> 1.0
                    version = version.substring(1);

                    // 最終バージョンと比較
                    if (version.equals(lastVersion)) {
                        return null;
                    }

                    JsonArray assetsJa = retJson.getAsJsonArray("assets");
                    JsonObject textureAssetsJo = null;
                    JsonObject mappingAssetsJo = null;

                    // テクスチャとマッピングのJsonを取得
                    for (JsonElement je : assetsJa) {
                        if (je instanceof JsonObject jo) {
                            if (jo.has("name")
                                    && jo.get("name").isJsonPrimitive() && jo.getAsJsonPrimitive("name").isString()) {
                                String name = jo.getAsJsonPrimitive("name").getAsString();
                                if (TEXTURE_PACK_NAME.equals(name)) {
                                    textureAssetsJo = jo;
                                } else if (PACK_MAPPING_NAME.equals(name)) {
                                    mappingAssetsJo = jo;
                                }
                            }
                        }

                        if (textureAssetsJo != null && mappingAssetsJo != null) {
                            break;
                        }
                    }

                    String texturePackUrl = textureAssetsJo == null ? null : textureAssetsJo.getAsJsonPrimitive("browser_download_url").getAsString();
                    String packMapUrl = mappingAssetsJo == null ? null : mappingAssetsJo.getAsJsonPrimitive("browser_download_url").getAsString();

                    return new ReleaseInfo(version, texturePackUrl, packMapUrl);
                }, SLExecutors.DEFAULT).thenApplyAsync(ret -> {
                    /* 非同期(IO)で最終リリース情報ファイルを更新 */
                    assertDispose();

                    if (ret != null) {
                        // 破棄後もしくは破棄中に保存されることを避ける
                        synchronized (this.disposed) {
                            assertDispose();
                            saveLastReleaseInfo(ret.version(), ret.texturePackUrl(), ret.packMapUrl());
                        }
                    }

                    return ret;
                }, SLExecutors.IO);
    }

    /**
     * 破棄処理
     */
    public void dispose() {

        synchronized (this.disposed) {
            this.disposed.set(true);
        }

        if (this.watchTask != null) {
            this.watchTask.cancel();
        }
    }

    private void assertDispose() {
        if (this.disposed.get()) {
            throw new RuntimeException("破棄済みです");
        }
    }

    /**
     * リリース情報
     *
     * @param version        パックのバージョン
     * @param texturePackUrl テクスチャのパックのURL
     * @param packMapUrl     パックマッピングのURL
     * @author MORIMORI0317
     */
    private record ReleaseInfo(@NotNull String version, @Nullable String texturePackUrl, @Nullable String packMapUrl) {
    }
}
