package dev.felnull.shortlifeplugin.gui;

import com.google.common.collect.ImmutableList;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroup;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroupManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.SlotElement;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.inventory.VirtualInventoryManager;
import xyz.xenondevs.invui.window.Window;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * 装備グループアイテム変更のGUI
 *
 * @author MORIMORI0317
 */
public class EquipmentGroupModifyItemGui {

    /**
     * アイテム変更スロットの列数
     */
    private static final int MODIFY_ITEM_COLUMN = 5;

    private EquipmentGroupModifyItemGui() {
    }

    /**
     * Windowを取得
     *
     * @param player         プレイヤー
     * @param equipmentGroup 変更対象の装備グループ
     * @return Window
     */
    @NotNull
    public static Window provide(@NotNull Player player, @NotNull EquipmentGroup equipmentGroup) {
        VirtualInventory virtualInventory = VirtualInventoryManager.getInstance().createNew(UUID.randomUUID(), MODIFY_ITEM_COLUMN * 9);

        for (int i = 0; i < virtualInventory.getSize(); i++) {
            virtualInventory.setMaxStackSize(i, 1);
        }

        List<ItemStack> stacks = equipmentGroup.itemStacks().stream()
                .limit(MODIFY_ITEM_COLUMN * 9)
                .toList();

        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i).clone();
            stack.setAmount(1);
            virtualInventory.setItemSilently(i, stack);
        }

        String[] structure = IntStream.range(0, MODIFY_ITEM_COLUMN)
                .mapToObj(it -> "#########")
                .toArray(String[]::new);

        Gui gui = Gui.normal()
                .setStructure(structure)
                .build();

        int size = gui.getSize();
        for (int i = 0; i < size; i++) {
            gui.setSlotElement(i, new SlotElement.InventorySlotElement(virtualInventory, i));
        }

        return Window.single()
                .setViewer(player)
                .setTitle(String.format("%sのアイテム変更", equipmentGroup.id()))
                .setGui(gui)
                .addCloseHandler(() -> {
                    saveItems(player, equipmentGroup, virtualInventory);
                    VirtualInventoryManager.getInstance().remove(virtualInventory);
                })
                .build();
    }


    private static void saveItems(@NotNull Player player, @NotNull EquipmentGroup equipmentGroup, @NotNull VirtualInventory virtualInventory) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();

        if (manager.getGroup(equipmentGroup.id()) == null) {
            player.sendRichMessage(String.format("指定された装備グループ(%s)が存在しません", equipmentGroup.id()));
            return;
        }

        List<ItemStack> stacks = Arrays.stream(virtualInventory.getItems())
                .filter(itemStack -> itemStack != null && !itemStack.isEmpty())
                .distinct()
                .toList();

        manager.removeGroup(equipmentGroup.id());
        EquipmentGroup newEquipmentGroup = new EquipmentGroup(equipmentGroup.id(), equipmentGroup.name(), ImmutableList.copyOf(stacks), equipmentGroup.restriction());
        manager.addGroup(newEquipmentGroup);
        player.sendRichMessage(String.format("装備グループ(%s)のアイテムを変更しました", equipmentGroup.id()));
    }
}
