package dev.felnull.shortlifeplugin.gui;

import dev.felnull.fnjl.util.FNMath;
import dev.felnull.shortlifeplugin.SLConfig;
import dev.felnull.shortlifeplugin.gui.item.MatchModeIconItem;
import dev.felnull.shortlifeplugin.gui.item.MatchModeSelectItem;
import dev.felnull.shortlifeplugin.gui.item.MatchRoomSelectItem;
import dev.felnull.shortlifeplugin.match.*;
import dev.felnull.shortlifeplugin.utils.MatchUtils;
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
 * @author MORIMORI0317
 */
public class MatchSelectorGui implements SLGuis.WindowProvider {

    /**
     * PVP試合ルームの数
     */
    private static final int PVP_ROOM_SIZE = 2;

    /**
     * PVE試合ルームの数
     */
    private static final int PVE_ROOM_SIZE = 0;

    /**
     * 1ページあたりのルーム数
     */
    private static final int ROOM_PER_PAGE_COUNT = 9;

    @Override
    public @NotNull Window provide(@NotNull Player player) {

        // 試合選択GUIをページごとに作成
        List<Gui> pages = new ArrayList<>();
        int pageCount = Math.max(PVP_ROOM_SIZE, PVE_ROOM_SIZE) / ROOM_PER_PAGE_COUNT + 1;
        for (int i = 0; i < pageCount; i++) {
            pages.add(createPage(i));
        }

        Gui gui = PagedGui.guis()
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

        return Window.single()
                .setViewer(player)
                .setTitle("試合選択")
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

        MatchManager matchManager = MatchUtils.getMatchManager();

        for (int i = 0; i < pvpRoomCount; i++) {
            gui.setItem(i, 0, new MatchRoomSelectItem(MatchType.PVP, firstRoomNum + i));

            Match match = matchManager.getMatch(getRoomMatchId(MatchType.PVP, firstRoomNum + i));
            if (match != null) {
                gui.setItem(i, 1, new MatchModeIconItem(match.getMatchMode()));
            }
        }

        for (int i = 0; i < pveRoomCount; i++) {
            gui.setItem(i, 3, new MatchRoomSelectItem(MatchType.PVE, firstRoomNum + i));

            Match match = matchManager.getMatch(getRoomMatchId(MatchType.PVE, firstRoomNum + i));
            if (match != null) {
                gui.setItem(i, 4, new MatchModeIconItem(match.getMatchMode()));
            }
        }

        return gui;
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

        List<MatchMode> modes = MatchModes.getAllModes().values().stream()
                .filter(mode -> mode.matchType() == matchType)
                .filter(mode -> SLConfig.isTestMode() || !mode.testOnly()) // テスト時のみテストモードを表示
                .limit(gui.getSize())
                .toList();

        for (int i = 0; i < modes.size(); i++) {
            gui.setItem(i, new MatchModeSelectItem(modes.get(i), matchId));
        }

        return Window.single()
                .setViewer(player)
                .setTitle("試合作成")
                .setGui(gui)
                .build();
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
