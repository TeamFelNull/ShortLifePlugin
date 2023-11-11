package dev.felnull.shortlifeplugin.listener;

import dev.felnull.shortlifeplugin.SLConfig;
import dev.felnull.shortlifeplugin.ShortLifePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 一般的なイベントリスナー
 *
 * @author MORIMORI0317
 */
public class CommonListener implements Listener {

    /**
     * 初期化
     *
     * @param plugin プラグイン
     */
    public static void init(ShortLifePlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(new CommonListener(), plugin);
    }

    /**
     * プレイヤーが参加した際のイベント
     *
     * @param e プレイヤー参加イベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {

        // テストモードの場合、参加時にメッセージを送信
        if (SLConfig.isTestMode()) {
            Component message = Component.text("ShortLifePluginはテストモードです。");
            message = message.appendNewline().append(Component.text("このメッセージを見た場合は運営に報告してください！"));
            message = message.color(NamedTextColor.GREEN);
            e.getPlayer().sendMessage(message);
        }
    }
}
