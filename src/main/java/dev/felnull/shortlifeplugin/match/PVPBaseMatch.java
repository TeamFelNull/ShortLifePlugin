package dev.felnull.shortlifeplugin.match;

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
     */
    protected PVPBaseMatch(@NotNull String id, @NotNull MatchMode matchMode) {
        super(id, matchMode);
    }
}
