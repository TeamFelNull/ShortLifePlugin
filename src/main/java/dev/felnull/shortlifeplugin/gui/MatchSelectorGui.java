package dev.felnull.shortlifeplugin.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * 試合選択GUI
 *
 * @author MORIMORI0317
 */
public class MatchSelectorGui implements SLGuis.WindowProvider {
    @Override
    public @NotNull Window provide(@NotNull Player player) {
        // TODO ここに試合選択GUIを実装

        Gui gui = Gui.normal()
                .setStructure("#A#A#A#A#")
                .addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)))
                .addIngredient('A', new SimpleItem(new ItemBuilder(Material.REDSTONE)))
                .build();

        return Window.single()
                .setViewer(player)
                .setTitle("InvUI")
                .setGui(gui)
                .build();
    }
}
