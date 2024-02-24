package dev.felnull.shortlifeplugin;

import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * このプラグインで利用するエクスキューター定義
 *
 * @author MORIMORI0317
 */
public class SLExecutors {

    /**
     * 汎用的なCPUの全コアを利用する処理用<br/>
     * 最低でも２スレッドは存在する
     */
    public static final ExecutorService DEFAULT =
            Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2), createThreadFactory("shortlife-default-worker-%d"));

    /**
     * IO処理用
     */
    public static final ExecutorService IO = Executors.newCachedThreadPool(createThreadFactory("shortlife-io-worker-%d"));

    static {
        SLUtils.getLogger().info("エクスキューターが作成されました");
        SLUtils.getLogger().info(String.format("利用可能なプロセッサコア数: %s", Runtime.getRuntime().availableProcessors()));
    }

    private SLExecutors() {
        throw new AssertionError();
    }

    private static ThreadFactory createThreadFactory(String namingPattern) {
        return new BasicThreadFactory.Builder().namingPattern(namingPattern).daemon(true).build();
    }

    /**
     * 破棄処理
     */
    protected static void dispose() {
        SLUtils.getLogger().info("エクスキューターをシャッドダウンします");

        try {
            shutdownExecutor(DEFAULT);
        } catch (Exception ex) {
            SLUtils.reportError(ex, "デフォルトエクスキューターのシャッドダウンに失敗");
        }

        try {
            shutdownExecutor(IO);
        } catch (Exception ex) {
            SLUtils.reportError(ex, "IOエクスキューターのシャッドダウンに失敗");
        }
    }

    private static void shutdownExecutor(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(194, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(194, TimeUnit.SECONDS)) {
                    SLUtils.getLogger().warning(MsgHandler.get("system-async-executor-stop-failed"));
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
