package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.MsgHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * 試合の長大なメッセージコンポーネントの格納所
 *
 * @author MORIMORI0317, Quarri6343
 */
public enum MatchMessageComponents {
    MATCH_JOIN_MESSAGE(Component.text(MsgHandler.get("match-joined")).color(NamedTextColor.WHITE)),

    MATCH_LEAVE_MESSAGE(Component.text(MsgHandler.get("match-left")).color(NamedTextColor.WHITE)),
    
    MATCH_START_MESSAGE(Component.text(MsgHandler.get("match-start")).color(NamedTextColor.GREEN)),
    
    MATCH_FINISH_MESSAGE(Component.text(MsgHandler.get("match-end")).color(NamedTextColor.GREEN)),
    
    MATCH_WAIT_LOAD_WORLD_MESSAGE(Component.text(MsgHandler.get("match-wait-world-load")).color(NamedTextColor.BLUE)),
    
    MATCH_CANCEL_FAILED_LOAD_WORLD_MESSAGE(Component.text(MsgHandler.get("match-cancel-failed-load-world")).color(NamedTextColor.DARK_RED)),
    
    MATCH_CANCEL_UNEXPECTED_ERROR_MESSAGE(Component.text(MsgHandler.get("match-cancel-unexpected-error-1")).color(NamedTextColor.DARK_RED).appendNewline()
            .append(Component.text(MsgHandler.get("match-cancel-unexpected-error-2")).color(NamedTextColor.GOLD))),
    
    MATCH_FINISH_INSUFFICIENT_PLAYER_MESSAGE(Component.text(MsgHandler.get("match-finish-insufficient-player")).color(NamedTextColor.RED)),

    MATCH_FINISH_TEAM_NO_PARTICIPANTS_MESSAGE(Component.text(MsgHandler.get("match-finish-team-no-participants")).color(NamedTextColor.RED));

    /**
     * コンポーネント本体
     */
    private final Component text;

    MatchMessageComponents(Component text) {
        this.text = text;
    }

    /**
     * コンポーネント取得
     *
     * @return コンポーネント本体
     */
    public Component get() {
        return text;
    }
}
