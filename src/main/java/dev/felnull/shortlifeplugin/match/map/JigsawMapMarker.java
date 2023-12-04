package dev.felnull.shortlifeplugin.match.map;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Jigsaw;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * ジグソー用のマップマーカー実装
 *
 * @param name           名前
 * @param pointName      地点名
 * @param replaceBlockId 置き換え先のブロックID
 * @param position       地点
 * @param direction      方向
 * @author MORIMORI0317, Quarri6343
 */
public record JigsawMapMarker(@NotNull NamespacedKey name, @NotNull NamespacedKey pointName,
                              @NotNull NamespacedKey replaceBlockId, @NotNull BlockVector3 position,
                              @NotNull BlockFace direction) implements MapMarker {

    @NotNull
    @Override
    public NamespacedKey getName() {
        return name;
    }

    @NotNull
    @Override
    public NamespacedKey getPointName() {
        return pointName;
    }

    @NotNull
    @Override
    public BlockVector3 getPosition() {
        return position;
    }

    @NotNull
    @Override
    public BlockFace getDirection() {
        return direction;
    }

    /**
     * ジグソーのブロックベースからマーカを作成
     *
     * @param baseBlock ベースブロック
     * @return マップマーカー
     */
    public static Optional<JigsawMapMarker> of(@NotNull BlockVector3 position, @NotNull BaseBlock baseBlock, @NotNull BlockData blockData) {
        CompoundTag tag = baseBlock.getNbtData();

        if (tag == null) {
            return Optional.empty();
        }

        if (!(blockData instanceof Jigsaw jigsaw)) {
            return Optional.empty();
        }

        NamespacedKey name = Objects.requireNonNull(NamespacedKey.fromString(tag.getString("name")));
        NamespacedKey pointName = Objects.requireNonNull(NamespacedKey.fromString(tag.getString("pool")));
        NamespacedKey replaceBlockId = Objects.requireNonNull(NamespacedKey.fromString(tag.getString("final_state")));

        BlockFace blockFace = orientationToBlockFace(jigsaw.getOrientation());

        return Optional.of(new JigsawMapMarker(name, pointName, replaceBlockId, position, blockFace));
    }

    @NotNull
    private static BlockFace orientationToBlockFace(Jigsaw.Orientation orientation) {
        return switch (orientation) {
            case DOWN_EAST, DOWN_NORTH, DOWN_SOUTH, DOWN_WEST -> BlockFace.DOWN;
            case UP_EAST, UP_NORTH, UP_SOUTH, UP_WEST -> BlockFace.UP;
            case WEST_UP -> BlockFace.WEST;
            case EAST_UP -> BlockFace.EAST;
            case NORTH_UP -> BlockFace.NORTH;
            case SOUTH_UP -> BlockFace.SOUTH;
        };
    }
}
