package dev.felnull.shortlifeplugin.gui.item;

import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.MatchMode;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import dev.felnull.shortlifeplugin.utils.MatchUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

/**
 * 試合モード選択GUIアイテム
 *
 * @author MORIMORI0317
 */
public class MatchModeSelectItem extends AbstractItem {

    /**
     * 試合を作成して参加する際のメッセージ
     */
    private static final Component CREATE_AND_JOIN_MATCH_MESSAGE = Component.text("試合を作成して参加しました").color(NamedTextColor.WHITE);

    /**
     * 試合の作成に失敗した際のメッセージ
     */
    private static final Component CREATE_MATCH_FAILURE_MESSAGE = Component.text("試合の作成に失敗しました").color(NamedTextColor.RED);

    /**
     * 利用可能なマップがない場合のメッセージ
     */
    private static final Component NO_MAP_AVAILABLE_MESSAGE = Component.text("利用可能なマップがありません").color(NamedTextColor.YELLOW);

    /**
     * 試合モード
     */
    @NotNull
    private final MatchMode matchMode;

    /**
     * 試合ID
     */
    @NotNull
    private final String matchId;

    /**
     * コンストラクタ
     *
     * @param matchMode 試合モード
     * @param matchId   試合ID
     */
    public MatchModeSelectItem(@NotNull MatchMode matchMode, @NotNull String matchId) {
        this.matchMode = matchMode;
        this.matchId = matchId;
    }

    @Override
    public ItemProvider getItemProvider() {
        ItemBuilder builder = new ItemBuilder(matchMode.iconItem());
        builder.setDisplayName(new AdventureComponentWrapper(matchMode.name()));
        return builder;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        MatchManager matchManager = MatchUtils.getMatchManager();
        MatchMap matchMap = MatchUtils.getMatchManager().getMapLoader().getRandomMap(matchMode);

        if (matchMap == null) {
            player.sendMessage(NO_MAP_AVAILABLE_MESSAGE);
        } else {
            Match match = matchManager.addMatch(matchId, matchMode, matchMap);

            boolean failure = false;

            if (match != null) {
                if (match.join(player, false)) {
                    player.sendMessage(CREATE_AND_JOIN_MATCH_MESSAGE);
                } else {
                    failure = true;
                }
            } else {
                failure = true;
            }

            if (failure) {
                player.sendMessage(CREATE_MATCH_FAILURE_MESSAGE);
            }
        }

        getWindows().forEach(Window::close);
    }
}
