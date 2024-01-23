package dev.felnull.shortlifeplugin.match;

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.math.BlockVector3;
import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.match.map.*;
import dev.felnull.shortlifeplugin.utils.MatchUtils;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static dev.felnull.shortlifeplugin.match.MatchMessageComponents.*;
import static dev.felnull.shortlifeplugin.match.MatchStatus.*;
import static org.bukkit.Sound.BLOCK_ANVIL_PLACE;
import static org.bukkit.Sound.BLOCK_NOTE_BLOCK_XYLOPHONE;

/**
 * 試合インスタンスクラス
 *
 * @author MORIMORI0317, Quarri6343
 */
public abstract class Match {

    /**
     * スポーン後の無敵時間(ms)
     */
    public static final long SPAWN_INVINCIBILITY_TIME = 1000 * 5;

    /**
     * ランダム
     */
    protected static final Random RANDOM = new Random();

    /**
     * 試合終了後にテレポートするまでの時間
     */
    protected static final long FINISH_WAIT_FOR_TELEPORT = 1000 * 10;

    /**
     * 試合が開始するまでの時間(ms)
     */
    private static final long START_WAIT_TIME = 1000 * 10;

    /**
     * 試合終了から破棄されるまでの時間(ms)
     */
    private static final long DISPOSE_WAIT_TIME = FINISH_WAIT_FOR_TELEPORT + (1000 * 5);

    /**
     * カウントを開始する残り秒数
     */
    private static final int COUNT_START_REMNANT_SECOND = 5;

    /**
     * 無効な時刻表示テキスト
     */
    private static final String TIME_DISPLAY_NONE_TEXT = "--:--";

    /**
     * マップ投票中の状態テキスト
     */
    private static final String MAP_VOTE_STATUS_TEXT = MsgHandler.get("match-status-display-map-vote");

    /**
     * 参加しているプレイヤーとプレイヤー情報
     */
    protected final Map<Player, PlayerInfo> players = new HashMap<>();

    /**
     * 同じ状態の経過Tick
     */
    protected int statusTick;

    /**
     * 試合用マップ<br/>
     * マップが未決定の時はnull
     */
    @Nullable
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
     * カウントダウン表示用ボスバー
     */
    private final MatchCountDownBossBar countDownBossbar = new MatchCountDownBossBar();

    /**
     * 試合マップ選択処理用クラスインスタンス
     */
    private final MapSelector mapSelector = new MapSelector(this);

    /**
     * 状態
     */
    @NotNull
    private MatchStatus status = NONE;

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
    private MatchCountDownTime countDownTime = new MatchCountDownTime();

    /**
     * 情報表示の更新が必要であるフラグ
     */
    private boolean dirtyAllInfo;

    /**
     * 試合が開始してからTick処理を行ったフラグ
     */
    private boolean ticked;

    /**
     * 試合開始までの残り時間<br/>
     * カウントダウンを行っていない時は-1
     */
    private int startRemainingTick = -1;

    /**
     * コンストラクタ
     *
     * @param id        試合ID
     * @param matchMode 試合モード
     */
    protected Match(@NotNull String id, @NotNull MatchMode matchMode) {
        this.id = id;
        this.matchMode = matchMode;
    }

    /**
     * 初期化処理
     */
    protected void init() {
        updateBossbar();
        SLUtils.getLogger().info(MsgHandler.getFormatted("match-created", getId()));
    }

    /**
     * 1Tickごとの処理
     */
    protected void tick() {

        this.ticked = true;

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


        // プレイヤーごとのTick処理
        this.players.forEach((player, playerInfo) -> playerInfo.tick(getStatus(), dirtyAllInfo, this::appendSidebarMatchInfo));

        Match.this.dirtyAllInfo = false;
    }

