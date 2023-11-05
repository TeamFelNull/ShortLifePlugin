package dev.felnull.shortlifeplugin.equipmentgroup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * 装備グループ
 *
 * @param id          ID
 * @param name        名前
 * @param itemStacks  アイテムスタックのリスト
 * @param restriction 制限
 * @author MORIMORI0317
 */
public record EquipmentGroup(@NotNull String id, @NotNull String name,
                             @NotNull @Unmodifiable List<ItemStack> itemStacks,
                             @NotNull Restriction restriction) {

    /**
     * リストに情報説明の追加を行う
     *
     * @param componentList 情報説明コンポーネントのリスト
     */
    public void appendInfoDesc(@NotNull List<Component> componentList) {
        componentList.add(Component.text("ID: ").append(Component.text(this.id)));
        componentList.add(Component.text("名前: ").append(Component.text(this.name)));

        Component[] itemComponents = this.itemStacks.stream()
                .map(ItemStack::displayName)
                .toArray(Component[]::new);

        JoinConfiguration.Builder builder = JoinConfiguration.builder();
        builder.separator(Component.text(", "));

        Component itemsComponent = Component.join(builder, itemComponents);

        componentList.add(Component.text("アイテム一覧: ").append(itemsComponent));

        componentList.add(Component.text(" - 装備制限 - "));

        Component maxHotbarExistsCountText;

        if (this.restriction.maxHotbarExistsCount() >= 0) {
            maxHotbarExistsCountText = Component.text(this.restriction.maxHotbarExistsCount()).append(Component.text("個"));
        } else {
            maxHotbarExistsCountText = Component.text("制限なし");
        }
        componentList.add(Component.text("ホットバーに存在できる最大数: ").append(maxHotbarExistsCountText));
    }

    /**
     * 装備制限
     *
     * @param maxHotbarExistsCount ホットバーに存在できる最大数
     * @author MORIMORI0317
     */
    public record Restriction(int maxHotbarExistsCount) {
    }
}
