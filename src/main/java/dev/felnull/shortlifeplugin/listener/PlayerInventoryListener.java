package dev.felnull.shortlifeplugin.listener;

import dev.felnull.shortlifeplugin.ShortLifePlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * プレイヤーのインベントリ関係のイベントリスナー
 *
 * @author sysnote8
 */
public class PlayerInventoryListener implements Listener {

    /**
     * 初期化
     *
     * @param plugin プラグイン
     */
    public static void init(ShortLifePlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerInventoryListener(), plugin);
    }

    /**
     * インベントリー内がクリックされた時のイベント
     *
     * @param event インベントリクリックイベント
     */
    @EventHandler
    public void onItemMoved(InventoryClickEvent event) {
        if (event.getClick().equals(ClickType.NUMBER_KEY) && (event.getClick().equals(ClickType.SHIFT_LEFT) || event.getClick().equals(ClickType.SHIFT_RIGHT))) {
            event.setCancelled(true);
        }
    }
}
