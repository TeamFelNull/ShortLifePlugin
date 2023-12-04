package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.match.map.MapMarker;
import dev.felnull.shortlifeplugin.match.map.MapMarkerPoints;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import dev.felnull.shortlifeplugin.match.map.MatchMapWorld;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        List<List<Player>> playerRankGroups = getPlayerRankGroups();

        for (int i = 0; i < playerRankGroups.size(); i++) {
            sendRankMessage(i, playerRankGroups.get(i));
        }
    }

    /**
     * プレイヤーをキル数ごとに分ける
     *
     * @return キル数ごとに分けられたプレイヤー
     */
    @NotNull
    private List<List<Player>> getPlayerRankGroups() {
        Map<Integer, List<Map.Entry<Player, PlayerInfo>>> playerKillCountGroups = this.players.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getValue().getKillCount()));

        return playerKillCountGroups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // kill数でソート
                .map(Map.Entry::getValue)// エントリのみを抽出
                .map(it -> it.stream()
                        .map(Map.Entry::getKey).toList()) // プレイヤーのみを抽出
                .toList();
    }

    /**
     * プレイヤーに順位メッセージを表示する
     *
     * @param rank 順位
     * @param rankPlayers 同じ順位の全プレイヤー
     */
    private static void sendRankMessage(int rank, List<Player> rankPlayers) {
        Audience audience = Audience.audience(rankPlayers);
        Title.Times times = Title.Times.times(Ticks.duration(10), Duration.ofMillis(FINISH_WAIT_FOR_TELEPORT - (FINISH_WAIT_FOR_TELEPORT / 4)), Ticks.duration(20));
        Title title = Title.title(Component.text(String.format("%s位", rank + 1)), Component.empty(), times);
        audience.showTitle(title);
    }


    @Override
    protected Optional<MapMarker> getSpawnMaker(@NotNull MatchMapWorld matchMapWorld, @NotNull Player player) {
        return matchMapWorld.getMakerRandom(MapMarkerPoints.SPAWN.get());
    }
}
