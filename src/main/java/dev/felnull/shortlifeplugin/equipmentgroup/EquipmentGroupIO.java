package dev.felnull.shortlifeplugin.equipmentgroup;


import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.*;

/**
 * 装備グループ読み書き<br/>
 *
 * @author Quarri6343
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
     * ディスクから装備グループを読み取る
     *
     * @throws IOException 読み取り失敗
     */
    public static Optional<Map<String, EquipmentGroup>> load() throws IOException {
        Map<String, EquipmentGroup> equipmentGroups = new HashMap<>();
        
        File savedJsonFile = SLFiles.equipmentGroupJson();

        // 保存ファイルが存在しない、またはディレクトリの場合
        if (!savedJsonFile.exists() || savedJsonFile.isDirectory()) {
            SLUtils.getLogger().info("装備グループの保存ファイルは存在しません");
            return Optional.empty();
        }

        // Json読み取り
        JsonObject savedJson;
        try (Reader reader = new BufferedReader(new FileReader(savedJsonFile))) {
            savedJson = GSON.fromJson(reader, JsonObject.class);
        }

        // バージョン確認
        int version = savedJson.getAsJsonPrimitive("_version").getAsInt();
        if (SAVE_JSON_VERSION != version) {
            throw new RuntimeException("サポートしていない保存ファイルバージョン");
        }

        // グループ読み取り
        JsonArray groupArray = savedJson.getAsJsonArray("groups");
        groupArray.forEach(groupJson -> {
            JsonObject jo = groupJson.getAsJsonObject();
            EquipmentGroup equipmentGroup = loadEquipmentGroup(jo);
            equipmentGroups.put(equipmentGroup.id(), equipmentGroup);
            SLUtils.getLogger().info(String.format("装備グループを読み込みました: %s", equipmentGroup.id()));
        });
        
        return Optional.of(equipmentGroups);
    }

    private static EquipmentGroup loadEquipmentGroup(JsonObject jsonObject) {
        String id = jsonObject.get("id").getAsString();
        String name = jsonObject.get("name").getAsString();

        // アイテムスタック読み込み
        List<ItemStack> itemStacks = new LinkedList<>();
        JsonArray itemStacksJson = jsonObject.getAsJsonArray("item_stacks");
        itemStacksJson.forEach(itemStackJo -> {
            String nbtStr = itemStackJo.getAsString();
            ReadWriteNBT nbt = NBT.parseNBT(nbtStr);
            itemStacks.add(NBT.itemStackFromNBT(nbt));
        });

        // 装備制限読み込み
        JsonObject restrictionJson = jsonObject.getAsJsonObject("restriction");
        int maxHotbarExistsCount = restrictionJson.get("maxHotbarExistsCount").getAsInt();
        EquipmentGroup.Restriction restriction = new EquipmentGroup.Restriction(maxHotbarExistsCount);

        return new EquipmentGroup(id, name, ImmutableList.copyOf(itemStacks), restriction);
    }

    public static void save(Map<String, EquipmentGroup> equipmentGroups) throws IOException {
        File savedJsonFile = SLFiles.equipmentGroupJson();

        JsonObject savedJson = new JsonObject();

        // バージョン書き込み
        savedJson.addProperty("_version", SAVE_JSON_VERSION);

        JsonArray groupArray = new JsonArray();
        equipmentGroups.values().forEach(equipmentGroup -> {
            JsonObject jsonObject = new JsonObject();
            saveEquipmentGroup(equipmentGroup, jsonObject);
            groupArray.add(jsonObject);
        });
        savedJson.add("groups", groupArray);

        // Json書き込み
        try (Writer writer = new BufferedWriter(new FileWriter(savedJsonFile))) {
            GSON.toJson(savedJson, writer);
        }
    }

    private static void saveEquipmentGroup(EquipmentGroup equipmentGroup, JsonObject jsonObject) {
        jsonObject.addProperty("id", equipmentGroup.id());
        jsonObject.addProperty("name", equipmentGroup.name());

        // アイテムスタック書き込み
        JsonArray itemStacksJson = new JsonArray();
        equipmentGroup.itemStacks().forEach(itemStack -> {
            ReadWriteNBT nbt = NBT.itemStackToNBT(itemStack);
            itemStacksJson.add(nbt.toString());
        });
        jsonObject.add("item_stacks", itemStacksJson);

        // 装備制限書き込み
        JsonObject restrictionJson = new JsonObject();
        restrictionJson.addProperty("maxHotbarExistsCount", equipmentGroup.restriction().maxHotbarExistsCount());
        jsonObject.add("restriction", restrictionJson);
    }
}