    /**
     * 試合開始前のTick処理
     */
    protected void beforeStartTick() {
        MatchManager matchManager = MatchManager.getInstance();
        MatchMapHandler mapHandler = matchManager.getMapHandler();

        this.mapSelector.tick();

        MatchMap selectedMatchMap = this.mapSelector.getSelectedMatchMap();
        if (this.matchMapInstance == null && selectedMatchMap != null) {
            /* マップが未読み込み時に選定されたマップが存在する場合 */

            // マップインスタンスを作成
            this.matchMapInstance = mapHandler.createMapInstance(this, this.id, selectedMatchMap);

            this.players.keySet().forEach(this::sendMapInfoMessage);

            this.countDownBossbar.progress(0, 1);
            updateBossbar();

            // マップ決定音
            allPlayerAudience()
                    .playSound(Sound.sound(BLOCK_NOTE_BLOCK_XYLOPHONE.key(), Sound.Source.MASTER, 1, 0.5f));
        }

        // マップの読み込みに失敗した場合は試合破棄
        if (this.matchMapInstance != null && this.matchMapInstance.isLoadFailed()) {
            broadcast(MATCH_CANCEL_FAILED_LOAD_WORLD_MESSAGE.get());

            Optional<Throwable> mapWorldError = this.matchMapInstance.getMapWordLoadError();

            if (mapWorldError.isPresent()) {
                SLUtils.reportError(mapWorldError.get(), MsgHandler.get("system-map-load-failed"));
            } else {
                SLUtils.reportError(new RuntimeException(MsgHandler.get("system-map-load-failed")));
            }

            destroy();
            return;
        }

        // カウントダウン処理
        updateCountDownTime(this.statusTick, SLUtils.toTick(TimeUnit.MILLISECONDS, START_WAIT_TIME), false);

        int startWaitTick = SLUtils.toTick(TimeUnit.MILLISECONDS, START_WAIT_TIME);

        // ボスバー処理
        if (this.matchMapInstance == null) {
            /* マップ決定前 */
            int deadlineTime = this.mapSelector.getDeadlineTime();

            if (deadlineTime >= 0) {
                int deadlineTotalTick = SLUtils.toTick(TimeUnit.MILLISECONDS, MapSelector.SELECTED_DEADLINE_TIME);
                this.countDownBossbar.progress(deadlineTotalTick - deadlineTime, deadlineTotalTick);
            } else {
                this.countDownBossbar.progress(0, 1);
            }
        } else {
            /* マップ決定後 */
            if (this.startRemainingTick >= 0) {
                this.countDownBossbar.progress(startWaitTick - this.startRemainingTick, startWaitTick);
            } else {
                this.countDownBossbar.progress(0, 1);
            }
        }

        // マップの読み込みが終わった場合
        if (!loadMapCompletion && this.matchMapInstance != null && this.matchMapInstance.isReady()) {
            loadMapCompletion = true;
            dirtyAllInfo();
        }

        // 試合開始カウントダウン処理
        int preStartRemainingTick = this.startRemainingTick;

        if (this.matchMapInstance != null && this.players.size() >= this.getMatchMode().minPlayerCount()) {
            /* マップが決定済みで、最低参加人数を超えていれば */
            if (this.startRemainingTick == -1) {
                this.startRemainingTick = startWaitTick;
            } else {
                this.startRemainingTick--;
            }
        } else {
            this.startRemainingTick = -1;
        }

        boolean sidebarDirty = false;

        if (this.startRemainingTick != preStartRemainingTick) {
            if (this.startRemainingTick <= -1) {
                sidebarDirty = preStartRemainingTick > -1;
            } else if (preStartRemainingTick <= -1) {
                sidebarDirty = true;
            } else if (this.startRemainingTick / 20 != preStartRemainingTick / 20) {
                sidebarDirty = true;
            }
        }

        if (sidebarDirty) {
            this.dirtyAllInfo();
        }

        // 試合開始処理
        boolean startFlag = false;

        if (this.startRemainingTick == 0) {
            // 開始待機時間が過ぎた場合の処理
            if (this.matchMapInstance != null && this.matchMapInstance.isReady()) {
                // マップの準備が終わっていれば試合開始
                startFlag = true;
            } else if (!waitLoadMapNotified) {
                // マップの準備が終わっていなければ、1度だけ通知
                waitLoadMapNotified = true;
                broadcast(MATCH_WAIT_LOAD_WORLD_MESSAGE.get());
            }
        }

        if (startFlag && !start()) {
            SLUtils.reportError(new RuntimeException(MsgHandler.get("system-match-cannot-start")));
            destroy();
        }
    }

