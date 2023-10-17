package dev.felnull.shortlifeplugin.match.map;

import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import dev.felnull.fnjl.util.FNDataUtil;
import dev.felnull.fnjl.util.FNStringUtil;
import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchMode;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 試合用マップの読み込み関係
 *
 * @author MORIMORI0317
 */
public class MatchMapLoader {

    /**
     * GSON
     */
    private static final Gson GSON = new Gson();

    /**
     * ランダム
     */
    private static final Random RANDOM = new Random();

    /**
     * ワールド名の接頭辞
     */
    private static final String WORLD_NAME_PREFIX = "world_match/";

    /**
     * Tickに同期して処理を行うExecutor
     */
    private final Executor tickExecutor = Bukkit.getScheduler().getMainThreadExecutor(SLUtils.getSLPlugin());

    /**
     * 非同期で処理を行うExecutor
     */
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 1),
            new BasicThreadFactory.Builder().namingPattern("map-loader-worker-%d").daemon(true).build());

    /**
     * 読み込まれた全てのマップ
     */
    private final Map<String, MatchMap> maps = new HashMap<>();

    /**
     * 試合用ワールドデータのキャッシュ
     */
    private final Supplier<CompletableFuture<File>> worldCache = Suppliers.memoize(this::createWorldCache);

    /**
     * マーカーのキャッシュ
     */
    private final Cache<HashCode, MapMarkerSet> mapMarkerCache = CacheBuilder.newBuilder()
            .maximumSize(30)
            .build();

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


    private void loadMap() throws IOException {
        File mapFolder = SLFiles.mapFolder();
        FNDataUtil.wishMkdir(mapFolder);

        // マップフォルダの全てのファイルから、マップを読み込み
        try (Stream<Path> paths = Files.walk(mapFolder.toPath())) {
            Stream<File> mapFiles = paths.map(Path::toFile) // ファイルへ変換
                    .filter(it -> !it.isDirectory()) // ディレクトリを除外
                    .filter(it -> "json".equalsIgnoreCase(FNStringUtil.getExtension(it.getName()))); // 拡張子がJsonのファイルでフィルタリング

            mapFiles.forEach(file -> {
                String id = SLUtils.getIdFromPath(file, mapFolder);

                // Jsonから試合用マップ情報を読み込む
                try (InputStream stream = new FileInputStream(file); Reader reader = new BufferedReader(new InputStreamReader(stream))) {
                    JsonObject jo = GSON.fromJson(reader, JsonObject.class);
                    MatchMap matchMap = MatchMap.of(id, jo);
                    maps.put(id, matchMap);

                    SLUtils.getLogger().info(String.format("試合用マップを読み込みました: %s", id));
                } catch (IOException | RuntimeException e) {
                    SLUtils.reportError(e, String.format("試合用マップの読み込みに失敗: %s", id));
                }

            });
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
        matchMapInstance.setMapWorld(load(match.getMatchMode(), matchMapInstance, mapInstanceId, matchMap));
        return matchMapInstance;
    }

    /**
     * 破棄処理
     */
    public void dispose() {

        // 非同期処理用Executorを停止
        this.asyncExecutor.shutdown();
        try {
            if (!this.asyncExecutor.awaitTermination(194, TimeUnit.SECONDS)) {
                this.asyncExecutor.shutdownNow();
                if (!this.asyncExecutor.awaitTermination(194, TimeUnit.SECONDS)) {
                    SLUtils.getLogger().warning("非同期処理用Executorの停止に失敗");
                }
            }
        } catch (InterruptedException e) {
            this.asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        clearMatchWorldFolder();
    }

    /**
     * 読み込み済みのマップを取得
     *
     * @param mapId マップID
     * @return 試合用マップ
     */
    @Nullable
    public MatchMap getMap(@NotNull String mapId) {
        return this.maps.get(mapId);
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
    @Nullable
    public MatchMap getRandomMap(@NotNull MatchMode matchMode) {
        List<MatchMap> availableMaps = this.maps.values().stream()
                .filter(map -> map.availableMatchModes().contains(matchMode))
                .toList();

        if (availableMaps.isEmpty()) {
            return null;
        } else if (availableMaps.size() == 1) {
            return availableMaps.get(0);
        } else {
            return availableMaps.get(RANDOM.nextInt(availableMaps.size()));
        }
    }

    private void clearMatchWorldFolder() {
        File matchWorldFolder = new File("./" + WORLD_NAME_PREFIX);

        try {
            FileUtils.deleteDirectory(matchWorldFolder);
        } catch (IOException e) {
            SLUtils.reportError(e, "試合用ワールドフォルダーの削除に失敗");
        }
    }

    private CompletableFuture<MatchMapWorld> load(@NotNull MatchMode matchMode, @NotNull MatchMapInstance matchMapInstance,
                                                  @NotNull String worldId, @NotNull MatchMap matchMap) {
        CompletableFuture<Pair<Clipboard, MapMarkerSet>> schemCompletableFuture = loadSchematic(worldId, matchMap);
        CompletableFuture<World> worldCompletableFuture = loadWorld(matchMapInstance, worldId);

        return worldCompletableFuture
                .thenCombineAsync(schemCompletableFuture, (world, clipboardMapMarkerSetPair) -> {
                    /* Tick同期でワールドにスケマティック構造物を生成 */

                    com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                        Operation operation = new ClipboardHolder(clipboardMapMarkerSetPair.getLeft())
                                .createPaste(editSession)
                                .to(matchMap.offset())
                                .copyEntities(true)
                                .copyBiomes(true)
                                .build();
                        Operations.complete(operation);
                    } catch (WorldEditException e) {
                        throw new RuntimeException(e);
                    }

                    SLUtils.getLogger().info(String.format("試合用マップインスタンス(%s)の準備完了", worldId));

                    return new MatchMapWorld(matchMap, world, clipboardMapMarkerSetPair.getRight());
                }, tickExecutor).thenApplyAsync(matchMapWorld -> {
                    /* Tick同期でマップ検証 */

                    if (!matchMode.mapValidator().test(matchMapWorld)) {
                        throw new RuntimeException("マップの検証に失敗");
                    }

                    return matchMapWorld;
                }, tickExecutor);
    }

    private CompletableFuture<Pair<Clipboard, MapMarkerSet>> loadSchematic(@NotNull String worldId, @NotNull MatchMap matchMap) {
        return CompletableFuture.supplyAsync(() -> {
            /* 非同期でスケマティックファイルを読み込む */

            // スケマティックファイルをクリップボードへ読み込む
            File schemFile = new File(SLFiles.schematicFolder(), matchMap.schematic() + ".schem");

            if (!schemFile.exists()) {
                throw new RuntimeException(String.format("%sは存在しません", schemFile.getName()));
            }

            if (schemFile.isDirectory()) {
                throw new RuntimeException(String.format("%sはディレクトリです", schemFile.getName()));
            }

            Clipboard clipboard;
            ClipboardFormat format = Objects.requireNonNull(ClipboardFormats.findByFile(schemFile));
            HashCode hashCode;

            try (HashingInputStream hashingInputStream = new HashingInputStream(Hashing.goodFastHash(160), new BufferedInputStream(new FileInputStream(schemFile)));
                 ClipboardReader reader = format.getReader(hashingInputStream)) {
                clipboard = reader.read();
                hashCode = hashingInputStream.hash();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            SLUtils.getLogger().info(String.format("試合用マップインスタンス(%s)のスケマティック読み込み完了", worldId));

            return Pair.of(clipboard, hashCode);
        }, asyncExecutor).thenApplyAsync(clipboardAndHashCode -> {
            /* 非同期でマーカーの集まりを取得する */
            Clipboard clipboard = clipboardAndHashCode.getLeft();
            MapMarkerSet markerSet;

            // キャッシュを参照して、存在しなければマーカーの集まりを取得する
            try {
                markerSet = mapMarkerCache.get(clipboardAndHashCode.getRight(), () -> computeMarkerSet(clipboard));
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }

            // ジグソーブロックを置き換え
            for (MapMarker maker : markerSet.makers().values()) {
                if (maker instanceof JigsawMapMarker jigsawMapMarker) {
                    Material replaceMaterial = Material.matchMaterial(jigsawMapMarker.replaceBlockId().toString());
                    if (replaceMaterial != null && replaceMaterial.isBlock()) {
                        BlockState replaceBlock = BukkitAdapter.adapt(replaceMaterial.createBlockData());
                        try {
                            clipboard.setBlock(jigsawMapMarker.position().add(clipboard.getOrigin()), replaceBlock);
                        } catch (WorldEditException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            SLUtils.getLogger().info(String.format("試合用マップインスタンス(%s)のマーカー読み込み完了", worldId));
            return Pair.of(clipboard, markerSet);
        }, asyncExecutor);
    }

    /**
     * 読み込んだスケマティックからマーカーの集まりを取得する
     *
     * @param clipboard スケマティックが読み込まれたクリップボード
     * @return マーカーの集まり
     */
    private MapMarkerSet computeMarkerSet(Clipboard clipboard) {
        ImmutableMultimap.Builder<NamespacedKey, MapMarker> mapMarksersBuilder = new ImmutableMultimap.Builder<>();

        BlockVector3 sizeMin = clipboard.getMinimumPoint();
        int sizeMinX = sizeMin.getX();
        int sizeMinY = sizeMin.getY();
        int sizeMinZ = sizeMin.getZ();

        BlockVector3 sizeMax = clipboard.getMaximumPoint();
        int sizeMaxX = sizeMax.getX();
        int sizeMaxY = sizeMax.getY();
        int sizeMaxZ = sizeMax.getZ();

        for (int x = sizeMinX; x < sizeMaxX; x++) {
            for (int y = sizeMinY; y < sizeMaxY; y++) {
                for (int z = sizeMinZ; z < sizeMaxZ; z++) {
                    BlockVector3 pos = BlockVector3.at(x, y, z);
                    BlockState block = clipboard.getBlock(pos);
                    BlockData blockData = BukkitAdapter.adapt(block);

                    if (blockData.getMaterial() == Material.JIGSAW) {
                        BaseBlock baseBlock = clipboard.getFullBlock(pos);
                        JigsawMapMarker jigsawMapMarker = JigsawMapMarker.of(pos.subtract(clipboard.getOrigin()), baseBlock, blockData);

                        if (jigsawMapMarker != null) {
                            mapMarksersBuilder.put(jigsawMapMarker.pointName(), jigsawMapMarker);
                        }
                    }
                }
            }
        }

        return new MapMarkerSet(mapMarksersBuilder.build());
    }

    private CompletableFuture<World> loadWorld(@NotNull MatchMapInstance matchMapInstance, @NotNull String worldId) {
        String worldName = WORLD_NAME_PREFIX + worldId;

        return this.worldCache.get().thenApplyAsync(worldCacheFile -> {
            /* 非同期でキャッシュワールドファイルをコピー */

            File worldFolder = new File(worldName);

            // 被り確認
            if (worldFolder.exists()) {
                throw new RuntimeException("既に同じ名前のワールドフォルダーが存在しています");
            }

            // フォルダーのコピー
            try {
                FileUtils.copyDirectory(worldCacheFile, worldFolder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return worldFolder;
        }, asyncExecutor).thenApplyAsync(worldFolder -> {
            /* Tick同期でワールドを生成 */

            // ワールドフォルダー確認
            if (!worldFolder.exists()) {
                throw new RuntimeException("ワールドフォルダーが用意できていません");
            }

            // ワールドのTick処理中確認
            if (Bukkit.isTickingWorlds()) {
                throw new RuntimeException("ワールドのTick処理中です");
            }

            // ワールド生成
            WorldCreator worldCreator = matchWorldCreator(worldName, worldId);
            World world = worldCreator.createWorld();

            if (world == null) {
                throw new RuntimeException("ワールドの生成に失敗");
            }

            matchMapInstance.setStrictWorld(world);

            SLUtils.getLogger().info(String.format("試合用マップインスタンス(%s)のワールド生成完了", worldId));

            return world;
        }, tickExecutor);
    }

    /**
     * ワールドのキャッシュ非同期生成CompletableFuture
     *
     * @return キャッシュファイルのCompletableFuture
     */
    private CompletableFuture<File> createWorldCache() {
        return CompletableFuture.supplyAsync(() -> {
            /* Tick同期でキャッシュ用ワールドのフォルダーを生成 */

            // ワールドのTick処理中確認
            if (Bukkit.isTickingWorlds()) {
                throw new RuntimeException("ワールドのTick処理中です");
            }

            // ワールドを生成
            String worldId = "cache_" + UUID.randomUUID();
            String worldName = WORLD_NAME_PREFIX + worldId;
            WorldCreator worldCreator = matchWorldCreator(worldName, worldId);
            World world = Objects.requireNonNull(worldCreator.createWorld());

            // ワールドのフォルダーを取得して、ワールドをアンロード
            File worldFolder = world.getWorldFolder();
            Bukkit.unloadWorld(world, true);

            SLUtils.getLogger().info("ワールドデータのキャッシュ生成完了");

            return worldFolder;
        }, tickExecutor).thenApplyAsync(worldFolder -> {
            /* 非同期でワールドフォルダーを一時ファイル用フォルダーへ移動 */

            FNDataUtil.wishMkdir(SLFiles.tmpFolder());
            File tmpWorldFolder = new File(SLFiles.tmpFolder(), worldFolder.getName());

            try {
                FileUtils.rename(worldFolder, tmpWorldFolder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // uid.datを削除
            File uidFile = new File(tmpWorldFolder, "uid.dat");
            if (!uidFile.delete()) {
                throw new RuntimeException("uid.datの削除に失敗");
            }

            return tmpWorldFolder;
        }, asyncExecutor);
    }

    private WorldCreator matchWorldCreator(String worldName, String worldId) {
        WorldCreator worldCreator = new WorldCreator(worldName, SLUtils.plLoc(worldId));
        worldCreator.generator(new MatchChunkGenerator());
        worldCreator.environment(World.Environment.NORMAL);
        return worldCreator;
    }
}
