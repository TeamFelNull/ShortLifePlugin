package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.match.map.MatchMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;


/**
 * どのような試合にするかの定義
 *
 * @param id             試合モードID
 * @param name           名前
 * @param iconItem       アイコンのアイテム
 * @param matchType      試合の種類
 * @param limitTime      制限時間(ms)
 * @param minPlayerCount 最低参加プレイヤー数
 * @param maxPlayerCount 最大参加プレイヤー数
 * @param matchProvider  試合インスタンスプロバイダー
 * @author MORIMORI0317
 */
public record MatchMode(@NotNull String id, @NotNull Component name, @NotNull Material iconItem,
                        @NotNull MatchType matchType, long limitTime,
                        int minPlayerCount, int maxPlayerCount, @NotNull MatchMode.MatchProvider matchProvider) {

    /**
     * 試合プロバイダー
     *
     * @author MORIMORI031
     */
    public interface MatchProvider {

        /**
         * 試合を作成
         *
         * @param id        試合ID
         * @param matchMode 試合モード
         * @param matchMap  試合マップ
         * @return 試合
         */
        Match provide(String id, MatchMode matchMode, MatchMap matchMap);
    }
}
