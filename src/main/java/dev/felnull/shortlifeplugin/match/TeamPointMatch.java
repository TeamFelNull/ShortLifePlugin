package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.match.map.MatchMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * ポイント制チーム試合
 *
 * @author MORIMORI0317
 */
public class TeamPointMatch extends TeamBaseMatch {

    /**
     * 勝利時のテキスト
     */
    private static final MatchEndTextProvider[] WIN_TEXTS = {
            (myTeam, winners, losers) -> "おめでとう！",
            (myTeam, winners, losers) -> String.format("%sチームに勝ち目はありません", losers.stream()
                    .min(Comparator.comparing(PointMatchTeam::getPoint))
                    .map(MatchTeam::getName)
                    .orElse("負けた"))
    };

    /**
     * 敗北時のテキスト
     */
    private static final MatchEndTextProvider[] LOSE_TEXTS = {
            (myTeam, winners, losers) -> "残念！",
            (myTeam, winners, losers) -> "次は頑張ろう",
            (myTeam, winners, losers) -> "何で負けたか、次の試合まで考えといてください",
            (myTeam, winners, losers) -> "なかなか難しいねんな..."
    };

    /**
     * 引き分け時のテキスト
     */
    private static final MatchEndTextProvider[] DRAW_TEXTS = {
            (myTeam, winners, losers) -> "こんなんじゃ勝負になんないよ"
    };

    /**
     * 勝利
     */
    private static final String WINNER = "winner";

    /**
     * コンストラクタ
     *
     * @param id        試合ID
     * @param matchMode 試合モード
     * @param matchMap  試合用マップ
     */
    protected TeamPointMatch(@NotNull String id, @NotNull MatchMode matchMode, @NotNull MatchMap matchMap) {
        super(id, matchMode, matchMap);
    }

    @Override
    protected MatchTeam createMatchTeam(@NotNull String name, @NotNull NamedTextColor color, @NotNull NamespacedKey respawnPointMaker) {
        return new PointMatchTeam(name, color, respawnPointMaker);
    }

    @Override
    protected PlayerInfo createPlayerInfo(@NotNull Player player) {
        return new PointTeamPlayerInfo(player);
    }

    /**
     * ポイント制チーム試合のプレイヤー情報を取得
     *
     * @param player プレイヤー
     * @return プレイヤー情報
     */
    @SuppressWarnings("unused")
    public Optional<TeamPointMatch.PointTeamPlayerInfo> getPointTeamPlayerInfo(@NotNull Player player) {
        return getPlayerInfo(player).map(playerInfo -> (PointTeamPlayerInfo) playerInfo);
    }

    @Override
    protected void onPlayerKill(@NotNull Player target, @NotNull Player attacker) throws IOException {
        super.onPlayerKill(target, attacker);

        Optional<PointMatchTeam> targetMatchTeam = getTeamByPlayer(target).map(matchTeam -> (PointMatchTeam) matchTeam);
        Optional<PointMatchTeam> attackerMatchTeam = getTeamByPlayer(attacker).map(matchTeam -> (PointMatchTeam) matchTeam);
        
        // ポイントを付与
        if (targetMatchTeam.isPresent() && attackerMatchTeam.isPresent() && !targetMatchTeam.equals(attackerMatchTeam)) {
            attackerMatchTeam.ifPresent(pointMatchTeam -> pointMatchTeam.setPoint(pointMatchTeam.getPoint() + 1));
        }
    }

