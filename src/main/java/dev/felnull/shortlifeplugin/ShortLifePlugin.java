package dev.felnull.shortlifeplugin;

import net.kunmc.lab.ikisugilogger.IkisugiLogger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * プラグインが有効、無効になった際の処理を行うクラス
 *
 * @author IDEA自動生成, MORIMORI0317
 */
public final class ShortLifePlugin extends JavaPlugin {

    @Override
    public void onEnable() {

        // IKISUGI LOGGER
        IkisugiLogger logger = new IkisugiLogger("very ikisugi\nshort life");
        logger.setColorType(IkisugiLogger.ColorType.RAINBOW);
        logger.setCenter(true);
        getLogger().info(logger.createLn());

        getLogger().info("ShortLife Plugin has started");
    }

    @Override
    public void onDisable() {
        getLogger().info("ShortLife Plugin has stopped");
    }
}
