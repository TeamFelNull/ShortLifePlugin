package dev.felnull.shortlifeplugin.gui;

import com.google.common.collect.ImmutableList;
import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroup;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroupManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.SlotElement;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.inventory.VirtualInventoryManager;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * 装備グループアイテム変更のGUI
 *
 * @author MORIMORI0317, Quarri6343, nin8995
 */
public class EquipmentGroupModifyItemGui {

    /**
     * アイテム変更スロットの列数
     */
    private static final int MODIFY_ITEM_COLUMN = 5;

    /**
     * ページごとのアイテム数
     */
    private static final int ITEM_PER_PAGE = MODIFY_ITEM_COLUMN * 9;

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
            player.sendMessage(Component.text(MsgHandler.get("gui-other-player-is-editing")));
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

        int pages = equipmentGroup.itemStacks().size() / ITEM_PER_PAGE + 2;
        VirtualInventory inv = createInventory(equipmentGroup, pages);

        List<Gui> guis = IntStream.range(0, pages).mapToObj(page -> {
            Gui gui = Gui.normal().setStructure(getStructure()).build();
            for (int i = 0; i < gui.getSize(); i++) {
                gui.setSlotElement(i, new SlotElement.InventorySlotElement(inv, i + page * ITEM_PER_PAGE));
            }
            return gui;
        }).toList();

        return buildWindow(player, equipmentGroup, buildGuiFromPages(guis), inv);
    }

    private static VirtualInventory createInventory(@NotNull EquipmentGroup equipmentGroup, int pages) {
        VirtualInventory inv = VirtualInventoryManager.getInstance().createNew(UUID.randomUUID(), pages * ITEM_PER_PAGE);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setMaxStackSize(i, 1);
        }

        List<ItemStack> stacks = equipmentGroup.itemStacks();

        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i).clone();
            stack.setAmount(1);
            inv.setItemSilently(i, stack);
        }

        return inv;
    }

    @NotNull
    private static String[] getStructure() {
        return IntStream.range(0, MODIFY_ITEM_COLUMN)
                .mapToObj(it -> "xxxxxxxxx").toList()
                .toArray(String[]::new);
    }

    @NotNull
    private static Gui buildGuiFromPages(List<Gui> pages) {
        ArrayList<String> structure = new ArrayList<>(Arrays.asList(getStructure()));
        structure.add("###<#>###");

        return PagedGui.guis()
                .setStructure(structure.toArray(String[]::new))
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(pages)
                .build();
    }

    @NotNull
    private static Window buildWindow(@NotNull Player player, @NotNull EquipmentGroup equipmentGroup, Gui gui, VirtualInventory inv) {
        return Window.single()
                .setViewer(player)
                .setTitle(MsgHandler.getFormatted("gui-equip-group-change-title", equipmentGroup.id()))
                .setGui(gui)
                .addCloseHandler(() -> {
                    saveItems(player, equipmentGroup, inv);
                    VirtualInventoryManager.getInstance().remove(inv);
                    openItemModifyGui = false;
                })
                .build();
    }


    private static void saveItems(@NotNull Player player, @NotNull EquipmentGroup equipmentGroup, @NotNull VirtualInventory virtualInventory) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();

        if (manager.getGroup(equipmentGroup.id()).isEmpty()) {
            player.sendRichMessage(MsgHandler.getFormatted("gui-equip-group-not-exists", equipmentGroup.id()));
            return;
        }

        List<ItemStack> stacks = getItemStacksFromInv(virtualInventory);

        manager.removeGroup(equipmentGroup.id());
        EquipmentGroup newEquipmentGroup = new EquipmentGroup(equipmentGroup.id(), equipmentGroup.name(), ImmutableList.copyOf(stacks), equipmentGroup.restriction());
        manager.addGroup(newEquipmentGroup);
        player.sendRichMessage(MsgHandler.getFormatted("gui-equip-group-item-changed", equipmentGroup.id()));
    }

    @NotNull
    private static List<ItemStack> getItemStacksFromInv(@NotNull VirtualInventory virtualInventory) {
        return Arrays.stream(virtualInventory.getItems())
                .filter(itemStack -> itemStack != null && !itemStack.isEmpty())
                .distinct()
                .toList();
    }
}
