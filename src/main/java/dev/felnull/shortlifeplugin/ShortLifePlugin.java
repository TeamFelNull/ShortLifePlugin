package dev.felnull.shortlifeplugin;

import dev.felnull.shortlifeplugin.commands.SLCommands;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroupManager;
import dev.felnull.shortlifeplugin.gui.SLGuis;
import dev.felnull.shortlifeplugin.listener.*;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.MatchModes;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import net.kunmc.lab.ikisugilogger.IkisugiLogger;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * プラグインが有効、無効になった際の処理を行うクラス
 *
 * @author IDEA自動生成, MORIMORI0317, miyabi0333, nin8995
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

    @Override
    public void onEnable() {
        SLConfig.init(this);
        versionCheck();

        // IKISUGI LOG
        IkisugiLogger logger = new IkisugiLogger("very ikisugi\nshort life");
        logger.setColorType(IkisugiLogger.ColorType.RAINBOW);
        logger.setCenter(true);
        getLogger().info(logger.createLn());

        SLCommands.register();
        SLUtils.clearTmpFolder(true);
        SLGuis.init();
        MatchModes.init();

        // イベントリスナー
        CommonListener.init(this);
        MatchListener.init(this);
        EquipmentGroupListener.init(this);
        WeaponMechanicsListener.init(this);
        PlayerInventoryListener.init(this);

        this.matchManager = new MatchManager();
        this.matchManager.init(this);

        this.equipmentGroupManager = new EquipmentGroupManager();
        this.equipmentGroupManager.init(this);

        getLogger().info("ShortLife Pluginが開始しました");
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

        boolean unstableVersion = false;

        if (artifactVersion.getQualifier().contains("+")) {
            // 1.x.x-alpha.x+pre.x のようにビルドメタデータが存在する場合は、不安定バージョンとみなす
            unstableVersion = true;
        } else if (artifactVersion.getMinorVersion() == 0 && artifactVersion.getMajorVersion() == 0
                && artifactVersion.getIncrementalVersion() == 0 && artifactVersion.getBuildNumber() == 0) {
            // 全てのバージョン値が0であれば、不正な形のバージョンであるため、不安定バージョンとみなす
            unstableVersion = true;
        }

        if (unstableVersion) {
            getLogger().warning("本番環境では、このバージョンを起動できません");
            getLogger().warning("もし本番環境ではない場合は、コンフィグでテストモードを有効にしてください");

            IkisugiLogger logger = new IkisugiLogger("not ikisugi\nshort life");
            logger.setColorType(IkisugiLogger.ColorType.CHRISTMAS);
            logger.setCenter(true);
            getLogger().info(logger.createLn());

            throw new RuntimeException("このバージョンはテストモードでのみ起動可能です");
        }
    }

    @Override
    public void onDisable() {

        // リロード後に補完が動かなくなるため、必ずコマンドを登録解除してください。
        SLCommands.unregisterAll();

        if (this.matchManager != null) {
            this.matchManager.dispose();
            this.matchManager = null;
        }

        if (this.equipmentGroupManager != null) {
            this.equipmentGroupManager.dispose();
            this.equipmentGroupManager = null;
        }

        SLUtils.clearTmpFolder(false);
        getLogger().info("ShortLife Pluginが停止しました");
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }

    public EquipmentGroupManager getEquipmentGroupManager() {
        return equipmentGroupManager;
    }

}
