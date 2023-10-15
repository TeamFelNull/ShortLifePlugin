package dev.felnull.shortlifeplugin.match.map;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sk89q.worldedit.math.BlockVector3;
import dev.felnull.shortlifeplugin.match.MatchMode;
import dev.felnull.shortlifeplugin.match.MatchModes;
import net.kyori.adventure.text.Component;
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
 * @author MORIMORI0317
 */
public record MatchMap(@NotNull String id, @NotNull Component name, @NotNull String schematic,
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

        // 旧バージョンのJson読み込み
        if (version == 0) {
            return ofV0(id, jsonObject);
        }

        if (version != VERSION) {
            throw new RuntimeException("サポートしていないバージョンです: " + version);
        }

        String name = jsonObject.get("name").getAsString();
        String schematic = jsonObject.get("schematic").getAsString();

        // オフセット読み込み
        JsonArray offsetArray = jsonObject.getAsJsonArray("offset");
        BlockVector3 offset = BlockVector3.at(offsetArray.get(0).getAsInt(), offsetArray.get(1).getAsInt(), offsetArray.get(2).getAsInt());

        // 利用可能な試合モードを読み込み
        JsonArray availableMatchModesArray = jsonObject.getAsJsonArray("available_match_modes");
        ImmutableList.Builder<MatchMode> availableMatchModes = new ImmutableList.Builder<>();

        for (JsonElement modeEntry : availableMatchModesArray) {
            if (modeEntry.isJsonPrimitive() && modeEntry.getAsJsonPrimitive().isString()) {
                MatchMode matchMode = MatchModes.getMode(modeEntry.getAsJsonPrimitive().getAsString());
                if (matchMode != null) {
                    availableMatchModes.add(matchMode);
                }
            }
        }

        return new MatchMap(id, Component.text(name), schematic, offset, availableMatchModes.build());
    }


    private static MatchMap ofV0(String id, JsonObject jsonObject) {
        String name = jsonObject.get("name").getAsString();
        String schematic = jsonObject.get("schematic").getAsString();

        JsonArray offsetArray = jsonObject.getAsJsonArray("offset");
        BlockVector3 offset = BlockVector3.at(offsetArray.get(0).getAsInt(), offsetArray.get(1).getAsInt(), offsetArray.get(2).getAsInt());

        return new MatchMap(id, Component.text(name), schematic, offset, ImmutableList.of());
    }
}
