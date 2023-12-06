package dev.felnull.shortlifeplugin.gui.item;

import dev.felnull.shortlifeplugin.MsgHandler;
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
import java.util.Optional;

/**
 * 試合選択GUIアイテム
 *
 * @author MORIMORI0317, Quarri6343
 */
public class MatchRoomSelectItem extends AbstractItem {

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

        return matchManager.getMatch(MatchSelectorGui.getRoomMatchId(matchType, roomNumber))
                .map(this::getItemBuilderFromMatch)
                .orElseGet(this::getItemBuilderWhenMatchNotExists);
    }

    /**
     * 試合からItemBuilderを取得する
     *
     * @param match 試合
     * @return ItemBuilder アイテムビルダー
     */
    @NotNull
    private ItemBuilder getItemBuilderFromMatch(Match match) {
        int playerCount = match.getAllJoinPlayers().size();
        int playerMaxCount = match.getMatchMode().maxPlayerCount();
        Material material;


        if (playerCount >= playerMaxCount) {
            material = (matchType == MatchType.PVP) ? Material.RED_WOOL : Material.RED_CONCRETE;
        } else {
            material = (matchType == MatchType.PVP) ? Material.LIME_WOOL : Material.LIME_CONCRETE;
        }

        ItemBuilder builder = new ItemBuilder(material);

        if (playerCount >= playerMaxCount) {
            builder.addLoreLines(new AdventureComponentWrapper(Component.text(MsgHandler.get("item-room-full")).color(NamedTextColor.RED)));
        } else {
            builder.addLoreLines(new AdventureComponentWrapper(Component.text(MsgHandler.get("item-room-has-space")).color(NamedTextColor.GREEN)));
        }

        addMatchInfo(match, builder);
        addRoomCommonInfo(builder);

        return builder;
    }

    /**
     * 試合が存在しない場合のItemBuilderを取得する
     *
     * @return ItemBuilder
     */
    @NotNull
    private ItemBuilder getItemBuilderWhenMatchNotExists() {
        ItemBuilder builder = new ItemBuilder((matchType == MatchType.PVP) ? Material.WHITE_WOOL : Material.WHITE_CONCRETE);
        builder.addLoreLines(new AdventureComponentWrapper(Component.text(MsgHandler.get("item-create-match")).color(NamedTextColor.GRAY)));

        addRoomCommonInfo(builder);

        return builder;
    }

    /**
     * 試合の情報をアイテムに追加
     *
     * @param match   試合
     * @param builder ItemBuilder
     */
    private static void addMatchInfo(Match match, ItemBuilder builder) {
        List<Component> desc = new LinkedList<>();
        match.appendInfoDesc(desc);
        desc.stream()
                .map(AdventureComponentWrapper::new)
                .forEach(builder::addLoreLines);
    }

    /**
     * 部屋の共通する情報をItemBuilderに追加する
     *
     * @param builder ItemBuilder
     */
    private void addRoomCommonInfo(ItemBuilder builder) {
        builder.setDisplayName(getRoomName(this.matchType, roomNumber));
        builder.setAmount(roomNumber + 1);
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        MatchManager matchManager = MatchManager.getInstance();
        Optional<Match> clickedMatch = matchManager.getMatch(MatchSelectorGui.getRoomMatchId(matchType, roomNumber));

        matchManager.getJoinedMatch(player).ifPresentOrElse(
                joinedMatch -> onAlreadyJoined(player, clickedMatch, joinedMatch), 
                () -> clickedMatch.ifPresentOrElse(
                        match -> tryJoinMatch(player, match), 
                        () -> windowTransition(player)));
    }

    /**
     * 別の試合に参加している場合の処理
     *
     * @param player       プレイヤー
     * @param clickedMatch クリックされた試合
     * @param joinedMatch  参加している試合
     */
    private static void onAlreadyJoined(@NotNull Player player, Optional<Match> clickedMatch, Match joinedMatch) {
        clickedMatch.stream().filter(match -> match == joinedMatch).findFirst().ifPresentOrElse(
                match -> player.sendMessage(Component.text(MsgHandler.get("item-already-joined-match"))), 
                () -> player.sendMessage(Component.text(MsgHandler.get("item-already-joined-other-match"))));
    }

    /**
     * 試合に参加させる
     *
     * @param player プレイヤー
     * @param match  試合
     */
    private void tryJoinMatch(@NotNull Player player, Match match) {
        if (match.join(player, false)) {
            player.sendMessage(
                    Component.text(MsgHandler.getFormatted("item-joined-match", getRoomName(this.matchType, roomNumber)))
                            .color(NamedTextColor.WHITE));
        } else {
            player.sendMessage(Component.text(MsgHandler.get("item-join-match-failure")).color(NamedTextColor.RED));
        }

        getWindows().forEach(Window::close);
    }

    /**
     * ウィンドウ遷移
     *
     * @param player プレイヤー
     */
    private void windowTransition(@NotNull Player player) {
        getWindows().forEach(Window::close);
        MatchSelectorGui.matchCreateWindow(player, matchType, MatchSelectorGui.getRoomMatchId(matchType, roomNumber)).open();
    }

    /**
     * 部屋名を取得
     *
     * @param type       試合の種類
     * @param roomNumber 部屋番号
     * @return 部屋名
     */
    public static String getRoomName(@NotNull MatchType type, int roomNumber) {
        return MsgHandler.getFormatted("item-room-name", type.getName().toUpperCase(Locale.ROOT), roomNumber);
    }
}
