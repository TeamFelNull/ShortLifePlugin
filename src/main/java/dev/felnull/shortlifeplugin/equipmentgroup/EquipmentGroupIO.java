package dev.felnull.shortlifeplugin.equipmentgroup;


import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

/**
 * 装備グループ読み書き
 *
 * @author MORIMORI0317, Quarri6343
 */
class EquipmentGroupIO {

    /**
     * GSON
     */
    private static final Gson GSON = new Gson();

    /**
     * 保存Jsonのバージョン
     */
    private static final int SAVE_JSON_VERSION = 0;
    
    private EquipmentGroupIO() {
        throw new AssertionError();
    }

    /**
     * データフォルダから装備グループを読み取る
     *
     * @throws IOException 読み取り失敗
     */
    public static Optional<Map<String, EquipmentGroup>> load() throws IOException {
        File savedJsonFile = SLFiles.equipmentGroupJson();

        if (isFileExists(savedJsonFile)) {
            return Optional.empty();
        }

        JsonObject savedJson = loadJson(savedJsonFile);
        
        verifyVersion(savedJson);

        return Optional.of(loadEquipmentGroups(savedJson));
    }

    /**
     * ファイルが存在するかどうか
     *
     * @param savedJsonFile ファイルのパス
     * @return true:ファイルが存在する場合, false:保存ファイルが存在しない、またはディレクトリの場合
     */
    private static boolean isFileExists(File savedJsonFile) {
        if (!savedJsonFile.exists() || savedJsonFile.isDirectory()) {
            SLUtils.getLogger().info(MsgHandler.get("equip-no-file"));
            return false;
        }
        
        return true;
    }

    /**
     * Json読み取り
     *
     * @param savedJsonFile ファイル
     * @return Jsonの中身
     * @throws IOException 読み取り失敗
     */
    private static JsonObject loadJson(File savedJsonFile) throws IOException {
        JsonObject savedJson;
        try (Reader reader = new BufferedReader(new FileReader(savedJsonFile))) {
            savedJson = GSON.fromJson(reader, JsonObject.class);
        }
        return savedJson;
    }

    /**
     * バージョン確認
     *
     * @param savedJson 保存されたJsonファイル
     */
    private static void verifyVersion(JsonObject savedJson) {
        int version = savedJson.getAsJsonPrimitive("_version").getAsInt();
        if (SAVE_JSON_VERSION != version) {
            throw new IllegalStateException(MsgHandler.get("error-unsupported-file-version"));
        }
    }

    /**
     * 全てのグループ読込
     *
     * @param savedJson Jsonの中身
     * @return 読み込まれたグループ
     */
    private static Map<String, EquipmentGroup> loadEquipmentGroups(JsonObject savedJson) {
        JsonArray groupArray = savedJson.getAsJsonArray("groups");
        Map<String, EquipmentGroup> equipmentGroups = new HashMap<>();
        
        groupArray.forEach(groupJson -> {
            JsonObject jo = groupJson.getAsJsonObject();
            EquipmentGroup equipmentGroup = loadEquipmentGroup(jo);
            equipmentGroups.put(equipmentGroup.id(), equipmentGroup);
            SLUtils.getLogger().info(MsgHandler.getFormatted("equip-file-loaded", equipmentGroup.id()));
        });
        
        return equipmentGroups;
    }

    /**
     * 単体グループ読み込み
     *
     * @param jsonObject 単体グループのJsonオブジェクト
     * @return 単体グループ
     */
    private static EquipmentGroup loadEquipmentGroup(JsonObject jsonObject) {
        String id = jsonObject.get("id").getAsString();
        String name = jsonObject.get("name").getAsString();
        List<ItemStack> itemStacks = loadItemStacks(jsonObject);
        EquipmentGroup.Restriction restriction = loadRestriction(jsonObject);

        return new EquipmentGroup(id, name, ImmutableList.copyOf(itemStacks), restriction);
    }

