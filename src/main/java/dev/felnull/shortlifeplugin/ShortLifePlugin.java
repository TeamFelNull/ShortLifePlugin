package dev.felnull.shortlifeplugin;

import dev.felnull.shortlifeplugin.commands.SLCommands;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroupManager;
import dev.felnull.shortlifeplugin.gui.SLGuis;
import dev.felnull.shortlifeplugin.listener.*;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.MatchModes;
import dev.felnull.shortlifeplugin.resources.ResourceSyncManager;
import dev.felnull.shortlifeplugin.resources.TextureReleaseWatcher;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import net.kunmc.lab.ikisugilogger.IkisugiLogger;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * プラグインが有効、無効になった際の処理を行うクラス
 *
 * @author IDEA自動生成, MORIMORI0317, miyabi0333, nin8995, Quarri6343
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

    /**
     * 装備グループマネージャー
     */
    private EquipmentGroupManager equipmentGroupManager;


    /**
     * ShortLifeTextureの監視
     */
    private TextureReleaseWatcher textureReleaseWatcher;

    /**
     * リソース同期マネージャー
     */
    private ResourceSyncManager resourceSyncManager;

    @Override
    public void onEnable() {
        MsgHandler.load(this);

        SLConfig.init(this);
        versionCheck();

        setUpLogger("system-logger-start", IkisugiLogger.ColorType.RAINBOW);

        SLCommands.register();
        SLUtils.clearTmpFolder(true);
        SLGuis.init();
        MatchModes.init();

        initEventListeners();

        this.resourceSyncManager = new ResourceSyncManager();
        this.resourceSyncManager.init();

        this.textureReleaseWatcher = new TextureReleaseWatcher();
        this.textureReleaseWatcher.init(this);

        this.matchManager = new MatchManager();
        this.matchManager.init(this);

        this.equipmentGroupManager = new EquipmentGroupManager();
        this.equipmentGroupManager.init(this);

        getLogger().info(MsgHandler.get("system-plugin-started"));
    }

    /**
     * IKISUGI LOG
     *
     * @param key       起動メッセージ
     * @param colorType 色の種類
     */
    private void setUpLogger(String key, IkisugiLogger.ColorType colorType) {
        IkisugiLogger logger = new IkisugiLogger(MsgHandler.get(key));
        logger.setColorType(colorType);
        logger.setCenter(true);
        getLogger().info(logger.createLn());
    }

    /**
     * イベントリスナー起動
     */
    private void initEventListeners() {
        CommonListener.init(this);
        MatchListener.init(this);
        EquipmentGroupListener.init(this);
        WeaponMechanicsListener.init(this);
        PlayerInventoryListener.init(this);
    }

    /**
     * バージョン確認
     */
    @SuppressWarnings("UnstableApiUsage")
    private void versionCheck() {
        // テストモードではバージョン確認をスキップ
        if (SLConfig.isTestMode()) {
            return;
        }

        String version = this.getPluginMeta().getVersion();
        DefaultArtifactVersion artifactVersion = new DefaultArtifactVersion(version);

        boolean unstableVersion = isUnstableVersion(artifactVersion);

        if (unstableVersion) {
            terminateUnstableVersionPlugin();
        }
    }

    /**
     * 不安定バージョンかどうか
     *
     * @param artifactVersion バージョン番号
     * @return 不安定バージョンかどうか
     */
    private static boolean isUnstableVersion(DefaultArtifactVersion artifactVersion) {
        if (artifactVersion.getQualifier().contains("+")) {
            // 1.x.x-alpha.x+pre.x のようにビルドメタデータが存在する場合は、不安定バージョンとみなす
            return true;
        } else if (artifactVersion.getMinorVersion() == 0 && artifactVersion.getMajorVersion() == 0
                && artifactVersion.getIncrementalVersion() == 0 && artifactVersion.getBuildNumber() == 0) {
            // 全てのバージョン値が0であれば、不正な形のバージョンであるため、不安定バージョンとみなす
            return true;
        }
        return false;
    }

    /**
     * 不安定バージョンのプラグインを強制停止する
     */
    private void terminateUnstableVersionPlugin() {
        getLogger().warning(MsgHandler.get("system-test-warn-1"));
        getLogger().warning(MsgHandler.get("system-test-warn-2"));

        setUpLogger("system-logger-test", IkisugiLogger.ColorType.CHRISTMAS);

        throw new RuntimeException(MsgHandler.get("system-unstable-version"));
    }

    @Override
    public void onDisable() {

        // リロード後に補完が動かなくなるため、必ずコマンドを登録解除してください。
        SLCommands.unregisterAll();

        if (this.textureReleaseWatcher != null) {
            this.textureReleaseWatcher.dispose();
            this.textureReleaseWatcher = null;
        }

        if (this.matchManager != null) {
            this.matchManager.dispose();
            this.matchManager = null;
        }

        if (this.equipmentGroupManager != null) {
            this.equipmentGroupManager.dispose();
            this.equipmentGroupManager = null;
        }

        SLExecutors.dispose();

        SLUtils.clearTmpFolder(false);
        getLogger().info(MsgHandler.get("system-plugin-stopped"));
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }

    public EquipmentGroupManager getEquipmentGroupManager() {
        return equipmentGroupManager;
    }

    public TextureReleaseWatcher getTextureReleaseWatcher() {
        return textureReleaseWatcher;
    }

    public ResourceSyncManager getResourceSyncManager() {
        return resourceSyncManager;
    }
}