    /**
     * 試合中のTick処理
     */
    protected void duringMatchTick() {

        boolean finishFlag = false;

        int totalTime = SLUtils.toTick(TimeUnit.MILLISECONDS, getMatchMode().limitTime());
        updateCountDownTime(this.statusTick, totalTime, true);

        if (players.size() < getMatchMode().minPlayerCount()) {
            // 参加プレイヤーの人数が、最低参加人数より少なければ試合を終了
            broadcast(MATCH_FINISH_INSUFFICIENT_PLAYER_MESSAGE.get());
            finishFlag = true;
        } else if (this.statusTick >= totalTime) {
            // 試合制限時間を過ぎた場合、試合を終了
            finishFlag = true;
        }

        if (finishFlag && !finish()) {
            SLUtils.reportError(new RuntimeException(MsgHandler.get("system-match-cannot-finish")));
            destroy();
        }
    }

    /**
     * 試合終了後のTick処理
     */
    protected void afterFinishTick() {

        int totalTime = SLUtils.toTick(TimeUnit.MILLISECONDS, FINISH_WAIT_FOR_TELEPORT);
        updateCountDownTime(this.statusTick, totalTime, true);

        // 終了後のテレポート
        if (!finishTeleport && this.statusTick >= totalTime) {
            finishTeleport = true;

            if (this.matchMapInstance != null) {
                Optional<World> leaveWorld = this.matchMapInstance.getMapWorld().map(MatchMapWorld::getWorld);
                players.keySet()
                        .forEach(player -> MatchUtils.teleportToLeave(player, leaveWorld));
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

        if (this.status != NONE) {

            if (this.matchMapInstance != null) {
                Optional<MatchMapWorld> matchMapWorld = this.matchMapInstance.getMapWorld();

                return matchMapWorld.isEmpty() || matchMapWorld.get().getWorld() == player.getWorld();
            }

            return false;
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
        if (MatchManager.getInstance().getJoinedMatch(player).isPresent()) {
            return false;
        }

        // 開始前、試合中以外は参加不可
        if (status != NONE && status != STARTED) {
            return false;
        }

        PlayerInfo playerInfo = createPlayerInfo(player);
        this.players.put(player, playerInfo);

        // プレイヤーのスコアボードを試合用スコアボードに変更
        playerInfo.setPreScoreboard(player.getScoreboard());
        player.setScoreboard(playerInfo.getScoreboard());

        // 試合が既に開始しているならば開始処理
        if (status == STARTED) {
            playerStart(player);
        }

        countDownBossbar.show(player);

        if (sendMessage) {
            player.sendMessage(MATCH_JOIN_MESSAGE.get());
        }

        player.playSound(Sound.sound(org.bukkit.Sound.UI_BUTTON_CLICK.key(), Sound.Source.MASTER, 1, 1.1f));

        broadcast(
                Component.text(MsgHandler.getFormatted("match-other-player-joined", player.getName())).color(NamedTextColor.WHITE),
                pl -> pl != player);

        if (this.status == NONE && this.mapSelector.getSelectedMatchMap() == null) {
            // FIXME マップ抽選の仮置き
            player.sendMessage(Component.text("マップ投票:"));

            MatchManager.getInstance().getMapHandler().getAvailableMaps(this.getMatchMode()).forEach(matchMap -> {
                Component selectorText = Component.text(String.format("[%s]", matchMap.id())).clickEvent(ClickEvent.callback(audience -> {
                    playerInfo.getMapSelectorInfo().setVotedMatchMap(matchMap);
                    player.sendMessage(Component.text("投票しました: " + matchMap.id()));
                }));
                player.sendMessage(selectorText);
            });
        }

        // マップ通知
        if (this.status == NONE && this.matchMapInstance != null) {
            sendMapInfoMessage(player);
        }

        playerInfo.updateInfo(this::appendSidebarMatchInfo);

        SLUtils.getLogger().info(MsgHandler.getFormatted("system-match-joined", player.getName(), getId()));

        return true;
    }

    /**
     * プレイヤー情報を作成
     *
     * @param player プレイヤー
     * @return プレイヤー情報
     */
    protected PlayerInfo createPlayerInfo(@NotNull Player player) {
        return new PlayerInfo(player);
    }

    /**
     * 指定されたプレイヤーの情報を取得
     *
     * @param player プレイヤー
     * @return プレイヤー情報
     */
    public Optional<PlayerInfo> getPlayerInfo(@NotNull Player player) {
        return Optional.of(this.players.get(player));
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

        PlayerInfo playerInfo = players.get(player);

        // 試合用スコアボードを参加前のスコアボードに戻す
        Scoreboard preScoreboard = playerInfo.getPreScoreboard();
        if (preScoreboard == null) {
            preScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        }
        player.setScoreboard(preScoreboard);

        this.players.remove(player);

        countDownBossbar.hide(player);

        // ゲームモードを元に戻す
        GameMode preGameMode = playerInfo.getPreGameMode();
        if (preGameMode != null) {
            player.setGameMode(preGameMode);
        }

        // 試合用ワールドにいる場合、ワールド外にテレポート
        if (this.matchMapInstance != null) {
            Optional<MatchMapWorld> matchMapWorld = this.matchMapInstance.getMapWorld();
            if (matchMapWorld.isPresent() && matchMapWorld.get().getWorld() == player.getWorld()) {
                MatchUtils.teleportToLeave(player, matchMapWorld.map(MatchMapWorld::getWorld));
            }
        }

        if (sendMessage) {
            player.sendMessage(MATCH_LEAVE_MESSAGE.get());
            broadcast(
                    Component.text(MsgHandler.getFormatted("match-other-player-left", player.getName())).color(NamedTextColor.WHITE),
                    pl -> pl != player);
        }

        player.playSound(Sound.sound(org.bukkit.Sound.UI_BUTTON_CLICK.key(), Sound.Source.MASTER, 1, 0.9f));

        SLUtils.getLogger().info(MsgHandler.getFormatted("system-match-left", player.getName(), getId()));
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
        if (getStatus() != NONE) {
            return false;
        }

        // マップが未決定、準備中、もしくは読み込み失敗の場合は開始不可
        if (this.matchMapInstance == null || !this.matchMapInstance.isReady() || this.matchMapInstance.isLoadFailed()) {
            return false;
        }

        changeStatus(STARTED);

        // 参加中のプレイヤーに開始処理
        this.players.keySet().forEach(this::playerStart);

        broadcast(MATCH_START_MESSAGE.get());

        SLUtils.getLogger().info(MsgHandler.getFormatted("system-match-started", getId()));
        return true;
    }

    /**
     * プレイヤーの開始処理
     *
     * @param player プレイヤー
     */
    protected void playerStart(@NotNull Player player) {
        Optional<PlayerInfo> playerInfo = getPlayerInfo(player);

        // ゲームモードを変更
        playerInfo.ifPresent(playerInfo1 -> playerInfo1.setPreGameMode(player.getGameMode()));
        player.setGameMode(GameMode.ADVENTURE);

        // 体力全回復
        AttributeInstance healthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            double maxHealth = healthAttribute.getValue();
            player.setHealth(maxHealth);
        }

        // 食料値全回復
        player.setFoodLevel(20);
        player.setSaturation(20);

        // エフェクト除去
        Collection<PotionEffect> potionEffects = player.getActivePotionEffects();
        potionEffects.forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));

        // プレイヤーを試合用ワールドにテレポート
        if (this.matchMapInstance != null && this.matchMapInstance.isReady()) {
            if (!teleportToJoin(player)) {
                SLUtils.reportError(new RuntimeException(MsgHandler.getFormatted("system-player-cannot-spawn", player.getName())));
            }
        } else {
            SLUtils.reportError(new RuntimeException(MsgHandler.get("system-world-is-not-ready")));
        }

        // スポーン保護指定
        playerInfo.ifPresent(playerInfo1 -> playerInfo1.setSpawnProtectTime(SLUtils.toTick(TimeUnit.MILLISECONDS, SPAWN_INVINCIBILITY_TIME)));

        // 開始音
        player.playSound(Sound.sound(org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP.key(), Sound.Source.MASTER, 1, 0.1f));
    }

