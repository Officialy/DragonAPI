package reika.dragonapi.instantiable.data.collections;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;

public class RelativePositionList {

    public static final RelativePositionList cornerDirections = new RelativePositionList();

    static {
        cornerDirections.addPosition(new BlockPos(-1, -1, -1));
        cornerDirections.addPosition(new BlockPos(-1, -1, 0));
        cornerDirections.addPosition(new BlockPos(-1, -1, 1));
        cornerDirections.addPosition(new BlockPos(-1, 0, -1));

        cornerDirections.addPosition(new BlockPos(-1, 0, 1));
        cornerDirections.addPosition(new BlockPos(-1, 1, -1));
        cornerDirections.addPosition(new BlockPos(-1, 1, 0));
        cornerDirections.addPosition(new BlockPos(-1, 1, 1));

        cornerDirections.addPosition(new BlockPos(0, -1, -1));

        cornerDirections.addPosition(new BlockPos(0, -1, 1));

        cornerDirections.addPosition(new BlockPos(0, 1, -1));

        cornerDirections.addPosition(new BlockPos(0, 1, 1));

        cornerDirections.addPosition(new BlockPos(1, -1, -1));
        cornerDirections.addPosition(new BlockPos(1, -1, 0));
        cornerDirections.addPosition(new BlockPos(1, -1, 1));
        cornerDirections.addPosition(new BlockPos(1, 0, -1));

        cornerDirections.addPosition(new BlockPos(1, 0, 1));
        cornerDirections.addPosition(new BlockPos(1, 1, -1));
        cornerDirections.addPosition(new BlockPos(1, 1, 0));
        cornerDirections.addPosition(new BlockPos(1, 1, 1));
    }

    private final BlockArray positions = new BlockArray();

    public RelativePositionList() {

    }

    public void addPosition(BlockPos pos) {
        positions.addBlockCoordinate(pos);
    }

    public void removePosition(BlockPos pos) {
        positions.remove(pos);
    }

    public boolean containsPosition(BlockPos pos) {
        return positions.hasBlock(pos);
    }

    public BlockArray getPositionsRelativeTo(BlockPos pos) {
        return positions.copy().offset(pos);
    }

    public int getSize() {
        return positions.getSize();
    }

    public BlockPos getNthPosition(BlockPos pos, int n) {
        BlockPos relative = this.getNthRelativePosition(n);
        return relative.offset(pos);
    }

    public BlockPos getNthRelativePosition(int n) {
        return positions.getNthBlock(n);
    }

    public Vec3 getVector(int n) {
        BlockPos d = this.getNthRelativePosition(n);
        return new Vec3(d.getX(), d.getY(), d.getZ());
    }
}
