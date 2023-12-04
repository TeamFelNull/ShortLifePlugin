package dev.felnull.shortlifeplugin.gui;

import com.google.common.collect.ImmutableList;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroup;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroupManager;
import net.kyori.adventure.text.Component;
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
 * @author MORIMORI0317, Quarri6343
 */
public class EquipmentGroupModifyItemGui {

    /**
     * アイテム変更スロットの列数
     */
    private static final int MODIFY_ITEM_COLUMN = 5;

    /**
     * 誰かがアイテム編集GUIを開いてるかどうか
     */
    private static boolean openItemModifyGui = false;

    private EquipmentGroupModifyItemGui() {
    }


    /**
     * GUIを開くことを試み
     *
     * @param player         プレイヤー
     * @param equipmentGroup 装備グループ
     */
    public static void tryOpenGui(@NotNull Player player, @NotNull EquipmentGroup equipmentGroup) {
        if (openItemModifyGui) {
            player.sendMessage(Component.text("別のプレイヤーが編集中です"));
            return;
        }

        Window window = EquipmentGroupModifyItemGui.provide(player, equipmentGroup);
        window.open();
    }

    /**
     * Windowを取得
     *
     * @param player         プレイヤー
     * @param equipmentGroup 変更対象の装備グループ
     * @return Window
     */
    @NotNull
    private static Window provide(@NotNull Player player, @NotNull EquipmentGroup equipmentGroup) {

        openItemModifyGui = true;
        VirtualInventory virtualInventory = VirtualInventoryManager.getInstance().createNew(UUID.randomUUID(), MODIFY_ITEM_COLUMN * 9);

        setItemStacksToInv(equipmentGroup, virtualInventory);

        String[] structure = getStructure();

        Gui gui = Gui.normal()
                .setStructure(structure)
                .build();

        int size = gui.getSize();
        for (int i = 0; i < size; i++) {
            gui.setSlotElement(i, new SlotElement.InventorySlotElement(virtualInventory, i));
        }

        return buildWindow(player, equipmentGroup, virtualInventory, gui);
    }

    private static void setItemStacksToInv(@NotNull EquipmentGroup equipmentGroup, VirtualInventory virtualInventory) {
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
    }

    @NotNull
    private static String[] getStructure() {
        return IntStream.range(0, MODIFY_ITEM_COLUMN)
                .mapToObj(it -> "#########")
                .toArray(String[]::new);
    }

    @NotNull
    private static Window buildWindow(@NotNull Player player, @NotNull EquipmentGroup equipmentGroup, VirtualInventory virtualInventory, Gui gui) {
        return Window.single()
                .setViewer(player)
                .setTitle(String.format("%sのアイテム変更", equipmentGroup.id()))
                .setGui(gui)
                .addCloseHandler(() -> {
                    saveItems(player, equipmentGroup, virtualInventory);
                    VirtualInventoryManager.getInstance().remove(virtualInventory);
                    openItemModifyGui = false;
                })
                .build();
    }


    private static void saveItems(@NotNull Player player, @NotNull EquipmentGroup equipmentGroup, @NotNull VirtualInventory virtualInventory) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();

        if (manager.getGroup(equipmentGroup.id()).isEmpty()) {
            player.sendRichMessage(String.format("指定された装備グループ(%s)が存在しません", equipmentGroup.id()));
            return;
        }

        List<ItemStack> stacks = getItemStacksFromInv(virtualInventory);

        manager.removeGroup(equipmentGroup.id());
        EquipmentGroup newEquipmentGroup = new EquipmentGroup(equipmentGroup.id(), equipmentGroup.name(), ImmutableList.copyOf(stacks), equipmentGroup.restriction());
        manager.addGroup(newEquipmentGroup);
        player.sendRichMessage(String.format("装備グループ(%s)のアイテムを変更しました", equipmentGroup.id()));
    }

    @NotNull
    private static List<ItemStack> getItemStacksFromInv(@NotNull VirtualInventory virtualInventory) {
        return Arrays.stream(virtualInventory.getItems())
                .filter(itemStack -> itemStack != null && !itemStack.isEmpty())
                .distinct()
                .toList();
    }
}
