package reika.dragonapi.libraries.io;

import java.io.DataInputStream;
import java.io.IOException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import reika.dragonapi.base.CoreContainer;

/** Validation shared by the compatibility protocol; never loads a chunk for a client command. */
public final class PacketValidation {
    public static final int MAX_PAYLOAD_BYTES = 1 << 20;
    private PacketValidation() {}

    public static int readIntCount(DataInputStream input) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > 4096 || count > input.available() / Integer.BYTES)
            throw new IOException("Invalid integer array length: " + count);
        return count;
    }

    public static boolean hasMenu(Player player, Level level, BlockPos pos, BlockEntity tile) {
        return !level.isClientSide() && player.level() == level && level.hasChunkAt(pos)
                && tile != null && !tile.isRemoved() && level.getBlockEntity(pos) == tile
                && player.containerMenu instanceof CoreContainer<?> menu
                && menu.tile == tile && menu.stillValid(player)
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }

    /** Dropper metadata is readable by players already receiving this live item entity. */
    public static boolean canRequestItemDropper(Player player, Level level,
            net.minecraft.world.entity.item.ItemEntity item) {
        return player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && player.level() == level && item != null && item.level() == level
                && item.isAlive() && !item.isRemoved() && !item.getItem().isEmpty()
                && level.getEntity(item.getId()) == item && level.hasChunkAt(item.blockPosition())
                && player.distanceToSqr(item) <= 128 * 128
                && serverLevel.getChunkSource().chunkMap.getPlayersWatching(item).contains(serverPlayer);
    }

    public static boolean canRequestSync(Player player, Level level, BlockPos pos) {
        return !level.isClientSide() && player.level() == level && level.hasChunkAt(pos)
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 128 * 128;
    }
}
