package dev.felnull.shortlifeplugin.gui;

import dev.felnull.fnjl.util.FNMath;
import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.SLConfig;
import dev.felnull.shortlifeplugin.gui.item.MatchModeIconItem;
import dev.felnull.shortlifeplugin.gui.item.MatchModeSelectItem;
import dev.felnull.shortlifeplugin.gui.item.MatchRoomSelectItem;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.MatchMode;
import dev.felnull.shortlifeplugin.match.MatchModes;
import dev.felnull.shortlifeplugin.match.MatchType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * 試合選択GUI
 *
 * @author MORIMORI0317, Quarri6343
 */
public class MatchSelectorGui implements SLGuis.WindowProvider {

    /**
     * PVP試合ルームの数
     */
    public static final int PVP_ROOM_SIZE = 2;

    /**
     * PVE試合ルームの数
     */
    public static final int PVE_ROOM_SIZE = 0;

    /**
     * 1ページあたりのルーム数
     */
    private static final int ROOM_PER_PAGE_COUNT = 9;

    /**
     * ページ数
     */
    private static final int PAGE_COUNT = Math.max(PVP_ROOM_SIZE, PVE_ROOM_SIZE) / ROOM_PER_PAGE_COUNT + 1;

    @Override
    public @NotNull Window provide(@NotNull Player player) {

        // 試合選択GUIをページごとに作成
        List<Gui> pages = new ArrayList<>();
        for (int i = 0; i < PAGE_COUNT; i++) {
            pages.add(createPage(i));
        }

        Gui gui = buildGuiFromPages(pages);

        return buildWindow(player, MsgHandler.get("gui-match-select"), gui);
    }

    @NotNull
    private static Gui buildGuiFromPages(List<Gui> pages) {
        return PagedGui.guis()
                .setStructure(
                        "xxxxxxxxx",
                        "xxxxxxxxx",
                        "xxxxxxxxx",
                        "xxxxxxxxx",
                        "xxxxxxxxx",
                        "###<#>###")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(pages)
                .build();
    }

    @NotNull
    private static Window buildWindow(@NotNull Player player, String title, Gui gui) {
        return Window.single()
                .setViewer(player)
                .setTitle(title)
                .setGui(gui)
                .build();
    }

    private Gui createPage(int pageNum) {
        Gui gui = Gui.normal()
                .setStructure(
                        "#########",
                        "#########",
                        "#########",
                        "#########",
                        "#########")
                .build();

        int firstRoomNum = ROOM_PER_PAGE_COUNT * pageNum;
        int pvpRoomCount = FNMath.clamp(PVP_ROOM_SIZE - firstRoomNum, 0, ROOM_PER_PAGE_COUNT);
        int pveRoomCount = FNMath.clamp(PVE_ROOM_SIZE - firstRoomNum, 0, ROOM_PER_PAGE_COUNT);

        MatchManager matchManager = MatchManager.getInstance();

        listPVPRoom(gui, firstRoomNum, pvpRoomCount, matchManager);
        listPVERoom(gui, firstRoomNum, pveRoomCount, matchManager);

        return gui;
    }

    private static void listPVPRoom(Gui gui, int firstRoomNum, int pvpRoomCount, MatchManager matchManager) {
        for (int i = 0; i < pvpRoomCount; i++) {
            gui.setItem(i, 0, new MatchRoomSelectItem(MatchType.PVP, firstRoomNum + i));

            int finalI = i;
            matchManager.getMatch(getRoomMatchId(MatchType.PVP, firstRoomNum + i)).ifPresent(match ->
                    gui.setItem(finalI, 1, new MatchModeIconItem(match.getMatchMode())));
        }
    }

    private static void listPVERoom(Gui gui, int firstRoomNum, int pveRoomCount, MatchManager matchManager) {
        for (int i = 0; i < pveRoomCount; i++) {
            gui.setItem(i, 3, new MatchRoomSelectItem(MatchType.PVE, firstRoomNum + i));

            int finalI = i;
            matchManager.getMatch(getRoomMatchId(MatchType.PVE, firstRoomNum + i)).ifPresent(match ->
                    gui.setItem(finalI, 4, new MatchModeIconItem(match.getMatchMode())));
        }
    }

    /**
     * 試合作成Window
     *
     * @param player    プレイヤー
     * @param matchType 試合タイプ
     * @param matchId   試合ID
     * @return Window
     */
    public static Window matchCreateWindow(@NotNull Player player, @NotNull MatchType matchType, @NotNull String matchId) {
        Gui gui = Gui.normal()
                .setStructure(
                        "#########",
                        "#########",
                        "#########")
                .build();

        List<MatchMode> modes = getAllMatchModes(matchType, gui);

        for (int i = 0; i < modes.size(); i++) {
            gui.setItem(i, new MatchModeSelectItem(modes.get(i), matchId));
        }

        return buildWindow(player, MsgHandler.get("gui-match-creation"), gui);
    }

    @NotNull
    private static List<MatchMode> getAllMatchModes(@NotNull MatchType matchType, Gui gui) {
        return MatchModes.getAllModes().values().stream()
                .filter(mode -> mode.matchType() == matchType)
                .filter(mode -> SLConfig.isTestMode() || !mode.testOnly()) // テスト時のみテストモードを表示
                .limit(gui.getSize())
                .toList();
    }

    /**
     * ルームごとの試合IDを取得<br/>
     * [試合タイプ]-room[ルーム番号]
     *
     * @param matchType  試合タイプ
     * @param roomNumber ルームID
     * @return 試合ID
     */
    public static String getRoomMatchId(MatchType matchType, int roomNumber) {
        return String.format("%s-room%d", matchType.getName(), roomNumber);
    }
}
