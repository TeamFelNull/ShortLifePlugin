package dev.felnull.shortlifeplugin.resources;

import com.google.common.collect.ImmutableMap;
import dev.felnull.shortlifeplugin.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * カスタムモデルが適用されたアイテムのインスタンス
 *
 * @author MORIMORI0317
 */
public class CustomModelItem {

    /**
     * モデルとアイテムの対応マップ
     */
    private static final Map<NamespacedKey, Material> MODEL_TO_ITEM
            = ImmutableMap.of(
            NamespacedKey.minecraft("item/slime_ball"), Material.SLIME_BALL,
            NamespacedKey.minecraft("item/clay_ball"), Material.CLAY_BALL,
            NamespacedKey.minecraft("item/flint"), Material.FLINT);

    /**
     * マッピングID<br/>
     * nullの場合は固定されたcustomModelを私用する
     */
    @Nullable
    private final String mappingId;

    /**
     * カスタムモデル
     */
    private ResourceSyncManager.CustomModel customModel;

    /**
     * キャッシュとして保持しているアイテムスタック
     */
    private ItemStack itemStack;

    /**
     * 最終更新カウント
     */
    private int lastUpdateCount = -1;

    /**
     * コンストラクタ
     *
     * @param mappingId マッピングID
     */
    public CustomModelItem(@Nullable String mappingId) {
        this.mappingId = mappingId;
    }

    /**
     * コンストラクタ<br/>
     * 固定のカスタムモデルで作成
     *
     * @param model  モデル
     * @param number カスタムモデル番号
     */
    public CustomModelItem(@NotNull NamespacedKey model, int number) {
        this.mappingId = null;
        this.customModel = new ResourceSyncManager.CustomModel(model, number);
    }

    /**
     * このカスタムモデルを適用したアイテムスタックを作成する
     *
     * @return アイテムスタック
     */
    @NotNull
    public ItemStack createItemStack() {

        if (this.mappingId != null) {
            ResourceSyncManager resourceSyncManager = ResourceSyncManager.getInstance();
            int updateCount = resourceSyncManager.getUpdateCount();

            // 更新カウントが異なる場合はモデルが更新されとする
            if (updateCount != this.lastUpdateCount) {
                this.lastUpdateCount = updateCount;
                this.customModel = resourceSyncManager.getCustomModel(this.mappingId);
            }
        }

        Objects.requireNonNull(this.customModel);

        Material modelItem = MODEL_TO_ITEM.get(this.customModel.model());

        if (modelItem == null) {
            throw new RuntimeException("モデルとアイテムの対応マップに未登録です");
        }

        ItemStack stack = new ItemStack(modelItem);
        ItemUtils.setCustomModelNumber(stack, this.customModel.number());

        return stack;
    }

    /**
     * このカスタムモデルを適用したアイテムスタックを取得する<br/>
     * 更新されるまで同じインスタンスを返す
     *
     * @return アイテムスタック
     */
    @NotNull
    public ItemStack getMemoizeItemStack() {
        boolean updateFlag = false;

        // マッピングを利用する場合、更新カウントと最終更新カウントが異なる時はメモ化アイテムスタックを作り直すフラグを立てる
        if (this.mappingId != null) {
            ResourceSyncManager resourceSyncManager = ResourceSyncManager.getInstance();
            int updateCount = resourceSyncManager.getUpdateCount();

            if (updateCount != this.lastUpdateCount) {
                updateFlag = true;
            }
        }

        if (this.itemStack == null || updateFlag) {
            this.itemStack = createItemStack();
        }

        return this.itemStack;
    }
}
