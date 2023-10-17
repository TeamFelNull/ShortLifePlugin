package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.match.map.MapMarker;
import dev.felnull.shortlifeplugin.match.map.MapMarkerPoints;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import dev.felnull.shortlifeplugin.match.map.MatchMapWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * チーム試合
 *
 * @author MORIMORI0317
 */
public class TeamPointMatch extends Match {

    /**
     * 正常に試合を終了できなかった場合のメッセージ
     */
    private static final Component MATCH_FINISH_TEAM_DISAPPEARS_MESSAGE = Component.text("チームが消滅したため試合を終了します").color(NamedTextColor.RED);


    /**
     * チームの参加者が存在しない場合のメッセージ
     */
    private static final Component MATCH_FINISH_TEAM_NO_PARTICIPANTS_MESSAGE = Component.text("チームに参加者が存在しないため試合を終了します").color(NamedTextColor.RED);

    /**
     * チームの色<br/>
     * 上から順に優先
     */
    private static final NamedTextColor[] TEAM_COLORS = {
            NamedTextColor.RED,
            NamedTextColor.BLUE,
            NamedTextColor.GREEN,
            NamedTextColor.YELLOW,
            NamedTextColor.LIGHT_PURPLE,
            NamedTextColor.AQUA,
            NamedTextColor.WHITE,
            NamedTextColor.BLACK,
            NamedTextColor.DARK_RED,
            NamedTextColor.DARK_BLUE,
            NamedTextColor.DARK_GREEN,
            NamedTextColor.GOLD,
            NamedTextColor.DARK_PURPLE,
            NamedTextColor.DARK_AQUA,
            NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY
    };

    /**
     * チームIDのフォーマット (slm-試合ID-チーム番号)
     */
    private static final String TEAM_ID_FORMAT = "slm-%s-%d";

    /**
     * 試合チーム
     */
    private final List<Team> teams = new ArrayList<>();

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
    protected void init() {
        super.init();
        initTeam();
    }

    /**
     * チーム初期化
     */
    private void initTeam() {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

        // チームを登録、既にある場合は登録解除して新規登録
        String team1id = String.format(TEAM_ID_FORMAT, getId(), 1);
        String team2id = String.format(TEAM_ID_FORMAT, getId(), 2);

        Team team1 = scoreboard.getTeam(team1id);
        Team team2 = scoreboard.getTeam(team2id);

        if (team1 != null) {
            team1.unregister();
        }
        if (team2 != null) {
            team2.unregister();
        }

        teams.add(craeteTeam(team1id));
        teams.add(craeteTeam(team2id));
    }

    private Team craeteTeam(String teamId) {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

        Team team = scoreboard.registerNewTeam(teamId);
        team.setAllowFriendlyFire(false);
        team.setCanSeeFriendlyInvisibles(true);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);

        /* チームカラー選定 */
        // 既存のチームカラー
        List<NamedTextColor> existColors = scoreboard.getTeams().stream()
                .filter(Team::hasColor)
                .map(Team::color)
                .filter(color -> color instanceof NamedTextColor)
                .map(color -> (NamedTextColor) color)
                .toList();

        // 使用不可のチームカラー
        List<NamedTextColor> unavailableColors = teams.stream()
                .filter(Team::hasColor)
                .map(Team::color)
                .filter(color -> color instanceof NamedTextColor)
                .map(color -> (NamedTextColor) color)
                .toList();

        // 使用可能なチームカラー
        Optional<NamedTextColor> namedTextColor = Arrays.stream(TEAM_COLORS)
                .filter(color -> !unavailableColors.contains(color)) // 使用不可のカラーを除外
                .map(color -> {
                    /*カラーと使用数のペアに変換*/

                    int count = (int) existColors.stream()
                            .filter(col -> col == color)
                            .count();

                    return Pair.of(color, count);
                })
                .min(Comparator.comparingInt(Pair::getRight))// 一番使用数が少ないカラーを抽出
                .map(Pair::getLeft);

        // 使用可能なチームカラーが無い場合はランダム取得
        team.color(namedTextColor
                .orElseGet(() -> TEAM_COLORS[RANDOM.nextInt(TEAM_COLORS.length)]));

