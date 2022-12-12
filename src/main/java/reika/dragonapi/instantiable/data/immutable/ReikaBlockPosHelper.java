package reika.dragonapi.instantiable.data.immutable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collection;

public class ReikaBlockPosHelper {

    public static BlockPos offset(BlockPos c, int dx, int dy, int dz) {
        return new BlockPos(c.getX()+dx, c.getY()+dy, c.getZ()+dz);
    }

    public static BlockPos offset(BlockPos c, Direction dir, int dist) {
        return offset(c, dir.getStepX()*dist, dir.getStepY()*dist, dir.getStepZ()*dist);
    }

    public static BlockPos offset(BlockPos c1, BlockPos c) {
        return offset(c1, c.getX(), c.getY(), c.getZ());
    }

    public static Collection<BlockPos> getAdjacentCoordinates(BlockPos c) {
        ArrayList<BlockPos> li = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            li.add(offset(c, Direction.values()[i], 1));
        }
        return li;
    }

}
