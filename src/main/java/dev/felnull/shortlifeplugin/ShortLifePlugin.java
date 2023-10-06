package dev.felnull.shortlifeplugin;

import dev.felnull.fnjl.util.FNDataUtil;
import dev.felnull.shortlifeplugin.commands.SLCommands;
import dev.felnull.shortlifeplugin.gui.SLGuis;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.MatchModes;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import net.kunmc.lab.ikisugilogger.IkisugiLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * プラグインが有効、無効になった際の処理を行うクラス
 *
 * @author IDEA自動生成, MORIMORI0317
 */
public final class ShortLifePlugin extends JavaPlugin {
    /**
     * このプラグインの名前
     */
    public static final String PLUGIN_NAME = "ShortLifePlugin";

    /**
     * このプラグインのID
     */
    public static final String PLUGIN_ID = "shortlifeplugin";

    /**
     * 試合マネージャー
     */
    private MatchManager matchManager;

    @Override
    public void onEnable() {
        // IKISUGI LOG
        IkisugiLogger logger = new IkisugiLogger("very ikisugi\nshort life");
        logger.setColorType(IkisugiLogger.ColorType.RAINBOW);
        logger.setCenter(true);
        getLogger().info(logger.createLn());

        clearTmpFolder(true);
        SLGuis.init();
        MatchModes.init();
        SLCommands.init();

        this.matchManager = new MatchManager();
        this.matchManager.init(this);

        getLogger().info("ShortLife Pluginが開始しました");
    }

    @Override
    public void onDisable() {

        if (this.matchManager != null) {
            this.matchManager.dispose();
            this.matchManager = null;
        }

        clearTmpFolder(false);
        getLogger().info("ShortLife Pluginが停止しました");
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }

    /**
     * 一時フォルダを削除
     *
     * @param regenerateFolder 空のTMPフォルダを再生成するかどうか
     */
    private static void clearTmpFolder(boolean regenerateFolder) {
        File tmpFolder = SLFiles.tmpFolder();

        try {
            FileUtils.deleteDirectory(tmpFolder);
        } catch (IOException e) {
            SLUtils.reportError(e, "一時フォルダの削除に失敗");
        }

        if (regenerateFolder) {
            FNDataUtil.wishMkdir(tmpFolder);
        }
    }
}