    @Override
    protected void matchEnd() {
        // 最も点数が多いチームを取得
        List<PointMatchTeam> mostPointTeams = new LinkedList<>();
        int maxPoint = -1;
        for (MatchTeam team : teams) {
            int point = ((PointMatchTeam) team).getPoint();
            if (maxPoint == -1 || point > maxPoint) {
                mostPointTeams.clear();
                maxPoint = point;
                mostPointTeams.add((PointMatchTeam) team);
            } else if (point == maxPoint) {
                mostPointTeams.add((PointMatchTeam) team);
            }
        }


        boolean draw = true;

        // 引き分け確認
        for (MatchTeam team : teams) {
            if (!mostPointTeams.contains((PointMatchTeam) team)) {
                draw = false;
                break;
            }
        }

        // 負けチーム、引き分け時は空
        List<PointMatchTeam> losers = new LinkedList<>();

        if (!draw) {
            // 負けチームのみ抽出
            losers.addAll(teams.stream()
                    .filter(it -> it instanceof PointMatchTeam)
                    .map(it -> (PointMatchTeam) it)
                    .filter(team -> !mostPointTeams.contains(team))
                    .limit(teams.size() - mostPointTeams.size())
                    .toList());
        }

        // 勝敗処理
        for (MatchTeam team : teams) {
            Component resultText;
            MatchEndTextProvider subtitleProvider;
            boolean win = false;

            if (draw) {
                resultText = Component.text("引き分け").color(NamedTextColor.GRAY);
                subtitleProvider = DRAW_TEXTS[RANDOM.nextInt(DRAW_TEXTS.length)];
            } else if (mostPointTeams.contains((PointMatchTeam) team)) {
                resultText = Component.text("勝利").color(NamedTextColor.RED);
                subtitleProvider = WIN_TEXTS[RANDOM.nextInt(WIN_TEXTS.length)];
                win = true;
            } else {
                resultText = Component.text("敗北").color(NamedTextColor.BLUE);
                subtitleProvider = LOSE_TEXTS[RANDOM.nextInt(LOSE_TEXTS.length)];
            }

            String subtitle = subtitleProvider.provide((PointMatchTeam) team, mostPointTeams, losers);
            Title.Times times = Title.Times.times(Ticks.duration(10), Duration.ofMillis(FINISH_WAIT_FOR_TELEPORT - (FINISH_WAIT_FOR_TELEPORT / 4)), Ticks.duration(20));
            Title title = Title.title(resultText, Component.text(subtitle), times);
            team.audience().showTitle(title);

            if (win) {
                team.getParticipationPlayers().forEach(player ->
                        getPlayerInfo(player).orElseThrow().runCommand(WINNER));
            }
        }
    }

    /**
     * ポイント制試合チーム
     *
     * @author MORIMORI0317
     */
    public class PointMatchTeam extends MatchTeam {

        /**
         * 獲得済みポイント
         */
        private int point;

        /**
         * コンストラクタ
         *
         * @param name              チーム名
         * @param color             チームカラー
         * @param respawnPointMaker リスポーン地点
         */
        public PointMatchTeam(String name, NamedTextColor color, NamespacedKey respawnPointMaker) {
            super(name, color, respawnPointMaker);
        }

        public int getPoint() {
            return point;
        }

        /**
         * ポイントセットと表示更新フラグを立てる
         *
         * @param point ポイント
         */
        public void setPoint(int point) {
            this.point = point;
            TeamPointMatch.this.dirtyAllInfo();
        }
    }

    /**
     * ポイント制チーム試合のプレイヤー情報
     *
     * @author MORIMORI0317
     */
    public class PointTeamPlayerInfo extends TeamPlayerInfo {

        /**
         * コンストラクタ
         *
         * @param player プレイヤー
         */
        public PointTeamPlayerInfo(@NotNull Player player) {
            super(player);
        }


        @Override
        protected void appendSidebarMatchInfo(@NotNull List<Component> sidebarInfos) {
            super.appendSidebarMatchInfo(sidebarInfos);

            // 得点表示
            List<Component> teamPontTexts = new LinkedList<>();

            for (MatchTeam team : TeamPointMatch.this.teams) {
                Component teamText = Component.text(team.getName())
                        .append(Component.text(":"))
                        .append(Component.text(((PointMatchTeam) team).getPoint()))
                        .color(team.getColor());

                teamPontTexts.add(teamText);
            }

            Component pointText = Component.text("ポイント ").color(NamedTextColor.AQUA)
                    .append(Component.join(JoinConfiguration.builder().separator(Component.text(" ")).build(),
                            teamPontTexts.toArray(Component[]::new)));

            sidebarInfos.add(pointText);
        }
    }

    /**
     * 試合終了時のテキスト取得用
     *
     * @author MORIMORI0317
     */
    private interface MatchEndTextProvider {

        /**
         * 試合終了時のテキスト取得
         *
         * @param myTeam  自分のチーム
         * @param winners 勝ったチーム
         * @param losers  負けチーム、引き分け時は空
         * @return 文字列
         */
        @NotNull
        String provide(@NotNull PointMatchTeam myTeam, @NotNull @Unmodifiable List<PointMatchTeam> winners, @NotNull @Unmodifiable List<PointMatchTeam> losers);
    }
}
