package dev.felnull.shortlifeplugin.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * WeaponMechanics関係のユーティリティクライアント
 *
 * @author MORIMORI0317
 */
public class WeaponMechanicsUtils {

    /**
     * 武器名のデータID
     */
    private static final NamespacedKey WEAPON_TITLE_KEY = new NamespacedKey("weaponmechanics", "weapon-title");

    private WeaponMechanicsUtils() {
    }

    /**
     * WMの武器名を取得
     *
     * @param stack アイテムスタック
     * @return 武器名
     */
    @Nullable
    public static String getWeaponTitle(@NotNull ItemStack stack) {
        Objects.requireNonNull(stack);

        if (!stack.isEmpty() && stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

            // NBT内のweaponmechanics:weapon-titleを取得
            if (dataContainer.has(WEAPON_TITLE_KEY, PersistentDataType.STRING)) {
                return dataContainer.get(WEAPON_TITLE_KEY, PersistentDataType.STRING);
            }
        }

        return null;
    }
}
