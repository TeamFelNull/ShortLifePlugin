package dev.felnull.shortlifeplugin.match.map;

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
     * 非同期読み込みされるマップワールド
     */
    private CompletableFuture<MatchMapWorld> mapWorld;

    /**
     * コンストラクタ
     */
    protected MatchMapInstance() {
    }

    /**
     * 破棄処理
     */
    public void dispose() {

        // マップワールドを破棄
        if (this.strictWorld != null) {
            List<Player> players = this.strictWorld.getPlayers();

            // ワールドに残るプレイヤーを強制退去
            for (Player player : players) {
                MatchUtils.teleportToLeave(player, Optional.ofNullable(this.strictWorld));
            }

            File worldFolder = this.strictWorld.getWorldFolder();
            Bukkit.unloadWorld(this.strictWorld, false);

            try {
                // https://www.riblab.net/blog/2023/09/10/devnote_2/
                // なぜか消せる...?
                FileUtils.deleteDirectory(worldFolder);
            } catch (IOException e) {
                SLUtils.reportError(e, "試合用ワールドの削除に失敗");
            }

            this.strictWorld = null;
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
}
