package dev.felnull.shortlifeplugin.integration;

import dev.felnull.shortlifeplugin.utils.SLUtils;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.CharacterAndFormat;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * TABの連携関係
 *
 * @author MORIMORI0317
 */
public class TABIntegration {

    /**
     * リセット用セクションテキスト
     */
    private static final String RESET_TEXT = String.valueOf(LegacyComponentSerializer.SECTION_CHAR) + CharacterAndFormat.RESET.character();

    private TABIntegration() {
    }

    /**
     * 指定したプレイヤーのプレイヤーリスト表記色を変更
     *
     * @param player プレイヤー
     * @param color  色、nullであれば無色化
     */
    public static void setPlayerTabListColor(@NotNull Player player, @Nullable TextColor color) {
        TabListFormatManager tabListFormatManager = getAPI().getTabListFormatManager();
        if (tabListFormatManager == null) {
            return;
        }

        TabPlayer tabPlayer = getAPI().getPlayer(player.getUniqueId());
        if (tabPlayer == null) {
            return;
        }

        if (color != null) {
            SLUtils.getCharacterAndFormatByTextColor(color)
                    // とりあえず、非対応文字カラーの場合は白に設定する
                    .or(() -> Optional.of(CharacterAndFormat.WHITE))
                    .ifPresent(characterAndFormat -> {
                        tabListFormatManager.setPrefix(tabPlayer, String.valueOf(LegacyComponentSerializer.SECTION_CHAR) + characterAndFormat.character());
                        tabListFormatManager.setSuffix(tabPlayer, RESET_TEXT);
                    });
        } else {
            tabListFormatManager.setPrefix(tabPlayer, null);
            tabListFormatManager.setSuffix(tabPlayer, null);
        }
    }


    private static TabAPI getAPI() {
        return TabAPI.getInstance();
    }
}
