package dev.felnull.shortlifeplugin.gui;

import dev.felnull.shortlifeplugin.gui.item.PotatoSenpaiItem;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.window.Window;

/**
 * テスト用GUI
 *
 * @author MORIMORI0317
 */
public class TestGui implements SLGuis.WindowProvider {

    @Override
    public @NotNull Window provide(@NotNull Player player) {
        Gui gui = Gui.normal()
                .setStructure(
                        "###.#.###",
                        "#.#.#.#.#",
                        "###.#.#.#",
                        "#.#.#.#.#",
                        "###.#.###")
                .addIngredient('#', PotatoSenpaiItem::new)
                .build();

        return Window.single()
                .setViewer(player)
                .setTitle("ｾﾞｪﾊｧ…ｾﾞｪﾊｧ(ﾎｫﾝ!)…ｱｱｯ!ﾊｧｯ…ﾊｯ ｲｷｽｷﾞｨ!(ﾎｫﾝ!)ｲｸｩｲｸｲｸｩｨｸ…ｱｯﾊｯ、ﾝｱ")
                .setGui(gui)
                .build();
    }
}
