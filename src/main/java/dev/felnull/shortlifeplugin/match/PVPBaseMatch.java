package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.match.map.MatchMap;
import org.jetbrains.annotations.NotNull;

/**
 * PVP試合
 *
 * @author MORIMORI0317
 */
public abstract class PVPBaseMatch extends Match {

    /**
     * コンストラクタ
     *
     * @param id        試合ID
     * @param matchMode 試合モード
     * @param matchMap  試合用マップ
     */
    protected PVPBaseMatch(@NotNull String id, @NotNull MatchMode matchMode, @NotNull MatchMap matchMap) {
        super(id, matchMode, matchMap);
    }
}
