package dev.felnull.shortlifeplugin.equipmentgroup;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.util.*;

/**
 * 装備グループ管理<br/>
 * このクラスはスレッドセーフではないので、サーバーTickからアクセスしてください。
 *
 * @author MORIMORI0317
 */
public class EquipmentGroupManager {

    /**
     * GSON
     */
    private static final Gson GSON = new Gson();

    /**
     * 保存Jsonのバージョン
     */
    private static final int SAVE_JSON_VERSION = 0;

    /**
     * 自動保存を行う周期Tick
     */
    private static final long AUTO_SAVE_TICK = 20 * 10;

    /**
     * 読み込み済み装備グループのリスト
     */
    private final Map<String, EquipmentGroup> equipmentGroups = new HashMap<>();

    /**
     * 装備グループが更新された場合のフラグ
     */
    private boolean dirty;

    /**
     * インスタンス取得
     *
     * @return 装備グループマネージャーインスタンス
     */
    public static EquipmentGroupManager getInstance() {
        EquipmentGroupManager manager = SLUtils.getSLPlugin().getEquipmentGroupManager();

        if (manager == null) {
            throw new IllegalStateException("インスタンスが未作成");
        }

        return manager;
    }

    /**
     * 初期化処理
     *
     * @param plugin プラグイン
     */
    public void init(ShortLifePlugin plugin) {
        try {
            load();
        } catch (IOException | RuntimeException ex) {
            SLUtils.reportError(ex, "装備グループの読み込みに失敗");
            throw new RuntimeException(ex);
        }

        plugin.getLogger().info("装備グループの読み込み完了");

        // 自動保存登録
        Bukkit.getScheduler().runTaskTimer(plugin, task -> this.autoSave(), AUTO_SAVE_TICK, AUTO_SAVE_TICK);
    }

    /**
     * 破棄
     */
    public void dispose() {
        try {
            save();
        } catch (IOException | RuntimeException ex) {
            SLUtils.reportError(ex, "装備グループの保存に失敗");
            throw new RuntimeException(ex);
        }

        SLUtils.getLogger().info("装備グループの保存完了");
    }

    private void load() throws IOException {
        File savedJsonFile = SLFiles.equipmentGroupJson();

        // 保存ファイルが存在しない、またはディレクトリの場合
        if (!savedJsonFile.exists() || savedJsonFile.isDirectory()) {
            SLUtils.getLogger().info("装備グループの保存ファイルは存在しません");
            return;
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
            this.equipmentGroups.put(equipmentGroup.id(), equipmentGroup);
            SLUtils.getLogger().info(String.format("装備グループを読み込みました: %s", equipmentGroup.id()));
        });
    }

    private EquipmentGroup loadEquipmentGroup(JsonObject jsonObject) {
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

    private void save() throws IOException {
        File savedJsonFile = SLFiles.equipmentGroupJson();

        JsonObject savedJson = new JsonObject();

        // バージョン書き込み
        savedJson.addProperty("_version", SAVE_JSON_VERSION);

        JsonArray groupArray = new JsonArray();
        this.equipmentGroups.values().forEach(equipmentGroup -> {
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

    private void saveEquipmentGroup(EquipmentGroup equipmentGroup, JsonObject jsonObject) {
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

    private void autoSave() {
        if (this.dirty) {
            this.dirty = false;

            try {
                save();
            } catch (IOException | RuntimeException ex) {
                SLUtils.reportError(ex, "装備グループの自動保存に失敗");
            }

            SLUtils.getLogger().info("装備グループの自動保存完了");
        }
    }

    /**
     * 装備グループを追加
     *
     * @param equipmentGroup 装備グループ
     */
    public void addGroup(@NotNull EquipmentGroup equipmentGroup) {
        Objects.requireNonNull(equipmentGroup);

        if (this.equipmentGroups.containsKey(equipmentGroup.id())) {
            throw new RuntimeException("登録済みの装備グループIDです");
        }

        this.equipmentGroups.put(equipmentGroup.id(), equipmentGroup);
        this.dirty = true;
    }

    /**
     * 指定したIDの装備グループを取得
     *
     * @param id 装備グループID
     * @return 装備グループ(存在しない場合はnullを返す)
     */
    @Nullable
    public EquipmentGroup getGroup(@NotNull String id) {
        Objects.requireNonNull(id);
        return this.equipmentGroups.get(id);
    }

    /**
     * 指定さ入れたIDの装備グループを削除
     *
     * @param id 装備グループID
     */
    public void removeGroup(@NotNull String id) {
        Objects.requireNonNull(id);

        this.equipmentGroups.remove(id);
        this.dirty = true;
    }

    @Unmodifiable
    public Map<String, EquipmentGroup> getAllGroup() {
        return ImmutableMap.copyOf(equipmentGroups);
    }


    /**
     * 指定したアイテムスタックが所属しているグループを取得
     *
     * @param stack 対象アイテムスタック
     * @return 所属グループのリスト
     */
    public List<EquipmentGroup> getBelongsGroups(@Nullable ItemStack stack) {

        // nullまたは空の場合は所属なし
        if (stack == null || stack.isEmpty()) {
            return ImmutableList.of();
        }

        return ImmutableList.copyOf(this.equipmentGroups.values().stream()
                .filter(it -> it.isBelongs(stack))
                .toList());
    }
}
