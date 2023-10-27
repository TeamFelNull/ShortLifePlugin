package dev.felnull.shortlifeplugin.match;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.sk89q.worldedit.math.BlockVector3;
import dev.felnull.fnjl.util.FNMath;
import dev.felnull.fnjl.util.FNStringUtil;
import dev.felnull.shortlifeplugin.match.map.MapMarker;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import dev.felnull.shortlifeplugin.match.map.MatchMapInstance;
import dev.felnull.shortlifeplugin.match.map.MatchMapWorld;
import dev.felnull.shortlifeplugin.utils.MatchUtils;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 試合インスタンスクラス
 *
 * @author MORIMORI0317
 */
public abstract class Match {

    /**
     * ランダム
     */
    protected static final Random RANDOM = new Random();

    /**
     * 試合が開始するまでの時間(ms)
     */
    private static final long START_WAIT_TIME = 1000 * 30;

    /**
     * 試合終了後にテレポートするまでの時間
     */
    private static final long FINISH_WAIT_FOR_TELEPORT = 1000 * 10;

    /**
     * 試合終了から破棄されるまでの時間(ms)
     */
    private static final long DISPOSE_WAIT_TIME = FINISH_WAIT_FOR_TELEPORT + (1000 * 5);

    /**
     * スポーン後の無敵時間
     */
    private static final long SPAWN_INVINCIBILITY_TIME = 1000 * 5;

    /**
     * カウントを開始する残り秒数
     */
    private static final int COUNT_START_REMNANT_SECOND = 10;

    /**
     * 試合参加時のメッセージ
     */
    private static final Component MATCH_JOIN_MESSAGE = Component.text("試合に参加しました").color(NamedTextColor.WHITE);

    /**
     * 試合退出時のメッセージ
     */
    private static final Component MATCH_LEAVE_MESSAGE = Component.text("試合から退出しました").color(NamedTextColor.WHITE);

    /**
     * 試合参加時、参加者以外に送信するメッセージ
     */
    private static final Function<Player, Component> MATCH_JOIN_BROADCAST_MESSAGE =
            player -> Component.text(String.format("%sが試合に参加しました", player.getName())).color(NamedTextColor.WHITE);

    /**
     * 試合退出時、参加者以外に送信するメッセージ
     */
    private static final Function<Player, Component> MATCH_LEAVE_BROADCAST_MESSAGE =
            player -> Component.text(String.format("%sが試合から退出しました", player.getName())).color(NamedTextColor.WHITE);

    /**
     * 試合開始時のメッセージ
     */
    private static final Component MATCH_START_MESSAGE = Component.text("試合開始...").color(NamedTextColor.GREEN);

    /**
     * 試合終了時のメッセージ
     */
    private static final Component MATCH_FINISH_MESSAGE = Component.text("試合終了...").color(NamedTextColor.GREEN);

    /**
     * 開始時のワールド読み込み待ちメッセージ
     */
    private static final Component MATCH_WAIT_LOAD_WORLD_MESSAGE = Component.text("マップの準備が終わるまでお待ちください").color(NamedTextColor.BLUE);

    /**
     * ワールド読み込み失敗で試合を中止する際のメッセージ
     */
    private static final Component MATCH_CANCEL_FAILED_LOAD_WORLD_MESSAGE = Component.text("マップの読み込みに失敗したため試合を中止します").color(NamedTextColor.DARK_RED);

    /**
     * 想定外のエラーが原因で試合を中止する際のメッセージ
     */
    private static final Component MATCH_CANCEL_UNEXPECTED_ERROR_MESSAGE = Component.text("想定外のエラーが発生したため試合を中止します").color(NamedTextColor.DARK_RED).appendNewline()
            .append(Component.text("この事を運営に報告してください！").color(NamedTextColor.GOLD));

    /**
     * 人不足で試合を終了する際のメッセージ
     */
    private static final Component MATCH_FINISH_INSUFFICIENT_PLAYER_MESSAGE = Component.text("参加人数が不足したため試合を終了します").color(NamedTextColor.RED);

    /**
     * 参加しているプレイヤーとプレイヤーデータ
     */
    protected final Map<Player, PlayerData> players = new HashMap<>();

