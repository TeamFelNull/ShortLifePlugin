package dev.felnull.shortlifeplugin.gui.item;

import dev.felnull.shortlifeplugin.gui.MatchSelectorGui;
import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.MatchType;
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
import xyz.xenondevs.invui.window.Window;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * 試合選択GUIアイテム
 *
 * @author MORIMORI0317
 */
public class MatchRoomSelectItem extends AbstractItem {

    /**
     * 別の試合に参加している場合のメッセージ
     */
    private static final Component ALREADY_JOIN_OTHER_MATCH_MESSAGE = Component.text("別の試合に参加しています");

    /**
     * 既に参加している場合のメッセージ
     */
    private static final Component ALREADY_JOIN_MATCH_MESSAGE = Component.text("既に参加しています");

    /**
     * 試合に参加した際のメッセージ
     */
    private static final Function<String, Component> JOIN_MATCH_MESSAGE = roomName -> Component.text(String.format("%sに参加しました", roomName)).color(NamedTextColor.WHITE);

    /**
     * 試合に参加出来なかった場合のメッセージ
     */
    private static final Component JOIN_MATCH_FAILURE_MESSAGE = Component.text("試合に参加できませんでした").color(NamedTextColor.RED);

    /**
     * 試合の種類
     */
    @NotNull
    private final MatchType matchType;

    /**
     * ルーム番号
     */
    private final int roomNumber;

    /**
     * コンストラクタ
     *
     * @param matchType  試合の種類
     * @param roomNumber ルーム番号
     */
    public MatchRoomSelectItem(@NotNull MatchType matchType, int roomNumber) {
        this.matchType = matchType;
        this.roomNumber = roomNumber;
    }

    @Override
    public ItemProvider getItemProvider() {
        MatchManager matchManager = MatchManager.getInstance();
        Match match = matchManager.getMatch(MatchSelectorGui.getRoomMatchId(matchType, roomNumber));

        ItemBuilder builder;

        if (match != null) {
            // 試合が存在する場合
            int playerCount = match.getAllJoinPlayers().size();
            int playerMaxCount = match.getMatchMode().maxPlayerCount();
            Material material;


            if (playerCount >= playerMaxCount) {
                material = (matchType == MatchType.PVP) ? Material.RED_WOOL : Material.RED_CONCRETE;
            } else {
                material = (matchType == MatchType.PVP) ? Material.LIME_WOOL : Material.LIME_CONCRETE;
            }

            builder = new ItemBuilder(material);

            if (playerCount >= playerMaxCount) {
                builder.addLoreLines(new AdventureComponentWrapper(Component.text("満員").color(NamedTextColor.RED)));
            } else {
                builder.addLoreLines(new AdventureComponentWrapper(Component.text("参加可能").color(NamedTextColor.GREEN)));
            }

            // 試合の情報を追加
            List<Component> desc = new LinkedList<>();
            match.appendInfoDesc(desc);
            desc.stream()
                    .map(AdventureComponentWrapper::new)
                    .forEach(builder::addLoreLines);
        } else {
            // 試合が存在しない場合
            builder = new ItemBuilder((matchType == MatchType.PVP) ? Material.WHITE_WOOL : Material.WHITE_CONCRETE);
            builder.addLoreLines(new AdventureComponentWrapper(Component.text("試合を作成").color(NamedTextColor.GRAY)));
        }

        builder.setDisplayName(getRoomName(this.matchType, roomNumber));
        builder.setAmount(roomNumber + 1);

        return builder;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        MatchManager matchManager = MatchManager.getInstance();
        Match match = matchManager.getMatch(MatchSelectorGui.getRoomMatchId(matchType, roomNumber));
        
        matchManager.getJoinedMatch(player).ifPresentOrElse(joinedMatch -> {
            // 別の試合に参加している場合の処理
            if (match == joinedMatch) {
                player.sendMessage(ALREADY_JOIN_MATCH_MESSAGE);
            } else {
                player.sendMessage(ALREADY_JOIN_OTHER_MATCH_MESSAGE);
            }
        }, () -> {
            if (match != null) {
                // 試合に参加
                if (match.join(player, false)) {
                    player.sendMessage(JOIN_MATCH_MESSAGE.apply(getRoomName(this.matchType, roomNumber)));
                } else {
                    player.sendMessage(JOIN_MATCH_FAILURE_MESSAGE);
                }

                getWindows().forEach(Window::close);
            } else {
                // Window遷移
                getWindows().forEach(Window::close);
                MatchSelectorGui.matchCreateWindow(player, matchType, MatchSelectorGui.getRoomMatchId(matchType, roomNumber)).open();
            }
        });
    }

    /**
     * 部屋名を取得
     *
     * @param type       試合の種類
     * @param roomNumber 部屋番号
     * @return 部屋名
     */
    public static String getRoomName(@NotNull MatchType type, int roomNumber) {
        return String.format("%sルーム%d", type.getName().toUpperCase(Locale.ROOT), roomNumber);
    }
}