    /**
     * アイテムスタック読み込み
     *
     * @param jsonObject 単体グループのJsonオブジェクト
     * @return アイテムスタック
     */
    @NotNull
    private static List<ItemStack> loadItemStacks(JsonObject jsonObject) {
        List<ItemStack> itemStacks = new LinkedList<>();
        JsonArray itemStacksJson = jsonObject.getAsJsonArray("item_stacks");
        itemStacksJson.forEach(itemStackJo -> {
            String nbtStr = itemStackJo.getAsString();
            ReadWriteNBT nbt = NBT.parseNBT(nbtStr);
            itemStacks.add(NBT.itemStackFromNBT(nbt));
        });
        return itemStacks;
    }

    /**
     * 装備制限読み込み
     *
     * @param jsonObject 単体グループのJsonオブジェクト
     * @return 装備制限
     */
    @NotNull
    private static EquipmentGroup.Restriction loadRestriction(JsonObject jsonObject) {
        JsonObject restrictionJson = jsonObject.getAsJsonObject("restriction");
        int maxHotbarExistsCount = restrictionJson.get("maxHotbarExistsCount").getAsInt();
        return new EquipmentGroup.Restriction(maxHotbarExistsCount);
    }

    public static void save(Map<String, EquipmentGroup> equipmentGroups) throws IOException {
        File savedJsonFile = SLFiles.equipmentGroupJson();

        JsonObject savedJson = new JsonObject();
        
        saveVersion(savedJson);

        saveEquipmentGroups(equipmentGroups, savedJson);
        
        saveToFile(savedJsonFile, savedJson);
    }

    /**
     * バージョン書き込み
     *
     * @param savedJson 保存対象のjson
     */
    private static void saveVersion(JsonObject savedJson) {
        savedJson.addProperty("_version", SAVE_JSON_VERSION);
    }

    /**
     * 全ての装備グループ書き込み
     *
     * @param equipmentGroups 全ての装備グループ
     * @param savedJson 保存対象のjson
     */
    private static void saveEquipmentGroups(Map<String, EquipmentGroup> equipmentGroups, JsonObject savedJson) {
        JsonArray groupArray = new JsonArray();
        equipmentGroups.values().forEach(equipmentGroup -> {
            JsonObject jsonObject = new JsonObject();
            saveEquipmentGroup(equipmentGroup, jsonObject);
            groupArray.add(jsonObject);
        });
        savedJson.add("groups", groupArray);
    }

    /**
     * 単体の装備グループ書き込み
     *
     * @param equipmentGroup 装備グループ
     * @param jsonObject 保存対象のjson
     */
    private static void saveEquipmentGroup(EquipmentGroup equipmentGroup, JsonObject jsonObject) {
        jsonObject.addProperty("id", equipmentGroup.id());
        jsonObject.addProperty("name", equipmentGroup.name());
        
        saveItemStacks(equipmentGroup, jsonObject);
        saveRestriction(equipmentGroup, jsonObject);
    }

    /**
     * アイテムスタック書き込み
     *
     * @param equipmentGroup 装備グループ
     * @param jsonObject 保存対象のjson
     */
    private static void saveItemStacks(EquipmentGroup equipmentGroup, JsonObject jsonObject) {
        JsonArray itemStacksJson = new JsonArray();
        equipmentGroup.itemStacks().forEach(itemStack -> {
            ReadWriteNBT nbt = NBT.itemStackToNBT(itemStack);
            itemStacksJson.add(nbt.toString());
        });
        jsonObject.add("item_stacks", itemStacksJson);
    }

    /**
     * 装備制限書き込み
     *
     * @param equipmentGroup 装備グループ
     * @param jsonObject 保存対象のjson
     */
    private static void saveRestriction(EquipmentGroup equipmentGroup, JsonObject jsonObject) {
        JsonObject restrictionJson = new JsonObject();
        restrictionJson.addProperty("maxHotbarExistsCount", equipmentGroup.restriction().maxHotbarExistsCount());
        jsonObject.add("restriction", restrictionJson);
    }
    
    /**
     * Json書き込み
     *
     * @param savedJsonFile 保存したい場所
     * @param savedJson 保存対象のjson
     * @throws IOException 書き込み失敗
     */
    private static void saveToFile(File savedJsonFile, JsonObject savedJson) throws IOException {
        try (Writer writer = new BufferedWriter(new FileWriter(savedJsonFile))) {
            GSON.toJson(savedJson, writer);
        }
    }
}