    /**
     * 同じ状態の経過Tick
     */
    protected int statusTick;

    /**
     * 試合用マップ
     */
    protected MatchMapInstance matchMapInstance;

    /**
     * 試合のID
     */
    @NotNull
    private final String id;

    /**
     * 試合モード
     */
    @NotNull
    private final MatchMode matchMode;

    /**
     * 試合用マップ
     */
    @NotNull
    private final MatchMap matchMap;

    /**
     * カウントダウン表示用ボスバー
     */
    private final BossBar countDownBossbar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);

    /**
     * 状態
     */
    @NotNull
    private Status status = Status.NONE;

    /**
     * 破棄するためのフラグ
     */
    private boolean destroyed = false;

    /**
     * マップの読み込まれ待ちを通知したかどうか
     */
    private boolean waitLoadMapNotified;

    /**
     * マップの読み込みが完了したフラグ
     */
    private boolean loadMapCompletion;

    /**
     * 試合終了後のテレポートを行ったかどうか
     */
    private boolean finishTeleport;

    /**
     * カウントダウンに表示する時間(ms)
     */
    private long countDownTime;

    /**
     * 情報表示の更新が必要であるフラグ
     */
    private boolean dirtyAllInfo;

    /**
     * コンストラクタ
     *
     * @param id        試合ID
     * @param matchMode 試合モード
     * @param matchMap  試合用マップ
     */
    protected Match(@NotNull String id, @NotNull MatchMode matchMode, @NotNull MatchMap matchMap) {
        this.id = id;
        this.matchMode = matchMode;
        this.matchMap = matchMap;
    }

    /**
     * 初期化処理
     */
    protected void init() {
        updateCountDownStatus();
        this.matchMapInstance = MatchUtils.getMatchManager().getMapLoader().createMapInstance(this, this.id, this.matchMap);
        SLUtils.getLogger().info(String.format("試合(%s)が作成されました", getId()));
    }

    /**
     * 1Tickごとの処理
     */
    protected void tick() {
        // 参加を維持できないプレイヤーを退出させる
        List<Player> leavePlayers = players.keySet().stream()
                .filter(pl -> !canMaintainJoinPlayer(pl))
                .toList();
        leavePlayers.forEach(player -> leave(player, true));

        // 試合参加者0人になれば試合破棄
        if (this.players.isEmpty()) {
            destroy();
            return;
        }

        this.statusTick++;

        baseTick();

        // 状態別の処理
        switch (getStatus()) {
            case NONE -> beforeStartTick();
            case STARTED -> duringMatchTick();
            case FINISHED -> afterFinishTick();
            default -> {
            }
        }

        // プレイヤーごとの処理
        this.players.forEach((player, playerData) -> {
            playerData.setLifeTime(playerData.getLifeTime() + 1);
            playerData.updateCheckAndInfo();
        });
        Match.this.dirtyAllInfo = false;
    }

    /**
     * 試合開始前のTick処理
     */
    protected void beforeStartTick() {

        // マップの読み込みに失敗した場合は試合破棄
        if (this.matchMapInstance.isLoadFailed()) {
            broadcast(MATCH_CANCEL_FAILED_LOAD_WORLD_MESSAGE);

            Optional<Throwable> mapWorldError = this.matchMapInstance.getMapWordLoadError();

            if (mapWorldError.isPresent()) {
                SLUtils.reportError(mapWorldError.get(), "試合用マップの読み込みに失敗");
            } else {
                SLUtils.reportError(new RuntimeException("試合用マップの読み込みに失敗"));
            }

            destroy();
            return;
        }

        boolean start = false;

        int totalTime = SLUtils.toTick(TimeUnit.MILLISECONDS, START_WAIT_TIME);
        updateCountDownTime(this.statusTick, totalTime);

        // マップの読み込みが終わった場合
        if (!loadMapCompletion && matchMapInstance.isReady()) {
            loadMapCompletion = true;
            dirtyAllInfo();
        }

        if (this.statusTick >= totalTime) {
            // 開始待機時間が過ぎた場合の処理
            if (matchMapInstance.isReady()) {
                // マップの準備が終わっていれば試合開始
                start = true;
            } else if (!waitLoadMapNotified) {
                // マップの準備が終わっていなければ、1度だけ通知
                waitLoadMapNotified = true;
                broadcast(MATCH_WAIT_LOAD_WORLD_MESSAGE);
            }
        }

        if (start && !start()) {
            SLUtils.reportError(new RuntimeException("試合を開始できませんでした。"));
            destroy();
        }
    }

    /**
     * 試合中のTick処理
     */
    protected void duringMatchTick() {

        boolean finish = false;

        int totalTime = SLUtils.toTick(TimeUnit.MILLISECONDS, getMatchMode().limitTime());
        updateCountDownTime(this.statusTick, totalTime);

        if (players.size() < getMatchMode().minPlayerCount()) {
            // 参加プレイヤーの人数が、最低参加人数より少なければ試合を終了
            broadcast(MATCH_FINISH_INSUFFICIENT_PLAYER_MESSAGE);
            finish = true;
        } else if (this.statusTick >= totalTime) {
            // 試合制限時間を過ぎた場合、試合を終了
            finish = true;
        }

        if (finish && !finish()) {
            SLUtils.reportError(new RuntimeException("試合を終了できませんでした。"));
            destroy();
        }
    }

    /**
     * 試合終了後のTick処理
     */
    protected void afterFinishTick() {

        int totalTime = SLUtils.toTick(TimeUnit.MILLISECONDS, FINISH_WAIT_FOR_TELEPORT);
        updateCountDownTime(this.statusTick, totalTime);

        // 終了後のテレポート
        if (!finishTeleport && this.statusTick >= totalTime) {
            finishTeleport = true;

            for (final Player player : players.keySet()) {
                MatchUtils.teleportToLeave(player, this.matchMapInstance.getMapWorld()
                        .map(MatchMapWorld::getWorld)
                        .orElse(null));
            }
        }

        // 破棄されるまでしばらく待機
        if (this.statusTick >= SLUtils.toTick(TimeUnit.MILLISECONDS, DISPOSE_WAIT_TIME)) {
            destroy();
        }
    }

    /**
     * 試合ごとのTick処理
     */
    protected void baseTick() {
    }

    /**
     * 試合への参加を維持できるかどうか
     *
     * @param player プレイヤー
     * @return 維持できるかどうか
     */
    protected boolean canMaintainJoinPlayer(@NotNull Player player) {
        Objects.requireNonNull(player);

        // このプレイヤーインスタンスが有効ではない場合は維持不可
        if (!player.isConnected() || !player.isOnline()) {
            return false;
        }

        if (this.status != Status.NONE) {
            Optional<MatchMapWorld> matchMapWorld = this.matchMapInstance.getMapWorld();

            if (matchMapWorld.isPresent() && matchMapWorld.get().getWorld() != player.getWorld()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 指定のプレイヤーを試合へ参加させる
     *
     * @param player      プレイヤー
     * @param sendMessage 参加メッセージを本人に送信するかどうか
     * @return 参加できたかどうか
     */
    public boolean join(@NotNull Player player, boolean sendMessage) {
        Objects.requireNonNull(player);

        // 最大参加人数を超えるか確認
        if (players.size() >= matchMode.maxPlayerCount()) {
            return false;
        }

        // すでにどれかしらの試合に参加しているプレイヤーは参加不可
        if (MatchUtils.getMatchManager().getJointedMach(player) != null) {
            return false;
        }

        // 開始前、試合中以外は参加不可
        if (status != Status.NONE && status != Status.STARTED) {
            return false;
        }

        PlayerData playerData = createPlayerData(player);
        this.players.put(player, playerData);

        // プレイヤーのスコアボードを試合用スコアボードに変更
        playerData.setPreScoreboard(player.getScoreboard());
        player.setScoreboard(playerData.getScoreboard());

        // 試合が既に開始しているならば開始処理
        if (status == Status.STARTED) {
            playerStart(player);
        }

        // カウントダウン用ボスバーを表示
        Audience.audience(player).showBossBar(countDownBossbar);

        if (sendMessage) {
            player.sendMessage(MATCH_JOIN_MESSAGE);
        }

        broadcast(MATCH_JOIN_BROADCAST_MESSAGE.apply(player), pl -> pl != player);

        SLUtils.getLogger().info(String.format("%sが試合(%s)に参加しました", player.getName(), getId()));

        return true;
    }

    /**
     * プレイヤーデータを作成
     *
     * @param player プレイヤー
     * @return プレイヤーデータ
     */
    protected PlayerData createPlayerData(@NotNull Player player) {
        return new PlayerData(player);
    }

    /**
     * 指定されたプレイヤーのデータを取得
     *
     * @param player プレイヤー
     * @return プレイヤーデータ
     */
    @Nullable
    public PlayerData getPlayerData(@NotNull Player player) {
        return this.players.get(player);
    }

    /**
     * 指定のプレイヤーを試合から退出させる
     *
     * @param player      プレイヤー
     * @param sendMessage 退出メッセージを送信するかどうか
     * @return 退出できたかどうか
     */
    public boolean leave(@NotNull Player player, boolean sendMessage) {
        Objects.requireNonNull(player);

        // 参加していないプレイヤーは退出不可
        if (!this.players.containsKey(player)) {
            return false;
        }

        PlayerData playerData = players.get(player);

        // 試合用スコアボードを参加前のスコアボードに戻す
        Scoreboard preScoreboard = playerData.getPreScoreboard();
        if (preScoreboard == null) {
            preScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        }
        player.setScoreboard(preScoreboard);

        this.players.remove(player);

        // カウントダウン用ボスバーを非表示化
        Audience.audience(player).hideBossBar(countDownBossbar);

        // ゲームモードを元に戻す
        GameMode preGameMode = playerData.getPreGameMode();
        if (preGameMode != null) {
            player.setGameMode(preGameMode);
        }

        // 試合用ワールドにいる場合、ワールド外にテレポート
        Optional<MatchMapWorld> matchMapWorld = this.matchMapInstance.getMapWorld();
        if (matchMapWorld.isPresent() && matchMapWorld.get().getWorld() == player.getWorld()) {
            MatchUtils.teleportToLeave(player, matchMapWorld
                    .map(MatchMapWorld::getWorld)
                    .orElse(null));
        }

        if (sendMessage) {
            player.sendMessage(MATCH_LEAVE_MESSAGE);
            broadcast(MATCH_LEAVE_BROADCAST_MESSAGE.apply(player), pl -> pl != player);
        }

        SLUtils.getLogger().info(String.format("%sが試合(%s)から退出しました", player.getName(), getId()));
        return true;
    }

    /**
     * 現在参加している全てのプレイヤー
     *
     * @return 参加しているプレイヤーの不変リスト
     */
    @Unmodifiable
    @NotNull
    public List<Player> getAllJoinPlayers() {
        return ImmutableList.copyOf(players.keySet());
    }

    /**
     * 指定されたプレイヤーが参加しているかどうか
     *
     * @param player プレイヤー
     * @return 参加していればtrue、そうでなければfalse
     */
    public boolean hasParticipation(@NotNull Player player) {
        return players.containsKey(player);
    }

    /**
     * 試合を開始
     *
     * @return 開始できたかどうか
     */
    public boolean start() {

        // 試合開始前状態以外は開始不可
        if (getStatus() != Status.NONE) {
            return false;
        }

        // マップの準備中、もしくは読み込み失敗の場合は開始不可
        if (!this.matchMapInstance.isReady() || this.matchMapInstance.isLoadFailed()) {
            return false;
        }

        changeStatus(Status.STARTED);

        // 参加中のプレイヤーに開始処理
        for (Player player : this.players.keySet()) {
            playerStart(player);
        }

        broadcast(MATCH_START_MESSAGE);

        SLUtils.getLogger().info(String.format("試合(%s)が開始しました", getId()));
        return true;
    }

    /**
     * プレイヤーの開始処理
     *
     * @param player プレイヤー
     */
    protected void playerStart(@NotNull Player player) {
        PlayerData playerData = getPlayerData(player);

        // ゲームモードを変更
        if (playerData != null) {
            playerData.setPreGameMode(player.getGameMode());
        }
        player.setGameMode(GameMode.ADVENTURE);


        // プレイヤーを試合用ワールドにテレポート
        if (this.matchMapInstance.isReady()) {
            if (!teleportToJoin(player)) {
                SLUtils.reportError(new RuntimeException(String.format("プレイヤー(%s)をスポーンさせることができませんでした", player.getName())));
            }
        } else {
            SLUtils.reportError(new RuntimeException("ワールドの用意が出来ていません"));
        }

        // 開始音
        Audience.audience(player).playSound(Sound.sound(org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP.key(), Sound.Source.MASTER, 1, 0.1f));
    }

    /**
     * 試合開始時のテレポート処理
     *
     * @param player プレイヤー
     * @return 結果
     */
    private boolean teleportToJoin(@NotNull Player player) {
        Location respawnPoint = lotterySpawnLocation(player);
        if (respawnPoint != null) {
            // 死亡している場合は強制リスポーン
            if (player.isDead()) {
                player.spigot().respawn();
            }

            player.teleport(respawnPoint);
            return true;
        }

        return false;
    }

    /**
     * 試合を終了
     *
     * @return 終了できたかどうか
     */
    public boolean finish() {

        // 試合中以外は終了不可
        if (getStatus() != Status.STARTED) {
            return false;
        }

        changeStatus(Status.FINISHED);

        // 終了音
        allPlayerAudience()
                .playSound(Sound.sound(org.bukkit.Sound.BLOCK_ANVIL_PLACE.key(), Sound.Source.MASTER, 1, 0.5f));

        matchEnd();

        broadcast(MATCH_FINISH_MESSAGE);

        SLUtils.getLogger().info(String.format("試合(%s)が終了しました", getId()));
        return true;
    }


    /**
     * 試合終了処理
     */
    protected abstract void matchEnd();

    /**
     * 試合を破棄するフラグを立てる
     */
    public void destroy() {
        // お兄さん許して、試合が壊れるなんて嫌よ
        // こわれちゃ＾～う

        this.destroyed = true;
    }

    /**
     * 破棄時の処理
     */
    protected void dispose() {
        changeStatus(Status.DISCARDED);

        // 全プレイヤー退出
        List<Player> leavePlayers = ImmutableList.copyOf(this.players.keySet());
        leavePlayers.forEach(player -> leave(player, false));

        // カウントダウン用ボスバーの表示を全プレイヤーから消す
        List<Player> bossbarViewers = Streams.stream(countDownBossbar.viewers())
                .filter(viewer -> viewer instanceof Player)
                .map(viewer -> (Player) viewer)
                .toList();
        Audience.audience(bossbarViewers).hideBossBar(countDownBossbar);

        // マップ破棄
        if (this.matchMapInstance != null) {
            this.matchMapInstance.dispose();
        }

        SLUtils.getLogger().info(String.format("試合(%s)が破棄されました", getId()));
    }

    /**
     * 予期せぬエラーが起きた時の処理
     */
    protected void unexpectedError() {
        broadcast(MATCH_CANCEL_UNEXPECTED_ERROR_MESSAGE);
    }

    /**
     * このフラグを確認して、試合マネージャーが試合の破棄を行う
     *
     * @return 破棄されるべきかどうか
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public MatchMode getMatchMode() {
        return matchMode;
    }

    @NotNull
    public Status getStatus() {
        return status;
    }

    @NotNull
    public MatchMap getMatchMap() {
        return matchMap;
    }

    private void changeStatus(@NotNull Status status) {

        // 現在のステータスと変わらないなら変更なし
        if (this.status == status) {
            return;
        }

        this.status = status;
        this.statusTick = 0;

        updateCountDownStatus();
    }

    private void updateCountDownStatus() {
        // カウントダウン用ボスバー更新
        this.countDownBossbar.color(status.getCountDownBossbarColor());
        this.countDownBossbar.name(Component.text(status.getShowName()));
    }

    private void updateCountDownTime(int compTime, int totalTime) {
        // カウントダウン用ボスバー更新
        float progress = FNMath.clamp((float) compTime / (float) totalTime, 0, 1);
        this.countDownBossbar.progress(1f - progress);

        int remnantTick = totalTime - compTime;
        long tmpCountDownTime = this.countDownTime;
        this.countDownTime = Math.max(remnantTick, 0) * 50L;

        //1秒ごとに更新
        if (tmpCountDownTime / 1000L != this.countDownTime / 1000L) {
            dirtyAllInfo();
        }

        if (status == Status.NONE || status == Status.STARTED) {
            // カウントダウン音
            int remnantSecond = remnantTick / 20;
            if (remnantTick >= 0 && COUNT_START_REMNANT_SECOND >= remnantSecond && remnantTick % 20 == 0) {
                allPlayerAudience()
                        .playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_HARP.key(), Sound.Source.MASTER, 1, 0.5f));
            }
        }

    }

    /**
     * リストに情報説明の追加を行う
     *
     * @param componentList 情報説明コンポーネントのリスト
     */
    public void appendInfoDesc(@NotNull List<Component> componentList) {
        componentList.add(Component.text("モード: ").append(Component.text(matchMode.name())));
        componentList.add(Component.text("参加人数: ").append(Component.text(String.format("%d/%d", players.size(), matchMode.maxPlayerCount()))));
        componentList.add(Component.text("状態: ").append(getStatus().getName()));
    }

    /**
     * 参加中のプレイヤー全てにメッセージを送信
     *
     * @param component メッセージ
     */
    public void broadcast(@NotNull Component component) {
        allPlayerAudience().sendMessage(component);
    }

    /**
     * 一部のプレイヤー以外にメッセージを送信
     *
     * @param component                メッセージ
     * @param exclusionPlayerPredicate プレイヤーフィルター
     */
    public void broadcast(@NotNull Component component, @NotNull Predicate<Player> exclusionPlayerPredicate) {
        Audience.audience(players.keySet().stream()
                .filter(exclusionPlayerPredicate)
                .toList()).sendMessage(component);
    }


    /**
     * この試合に参加している全てのプレイヤーに向けたオーディエンスを取得
     *
     * @return オーディエンス
     */
    public Audience allPlayerAudience() {
        return Audience.audience(players.keySet());
    }

    /**
     * プレイヤーのスポーン地点を抽選<br/>
     * スポーン地点を定義していない場合はnullを返す
     *
     * @param player プレイヤー
     * @return 場所
     */
    @Nullable
    public Location lotterySpawnLocation(@NotNull Player player) {

        // マップが読み込み済みならばスポーン地点を抽選する
        if (this.matchMapInstance.getMapWorld().isPresent()) {
            MatchMapWorld matchMapWorld = this.matchMapInstance.getMapWorld().get();
            World world = matchMapWorld.getWorld();
            MapMarker spawnMarker = getSpawnMaker(matchMapWorld, player);

            // スポーン地点のマーカーを取得できない場合はnullを返す
            if (spawnMarker == null) {
                return null;
            }

            BlockVector3 spawnPos = matchMapWorld.offsetCorrection(spawnMarker.getPosition());

            Location location = new Location(world, spawnPos.getX() + 0.5f, spawnPos.getY(), spawnPos.getZ() + 0.5f);
            location.setDirection(spawnMarker.getDirection().getDirection());

            return location;
        }

        return null;
    }


    /**
     * スポーン地点のマーカーを取得
     *
     * @param matchMapWorld 試合用ワールド
     * @param player        プレイヤー
     * @return マーカー
     */
    @Nullable
    protected abstract MapMarker getSpawnMaker(@NotNull MatchMapWorld matchMapWorld, @NotNull Player player);

    /**
     * 指定されたプレイヤーが無敵かどうか
     *
     * @param player プレイヤー
     * @return 無敵かどうか
     */
    public boolean isInvinciblePlayer(@NotNull Player player) {
        if (status == Status.FINISHED) {
            // 試合終了後、試合ワールドにいる場合は無敵
            Optional<MatchMapWorld> matchMapWorld = matchMapInstance.getMapWorld();
            return matchMapWorld.isPresent() && matchMapWorld.get().getWorld() == player.getWorld();
        } else if (status == Status.STARTED) {
            // 試合中、スポーン直後は無敵
            PlayerData playerData = getPlayerData(player);

            if (playerData != null) {
                int invTick = SLUtils.toTick(TimeUnit.MILLISECONDS, SPAWN_INVINCIBILITY_TIME);
                return playerData.getLifeTime() <= invTick;
            }

            return false;
        }
        return false;
    }

    public MatchMapInstance getMatchMapInstance() {
        return matchMapInstance;
    }

    /**
     * プレイヤー死亡時に呼び出し
     *
     * @param target 死亡したプレイヤー
     */
    public void onDeath(@NotNull Player target) {
        PlayerData targetData = players.get(target);

        if (targetData != null) {
            targetData.setDeathCount(targetData.getDeathCount() + 1);
            targetData.setKillStreakCount(0);
            targetData.setLifeTime(0);
        }

        Player attacker = target.getKiller();
        if (attacker != null && players.containsKey(attacker)) {
            onPlayerKill(target, attacker);
        }
    }

    /**
     * プレイヤーキル時に呼び出し
     *
     * @param target   対象
     * @param attacker 攻撃者
     */
    protected void onPlayerKill(@NotNull Player target, @NotNull Player attacker) {
        PlayerData attackerData = players.get(attacker);

        if (attackerData != null) {
            attackerData.setKillCount(attackerData.getKillCount() + 1);
            attackerData.setKillStreakCount(attackerData.getKillStreakCount() + 1);
        }
    }

    /**
     * 情報表示を更新するフラグを立てる
     */
    protected void dirtyAllInfo() {
        this.dirtyAllInfo = true;
    }

    /**
     * プレイヤーごとのデータ
     *
     * @author MORIMORI0317
     */
    public class PlayerData {

        /**
         * このデータのプレイヤー
         */
        @NotNull
        private final Player player;

        /**
         * プレイヤーごとの試合用スコアボード<br/>
         */
        private final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        /**
         * 情報表示用サイドバーのディスプレイ
         */
        @NotNull
        private final SidebarDisplay infoSidebarDisplay;

        /**
         * 試合用スコアボードにセットされる前のスコアボード
         */
        private Scoreboard preScoreboard;

        /**
         * 試合開始前のゲームモード
         */
        private GameMode preGameMode;

        /**
         * キル数
         */
        private int killCount;

        /**
         * 連続キル数
         */
        private int killStreakCount;

        /**
         * 死亡数
         */
        private int deathCount;

        /**
         * 情報表示の更新が必要であるフラグ
         */
        private boolean dirtyInfo;

        /**
         * 生存時間 (Tick)
         */
        private int lifeTime;

        /**
         * コンストラクタ
         *
         * @param player プレイヤー
         */
        protected PlayerData(@NotNull Player player) {
            this.player = player;
            Objective sidebarObjective = scoreboard.registerNewObjective("sidebar-info", Criteria.DUMMY,
                    Component.text("試合情報").style(Style.style().color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD).build()));
            sidebarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
            this.infoSidebarDisplay = new SidebarDisplay(sidebarObjective);
        }

        /**
         * サイドバー情報の確認と更新
         */
        public void updateCheckAndInfo() {
            // 表示更新フラグの確認とリセット
            if (!(this.dirtyInfo || Match.this.dirtyAllInfo)) {
                return;
            }
            this.dirtyInfo = false;

            updateInfo();
        }

        /**
         * 表示情報を更新
         */
        protected void updateInfo() {
            // サイドバーを更新
            List<String> sidebarInfos = new LinkedList<>();
            appendSidebarInfo(sidebarInfos);
            this.infoSidebarDisplay.update(sidebarInfos);
        }

        /**
         * サイドバー情報として、表示する文字列をリストに追加する
         *
         * @param sidebarInfos 情報表示の文字列リスト
         */
        protected void appendSidebarInfo(@NotNull List<String> sidebarInfos) {
            appendSidebarMatchInfo(sidebarInfos);
            sidebarInfos.add("");
            appendSidebarPlayerInfo(sidebarInfos);
        }

        /**
         * 試合関係の情報をサイドバーに追加
         *
         * @param sidebarInfos サイドバー情報の文字列リスト
         */
        protected void appendSidebarMatchInfo(@NotNull List<String> sidebarInfos) {
            sidebarInfos.add(String.format("モード: %s", Match.this.matchMode.name()));
            sidebarInfos.add(String.format("状態: %s", Match.this.getStatus().getShowName()));

            String mapText = matchMap.name();

            if (!matchMapInstance.isReady()) {
                mapText += "(読み込み中)";
            }

            sidebarInfos.add(String.format("マップ: %s", mapText));
            sidebarInfos.add(String.format("参加人数: %d/%d", players.size(), matchMode.maxPlayerCount()));
            sidebarInfos.add(String.format("残り時間: %s", FNStringUtil.getTimeFormat(Match.this.countDownTime)));
        }

        /**
         * プレイヤー関係の情報をサイドバーに追加
         *
         * @param sidebarInfos サイドバー情報の文字列リスト
         */
        protected void appendSidebarPlayerInfo(@NotNull List<String> sidebarInfos) {
            sidebarInfos.add(String.format("キル数: %s", this.killCount));
            sidebarInfos.add(String.format("連続キル数: %s", this.killStreakCount));
            sidebarInfos.add(String.format("死亡数: %s", this.deathCount));
        }

        public Scoreboard getPreScoreboard() {
            return preScoreboard;
        }

        public void setPreScoreboard(Scoreboard preScoreboard) {
            this.preScoreboard = preScoreboard;
        }

        public Scoreboard getScoreboard() {
            return scoreboard;
        }

        public int getDeathCount() {
            return deathCount;
        }

        /**
         * 死亡数セットと表示更新フラグを立てる
         *
         * @param deathCount 死亡数
         */
        public void setDeathCount(int deathCount) {
            this.deathCount = deathCount;
            dirtyInfo();
        }

        public int getKillCount() {
            return killCount;
        }

        /**
         * キル数セットと表示更新フラグを立てる
         *
         * @param killCount キル数
         */
        public void setKillCount(int killCount) {
            this.killCount = killCount;
            dirtyInfo();
        }

        public int getKillStreakCount() {
            return killStreakCount;
        }

        /**
         * 連続キル数セットと表示更新フラグを立てる
         *
         * @param killStreakCount 連続キル数
         */
        public void setKillStreakCount(int killStreakCount) {
            this.killStreakCount = killStreakCount;
            dirtyInfo();
        }

        /**
         * 情報表示を更新するフラグを立てる
         */
        protected void dirtyInfo() {
            this.dirtyInfo = true;
        }

        public @NotNull Player getPlayer() {
            return player;
        }

        public int getLifeTime() {
            return lifeTime;
        }

        public void setLifeTime(int lifeTime) {
            this.lifeTime = lifeTime;
        }

        public GameMode getPreGameMode() {
            return preGameMode;
        }

        public void setPreGameMode(GameMode preGameMode) {
            this.preGameMode = preGameMode;
        }
    }

    /**
     * 試合の状態
     *
     * @author MORIMORI0317
     */
    public enum Status {

        /**
         * 開始前
         */
        NONE(Component.text("無し"), "試合開始待ち", BossBar.Color.YELLOW),

        /**
         * 開始
         */
        STARTED(Component.text("開始"), "試合中", BossBar.Color.GREEN),

        /**
         * 終了
         */
        FINISHED(Component.text("終了"), "試合終了", BossBar.Color.BLUE),

        /**
         * 破棄済み
         */
        DISCARDED(Component.text("破棄"), "破棄済み", BossBar.Color.RED);

        /**
         * 状態名
         */
        private final Component name;

        /**
         * 表示名
         */
        private final String showName;

        /**
         * カウントダウン用ボスバーの色
         */
        private final BossBar.Color countDownBossbarColor;

        /**
         * コンストラクタ
         *
         * @param name                  状態名
         * @param showName              カウントダウン用ボスバーの表示名
         * @param countDownBossbarColor カウントダウン用ボスバーの色
         */
        Status(Component name, String showName, BossBar.Color countDownBossbarColor) {
            this.name = name;
            this.showName = showName;
            this.countDownBossbarColor = countDownBossbarColor;
        }

        public Component getName() {
            return name;
        }

        public BossBar.Color getCountDownBossbarColor() {
            return countDownBossbarColor;
        }

        public String getShowName() {
            return showName;
        }
    }
}
