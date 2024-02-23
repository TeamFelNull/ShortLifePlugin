package dev.felnull.shortlifeplugin.utils;

import java.io.File;

/**
 * このプラグインで利用するファイル
 *
 * @author MORIMORI0317
 */
public final class SLFiles {

    private SLFiles() {
        throw new AssertionError();
    }

    /**
     * マップフォルダ
     *
     * @return マップフォルダのFile
     */
    public static File mapFolder() {
        return new File(SLUtils.getSLPlugin().getDataFolder(), "maps");
    }

    /**
     * スケマティックフォルダ
     *
     * @return スケマティックフォルダのFile
     */
    public static File schematicFolder() {
        return new File(SLUtils.getSLPlugin().getDataFolder(), "schematics");
    }

    /**
     * ホモ特有のTMPフォルダ
     *
     * @return 一時フォルダのFile
     */
    public static File tmpFolder() {
        return new File(SLUtils.getSLPlugin().getDataFolder(), "tmp");
    }

    /**
     * 装備グループ保存Json
     *
     * @return 装備グループ保存JsonのFile
     */
    public static File equipmentGroupJson() {
        return new File(SLUtils.getSLPlugin().getDataFolder(), "equipmentgroup.json");
    }

    /**
     * 報酬コマンドコンフィグ保存Json
     *
     * @return 報酬コマンドコンフィグ保存Jsonのファイル
     * @author raindazo
     */
    public static File rewardCommandConfigJson() {
        return new File(SLUtils.getSLPlugin().getDataFolder(), "rewardCommandConfig.json");
    }

    /**
     * キャッシュ保存先フォルダ
     *
     * @return キャッシュ保存先ファイル
     */
    public static File cacheFolder() {
        return new File(SLUtils.getSLPlugin().getDataFolder(), "cache");
    }

    /**
     * テクスチャリリースキャッシュ保存先フォルダ
     *
     * @return テクスチャリリースキャッシュ保存先ファイル
     */
    public static File textureReleaseCacheFolder() {
        return new File(cacheFolder(), "textureRelease");
    }
}
