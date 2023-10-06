package dev.felnull.shortlifeplugin.match.map;

import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
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
     * 破棄用のワールド<br/>
     * {@link #mapWorld}のワールドと同じだが、こちらは破棄時にのみ使用する
     */
    protected World disposableWorld;

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

        // マップを破棄
        if (this.disposableWorld != null) {
            File worldFolder = this.disposableWorld.getWorldFolder();
            Bukkit.unloadWorld(this.disposableWorld, false);

            try {
                // https://www.riblab.net/blog/2023/09/10/devnote_2/
                // なぜか消せる...?
                FileUtils.deleteDirectory(worldFolder);
            } catch (IOException e) {
                SLUtils.reportError(e, "試合用ワールドの削除に失敗");
            }
        }

    }

    protected void setMapWorld(CompletableFuture<MatchMapWorld> mapWorld) {
        this.mapWorld = mapWorld;
    }

    protected void setDisposableWorld(World disposableWorld) {
        this.disposableWorld = disposableWorld;
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
}
