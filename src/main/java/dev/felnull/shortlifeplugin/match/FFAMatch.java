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
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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
        Map<Integer, List<Map.Entry<Player, PlayerData>>> playerKillCountGroups = this.players.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getValue().getKillCount()));

        List<List<Player>> playerRankGroups = playerKillCountGroups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // kill数でソート
                .map(Map.Entry::getValue)// エントリのみを抽出
                .map(it -> it.stream()
                        .map(Map.Entry::getKey).toList()) // プレイヤーのみを抽出
                .toList(); // リスト化


        for (int i = 0; i < playerRankGroups.size(); i++) {
            List<Player> rankPlayers = playerRankGroups.get(i);
            Audience audience = Audience.audience(rankPlayers);

            Title.Times times = Title.Times.times(Ticks.duration(10), Duration.ofMillis(FINISH_WAIT_FOR_TELEPORT - (FINISH_WAIT_FOR_TELEPORT / 4)), Ticks.duration(20));
            Title title = Title.title(Component.text(String.format("%s位", i + 1)), Component.empty(), times);
            audience.showTitle(title);
        }
    }

    @Override
    protected @Nullable MapMarker getSpawnMaker(@NotNull MatchMapWorld matchMapWorld, @NotNull Player player) {
        return matchMapWorld.getMakerRandom(MapMarkerPoints.SPAWN);
    }
}
