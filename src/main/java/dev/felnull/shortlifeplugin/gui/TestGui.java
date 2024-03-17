package dev.felnull.shortlifeplugin.gui;

import dev.felnull.shortlifeplugin.gui.item.PotatoSenpaiItem;
import dev.felnull.shortlifeplugin.resources.CustomModelItem;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * テスト用GUI
 *
 * @author MORIMORI0317
 */
public class TestGui implements SLGuis.WindowProvider {
    /**
     * テストカスタムモデル
     */
    private static final CustomModelItem TEST_MODEL = new CustomModelItem("test");

    /**
     * テストカスタムモデル2
     */
    private static final CustomModelItem TEST2_MODEL = new CustomModelItem("test2");

    @Override
    public @NotNull Window provide(@NotNull Player player) {
        Gui gui = Gui.normal()
                .setStructure(
                        "111.1.111",
                        "2.2.2.2.2",
                        "###.#.#.#",
                        "#.#.#.#.#",
                        "###.#.###")
                .addIngredient('#', PotatoSenpaiItem::new)
                .addIngredient('1', () -> new SimpleItem(new ItemBuilder(TEST_MODEL.getMemoizeItemStack())))
                .addIngredient('2', () -> new SimpleItem(new ItemBuilder(TEST2_MODEL.getMemoizeItemStack())))
                .build();

        return Window.single()
                .setViewer(player)
                .setTitle("ｾﾞｪﾊｧ…ｾﾞｪﾊｧ(ﾎｫﾝ!)…ｱｱｯ!ﾊｧｯ…ﾊｯ ｲｷｽｷﾞｨ!(ﾎｫﾝ!)ｲｸｩｲｸｲｸｩｨｸ…ｱｯﾊｯ、ﾝｱ")
                .setGui(gui)
                .build();
    }
}
