package dev.felnull.shortlifeplugin.gui.item;

import dev.felnull.shortlifeplugin.match.MatchMode;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

/**
 * 試合モードのアイコン用GUIアイテム
 *
 * @author MORIMORI0317
 */
public class MatchModeIconItem extends SimpleItem {

    /**
     * コンストラクタ
     *
     * @param matchMode 試合モード
     */
    public MatchModeIconItem(MatchMode matchMode) {
        super(new ItemBuilder(matchMode.iconItem())
                .setDisplayName(new AdventureComponentWrapper(matchMode.name())));
    }
}
