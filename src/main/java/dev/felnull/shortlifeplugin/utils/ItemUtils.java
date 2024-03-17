package dev.felnull.shortlifeplugin.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * アイテム関係のユーティリティクラス
 *
 * @author MORIMORI0317
 */
public final class ItemUtils {

    private ItemUtils() {
        throw new AssertionError();
    }

    /**
     * アイテムスタックにカスタムモデル番号を設定する
     *
     * @param stack  対象のアイテムスタック
     * @param number カスタムモデル番号
     */
    public static void setCustomModelNumber(@NotNull ItemStack stack, int number) {
        Objects.requireNonNull(stack);

        ItemMeta meta = stack.getItemMeta();
        meta.setCustomModelData(number);
        stack.setItemMeta(meta);
    }
}
