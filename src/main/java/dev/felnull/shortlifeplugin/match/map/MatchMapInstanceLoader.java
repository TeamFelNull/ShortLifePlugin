package dev.felnull.shortlifeplugin.match.map;

import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Identity;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.felnull.fnjl.concurrent.IkisugiExecutors;
import dev.felnull.fnjl.concurrent.InvokeExecutor;
import dev.felnull.fnjl.util.FNDataUtil;
import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.match.MatchMode;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static dev.felnull.shortlifeplugin.match.map.MatchMapHandler.WORLD_NAME_PREFIX;

/**
 * 試合用マップインスタンスをロードする
 *
 * @author MORIMORI0317, Quarri6343
 */
public class MatchMapInstanceLoader {

    /**
     * ワールドガードのグローバルリージョンID
     */
    private static final String GLOBAL_REGION_ID = "__global__";

    /**
     * 地形生成を分割する際のサイズ
     */
    private static final int SCHEM_GENERATE_SPLIT_SIZE = 16;

    /**
     * ワールドのキャッシュ生成時に送信する注意喚起メッセージ
     */
    private static final Component WORLD_CACHE_ATTENTION_MESSAGE = MsgHandler.getComponent("match-map-cache-attention").color(NamedTextColor.GOLD);

    /**
     * Tickに同期して処理を行うExecutor
     */
    private final Executor tickExecutor = Bukkit.getScheduler().getMainThreadExecutor(SLUtils.getSLPlugin());

    /**
     * 構造物生成用のTickに同期して処理を行うExecutor<br/>
     * 1Tickに1回のみ処理が行われる
     */
    private final InvokeExecutor generateTickExecutor = IkisugiExecutors.newInvokeExecutor();

    /**
     * 試合用ワールド情報のキャッシュ
     */
    private final Supplier<CompletableFuture<File>> worldCache = Suppliers.memoize(this::createWorldCache);

    /**
     * マーカーのキャッシュ
     */
    private final Cache<HashCode, MapMarkerSet> mapMarkerCache = CacheBuilder.newBuilder()
            .maximumSize(30)
            .build();

