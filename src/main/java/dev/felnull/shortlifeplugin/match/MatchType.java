package dev.felnull.shortlifeplugin.match;

/**
 * 試合の種類
 *
 * @author MORIMORI0317
 */
public enum MatchType {
    /**
     * Player VS Player
     */
    PVP("pvp"),

    /**
     * Player VS Environment
     */
    PVE("pve");

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
