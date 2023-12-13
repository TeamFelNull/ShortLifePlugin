package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.MsgHandler;

/**
 * 試合の種類
 *
 * @author MORIMORI0317
 */
public enum MatchType {
    /**
     * Player VS Player
     */
    PVP(MsgHandler.get("match-type-pvp")),

    /**
     * Player VS Environment
     */
    PVE(MsgHandler.get("match-type-pve"));

    /**
     * 名前
     */
    private final String name;

    MatchType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