        return team;
    }

    @Override
    public boolean start() {
        for (Player player : players) {
            joinTeam(player);
        }

        return super.start();
    }

    @Override
    public boolean join(@NotNull Player player, boolean sendMessage) {
        if (getStatus() != Status.NONE) {
            joinTeam(player);
        }

        return super.join(player, sendMessage);
    }

    @Override
    public boolean leave(@NotNull Player player, boolean sendMessage) {
        // チームからプレイヤーを削除
        for (Team team : teams) {
            if (isExistTeam(team)) {
                team.removePlayer(player);
            }
        }

        return super.leave(player, sendMessage);
    }

    /**
     * チームにプレイヤーを参加させる
     *
     * @param player プレイヤー
     */
    private void joinTeam(Player player) {

        // 人数が最も少ないチームを取得
        List<Team> minTeams = new LinkedList<>();
        int minPlayerCount = -1;

        for (Team team : teams) {
            int playerCount = (int) players.stream()
                    .filter(team::hasPlayer)
                    .count();

            if (minPlayerCount == -1 || minPlayerCount > playerCount) {
                minPlayerCount = playerCount;
                minTeams.clear();

                minTeams.add(team);
            } else if (minPlayerCount == playerCount) {
                minTeams.add(team);
            }
        }


        Team team;
        if (minTeams.isEmpty()) {
            // 人数が最も少ないチームが無い場合はランダムに参加
            team = teams.get(RANDOM.nextInt(teams.size()));
        } else {
            // 人数が最も少ないチームにランダム参加
            team = minTeams.get(RANDOM.nextInt(minTeams.size()));
        }

        team.addPlayer(player);
    }

    @Override
    protected void tick() {

        boolean destroyFlag = false;

        for (Team team : teams) {
            // チームが消滅した際に試合終了
            if (!isExistTeam(team)) {
                broadcast(MATCH_FINISH_TEAM_DISAPPEARS_MESSAGE);
                destroyFlag = true;
                break;
            }

            // チームに参加者が居なければ試合終了
            if (getStatus() != Status.NONE && players.stream().noneMatch(team::hasPlayer)) {
                broadcast(MATCH_FINISH_TEAM_NO_PARTICIPANTS_MESSAGE);
                destroyFlag = true;
                break;
            }
        }

        if (destroyFlag) {
            destroy();
            return;
        }

        super.tick();
    }

    @Override
    protected void baseTick() {
        super.baseTick();
    }

    @Override
    protected void dispose() {
        super.dispose();

        // チーム登録解除
        for (Team team : teams) {
            if (isExistTeam(team)) {
                team.unregister();
            }
        }
    }

    @Override
    protected boolean canMaintainJoinPlayer(@NotNull Player player) {
        if (!super.canMaintainJoinPlayer(player)) {
            return false;
        }

        // 試合開始後、どれかしらのチームに所属していない場合は維持不可
        return getStatus() == Status.NONE || teams.stream()
                .anyMatch(team -> team.hasPlayer(player));
    }

    /**
     * チームの存在確認
     *
     * @param team チーム
     * @return 存在しているかどうか
     */
    private boolean isExistTeam(@NotNull Team team) {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();
        return scoreboard.getTeams().contains(team);
    }

    @Override
    protected @Nullable MapMarker getSpawnMaker(@NotNull MatchMapWorld matchMapWorld, @NotNull Player player) {
        // プレイヤーが所属するチーム
        Optional<Team> team = teams.stream()
                .filter(this::isExistTeam)
                .filter(tm -> tm.hasPlayer(player))
                .limit(1)
                .findFirst();

        // 所属していなければnull
        if (team.isEmpty()) {
            return null;
        }

        // リストの位置をもとに、チーム番号を特定してスポーン地点を決める
        NamespacedKey pointKey = (teams.indexOf(team.get()) == 0) ? MapMarkerPoints.SPAWN_TEAM1 : MapMarkerPoints.SPAWN_TEAM2;
        return matchMapWorld.getMakerRandom(pointKey);
    }
}
