package dev.felnull.shortlifeplugin.match.map;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;

/**
 * 試合用マップの情報レコード
 *
 * @param id        試合用マップID
 * @param name      名前
 * @param schematic スケマティックのID
 * @author MORIMORI0317
 */
public record MatchMap(String id, Component name, String schematic) {

    /**
     * 試合マップJsonのバージョン
     */
    private static final int VERSION = 0;

    /**
     * Jsonから試合用マップの情報を作成
     *
     * @param id         試合ID
     * @param jsonObject Jsonオブジェクト
     * @return 試合用マップ
     */
    public static MatchMap of(String id, JsonObject jsonObject) {
        int version = jsonObject.get("_version").getAsInt();

        if (version != VERSION) {
            throw new RuntimeException("サポートしていないバージョンです: " + version);
        }

        String name = jsonObject.get("name").getAsString();
        String schematic = jsonObject.get("schematic").getAsString();

        return new MatchMap(id, Component.text(name), schematic);
    }
}
