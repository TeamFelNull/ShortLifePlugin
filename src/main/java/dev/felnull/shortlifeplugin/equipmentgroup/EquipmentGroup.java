package dev.felnull.shortlifeplugin.equipmentgroup;

import dev.felnull.shortlifeplugin.MsgHandler;
import me.deecaad.weaponmechanics.WeaponMechanicsAPI;
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
 * @author MORIMORI0317, Quarri6343
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
        componentList.add(Component.text(MsgHandler.get("equip-id")).append(Component.text(this.id)));
        componentList.add(Component.text(MsgHandler.get("equip-name")).append(Component.text(this.name)));

        addItemListDesc(componentList);

        addRestrictionDesc(componentList);
    }

    /**
     * リストにアイテムリストの追加を行う
     *
     * @param componentList 情報説明コンポーネントのリスト
     */
    private void addItemListDesc(@NotNull List<Component> componentList) {
        Component[] itemComponents = this.itemStacks.stream()
                .map(ItemStack::displayName)
                .toArray(Component[]::new);

        JoinConfiguration.Builder builder = JoinConfiguration.builder();
        builder.separator(Component.text(", "));

        Component itemsComponent = Component.join(builder, itemComponents);

        componentList.add(Component.text(MsgHandler.get("equip-item-list")).append(itemsComponent));
    }

    /**
     * リストに装備制限説明の追加を行う
     *
     * @param componentList 情報説明コンポーネントのリスト
     */
    private void addRestrictionDesc(@NotNull List<Component> componentList) {
        componentList.add(Component.text(MsgHandler.get("equip-restriction")));

        Component maxHotbarExistsCountText;

        if (this.restriction.maxHotbarExistsCount() >= 0) {
            maxHotbarExistsCountText = Component.text(this.restriction.maxHotbarExistsCount()).append(Component.text(MsgHandler.get("equip-number")));
        } else {
            maxHotbarExistsCountText = Component.text(MsgHandler.get("equip-no-restriction"));
        }
        componentList.add(Component.text(MsgHandler.get("equip-max-hotbar-number")).append(maxHotbarExistsCountText));
    }

    /**
     * 指定したアイテムスタックが所属しているかどうか
     *
     * @param stack アイテムスタック
     * @return 所属していればtrue、してなければfalse
     */
    public boolean isBelongs(@NotNull ItemStack stack) {
        return itemStacks.stream().anyMatch(itemStack -> isMatch(itemStack, stack));
    }


    /**
     * 指定したアイテムスタック2つが同じ種類かどうか
     *
     * @param stack1 アイテムスタック1
     * @param stack2 アイテムスタック2
     * @return 同じ種類かどうか
     */
    private boolean isMatch(@NotNull ItemStack stack1, @NotNull ItemStack stack2) {

        // 完全に同じかどうか比較
        if (stack1.isSimilar(stack2)) {
            return true;
        }

        // 武器タイトルで比較
        String stackWeaponTitle1 = WeaponMechanicsAPI.getWeaponTitle(stack1);

        if (stackWeaponTitle1 != null) {
            String stackWeaponTitle2 = WeaponMechanicsAPI.getWeaponTitle(stack2);
            if (stackWeaponTitle2 != null) {
                return stackWeaponTitle1.equals(stackWeaponTitle2);
            }
        }

        return false;
    }

    /**
     * 使用制限がかかるかどうか確認
     *
     * @param hotbarStacks ホットバーのアイテムスタックリスト
     * @return 制限がかかるかどうか
     */
    public boolean isRestricted(@NotNull @Unmodifiable List<ItemStack> hotbarStacks) {
        int maxHotbarCount = restriction().maxHotbarExistsCount();

        if (maxHotbarCount >= 0) {
            // ホットバーのアイテム数確認
            int hotbarCount = (int) hotbarStacks.stream()
                    .filter(this::isBelongs)
                    .limit(maxHotbarCount + 1) // 最大数以上は確認不要
                    .count();

            return hotbarCount > maxHotbarCount;
        }

        return false;
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
