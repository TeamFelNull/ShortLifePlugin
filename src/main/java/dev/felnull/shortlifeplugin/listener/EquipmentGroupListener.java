package dev.felnull.shortlifeplugin.listener;

import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroup;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroupManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedList;
import java.util.List;

/**
 * 装備グループ用イベントリスナー
 *
 * @author MORIMORI0317
 */
public class EquipmentGroupListener implements Listener {

    /**
     * 初期化
     *
     * @param plugin プラグイン
     */
    public static void init(ShortLifePlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(new EquipmentGroupListener(), plugin);
    }


    /**
     * プレイヤーが作用させた場合のイベント
     *
     * @param e プレイヤーインタラクトイベント
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEvent(PlayerInteractEvent e) {
        // 左右のクリックのみ
        if (e.getAction().isLeftClick() || e.getAction().isRightClick()) {
            ItemStack stack = e.getItem();
            EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
            List<EquipmentGroup> equipmentGroups = manager.getBelongsGroups(stack);
            Player player = e.getPlayer();
            Inventory inventory = player.getInventory();

            List<ItemStack> hotbarStacks = new LinkedList<>();
            // 0~8のスロットがホットバー
            for (int i = 0; i < 8; i++) {
                ItemStack hotbarStack = inventory.getItem(i);
                if (hotbarStack != null && !hotbarStack.isEmpty()) {
                    hotbarStacks.add(hotbarStack);
                }
            }

            // 制限されるグループリスト
            List<EquipmentGroup> restrictedEquipmentGroups = equipmentGroups.stream()
                    .filter(equipmentGroup -> equipmentGroup.isRestricted(hotbarStacks))
                    .toList();

            // 制限されるグループが存在する場合
            if (!restrictedEquipmentGroups.isEmpty()) {
                JoinConfiguration.Builder restrictedGroupComponentBuilder = JoinConfiguration.builder();
                restrictedGroupComponentBuilder.separator(Component.text(","));

                Component[] groupComponents = restrictedEquipmentGroups.stream()
                        .map(equipmentGroup -> Component.text(String.format("%sは%d個", equipmentGroup.name(), equipmentGroup.restriction().maxHotbarExistsCount())))
                        .toArray(Component[]::new);

                Component restrictedGroupComponent = Component.join(restrictedGroupComponentBuilder, groupComponents)
                        .append(Component.text("より多くホットバーに存在できません"))
                        .color(NamedTextColor.RED);

                player.sendMessage(restrictedGroupComponent);
                e.setCancelled(true);
            }
        }
    }
}
