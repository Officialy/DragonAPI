package reika.dragonapi.instantiable.data.blockstruct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collection;

public class CoordHelper {
    public static Collection<BlockPos> getAdjacentCoordinates(BlockPos pos) {
        ArrayList<BlockPos> li = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
			li.add(pos.relative(Direction.values()[i]));
        }
        return li;
    }

    public static BlockPos offset(Direction dir, int dist) {
        return new BlockPos(dir.getStepX() * dist, dir.getStepY() * dist, dir.getStepZ() * dist);
    }

}
