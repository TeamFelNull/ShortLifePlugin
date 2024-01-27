package dev.felnull.shortlifeplugin.match.map;

import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.utils.MatchUtils;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * 試合用マップインスタンス用クラス<br/>
 * 使用後は必ず {@link #dispose()}を呼んでください
 *
 * @author MORIMORI0317
 */
public class MatchMapInstance {

    /**
     * 厳密なワールド<br/>
     * {@link #mapWorld}のワールドと同じだが、こちらは構造物生成など準備が終わる前に利用可能<br/>
     * また、破棄後は利用不可(null)
     */
    @Nullable
    protected World strictWorld;

    /**
     * この試合マップインスタンスが破棄済みかどうか
     */
    private volatile boolean destroyed = false;

    /**
     * ワールドファイルの操作用ロック
     */
    private final Object worldFileLock = new Object();

    /**
     * 非同期読み込みされるマップワールド
     */
    private CompletableFuture<MatchMapWorld> mapWorld;

    /**
     * マップID
     */
    @NotNull
    private final String id;

    /**
     * 試合マップ
     */
    @NotNull
    private final MatchMap matchMap;

    /**
     * コンストラクタ
     *
     * @param id       ID
     * @param matchMap 試合マップ
     */
    protected MatchMapInstance(@NotNull String id, @NotNull MatchMap matchMap) {
        this.id = id;
        this.matchMap = matchMap;
    }

    /**
     * 破棄処理
     */
    public void dispose() {
        this.destroyed = true;
        File worldFolder = null;

        // マップワールドを破棄
        if (this.strictWorld != null) {
            List<Player> players = this.strictWorld.getPlayers();

            // ワールドに残るプレイヤーを強制退去
            players.forEach(player -> MatchUtils.teleportToLeave(player, Optional.ofNullable(this.strictWorld)));

            worldFolder = this.strictWorld.getWorldFolder();
            Bukkit.unloadWorld(this.strictWorld, false);

            this.strictWorld = null;
        }

        // ワールドが未作成の場合は、IDからワールドフォルダを取得する
        if (worldFolder == null) {
            worldFolder = new File(MatchMapHandler.WORLD_NAME_PREFIX + id);
        }


        // 重たい場合は別スレッドで削除するように変更してください。
        synchronized (this.worldFileLock) {
            try {
                // https://www.riblab.net/blog/2023/09/10/devnote_2/
                // なぜか消せる...?
                FileUtils.deleteDirectory(worldFolder);
            } catch (IOException e) {
                SLUtils.reportError(e, MsgHandler.get("system-map-deletion-failed"));
            }
        }
    }

    protected void setMapWorld(CompletableFuture<MatchMapWorld> mapWorld) {
        this.mapWorld = mapWorld;
    }

    protected void setStrictWorld(@Nullable World strictWorld) {
        this.strictWorld = strictWorld;
    }

    public boolean isLoadFailed() {
        return this.mapWorld.isCompletedExceptionally();
    }

    public boolean isReady() {
        return this.mapWorld.isDone() && !isLoadFailed();
    }

    /**
     * 試合ワールドを取得<br/>
     * まだ準備が終わってない場合、もしくは読み込みに失敗した場合は空を返す
     *
     * @return オプショナルな試合ワールド
     */
    public Optional<MatchMapWorld> getMapWorld() {
        if (!this.mapWorld.isDone() || this.mapWorld.isCompletedExceptionally()) {
            return Optional.empty();
        }

        return Optional.of(this.mapWorld.getNow(null));
    }

    /**
     * ワールドの読み込みエラーを取得
     *
     * @return オプショナルな読み込みエラー
     */
    public Optional<Throwable> getMapWordLoadError() {

        if (this.mapWorld.isDone() && this.mapWorld.isCompletedExceptionally()) {
            try {
                this.mapWorld.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                return Optional.of(e.getCause());
            } catch (CancellationException e) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    /**
     * このマップインスタンスの厳密なワールドが、指定されたワールドと一致するかどうか
     *
     * @param world ワールド
     * @return 一致するかどうか
     */
    public boolean isStrictWorldMatch(@NotNull World world) {
        return this.strictWorld != null && this.strictWorld == world;
    }

    public Optional<World> getStrictWorld() {
        return Optional.ofNullable(strictWorld);
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public Object getWorldFileLock() {
        return worldFileLock;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public MatchMap getMatchMap() {
        return matchMap;
    }
}
