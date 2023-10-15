package dev.felnull.shortlifeplugin.gui.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

/**
 * ジャガイモ先輩
 *
 * @author MORIMORI0317
 */
public class PotatoSenpaiItem extends AbstractItem {

    /**
     * 焼けたかな？
     */
    private boolean bake;

    @Override
    public ItemProvider getItemProvider() {
        if (bake) {
            return new ItemBuilder(Material.BAKED_POTATO)
                    .setDisplayName(new AdventureComponentWrapper(Component.text("ベイクドポテト先輩").color(NamedTextColor.GOLD)));
        } else {
            return new ItemBuilder(Material.POTATO)
                    .setDisplayName(new AdventureComponentWrapper(Component.text("ジャガイモ先輩").color(NamedTextColor.YELLOW)));
        }
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        bake = !bake;

        notifyWindows();
    }


}