    /**
     * 試合開始時のテレポート処理
     *
     * @param player プレイヤー
     * @return 結果
     */
    private boolean teleportToJoin(@NotNull Player player) {
        return lotterySpawnLocation(player).map(respawnPoint -> {
            // 死亡している場合は強制リスポーン
            if (player.isDead()) {
                player.spigot().respawn();
            }

            player.teleport(respawnPoint);
            return true;
        }).orElse(false);
    }

    /**
     * 試合を終了
     *
     * @return 終了できたかどうか
     */
    public boolean finish() {

        // 試合中以外は終了不可
        if (getStatus() != STARTED) {
            return false;
        }

        changeStatus(FINISHED);

        // 終了音
        allPlayerAudience()
                .playSound(Sound.sound(BLOCK_ANVIL_PLACE.key(), Sound.Source.MASTER, 1, 0.5f));

        matchEnd();

        broadcast(MATCH_FINISH_MESSAGE.get());

        SLUtils.getLogger().info(MsgHandler.getFormatted("system-match-ended", getId()));
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
        changeStatus(MatchStatus.DISCARDED);

        // 全プレイヤー退出
        List<Player> leavePlayers = ImmutableList.copyOf(this.players.keySet());
        leavePlayers.forEach(player -> leave(player, false));

        countDownBossbar.hideAll();

        // マップ破棄
        if (this.matchMapInstance != null) {
            this.matchMapInstance.dispose();
        }

        SLUtils.getLogger().info(MsgHandler.getFormatted("system-match-discarded", getId()));
    }

