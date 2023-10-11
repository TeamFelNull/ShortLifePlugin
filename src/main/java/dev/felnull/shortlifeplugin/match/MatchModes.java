package dev.felnull.shortlifeplugin.match;

import com.google.common.collect.ImmutableMap;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 試合モードのレジストリ
 *
 * @author MORIMORI0317
 */
public final class MatchModes {

    /**
     * テスト用試合モード
     */
    public static final MatchMode TEST = new MatchMode("test", Component.text("テスト"), MatchType.PVP, 1000 * 60 * 3, 1, 1, TestMatch::new);

    /**
     * チームポイント制試合モード
     */
    public static final MatchMode TEAM = new MatchMode("team", Component.text("チーム"), MatchType.PVP, 1000 * 60 * 10, 2, 2, TeamMatch::new);

    /**
     * 試合モードのレジストリマップ
     */
    private static final Map<String, MatchMode> MODE_REGISTRY = new HashMap<>();

    private MatchModes() {
    }

    /**
     * 初期処理
     */
    public static void init() {
        register(TEST);
        register(TEAM);
    }

    public static Map<String, MatchMode> getAllModes() {
        return ImmutableMap.copyOf(MODE_REGISTRY);
    }

    private static void register(@NotNull MatchMode matchMode) {
        MODE_REGISTRY.put(matchMode.id(), matchMode);
    }
}
