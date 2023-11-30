package dev.felnull.shortlifeplugin.equipmentgroup;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.*;

/**
 * 装備グループ管理<br/>
 * このクラスはスレッドセーフではないので、サーバーTickからアクセスしてください。
 *
 * @author MORIMORI0317
 */
public class EquipmentGroupManager {

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
        EquipmentGroupManager instance = SLUtils.getSLPlugin().getEquipmentGroupManager();

        if (instance == null) {
            throw new IllegalStateException("インスタンスが未作成");
        }

        return instance;
    }

    /**
     * 初期化処理
     *
     * @param plugin プラグイン
     */
    public void init(ShortLifePlugin plugin) {
        try {
            EquipmentGroupIO.load().ifPresent(equipmentGroups::putAll);
        } catch (IOException | RuntimeException ex) {
            SLUtils.reportError(ex, "装備グループの読み込みに失敗");
            throw new RuntimeException(ex);
        }

        plugin.getLogger().info("装備グループの読み込み完了");
        
        registerAutoSave(plugin);
    }

    /**
     * 自動保存登録
     *
     * @param plugin プラグイン
     */
    private void registerAutoSave(ShortLifePlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> this.autoSave(), AUTO_SAVE_TICK, AUTO_SAVE_TICK);
    }

    /**
     * 自動保存
     */
    private void autoSave() {
        if (this.dirty) {
            this.dirty = false;

            try {
                EquipmentGroupIO.save(equipmentGroups);
            } catch (IOException | RuntimeException ex) {
                SLUtils.reportError(ex, "装備グループの自動保存に失敗");
            }

            SLUtils.getLogger().info("装備グループの自動保存完了");
        }
    }

    /**
     * 破棄
     */
    public void dispose() {
        if (!this.dirty) {
            return;
        }

        try {
            EquipmentGroupIO.save(equipmentGroups);
        } catch (IOException | RuntimeException ex) {
            SLUtils.reportError(ex, "装備グループの保存に失敗");
            throw new RuntimeException(ex);
        }
        SLUtils.getLogger().info("装備グループの保存完了");
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
    public Optional<EquipmentGroup> getGroup(@NotNull String id) {
        Objects.requireNonNull(id);
        return Optional.ofNullable(this.equipmentGroups.get(id));
    }

    /**
     * 指定されたIDの装備グループを削除
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
