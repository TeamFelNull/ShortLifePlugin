package dev.felnull.shortlifeplugin.match.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sk89q.worldedit.math.BlockVector3;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 試合用マップの情報レコード
 *
 * @param id        試合用マップID
 * @param name      名前
 * @param schematic スケマティックのID
 * @param offset    スケマティック生成オフセット
 * @author MORIMORI0317
 */
public record MatchMap(@NotNull String id, @NotNull Component name, @NotNull String schematic,
                       @NotNull BlockVector3 offset) {

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

        JsonArray offsetArray = jsonObject.getAsJsonArray("offset");
        BlockVector3 offset = BlockVector3.at(offsetArray.get(0).getAsInt(), offsetArray.get(1).getAsInt(), offsetArray.get(2).getAsInt());

        return new MatchMap(id, Component.text(name), schematic, offset);
    }

}
