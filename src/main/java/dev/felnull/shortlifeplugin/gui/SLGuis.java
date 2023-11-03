package dev.felnull.shortlifeplugin.gui;

import com.google.common.collect.ImmutableList;
import dev.felnull.shortlifeplugin.gui.item.SLPageItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.window.Window;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * このプラグインのGUIレジストリ
 *
 * @author MORIMORI0317
 */
public final class SLGuis {

    /**
     * TEST用GUIのID
     */
    public static final String TEST = "test";

    /**
     * 試合選択GUIのID
     */
    public static final String MATCH_SELECTOR = "match_selector";

    /**
     * 登録されたGUIのIDとプロバイダーのマップ<br/>
     * 今後FNJLのレジストリシステムに置き換え予定
     */
    private static final Map<String, WindowProvider> GUI_REGISTRY = new HashMap<>();

    private SLGuis() {
    }

    /**
     * 初期化
     */
    @SuppressWarnings("checkstyle:WhitespaceAfter")
    public static void init() {

        // Invuiのグローバルアイテム登録
        // 英数字の登録は避けてください
        Structure.addGlobalIngredient('<', () -> new SLPageItem(false));
        Structure.addGlobalIngredient('>', () -> new SLPageItem(true));
        Structure.addGlobalIngredient('#', getBorderItem());


        GUI_REGISTRY.put(TEST, new TestGui());
        GUI_REGISTRY.put(MATCH_SELECTOR, new MatchSelectorGui());
    }

    /**
     * Windowファクトリを取得
     *
     * @param id ID
     * @return WindowProvider
     */
    @Nullable
    public static SLGuis.WindowProvider getWindowProvider(String id) {
        return GUI_REGISTRY.get(id);
    }

    @Unmodifiable
    @NotNull
    public static List<String> getAllGuiIds() {
        return ImmutableList.copyOf(GUI_REGISTRY.keySet());
    }

    public static ItemProvider getBorderItem() {
        return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName("§r");
    }

    /**
     * Window作成用インターフェイス
     *
     * @author MORIMORI0317
     */
    public interface WindowProvider {

        /**
         * Windowを作成
         *
         * @param player 開く対象のプレイヤー
         * @return Window
         */
        @NotNull
        Window provide(@NotNull Player player);
    }
}
