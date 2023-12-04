package dev.felnull.shortlifeplugin.gui.item;

import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.MatchMode;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

/**
 * 試合モード選択GUIアイテム
 *
 * @author MORIMORI0317, Quarri6343
 */
public class MatchModeSelectItem extends AbstractItem {

    /**
     * 試合を作成して参加する際のメッセージ
     */
    private static final Component CREATE_AND_JOIN_MATCH_MESSAGE = Component.text("試合を作成して参加しました").color(NamedTextColor.WHITE);

    /**
     * 試合の作成に失敗した際のメッセージ
     */
    private static final Component CREATE_MATCH_FAILURE_MESSAGE = Component.text("試合の作成に失敗しました").color(NamedTextColor.RED);

    /**
     * 利用可能なマップがない場合のメッセージ
     */
    private static final Component NO_MAP_AVAILABLE_MESSAGE = Component.text("利用可能なマップがありません").color(NamedTextColor.YELLOW);

    /**
     * 試合モード
     */
    @NotNull
    private final MatchMode matchMode;

    /**
     * 試合ID
     */
    @NotNull
    private final String matchId;

    /**
     * コンストラクタ
     *
     * @param matchMode 試合モード
     * @param matchId   試合ID
     */
    public MatchModeSelectItem(@NotNull MatchMode matchMode, @NotNull String matchId) {
        this.matchMode = matchMode;
        this.matchId = matchId;
    }

    @Override
    public ItemProvider getItemProvider() {
        ItemBuilder builder = new ItemBuilder(matchMode.iconItem());
        builder.setDisplayName(new AdventureComponentWrapper(Component.text(matchMode.name())));
        return builder;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        MatchManager matchManager = MatchManager.getInstance();

        MatchManager.getInstance().getMapLoader().getRandomMap(matchMode).ifPresentOrElse(
                matchMap -> addMatch(player, matchManager, matchMap),
                () -> player.sendMessage(NO_MAP_AVAILABLE_MESSAGE));

        getWindows().forEach(Window::close);
    }

    /**
     * MatchManagerに試合を追加する
     *
     * @param player メッセージ送信対象
     * @param matchManager マッチマネージャー
     * @param matchMap マップ
     */
    private void addMatch(@NotNull Player player, MatchManager matchManager, MatchMap matchMap) {
        boolean failure = matchManager.addMatch(matchId, matchMode, matchMap).map(match -> {
            if (match.join(player, false)) {
                player.sendMessage(CREATE_AND_JOIN_MATCH_MESSAGE);
                broadcastMatch(player, match);
                return false;
            } else {
                return true;
            }
        }).orElse(true);

        if (failure) {
            player.sendMessage(CREATE_MATCH_FAILURE_MESSAGE);
        }
    }

    /**
     * 誰かが試合を作成したとき、一連のメッセージを試合に参加していない人に告知する
     *
     * @param player 試合作成者
     * @param match 試合
     */
    private void broadcastMatch(@NotNull Player player, @NotNull Match match) {
        MatchManager matchManager = MatchManager.getInstance();
        Audience sendTarget = getEveryPlayerNotInMatch(player, matchManager);

        broadcastMatchCreated(player, match, sendTarget);
        broadcastJoinMatchButton(match, sendTarget);
    }

    /**
     * 誰かが試合を作成した情報を試合に参加していない人に告知する
     *
     * @param player 試合作成者
     * @param match 試合
     * @param sendTarget 試合に参加していない人
     */
    private static void broadcastMatchCreated(@NotNull Player player, @NotNull Match match, Audience sendTarget) {
        Component notifierMessage = getMatchCreatedMessage(player, match);
        sendTarget.sendMessage(notifierMessage);
    }

    /**
     * 試合作成の告知メッセージを取得
     *
     * @param player プレイヤー
     * @param match 試合
     * @return 告知メッセージ
     */
    @NotNull
    private static Component getMatchCreatedMessage(@NotNull Player player, @NotNull Match match) {
        return Component.text(player.getName())
                .append(Component.text("が試合("))
                .append(Component.text(match.getMatchMode().name()))
                .append(Component.text(")を作成しました"))
                .color(NamedTextColor.GREEN);
    }

    /**
     * 試合に参加するボタンを試合に参加していない人に告知する
     *
     * @param match 試合
     * @param sendTarget 試合に参加していない人
     */
    private static void broadcastJoinMatchButton(@NotNull Match match, Audience sendTarget) {
        Component joinHereMessage = getJoinHereMessage(match);
        sendTarget.sendMessage(joinHereMessage);
    }

    /**
     * 試合に参加するボタンを取得
     *
     * @param match 試合
     * @return テキスト
     */
    @NotNull
    private static Component getJoinHereMessage(@NotNull Match match) {
        Component clickHere = Component.text("[ここをクリック]")
                .style(Style.style().color(NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/room join " + match.getId())).build());

        return Component.text("参加するには")
                .append(clickHere)
                .append(Component.text("してください"))
                .color(NamedTextColor.LIGHT_PURPLE);
    }

    /**
     * 試合に参加していない全てのプレイヤーを取得
     *
     * @param player 試合作成者
     * @param matchManager マッチマネージャ
     * @return 全てのプレイヤー
     */
    @NotNull
    private static Audience getEveryPlayerNotInMatch(@NotNull Player player, MatchManager matchManager) {
        return Audience.audience(Bukkit.getOnlinePlayers().stream()
                .filter(pl -> pl != player)
                .filter(pl -> matchManager.getJoinedMatch(pl).isEmpty())
                .toList());
    }
}
