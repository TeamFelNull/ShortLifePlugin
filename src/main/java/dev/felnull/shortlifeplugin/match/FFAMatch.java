package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.match.map.MapMarker;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import dev.felnull.shortlifeplugin.match.map.MatchMapWorld;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * FFA試合
 *
 * @author MORIMORI0317
 */
public class FFAMatch extends PVPBaseMatch {

    /**
     * コンストラクタ
     *
     * @param id        試合ID
     * @param matchMode 試合モード
     * @param matchMap  試合用マップ
     */
    protected FFAMatch(@NotNull String id, @NotNull MatchMode matchMode, @NotNull MatchMap matchMap) {
        super(id, matchMode, matchMap);
    }

    @Override
    protected void matchEnd() {

    }

    @Override
    protected @Nullable MapMarker getSpawnMaker(@NotNull MatchMapWorld matchMapWorld, @NotNull Player player) {
        return null;
    }
}
