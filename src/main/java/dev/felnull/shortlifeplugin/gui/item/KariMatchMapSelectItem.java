package dev.felnull.shortlifeplugin.gui.item;

import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.PlayerInfo;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

import java.util.List;
import java.util.Optional;

/**
 * 仮置きマップ選択アイテム
 *
 * @author MORIMORI0317
 */
public class KariMatchMapSelectItem extends AbstractItem {

    /**
     * 試合
     */
    private final Match match;

    /**
     * 試合マップ
     */
    private final MatchMap matchMap;

    /**
     * コンストラクタ
     *
     * @param match    試合
     * @param matchMap 試合マップ
     */
    public KariMatchMapSelectItem(Match match, MatchMap matchMap) {
        this.match = match;
        this.matchMap = matchMap;
    }

    @Override
    public ItemProvider getItemProvider() {
        ItemBuilder builder = new ItemBuilder(Material.MAP);
        builder.setDisplayName(new AdventureComponentWrapper(Component.text(matchMap.name())));
        return builder;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        if (this.match.isValid()) {
            List<MatchMap> selectableMaps = this.match.getMapSelector().getSelectableMaps();
            if (selectableMaps != null && selectableMaps.contains(this.matchMap)) {
                Optional<PlayerInfo> playerInfo = this.match.getPlayerInfo(player);
                playerInfo.ifPresent(info -> info.getMapSelectorInfo().setVotedMatchMap(matchMap));
                player.sendMessage(Component.text(matchMap.name() + "に投票しました"));
            }
        }
        getWindows().forEach(Window::close);
    }
}