    /**
     * 非同期で処理を行うExecutor
     */
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 1),
            new BasicThreadFactory.Builder().namingPattern("map-loader-worker-%d").daemon(true).build());

    /**
     * 非同期処理用Executorを停止
     */
    public void stopAsyncExecutor() {
        this.asyncExecutor.shutdown();
        try {
            if (!this.asyncExecutor.awaitTermination(194, TimeUnit.SECONDS)) {
                this.asyncExecutor.shutdownNow();
                if (!this.asyncExecutor.awaitTermination(194, TimeUnit.SECONDS)) {
                    SLUtils.getLogger().warning(MsgHandler.get("system-async-executor-stop-failed"));
                }
            }
        } catch (InterruptedException e) {
            this.asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Tick処理
     */
    protected void tick() {
        this.generateTickExecutor.runTasks(1);
    }

    /**
     * 試合用マップをロードする
     *
     * @param matchMode        試合モード
     * @param matchMapInstance マップインスタンス(ワールドは空)
     * @return 完成した試合用マップ
     */
    public CompletableFuture<MatchMapWorld> load(@NotNull MatchMode matchMode, @NotNull MatchMapInstance matchMapInstance) {
        CompletableFuture<Pair<Clipboard, MapMarkerSet>> schemCompletableFuture = loadSchematic(matchMapInstance);
        CompletableFuture<World> worldCompletableFuture = loadWorld(matchMapInstance);

        CompletableFuture<Void> schemGenerateCompletableFuture = worldCompletableFuture
                .thenCombineAsync(schemCompletableFuture,
                        (world, clipboardMapMarkerSetPair) -> {
                            /* Tick同期でワールドと生成物のクリップボードを合体 */
                            assertNoDestroyedInstance(matchMapInstance);
                            return Pair.of(world, clipboardMapMarkerSetPair.getLeft());
                        }, this.tickExecutor)
                .thenComposeAsync(worldClipboardMapMarkerSetTriple -> {
                    /* Tick同期で構造物生成コンプリータブルフューチャーを作成 */
                    assertNoDestroyedInstance(matchMapInstance);
                    return generateSchematicStructure(matchMapInstance, worldClipboardMapMarkerSetTriple.getRight(), worldClipboardMapMarkerSetTriple.getLeft());
                }, this.tickExecutor);

        return schemCompletableFuture.thenCombineAsync(worldCompletableFuture, (clipboardMapMarkerSetPair, world) -> {
                    /* Tick同期でワールドとマーカの集まりを合体 */
                    assertNoDestroyedInstance(matchMapInstance);
                    return Pair.of(world, clipboardMapMarkerSetPair.getRight());
                }, this.tickExecutor)
                .thenCombineAsync(schemGenerateCompletableFuture, (worldMapMarkerSetPair, unused) -> {
                    /* Tick同期で試合マップワールドを作成 */
                    assertNoDestroyedInstance(matchMapInstance);
                    return new MatchMapWorld(matchMapInstance.getMatchMap(), worldMapMarkerSetPair.getKey(), worldMapMarkerSetPair.getRight());
                }, this.tickExecutor)
                .thenApplyAsync(matchMapWorld -> {
                    /* Tick同期でマップ検証 */
                    assertNoDestroyedInstance(matchMapInstance);
                    matchMode.mapValidator().validate(matchMapWorld);
                    return matchMapWorld;
                }, this.tickExecutor);
    }

    /**
     * 構造物の生成をTick同期で行うコンプリータブルフューチャーを作成する
     *
     * @param matchMapInstance 試合マップインスタンス
     * @param clipboard        スケマティックのクリップボード
     * @param world            ワールド
     * @return コンプリータブルフューチャー
     */
    private CompletableFuture<Void> generateSchematicStructure(MatchMapInstance matchMapInstance, Clipboard clipboard, World world) {

        MatchMap matchMap = matchMapInstance.getMatchMap();

        // サイズを取得
        BlockVector3 sizeMin = clipboard.getMinimumPoint();
        BlockVector3 sizeMax = clipboard.getMaximumPoint();

        int minX = sizeMin.getX();
        int minZ = sizeMin.getZ();
        int maxX = sizeMax.getX();
        int maxZ = sizeMax.getZ();

        int sizeX = sizeMax.getX() - minX;
        int sizeZ = sizeMax.getZ() - minZ;

        // 分割数を求める
        int splitX = (int) Math.ceil((double) sizeX / SCHEM_GENERATE_SPLIT_SIZE);
        int splitZ = (int) Math.ceil((double) sizeZ / SCHEM_GENERATE_SPLIT_SIZE);

        // 分割された塊ごとにCompletableFutureを作成してつなげる
        // allOfを使用すると途中でキャンセルされた際に無駄なタスクが走るため、thenRunAsyncでつなげる
        CompletableFuture<Void> cf = null;
        for (int schemX = 0; schemX < splitX; schemX++) {
            for (int schemZ = 0; schemZ < splitZ; schemZ++) {

                int schemOffsetX = minX + schemX * SCHEM_GENERATE_SPLIT_SIZE;
                int schemOffsetZ = minZ + schemZ * SCHEM_GENERATE_SPLIT_SIZE;
                int schemSizeX = Math.min(maxX, schemOffsetX + SCHEM_GENERATE_SPLIT_SIZE);
                int schemSizeZ = Math.min(maxZ, schemOffsetZ + SCHEM_GENERATE_SPLIT_SIZE);

                Region region = new CuboidRegion(BlockVector3.at(schemOffsetX, sizeMin.getY(), schemOffsetZ), BlockVector3.at(schemSizeX, sizeMax.getY(), schemSizeZ));

                if (cf == null) {
                    cf = CompletableFuture.runAsync(() -> generateSplitSchematicOnTick(region, matchMap.offset(), matchMapInstance, clipboard, world),
                            this.generateTickExecutor);
                } else {
                    cf = cf.thenRunAsync(() -> generateSplitSchematicOnTick(region, matchMap.offset(), matchMapInstance, clipboard, world), this.generateTickExecutor);
                }
            }
        }

        if (cf == null) {
            cf = CompletableFuture.completedFuture(null);
        }

        return cf;
    }

    private void generateSplitSchematicOnTick(Region region, BlockVector3 offset, MatchMapInstance matchMapInstance, Clipboard clipboard, World world) {
        assertNoDestroyedInstance(matchMapInstance);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            Transform transform = new Identity();
            BlockTransformExtent extent = new BlockTransformExtent(clipboard, transform);
            ForwardExtentCopy copy = new ForwardExtentCopy(extent, region, clipboard.getOrigin(), editSession, offset);
            copy.setTransform(transform);
            copy.setSourceMask(Masks.alwaysTrue());
            copy.setCopyingEntities(true);
            copy.setCopyingBiomes(clipboard.hasBiomes());
            Operations.complete(copy);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<Pair<Clipboard, MapMarkerSet>> loadSchematic(@NotNull MatchMapInstance matchMapInstance) {
        String worldId = matchMapInstance.getId();

        return CompletableFuture.supplyAsync(() -> {
            /* 非同期でスケマティックファイルを読み込む */

            assertNoDestroyedInstance(matchMapInstance);

            // スケマティックファイルをクリップボードへ読み込む
            File schemFile = new File(SLFiles.schematicFolder(), matchMapInstance.getMatchMap().schematic() + ".schem");

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

            assertNoDestroyedInstance(matchMapInstance);

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
                        JigsawMapMarker.of(pos.subtract(clipboard.getOrigin()), baseBlock, blockData)
                                .ifPresent(jigsawMapMarker -> mapMarksersBuilder.put(jigsawMapMarker.pointName(), jigsawMapMarker));
                    }
                }
            }
        }

        return new MapMarkerSet(mapMarksersBuilder.build());
    }

    private CompletableFuture<World> loadWorld(@NotNull MatchMapInstance matchMapInstance) {
        String worldId = matchMapInstance.getId();
        String worldName = WORLD_NAME_PREFIX + worldId;

        return this.worldCache.get().thenApplyAsync(worldCacheFile -> {
            /* 非同期でキャッシュワールドファイルをコピー */

            assertNoDestroyedInstance(matchMapInstance);

            File worldFolder = new File(worldName);

            synchronized (matchMapInstance.getWorldFileLock()) {
                assertNoDestroyedInstance(matchMapInstance);

                // 被り確認
                if (worldFolder.exists()) {
                    throw new RuntimeException("既に同じ名前のワールドフォルダーが存在しています");
                }

                // フォルダーのコピー
                try {
                    FileUtils.copyDirectoryStructure(worldCacheFile, worldFolder);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return worldFolder;
        }, asyncExecutor).thenApplyAsync(worldFolder -> {
            /* Tick同期でワールドを生成 */

            assertNoDestroyedInstance(matchMapInstance);

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

            worldSetting(world);

            SLUtils.getLogger().info(String.format("試合用マップインスタンス(%s)のワールド生成完了", worldId));
            return world;
        }, tickExecutor);
    }

    /**
     * ワールド設定適用
     *
     * @param world ワールド
     */
    private void worldSetting(World world) {
        world.setAutoSave(false);
        world.setDifficulty(Difficulty.NORMAL);

        // ゲームルール変更
        // https://minecraft.fandom.com/ja/wiki/%E3%82%B2%E3%83%BC%E3%83%A0%E3%83%AB%E3%83%BC%E3%83%AB
        world.setGameRule(GameRule.DISABLE_RAIDS, true);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_INSOMNIA, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        world.setGameRule(GameRule.DO_WARDEN_SPAWNING, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);

        protectWorld(world);
    }

    /**
     * Tick同期でマップ保護
     *
     * @param world ワールド
     * @see <a href="https://worldguard.enginehub.org/en/latest/developer/regions/managers/">参考</a>
     * @see <a href="https://worldguard.enginehub.org/en/latest/developer/regions/protected-region/">参考</a>
     * @see <a href="https://worldguard.enginehub.org/en/latest/regions/global-region/">参考</a>
     */
    private void protectWorld(@NotNull World world) {
        RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));

        if (regionManager == null) {
            throw new RuntimeException(MsgHandler.get("system-worldguard-instance-failed"));
        }

        ProtectedRegion region = getRegionToProtect(regionManager);

        setWorldGuardRegionFlag(region);
    }

    /**
     * 保護対象のリージョンを取得
     *
     * @param regionManager リージョンマネージャ
     * @return 保護対象のリージョン
     */
    @NotNull
    private static ProtectedRegion getRegionToProtect(RegionManager regionManager) {
        ProtectedRegion region = regionManager.getRegion(GLOBAL_REGION_ID);
        if (region == null) {
            // __global__を使用しているが、APIから作成することを想定してなさそうなので、不具合が出る可能性が微レ存
            region = new GlobalProtectedRegion(GLOBAL_REGION_ID, true);
            regionManager.addRegion(region);
        }
        return region;
    }

    /**
     * ワールドガードのリージョンフラグ指定
     *
     * @param region リージョン
     */
    private void setWorldGuardRegionFlag(ProtectedRegion region) {
        region.setFlag(Flags.PVP, StateFlag.State.ALLOW);
        region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
        region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
        region.setFlag(Flags.INTERACT, StateFlag.State.DENY);
        region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
        region.setFlag(Flags.SCULK_GROWTH, StateFlag.State.DENY);
        region.setFlag(Flags.BUILD, StateFlag.State.DENY);
        region.setFlag(Flags.ITEM_DROP, StateFlag.State.DENY);
        region.setFlag(Flags.ITEM_PICKUP, StateFlag.State.DENY);
    }

    /**
     * ワールドのキャッシュ非同期生成CompletableFuture
     *
     * @return キャッシュファイルのCompletableFuture
     */
    private CompletableFuture<File> createWorldCache() {
        return CompletableFuture.supplyAsync(() -> {
            /* Tick同期でキャッシュ用ワールドのフォルダーを生成 */

            // 注意喚起
            Bukkit.broadcast(WORLD_CACHE_ATTENTION_MESSAGE);

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

            // uid.datを削除
            File uidFile = new File(worldFolder, "uid.dat");
            if (!uidFile.delete()) {
                throw new RuntimeException("uid.datの削除に失敗");
            }

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

            return tmpWorldFolder;
        }, asyncExecutor);
    }

    private WorldCreator matchWorldCreator(String worldName, String worldId) {
        WorldCreator worldCreator = new WorldCreator(worldName, SLUtils.plLoc(worldId));
        worldCreator.generator(new MatchChunkGenerator());
        worldCreator.environment(World.Environment.NORMAL);
        return worldCreator;
    }

    /**
     * 試合マップインスタンスが破棄されていないか確認<br/>
     * 破棄済みの場合は、例外を投げる
     *
     * @param matchMapInstance 試合マップインスタンス
     */
    private void assertNoDestroyedInstance(MatchMapInstance matchMapInstance) {
        if (matchMapInstance.isDestroyed()) {
            throw new RuntimeException("試合マップインスタンスが破棄されています");
        }
    }
}
