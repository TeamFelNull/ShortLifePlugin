package dev.felnull.shortlifeplugin.weaponmechanics;

import me.deecaad.weaponmechanics.weapon.projectile.AProjectile;
import me.deecaad.weaponmechanics.weapon.projectile.ProjectileScriptManager;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * このプラグインの発射物マネージャー
 *
 * @author MORIMORI0317
 */
public class SLScriptManager extends ProjectileScriptManager {

    /**
     * コンストラクタ
     *
     * @param plugin プラグイン
     */
    public SLScriptManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void attach(@NotNull AProjectile aProjectile) {
        // 今のところ未実装だが、今後使う可能性あり
    }
}
