package dev.felnull.shortlifeplugin.gui;

import com.google.common.collect.ImmutableList;
import dev.felnull.shortlifeplugin.gui.item.KariMatchMapSelectItem;
import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.window.Window;

import java.util.List;


/**
 * 仮置きマップ選択GUI
 *
 * @author MORIMORI0317
 */
public class KariMapSelectGui {
    private KariMapSelectGui() {
    }

    /**
     * 仮置きマップ選択GUIを開く
     *
     * @param match  試合
     * @param player プレイヤー
     */
    public static void openGui(@NotNull Match match, @NotNull Player player) {
        Window window = provide(match, player);
        window.open();
    }

    @NotNull
    private static Window provide(@NotNull Match match, @NotNull Player player) {
        List<MatchMap> selectableMaps = match.getMapSelector().getSelectableMaps();

        if (selectableMaps == null) {
            selectableMaps = ImmutableList.of();
        }

        Gui gui = Gui.normal()
                .setStructure(
                        "#########",
                        "#########",
                        "#########")
                .build();

        if (!selectableMaps.isEmpty()) {
            gui.setItem(10, new KariMatchMapSelectItem(match, selectableMaps.get(0)));
        }

        if (selectableMaps.size() >= 2) {
            gui.setItem(12, new KariMatchMapSelectItem(match, selectableMaps.get(1)));
        }

        if (selectableMaps.size() >= 3) {
            gui.setItem(14, new KariMatchMapSelectItem(match, selectableMaps.get(2)));
        }

        if (selectableMaps.size() >= 4) {
            gui.setItem(16, new KariMatchMapSelectItem(match, selectableMaps.get(3)));
        }

        return Window.single()
                .setViewer(player)
                .setTitle("マップ選択")
                .setGui(gui)
                .build();
    }
}
