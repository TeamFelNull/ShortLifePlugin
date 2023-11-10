package dev.felnull.shortlifeplugin.utils;

import java.io.File;

/**
 * このプラグインで利用するファイル
 *
 * @author MORIMORI0317
 */
public final class SLFiles {

    private SLFiles() {
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
}
