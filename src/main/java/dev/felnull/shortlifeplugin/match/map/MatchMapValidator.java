package dev.felnull.shortlifeplugin.match.map;

import org.jetbrains.annotations.NotNull;

/**
 * マップが読み込み可能か検証するためのインタフェース
 *
 * @author MORIMORI0317
 */
public interface MatchMapValidator {

    /**
     * 検証を行う
     *
     * @param matchMapWorld マップワールド
     */
    void validate(@NotNull MatchMapWorld matchMapWorld);
}
