package dev.felnull.shortlifeplugin.listener;

import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import dev.felnull.shortlifeplugin.weaponmechanics.SLScriptManager;
import me.deecaad.core.events.QueueSerializerEvent;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * ウェポンメカニクス関係のイベントリスナー
 *
 * @author MORIMORI0317
 */
public class WeaponMechanicsListener implements Listener {

    /**
     * ウェポンメカニクスのプラグイン名
     */
    private static final String WEAPON_MECHANICS = "WeaponMechanics";

    /**
     * 初期化
     *
     * @param plugin プラグイン
     */
    public static void init(ShortLifePlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(new WeaponMechanicsListener(), plugin);
    }

    /**
     * ウェポンメカニクス初期化イベント
     *
     * @param e キューシリアライズイベント
     * @see <a href="https://cjcrafter.gitbook.io/weaponmechanics/developer-api/projectile-scripts">参考</a>
     */
    @EventHandler
    public void queueSerializers(QueueSerializerEvent e) {
        if (!e.getSourceName().equals(WEAPON_MECHANICS)) {
            return;
        }

        WeaponMechanics.getProjectilesRunnable().addScriptManager(new SLScriptManager(SLUtils.getSLPlugin()));
    }
}