    /**
     * 予期せぬエラーが起きた時の処理
     */
    protected void unexpectedError() {
        broadcast(MATCH_CANCEL_UNEXPECTED_ERROR_MESSAGE.get());
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
    public MatchStatus getStatus() {
        return status;
    }

    /**
     * 試合マップを取得
     *
     * @return 試合マップ
     */
    @Nullable
    public MatchMap getMatchMap() {
        if (this.matchMapInstance != null) {
            return this.matchMapInstance.getMatchMap();
        } else {
            return null;
        }
    }

    private void changeStatus(@NotNull MatchStatus matchStatus) {

        // 現在のステータスと変わらないなら変更なし
        if (this.status == matchStatus) {
            return;
        }

        this.status = matchStatus;
        this.statusTick = 0;

        updateBossbar();
    }

    private void updateBossbar() {
        if (this.status == NONE && this.matchMapInstance == null) {
            /* マップ選択期間 */
            this.countDownBossbar.updateTextAndColor(BossBar.Color.WHITE, MAP_VOTE_STATUS_TEXT);
        } else {
            this.countDownBossbar.updateCountDownStatus(this.status);
        }
    }

    private void updateCountDownTime(int compTime, int totalTime, boolean updateBossbar) {

        if (updateBossbar) {
            this.countDownBossbar.progress(compTime, totalTime);
        }

        int remnantTick = getRemnantTick(compTime, totalTime);
        countDownTime.update(remnantTick);
        if (countDownTime.shouldDirtyDisplay()) {
            dirtyAllInfo();
        }

        // カウントダウン演出処理
        if (this.status == NONE || this.status == STARTED) {
            int countDownTick;

            // 試合前の場合のみ、開始までの残り時間でカウントダウンを確認する
            if (this.status == NONE) {
                countDownTick = this.startRemainingTick;
            } else {
                countDownTick = remnantTick;
            }

            // カウントダウン音
            int countDownSecond = countDownTick / 20;
            if (countDownTick >= 0 && COUNT_START_REMNANT_SECOND >= countDownSecond && countDownTick % 20 == 0) {
                allPlayerAudience()
                        .playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_HARP.key(), Sound.Source.MASTER, 1, 0.5f));
            }
        }

    }

    private int getRemnantTick(int compTime, int totalTime) {
        return totalTime - compTime;
    }

    /**
     * リストに情報説明の追加を行う
     *
     * @param componentList 情報説明コンポーネントのリスト
     */
    public void appendInfoDesc(@NotNull List<Component> componentList) {
        componentList.add(Component.text(MsgHandler.get("match-mode")).append(Component.text(matchMode.name())));
        componentList.add(Component.text(MsgHandler.get("match-players"))
                .append(Component.text(MsgHandler.getFormatted("match-players-count", players.size(), matchMode.maxPlayerCount()))));
        componentList.add(Component.text(MsgHandler.get("match-stats")).append(getStatus().getName()));
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
    public Optional<Location> lotterySpawnLocation(@NotNull Player player) {

        if (this.matchMapInstance == null) {
            return Optional.empty();
        }

        return this.matchMapInstance.getMapWorld().flatMap(matchMapWorld ->
                // マップが読み込み済みならばスポーン地点を抽選する
                getSpawnMaker(matchMapWorld, player).map(mapMarker -> {
                    BlockVector3 spawnPos = matchMapWorld.offsetCorrection(mapMarker.getPosition());

                    Location unSetLocation = new Location(matchMapWorld.getWorld(), spawnPos.getX() + 0.5f, spawnPos.getY(), spawnPos.getZ() + 0.5f);
                    unSetLocation.setDirection(mapMarker.getDirection().getDirection());
                    return Optional.of(unSetLocation);
                }).orElseGet(Optional::empty));
    }


    /**
     * スポーン地点のマーカーを取得
     *
     * @param matchMapWorld 試合用ワールド
     * @param player        プレイヤー
     * @return マーカー
     */
    protected abstract Optional<MapMarker> getSpawnMaker(@NotNull MatchMapWorld matchMapWorld, @NotNull Player player);

    /**
     * 指定されたプレイヤーが無敵かどうか
     *
     * @param player プレイヤー
     * @return 無敵かどうか
     */
    public boolean isInvinciblePlayer(@NotNull Player player) {
        if (status == FINISHED) {
            /* 試合終了後 */
            // 試合ワールドにいる場合は無敵
            Optional<MatchMapWorld> matchMapWorld = matchMapInstance.getMapWorld();
            return matchMapWorld.isPresent() && matchMapWorld.get().getWorld() == player.getWorld();
        } else if (status == STARTED) {
            /* 試合中 */
            // スポーン保護状態は無敵
            return getPlayerInfo(player).map(PlayerInfo::isSpawnProtect).orElse(false);
        }
        return false;
    }

    public @Nullable MatchMapInstance getMatchMapInstance() {
        return matchMapInstance;
    }

    /**
     * リスポーン時に呼び出し
     *
     * @param player プレイヤー
     */
    public void onRespawn(@NotNull Player player) {
        // スポーン保護指定
        getPlayerInfo(player).ifPresent(
                playerInfo -> playerInfo.setSpawnProtectTime(SLUtils.toTick(TimeUnit.MILLISECONDS, SPAWN_INVINCIBILITY_TIME)));
    }

    /**
     * プレイヤー死亡時に呼び出し
     *
     * @param target 死亡したプレイヤー
     */
    public void onPlayerDeath(@NotNull Player target) throws IOException {
        // 試合中のみ
        if (getStatus() == STARTED) {

            getPlayerInfo(target).ifPresent(targetInfo -> {
                targetInfo.setDeathCount(targetInfo.getDeathCount() + 1);

                if (targetInfo.getKillStreakCount() > targetInfo.getMaxKillStreakCount()) {
                    targetInfo.setMaxKillStreakCount(targetInfo.getKillStreakCount());
                }

                targetInfo.setKillStreakCount(0);
                targetInfo.setLifeTime(0);
                targetInfo.initRewardFlag();
            });
        }

        Player attacker = target.getKiller();
        if (attacker != null && target != attacker && players.containsKey(attacker)) {
            onPlayerKill(target, attacker);
        }
    }

    /**
     * 自分以外のプレイヤーキル時に呼び出し
     *
     * @param target   対象
     * @param attacker 攻撃者
     */
    protected void onPlayerKill(@NotNull Player target, @NotNull Player attacker) throws IOException {
        PlayerInfo attackerInfo = players.get(attacker);

        if (attackerInfo != null) {
            attackerInfo.setKillCount(attackerInfo.getKillCount() + 1);
            attackerInfo.setKillStreakCount(attackerInfo.getKillStreakCount() + 1);
            attackerInfo.giveReward();
            attacker.playSound(attacker, BLOCK_ANVIL_PLACE, 0.6f, 0.5f);

            Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(1000), Duration.ofMillis(1000));
            Title kill = Title.title(Component.empty(), Component.text("1").color(NamedTextColor.RED)
                    .append(Component.text(MsgHandler.get("match-kill")).color(NamedTextColor.GRAY)), times);
            Title killstreak = Title.title(
                    Component.empty(),
                    Component.text(attackerInfo.getKillStreakCount()).color(NamedTextColor.RED)
                            .append(Component.text(MsgHandler.get("match-kill-streak")).color(NamedTextColor.GRAY)),
                    times);


            attacker.showTitle(kill);
            new BukkitRunnable() {
                @Override
                public void run() {
                    attacker.showTitle(killstreak);
                }
            }.runTaskLater(SLUtils.getSLPlugin(), 50);

        }
    }

    /**
     * ダメージを受けた時に呼び出し
     *
     * @param target       ダメージを受けたプレイヤー
     * @param attacker     ダメージを与えたプレイヤー
     * @param damageAmount ダメージ量
     * @param damageCause  ダメージケース
     * @return falseであればダメージをキャンセル
     */
    public boolean onPlayerDamage(@NotNull Player target, @Nullable Player attacker,
                                  @SuppressWarnings("unused") double damageAmount, @NotNull EntityDamageEvent.DamageCause damageCause) {
        // 無敵とされているプレイヤーであれば、Kill以外のダメージをキャンセル
        if (damageCause != EntityDamageEvent.DamageCause.KILL && isInvinciblePlayer(target)) {
            getPlayerInfo(target).ifPresent(playerInfo -> {
                /* スポーン保護状態の場合 */
                if (playerInfo.isSpawnProtect()) {

                    // 攻撃者に警告音
                    if (attacker != null) {
                        attacker.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL.key(), Sound.Source.MASTER, 30, 1.5f + RANDOM.nextFloat() * 0.07f), target);
                    }
                }
            });

            return false;
        }

        return true;
    }

    /**
     * 情報表示を更新するフラグを立てる
     */
    protected void dirtyAllInfo() {
        this.dirtyAllInfo = true;
    }

    private void sendMapInfoMessage(Audience audience) {
        if (this.matchMapInstance != null) {

            Component mapInfoText = Component.text(MsgHandler.get("match-map-info-header")).appendNewline();
            mapInfoText = mapInfoText.append(Component.text(this.matchMapInstance.getMatchMap().name())).appendNewline();

            // TODO 作者等を表示

    /*    mapInfoText = mapInfoText.append(Component.text("作者: ").append(Component.text("N/A"))).appendNewline();
        mapInfoText = mapInfoText.append(Component.text("説明: ").append(Component.text("TEST"))).appendNewline();*/
            mapInfoText = mapInfoText.append(Component.text(MsgHandler.get("match-map-info-footer")));
            audience.sendMessage(mapInfoText);
        }
    }

    /**
     * 表示用コンポーネントを作成
     *
     * @return 表示用コンポーネント
     */
    @NotNull
    public Component createDisplayComponent() {
        return createDisplayComponent(true);
    }

    /**
     * 表示用コンポーネントを作成
     *
     * @param encircle "["と"]"で囲むかどうか
     * @return 表示用コンポーネント
     */
    @NotNull
    public Component createDisplayComponent(boolean encircle) {
        Component displayComponent;

        if (encircle) {
            displayComponent = Component.text("[")
                    .append(Component.text(this.getId()))
                    .append(Component.text("]"));
        } else {
            displayComponent = Component.text(this.getId());
        }

        List<Component> infoComponents = new LinkedList<>();
        this.appendInfoDesc(infoComponents);

        JoinConfiguration.Builder hoverTextBuilder = JoinConfiguration.builder();
        hoverTextBuilder.separator(Component.newline());
        Component hoverText = Component.join(hoverTextBuilder, infoComponents);

        displayComponent = displayComponent.hoverEvent(HoverEvent.showText(hoverText));

        return displayComponent;
    }

    /**
     * 試合関係の情報をサイドバーに追加
     *
     * @param sidebarInfos サイドバー情報のコンポーネントリスト
     */
    protected void appendSidebarMatchInfo(@NotNull List<Component> sidebarInfos) {
        sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-mode")).color(NamedTextColor.AQUA)
                .append(Component.text(Match.this.matchMode.name()).color(NamedTextColor.WHITE)));

        sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-stats")).color(NamedTextColor.AQUA)
                .append(Component.text(Match.this.getStatus().getShowName()).color(Match.this.getStatus().getColor())));

        if (this.matchMapInstance != null) {
            String mapText = this.matchMapInstance.getMatchMap().name();

            if (!this.matchMapInstance.isReady()) {
                mapText += MsgHandler.get("match-sidebar-loading");
            }

            sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-map")).color(NamedTextColor.AQUA)
                    .append(Component.text(mapText).color(NamedTextColor.WHITE)));
        }

        int participantPlayerCount = players.size();
        int participantPlayerMax = matchMode.maxPlayerCount();
        TextColor participantPlayerColor;
        // 参加人数テキストの色選定
        if (participantPlayerCount >= participantPlayerMax) {
            participantPlayerColor = NamedTextColor.RED;
        } else if (participantPlayerCount >= matchMode.minPlayerCount()) {
            participantPlayerColor = NamedTextColor.GOLD;
        } else {
            participantPlayerColor = NamedTextColor.WHITE;
        }

        sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-joined-people")).color(NamedTextColor.AQUA)
                .append(Component.text(participantPlayerCount)
                        .append(Component.text("/"))
                        .append(Component.text(participantPlayerMax))
                        .color(participantPlayerColor)));

        getSidebarTimeText().ifPresent(sidebarInfos::add);
    }

    private Optional<Component> getSidebarTimeText() {

        if (getStatus() == NONE) {
            /* 試合前 */
            if (this.matchMapInstance != null) {
                /* マップ決定後 */
                if (this.startRemainingTick >= 0) {
                    return Optional.of(Component.text(MsgHandler.get("match-sidebar-time-start")).color(NamedTextColor.AQUA)
                            .append(Component.text(getTimeDisplayText(this.startRemainingTick / 20)).color(NamedTextColor.WHITE)));
                }
            } else {
                /* マップ決定前 */
                int deadlineTime = this.mapSelector.getDeadlineTime();

                // 締め切りカウントダウンが開始していれば表示
                if (deadlineTime >= 0) {
                    return Optional.of(Component.text(MsgHandler.get("match-sidebar-time-vote")).color(NamedTextColor.AQUA)
                            .append(Component.text(getTimeDisplayText(deadlineTime / 20)).color(NamedTextColor.WHITE)));
                }
            }
        } else if (getStatus() == STARTED) {
            /* 試合中 */
            int remainingTimeSecond = this.ticked ? countDownTime.getSecond() : -1;
            TextColor remainingTimeColor;
            // 残り時間テキストの色選定
            if (remainingTimeSecond < 0 || remainingTimeSecond > 30) {
                remainingTimeColor = NamedTextColor.WHITE;
            } else if (remainingTimeSecond > 10) {
                remainingTimeColor = NamedTextColor.GOLD;
            } else {
                remainingTimeColor = (remainingTimeSecond % 2 == 0) ? NamedTextColor.RED : NamedTextColor.DARK_RED;
            }

            return Optional.of(Component.text(MsgHandler.get("match-sidebar-time-left")).color(NamedTextColor.AQUA)
                    .append(Component.text(getTimeDisplayText(remainingTimeSecond)).color(remainingTimeColor)));
        }

        return Optional.empty();
    }

    private String getTimeDisplayText(int second) {

        if (second < 0) {
            return TIME_DISPLAY_NONE_TEXT;
        }

        int minTime = second / 60;
        int secTime = second % 60;

        return String.format("%02d:%02d", minTime, secTime);
    }
}
