package dev.felnull.shortlifeplugin.gui.item;

import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.gui.SLGuis;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

/**
 * このプラグイン用のページ送り/戻りGUIアイテム
 *
 * @author MORIMORI0317
 */
public class SLPageItem extends PageItem {
    /**
     * ページを戻す場合のテキスト
     */
    private static final Component BACK_PAGE_TEXT = Component.text(MsgHandler.get("item-previous-page")).color(NamedTextColor.GRAY);

    /**
     * ページを送る場合のテキスト
     */
    private static final Component NEXT_PAGE_TEXT = Component.text(MsgHandler.get("item-next-page")).color(NamedTextColor.GRAY);

    /**
     * ページ送りかどうか
     */
    private final boolean forward;

    /**
     * コンストラクタ
     *
     * @param forward ページ送りかどうか
     */
    public SLPageItem(boolean forward) {
        super(forward);
        this.forward = forward;
    }

    @Override
    public ItemProvider getItemProvider(PagedGui<?> gui) {

        // これ以上ページを移動できない場合
        if ((forward && !gui.hasNextPage()) || (!forward && !gui.hasPreviousPage())) {
            return SLGuis.getBorderItem();
        }


        ItemBuilder builder = new ItemBuilder(Material.FEATHER);
        builder.setDisplayName(new AdventureComponentWrapper(forward ? NEXT_PAGE_TEXT : BACK_PAGE_TEXT));


        return builder;
    }
}
