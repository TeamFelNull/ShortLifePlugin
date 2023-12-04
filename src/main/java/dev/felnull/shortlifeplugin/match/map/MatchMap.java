package dev.felnull.shortlifeplugin.match.map;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sk89q.worldedit.math.BlockVector3;
import dev.felnull.shortlifeplugin.match.MatchMode;
import dev.felnull.shortlifeplugin.match.MatchModes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * 試合用マップの情報レコード
 *
 * @param id                  試合用マップID
 * @param name                名前
 * @param schematic           スケマティックのID
 * @param offset              スケマティック生成オフセット
 * @param availableMatchModes 使用可能な試合モード
 * @author MORIMORI0317, Quarri6343
 */
public record MatchMap(@NotNull String id, @NotNull String name, @NotNull String schematic,
                       @NotNull BlockVector3 offset, @NotNull @Unmodifiable List<MatchMode> availableMatchModes) {

    /**
     * 現在の試合マップJsonバージョン
     */
    private static final int VERSION = 1;

    /**
     * Jsonから試合用マップの情報を作成
     *
     * @param id         試合ID
     * @param jsonObject Jsonオブジェクト
     * @return 試合用マップ
     */
    public static MatchMap of(String id, JsonObject jsonObject) {
        int version = jsonObject.get("_version").getAsInt();

        if (version == 0) {
            return ofV0(id, jsonObject);
        }

        if (version != VERSION) {
            throw new RuntimeException("サポートしていないバージョンです: " + version);
        }

        String name = jsonObject.get("name").getAsString();
        String schematic = jsonObject.get("schematic").getAsString();

        BlockVector3 offset = loadOffset(jsonObject);

        ImmutableList<MatchMode> availableMatchModes = loadAvailableMatchMode(jsonObject);

        return new MatchMap(id, name, schematic, offset, availableMatchModes);
    }

    /**
     * オフセット読み込み
     *
     * @param jsonObject Jsonオブジェクト
     * @return オフセット
     */
    private static BlockVector3 loadOffset(JsonObject jsonObject) {
        JsonArray offsetArray = jsonObject.getAsJsonArray("offset");
        return BlockVector3.at(offsetArray.get(0).getAsInt(), offsetArray.get(1).getAsInt(), offsetArray.get(2).getAsInt());
    }

    /**
     * 利用可能な試合モードを読み込み
     *
     * @param jsonObject Jsonオブジェクト
     * @return 試合モードのリスト
     */
    private static ImmutableList<MatchMode> loadAvailableMatchMode(JsonObject jsonObject) {
        JsonArray availableMatchModesArray = jsonObject.getAsJsonArray("available_match_modes");
        ImmutableList.Builder<MatchMode> availableMatchModes = new ImmutableList.Builder<>();

        for (JsonElement modeEntry : availableMatchModesArray) {
            if (modeEntry.isJsonPrimitive() && modeEntry.getAsJsonPrimitive().isString()) {
                MatchModes.getMode(modeEntry.getAsJsonPrimitive().getAsString()).ifPresent(availableMatchModes::add);
            }
        }

        return availableMatchModes.build();
    }

    /**
     * 旧バージョンのJson読み込み
     *
     * @param id         試合ID
     * @param jsonObject Jsonオブジェクト
     * @return 試合マップ
     */
    @Deprecated
    private static MatchMap ofV0(String id, JsonObject jsonObject) {
        String name = jsonObject.get("name").getAsString();
        String schematic = jsonObject.get("schematic").getAsString();

        BlockVector3 offset = loadOffset(jsonObject);

        return new MatchMap(id, name, schematic, offset, ImmutableList.of());
    }
}
